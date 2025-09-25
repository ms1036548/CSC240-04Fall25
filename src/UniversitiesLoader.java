import java.net.http.*;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import org.json.*;
import java.util.*;

public class UniversitiesLoader {
    private static final String DB_URL = "jdbc:sqlite:data/app.db";
    private static final String UNI_URL = "http://universities.hipolabs.com/search?country=";
    private static final Path SUMMARY_FILE = Paths.get("summaries/universities_summary.txt");

    // Limit how many countries during testing (set to -1 for all)
    private static final int COUNTRY_LIMIT = 30;

    public static void main(String[] args) throws Exception {
        Files.createDirectories(SUMMARY_FILE.getParent());
        Files.createDirectories(Paths.get("data"));

        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            conn.setAutoCommit(false);

            // 1) Clear table
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("DELETE FROM universities");
            }

            // 2) Read country names from DB
            List<String> countries = new ArrayList<>();
            try (Statement st = conn.createStatement();
                    ResultSet rs = st.executeQuery("SELECT name FROM countries ORDER BY name")) {
                while (rs.next())
                    countries.add(rs.getString(1));
            }
            if (countries.isEmpty())
                throw new RuntimeException("Load countries first (run CountriesLoader).");
            // 3) Call API , parse JSON and insert rows for each country
            HttpClient client = HttpClient.newHttpClient();
            String sql = "INSERT INTO universities(name, country, alpha_two_code, domain, web_page) VALUES (?,?,?,?,?)";
            int total = 0;

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                int processed = 0;
                for (String country : countries) {
                    if (COUNTRY_LIMIT > 0 && processed >= COUNTRY_LIMIT)
                        break;
                    processed++;

                    String url = UNI_URL + URLEncoder.encode(country, StandardCharsets.UTF_8);
                    HttpRequest req = HttpRequest.newBuilder(URI.create(url)).GET().build();
                    HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                    if (resp.statusCode() != 200)
                        continue;
                    
                    //Parse the array for said country
                    JSONArray arr = new JSONArray(resp.body());
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject o = arr.getJSONObject(i);
                        String name = o.getString("name");
                        String ctry = o.getString("country");
                        String alpha2 = o.optString("alpha_two_code", null);

                        String domain = null;
                        JSONArray domains = o.optJSONArray("domains");
                        if (domains != null && domains.length() > 0)
                            domain = domains.getString(0);

                        String web = null;
                        JSONArray webs = o.optJSONArray("web_pages");
                        if (webs != null && webs.length() > 0)
                            web = webs.getString(0);

                        ps.setString(1, name);
                        ps.setString(2, ctry);
                        ps.setString(3, alpha2);
                        ps.setString(4, domain);
                        ps.setString(5, web);
                        ps.addBatch();
                        total++;
                    }
                    ps.executeBatch();
                }
                conn.commit();  //Commit the rows 

                //Summary file
                String timestamp = ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                String summary = "timestamp=" + timestamp + "\nrecords=" + total + "\n";
                Files.writeString(SUMMARY_FILE, summary);
                System.out.println("Universities loaded: " + total);
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            }
        }
    }
}
