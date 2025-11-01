package dataapi;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

public class DataApiServer {

    private static final String DB_URL = "jdbc:sqlite:data/app.db";

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(7001), 0);
        server.createContext("/countries", DataApiServer::handleCountries); // GET /countries, /countries/{id}
        server.createContext("/universities", DataApiServer::handleUniversities); // GET /universities,
                                                                                  // /universities/{id}, ?country=Name
        server.setExecutor(null);
        System.out.println("Data API listening on http://localhost:7001");
        server.start();
    }

    // ----------- Countries -----------
    private static void handleCountries(HttpExchange ex) throws IOException {
        try {
            if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
                send(ex, 405, "Only GET");
                return;
            }
            String path = ex.getRequestURI().getPath(); // /countries or /countries/123
            String[] parts = path.split("/");
            if (parts.length == 2) { // /countries
                JSONArray arr = new JSONArray();
                try (Connection c = DriverManager.getConnection(DB_URL);
                        Statement st = c.createStatement();
                        ResultSet rs = st.executeQuery(
                                "SELECT id,name,code2,region,capital,population FROM countries ORDER BY id")) {
                    while (rs.next()) {
                        JSONObject o = new JSONObject()
                                .put("id", rs.getInt("id"))
                                .put("name", rs.getString("name"))
                                .put("code2", opt(rs.getString("code2")))
                                .put("region", opt(rs.getString("region")))
                                .put("capital", opt(rs.getString("capital")))
                                .put("population", rs.getObject("population"));
                        arr.put(o);
                    }
                }
                sendJson(ex, 200, arr);
            } else if (parts.length == 3) { // /countries/{id}
                int id = Integer.parseInt(parts[2]);
                JSONObject o = null;
                try (Connection c = DriverManager.getConnection(DB_URL);
                        PreparedStatement ps = c.prepareStatement(
                                "SELECT id,name,code2,region,capital,population FROM countries WHERE id=?")) {
                    ps.setInt(1, id);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            o = new JSONObject()
                                    .put("id", rs.getInt("id"))
                                    .put("name", rs.getString("name"))
                                    .put("code2", opt(rs.getString("code2")))
                                    .put("region", opt(rs.getString("region")))
                                    .put("capital", opt(rs.getString("capital")))
                                    .put("population", rs.getObject("population"));
                        }
                    }
                }
                if (o == null)
                    send(ex, 404, "Not found");
                else
                    sendJson(ex, 200, o);
            } else {
                send(ex, 404, "Not found");
            }
        } catch (Exception e) {
            send(ex, 500, "Error: " + e.getMessage());
        }
    }

    // ----------- Universities -----------
    private static void handleUniversities(HttpExchange ex) throws IOException {
        try {
            if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
                send(ex, 405, "Only GET");
                return;
            }
            String path = ex.getRequestURI().getPath(); // /universities or /universities/123
            String[] parts = path.split("/");
            Map<String, String> q = parseQuery(ex.getRequestURI().getRawQuery());

            if (parts.length == 2) { // /universities (optionally ?country=Name)
                JSONArray arr = new JSONArray();
                String baseSql = "SELECT id,name,country,alpha_two_code,domain,web_page FROM universities";
                String order = " ORDER BY id";
                String sql;
                boolean filterCountry = q.containsKey("country");
                if (filterCountry) {
                    sql = baseSql + " WHERE country = ?" + order;
                } else {
                    sql = baseSql + order;
                }
                try (Connection c = DriverManager.getConnection(DB_URL);
                        PreparedStatement ps = c.prepareStatement(sql)) {
                    if (filterCountry)
                        ps.setString(1, q.get("country"));
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            JSONObject o = new JSONObject()
                                    .put("id", rs.getInt("id"))
                                    .put("name", rs.getString("name"))
                                    .put("country", rs.getString("country"))
                                    .put("alpha_two_code", opt(rs.getString("alpha_two_code")))
                                    .put("domain", opt(rs.getString("domain")))
                                    .put("web_page", opt(rs.getString("web_page")));
                            arr.put(o);
                        }
                    }
                }
                sendJson(ex, 200, arr);

            } else if (parts.length == 3) { // /universities/{id}
                int id = Integer.parseInt(parts[2]);
                JSONObject o = null;
                try (Connection c = DriverManager.getConnection(DB_URL);
                        PreparedStatement ps = c.prepareStatement(
                                "SELECT id,name,country,alpha_two_code,domain,web_page FROM universities WHERE id=?")) {
                    ps.setInt(1, id);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            o = new JSONObject()
                                    .put("id", rs.getInt("id"))
                                    .put("name", rs.getString("name"))
                                    .put("country", rs.getString("country"))
                                    .put("alpha_two_code", opt(rs.getString("alpha_two_code")))
                                    .put("domain", opt(rs.getString("domain")))
                                    .put("web_page", opt(rs.getString("web_page")));
                        }
                    }
                }
                if (o == null)
                    send(ex, 404, "Not found");
                else
                    sendJson(ex, 200, o);

            } else {
                send(ex, 404, "Not found");
            }
        } catch (Exception e) {
            send(ex, 500, "Error: " + e.getMessage());
        }
    }

    // ---------- helpers ----------
    private static Object opt(String s) {
        return (s == null) ? JSONObject.NULL : s;
    }

    private static void send(HttpExchange ex, int status, String msg) throws IOException {
        byte[] b = msg.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        ex.sendResponseHeaders(status, b.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(b);
        }
    }

    private static void sendJson(HttpExchange ex, int status, Object json) throws IOException {
        byte[] b = json.toString().getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        ex.sendResponseHeaders(status, b.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(b);
        }
    }

    private static Map<String, String> parseQuery(String raw) throws UnsupportedEncodingException {
        Map<String, String> m = new HashMap<>();
        if (raw == null || raw.isEmpty())
            return m;
        for (String pair : raw.split("&")) {
            int i = pair.indexOf('=');
            if (i > 0) {
                String k = URLDecoder.decode(pair.substring(0, i), "UTF-8");
                String v = URLDecoder.decode(pair.substring(i + 1), "UTF-8");
                m.put(k, v);
            } else {
                m.put(URLDecoder.decode(pair, "UTF-8"), "");
            }
        }
        return m;
    }
}
