// Verifies that the number of rows in the universities table matches
import java.nio.file.*;
import java.sql.*;
import java.util.*;

public class UniversitiesVerifier {
    private static final String DB_URL = "jdbc:sqlite:data/app.db";
    private static final Path SUMMARY_FILE = Paths.get("summaries/universities_summary.txt");

    public static void main(String[] args) throws Exception {
        if (!Files.exists(SUMMARY_FILE))
            throw new RuntimeException("universities_summary.txt not found – run UniversitiesLoader first");
        int expected = readExpectedCount(SUMMARY_FILE);
        int actual = getTableCount();
        if (expected == actual) {
            System.out.println("OK , summary records (" + expected + ") match table count");
        } else {
            System.out.println("MISMATCH , summary=" + expected + ", table=" + actual);
        }
    }

    // Same parser as the countries verifier
    private static int readExpectedCount(Path p) throws Exception {
        List<String> lines = Files.readAllLines(p);
        for (String line : lines) {
            if (line.startsWith("records="))
                return Integer.parseInt(line.substring("records=".length()).trim());
        }
        throw new RuntimeException("records= line not found in summary");
    }
        
    // Executes SELECT COUNT(*) FROM universities
    private static int getTableCount() throws Exception {
        try (Connection conn = DriverManager.getConnection(DB_URL);
                Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM universities")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }
}
