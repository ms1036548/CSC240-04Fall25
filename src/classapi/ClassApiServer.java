package classapi;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ClassApiServer {

    private static final int PORT = 7002;
    // This must point to your Data API (7001)
    private static final String DATA_BASE = "http://localhost:7001";

    private static final HttpClient HTTP = HttpClient.newHttpClient();

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        server.createContext("/api/health", ex -> {
            JSONObject out = new JSONObject()
                    .put("dataApi", DATA_BASE)
                    .put("status", "ok");
            sendJson(ex, 200, out);
        });

        // Derived stats pulled from Data API
        server.createContext("/stats/overview", ex -> {
            try {
                String countriesStr = get(DATA_BASE + "/countries");
                String unisStr = get(DATA_BASE + "/universities");

                JSONArray countries = new JSONArray(countriesStr);
                JSONArray universities = new JSONArray(unisStr);

                // count by region
                Map<String, Integer> byRegion = new HashMap<>();
                for (int i = 0; i < countries.length(); i++) {
                    JSONObject c = countries.getJSONObject(i);
                    String region = c.optString("region", "Unknown");
                    byRegion.put(region, byRegion.getOrDefault(region, 0) + 1);
                }

                JSONObject out = new JSONObject()
                        .put("countryCount", countries.length())
                        .put("universityCount", universities.length())
                        .put("countriesByRegion", new JSONObject(byRegion));

                sendJson(ex, 200, out);
            } catch (Exception e) {
                sendError(ex, 500, "stats-failed", e);
            }
        });

        // Join: country + its universities (by country name)
        // GET /country-with-universities/{id}
        server.createContext("/country-with-universities", ex -> {
            try {
                String[] parts = ex.getRequestURI().getPath().split("/");
                if (parts.length < 3) {
                    sendError(ex, 400, "missing-id", null);
                    return;
                }
                String idStr = parts[parts.length - 1];
                int id = Integer.parseInt(idStr);

                String countryStr = get(DATA_BASE + "/countries/" + id);
                JSONObject country = new JSONObject(countryStr);
                String name = country.getString("name");

                String q = URLEncoder.encode(name, StandardCharsets.UTF_8);
                String unisStr = get(DATA_BASE + "/universities?country=" + q);
                JSONArray unis = new JSONArray(unisStr);

                JSONObject out = new JSONObject()
                        .put("country", country)
                        .put("universities", unis);

                sendJson(ex, 200, out);
            } catch (NumberFormatException nfe) {
                sendError(ex, 400, "bad-id", nfe);
            } catch (Exception e) {
                sendError(ex, 500, "join-failed", e);
            }
        });

        server.start();
        System.out.println("Class API listening on http://localhost:" + PORT);
    }

    // ---- helpers ----

    private static String get(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() / 100 != 2) {
            throw new IOException("Upstream " + url + " -> " + resp.statusCode());
        }
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
