package site;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Phase 3 Site Generator
 *
 * - Calls UI/Data APIs THROUGH APISIX (localhost:9080)
 * - Fetches JSON
 * - Generates static HTML pages in target/site
 */
public class SiteGenerator {

    // All traffic goes through APISIX
    private static final String API_BASE = "http://localhost:9080";

    // Endpoints exposed by your Phase 2 + APISIX setup
    private static final String COUNTRIES_URL  = API_BASE + "/data/countries";
    private static final String DASHBOARD_URL  = API_BASE + "/ui/dashboard";

    // Where to write the generated site
    private static final Path OUTPUT_DIR = Paths.get("target", "site");

    public static void main(String[] args) throws Exception {
        System.out.println("[SiteGenerator] Starting…");
        Files.createDirectories(OUTPUT_DIR);

        // 1) Fetch JSON from your APIs (through APISIX)
        System.out.println("[SiteGenerator] Fetching data from APIs via " + API_BASE);
        String countriesJson  = fetchJson(COUNTRIES_URL);
        String dashboardJson  = fetchJson(DASHBOARD_URL);

        // 2) Generate pages
        System.out.println("[SiteGenerator] Generating HTML pages…");
        generateIndexPage(dashboardJson);
        generateCountriesPage(countriesJson);

        System.out.println("[SiteGenerator] Done!");
        System.out.println("Open target/site/index.html in a browser,");
        System.out.println("or point Apache httpd DocumentRoot to: " + OUTPUT_DIR.toAbsolutePath());
    }

    /**
     * Simple HTTP GET helper using java.net.http.HttpClient.
     */
    private static String fetchJson(String url) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException(
                    "HTTP " + response.statusCode() + " from " + url +
                    "\nBody: " + response.body()
            );
        }

        return response.body();
    }

    /**
     * index.html:
     *  - Uses /ui/dashboard JSON
     *  - Shows overall metrics & region breakdown
     *  - Links to countries.html
     */
    private static void generateIndexPage(String dashboardJson) throws Exception {
        JSONObject root = new JSONObject(dashboardJson);

        String title = root.optString("title", "Global Education Dashboard");

        JSONObject metrics = root.optJSONObject("metrics");
        int totalCountries    = metrics != null ? metrics.optInt("countriesTotal", 0)    : 0;
        int totalUniversities = metrics != null ? metrics.optInt("universitiesTotal", 0) : 0;

        JSONObject breakdowns = root.optJSONObject("breakdowns");
        JSONObject byRegion   = breakdowns != null ? breakdowns.optJSONObject("countriesByRegion") : null;

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang='en'>\n<head>\n");
        html.append("  <meta charset='UTF-8'>\n");
        html.append("  <title>").append(escapeHtml(title)).append("</title>\n");
        html.append("  <style>\n");
        html.append("    body { font-family: Arial, sans-serif; margin: 2rem; }\n");
        html.append("    h1 { color: #333; }\n");
        html.append("    .metrics { margin-bottom: 1.5rem; }\n");
        html.append("    .card { border: 1px solid #ddd; padding: 1rem; border-radius: 8px; max-width: 500px; }\n");
        html.append("    ul { list-style: none; padding-left: 0; }\n");
        html.append("    li { margin: 0.25rem 0; }\n");
        html.append("    a { color: #0066cc; text-decoration: none; }\n");
        html.append("    a:hover { text-decoration: underline; }\n");
        html.append("  </style>\n");
        html.append("</head>\n<body>\n");

        html.append("<h1>").append(escapeHtml(title)).append("</h1>\n");

        // Metrics card
        html.append("<div class='metrics card'>\n");
        html.append("  <h2>Overview</h2>\n");
        html.append("  <p><strong>Total countries:</strong> ").append(totalCountries).append("</p>\n");
        html.append("  <p><strong>Total universities:</strong> ").append(totalUniversities).append("</p>\n");
        html.append("</div>\n");

        // Region breakdown
        if (byRegion != null) {
            html.append("<div class='card'>\n");
            html.append("  <h2>Countries by Region</h2>\n");
            html.append("  <ul>\n");
            for (String key : byRegion.keySet()) {
                int count = byRegion.optInt(key, 0);
                html.append("    <li>")
                    .append(escapeHtml(key))
                    .append(": ")
                    .append(count)
                    .append("</li>\n");
            }
            html.append("  </ul>\n");
            html.append("</div>\n");
        }

        html.append("<p style='margin-top:2rem;'>");
        html.append("<a href='countries.html'>View full countries list &raquo;</a>");
        html.append("</p>\n");

        html.append("</body>\n</html>");

        Files.writeString(OUTPUT_DIR.resolve("index.html"), html.toString());
    }

    /**
     * countries.html:
     *  - Table of all countries from /data/countries
     */
    private static void generateCountriesPage(String countriesJson) throws Exception {
        JSONArray arr = new JSONArray(countriesJson);

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang='en'>\n<head>\n");
        html.append("  <meta charset='UTF-8'>\n");
        html.append("  <title>Countries</title>\n");
        html.append("  <style>\n");
        html.append("    body { font-family: Arial, sans-serif; margin: 2rem; }\n");
        html.append("    table { border-collapse: collapse; width: 100%; }\n");
        html.append("    th, td { border: 1px solid #ddd; padding: 0.5rem; }\n");
        html.append("    th { background: #f0f0f0; text-align: left; }\n");
        html.append("    tr:nth-child(even) { background: #fafafa; }\n");
        html.append("    a { color: #0066cc; text-decoration: none; }\n");
        html.append("    a:hover { text-decoration: underline; }\n");
        html.append("  </style>\n");
        html.append("</head>\n<body>\n");

        html.append("<h1>Countries</h1>\n");
        html.append("<p><a href='index.html'>&laquo; Back to dashboard</a></p>\n");

        html.append("<table>\n");
        html.append("  <tr>\n");
        html.append("    <th>Name</th>\n");
        html.append("    <th>Region</th>\n");
        html.append("    <th>Capital</th>\n");
        html.append("    <th>Population</th>\n");
        html.append("  </tr>\n");

        for (int i = 0; i < arr.length(); i++) {
            JSONObject c = arr.getJSONObject(i);
            String name       = c.optString("name", "");
            String region     = c.optString("region", "");
            String capital    = c.optString("capital", "");
            long population   = c.optLong("population", 0);

            html.append("  <tr>\n");
            html.append("    <td>").append(escapeHtml(name)).append("</td>\n");
            html.append("    <td>").append(escapeHtml(region)).append("</td>\n");
            html.append("    <td>").append(escapeHtml(capital)).append("</td>\n");
            html.append("    <td>").append(population).append("</td>\n");
            html.append("  </tr>\n");
        }

        html.append("</table>\n");
        html.append("</body>\n</html>");

        Files.writeString(OUTPUT_DIR.resolve("countries.html"), html.toString());
    }

    /**
     * Tiny helper to avoid breaking HTML when printing strings.
     */
    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
