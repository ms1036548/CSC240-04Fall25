
// src/CountriesLoader.java
import java.net.http.*;
import java.net.URI;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import org.json.*;

public class CountriesLoader {
    private static final String DB_URL = "jdbc:sqlite:data/app.db";
    private static final String COUNTRIES_URL = "https://restcountries.com/v3.1/all?fields=name,cca2,region,capital,population";
    private static final Path SUMMARY_FILE = Paths.get("summaries/countries_summary.txt");

    public static void main(String[] args) throws Exception {
        Files.createDirectories(SUMMARY_FILE.getParent());
        Files.createDirectories(Paths.get("data"));

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder(URI.create(COUNTRIES_URL))
                .header("User-Agent", "CSC240/1.0")
                .header("Accept", "application/json")
                .GET().build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200)
            throw new RuntimeException("REST Countries API failed: " + resp.statusCode());

        JSONArray arr = new JSONArray(resp.body());

        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            conn.setAutoCommit(false);
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("DELETE FROM countries");
            }
            String sql = "INSERT INTO countries(name, code2, region, capital, population) VALUES (?,?,?,?,?)";
            int count = 0;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    String name = o.getJSONObject("name").getString("common");
                    String code2 = o.optString("cca2", null);
                    String region = o.optString("region", null);
                    String capital = null;
                    if (o.has("capital")) {
                        JSONArray caps = o.optJSONArray("capital");
                        if (caps != null && caps.length() > 0)
                            capital = caps.getString(0);
                    }
                    Long population = o.has("population") ? o.getLong("population") : null;

                    ps.setString(1, name);
                    ps.setString(2, code2);
                    ps.setString(3, region);
                    ps.setString(4, capital);
                    if (population == null)
                        ps.setNull(5, Types.INTEGER);
                    else
                        ps.setLong(5, population);
                    ps.addBatch();
                    count++;
                }
                ps.executeBatch();
                conn.commit();

                String ls = System.lineSeparator();
                String timestamp = ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                String summary = "timestamp=" + timestamp + ls + "records=" + count + ls;
                Files.writeString(SUMMARY_FILE, summary,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                System.out.println("Summary written to: " + SUMMARY_FILE.toAbsolutePath());
                System.out.println("Countries loaded: " + count);
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            }
        }
    }
}