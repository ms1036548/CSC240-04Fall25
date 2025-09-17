
// src/CountriesVerifier.java
import java.nio.file.*;
import java.sql.*;
import java.util.regex.*;

public class CountriesVerifier {
    private static final String DB_URL = "jdbc:sqlite:data/app.db";
    private static final Path SUMMARY_FILE = Paths.get("summaries/countries_summary.txt");

    public static void main(String[] args) throws Exception {
        System.out.println("Reading summary from: " + SUMMARY_FILE.toAbsolutePath());
        if (!Files.exists(SUMMARY_FILE)) {
            throw new RuntimeException("countries_summary.txt not found – run CountriesLoader first");
        }
        int expected = readExpectedCount(SUMMARY_FILE);
        int actual = getTableCount();

        if (expected == actual) {
            System.out.println("OK , summary records (" + expected + ") match table count");
        } else {
            System.out.println("MISMATCH , summary=" + expected + ", table=" + actual);
        }
    }

    private static int readExpectedCount(Path p) throws Exception {
        String all = Files.readString(p);
        // accept: "records=123", "records = 123", anywhere, any case
        Matcher m = Pattern.compile("\\brecords\\s*=\\s*(\\d+)", Pattern.CASE_INSENSITIVE).matcher(all);
        if (m.find())
            return Integer.parseInt(m.group(1));
        // ultra-fallback: last integer in file
        Matcher any = Pattern.compile("(\\d+)").matcher(all);
        int last = -1;
        while (any.find())
            last = Integer.parseInt(any.group(1));
        if (last >= 0)
            return last;
        throw new RuntimeException("records= line not found in summary");
    }

    private static int getTableCount() throws Exception {
        try (Connection conn = DriverManager.getConnection(DB_URL);
                Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM countries")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }
}