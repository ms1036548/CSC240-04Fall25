package uiapi;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class UiApiServer {

    private static final int PORT = 7003;
    // This must point to your Class API (7002)
    private static final String CLASS_BASE = "http://localhost:7002";

    private static final HttpClient HTTP = HttpClient.newHttpClient();

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        server.createContext("/api/health", ex -> {
            JSONObject out = new JSONObject()
                    .put("classApi", CLASS_BASE)
                    .put("status", "ok");
            sendJson(ex, 200, out);
        });

        // Simple dashboard aggregating what Class API exposes
        server.createContext("/dashboard", ex -> {
            try {
                String stats = get(CLASS_BASE + "/stats/overview");
                JSONObject s = new JSONObject(stats);

                JSONObject out = new JSONObject()
                        .put("title", "Global Education Dashboard")
                        .put("metrics", new JSONObject()
                                .put("countriesTotal", s.optInt("countryCount"))
                                .put("universitiesTotal", s.optInt("universityCount")))
                        .put("breakdowns", new JSONObject()
                                .put("countriesByRegion", s.optJSONObject("countriesByRegion")));

                sendJson(ex, 200, out);
            } catch (Exception e) {
                sendError(ex, 500, "dashboard-failed", e);
            }
        });

        server.start();
        System.out.println("UI API listening on http://localhost:" + PORT);
    }

    // ---- helpers ----

    private static String get(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() / 100 != 2)
            throw new IOException("Upstream " + url + " -> " + resp.statusCode());
        return resp.body();
    }

    private static void sendJson(HttpExchange ex, int status, Object body) throws IOException {
        byte[] bytes = (body instanceof String ? ((String) body) : body.toString())
                .getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void sendError(HttpExchange ex, int status, String code, Exception e) throws IOException {
        JSONObject out = new JSONObject().put("error", code);
        if (e != null)
            out.put("detail", e.getMessage());
        sendJson(ex, status, out);
    }
}
