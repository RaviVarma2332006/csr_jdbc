import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Prints the reports a volunteer team needs day to day.
 *
 * Run from the csr_jdbc folder:
 *   javac -cp "lib/*" -d out src/*.java
 *   java -cp "out;lib/*" Reports
 */
public class Reports {

    private static final String STATUS_ORDER =
        "FIELD(outreach_status,'Not Contacted','Researching','Email Sent','Call Made'," +
        "'Meeting Scheduled','Proposal Submitted','In Discussion','Funded/Partnered','Declined','On Hold')";

    private static final String PRIORITY_ORDER =
        "FIELD(priority,'High','Medium-High','Medium','Low-Medium','Low')";

    public static void main(String[] args) {
        try (Connection conn = DBConnection.getConnection()) {

            run(conn, "1. OUTREACH STATUS BREAKDOWN",
                "SELECT outreach_status AS status, COUNT(*) AS companies " +
                "FROM csr_companies GROUP BY outreach_status ORDER BY " + STATUS_ORDER);

            run(conn, "2. PRIORITY BREAKDOWN",
                "SELECT priority, COUNT(*) AS companies " +
                "FROM csr_companies GROUP BY priority ORDER BY " + PRIORITY_ORDER);

            run(conn, "3. REGION BREAKDOWN",
                "SELECT region, COUNT(*) AS companies, " +
                "SUM(priority = 'High') AS high_count " +
                "FROM csr_companies GROUP BY region ORDER BY companies DESC");

            run(conn, "4. EACH VOLUNTEER'S PIPELINE",
                "SELECT COALESCE(NULLIF(assigned_to, ''), '(Unassigned)') AS volunteer, " +
                "COUNT(*) AS companies, " +
                "SUM(outreach_status = 'Not Contacted') AS not_contacted, " +
                "SUM(outreach_status IN ('Email Sent','Call Made','Meeting Scheduled'," +
                "'Proposal Submitted','In Discussion')) AS in_progress, " +
                "SUM(outreach_status = 'Funded/Partnered') AS partnered " +
                "FROM csr_companies GROUP BY volunteer ORDER BY companies DESC");

            run(conn, "5. FOLLOW-UPS DUE (today or overdue)",
                "SELECT company_name, assigned_to, outreach_status, next_followup_date " +
                "FROM csr_companies " +
                "WHERE next_followup_date IS NOT NULL AND next_followup_date <= CURDATE() " +
                "AND outreach_status NOT IN ('Funded/Partnered','Declined') " +
                "ORDER BY next_followup_date");

            run(conn, "6. HIGH-PRIORITY COMPANIES NOBODY HAS CONTACTED YET",
                "SELECT company_name, assigned_to, region, csr_email " +
                "FROM csr_companies " +
                "WHERE priority = 'High' AND outreach_status = 'Not Contacted' " +
                "ORDER BY assigned_to, company_name");

            run(conn, "7. COMPANIES WITH NO VOLUNTEER ASSIGNED (High / Medium-High only)",
                "SELECT company_name, priority, region " +
                "FROM csr_companies " +
                "WHERE (assigned_to IS NULL OR assigned_to = '') " +
                "AND priority IN ('High','Medium-High') " +
                "ORDER BY " + PRIORITY_ORDER + ", company_name");

        } catch (SQLException e) {
            System.err.println("Could not run reports: " + e.getMessage());
        }
    }

    /** Runs one query and prints the result as an aligned text table. */
    private static void run(Connection conn, String title, String sql) {
        System.out.println();
        System.out.println("=== " + title + " ===");

        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            ResultSetMetaData md = rs.getMetaData();
            int cols = md.getColumnCount();

            String[] head = new String[cols];
            int[] width = new int[cols];
            for (int i = 0; i < cols; i++) {
                head[i] = md.getColumnLabel(i + 1);
                width[i] = head[i].length();
            }

            List<String[]> rows = new ArrayList<>();
            while (rs.next()) {
                String[] row = new String[cols];
                for (int i = 0; i < cols; i++) {
                    String v = rs.getString(i + 1);
                    if (v == null || v.trim().isEmpty()) v = "-";
                    if (v.length() > 55) v = v.substring(0, 52) + "...";
                    row[i] = v;
                    width[i] = Math.max(width[i], v.length());
                }
                rows.add(row);
            }

            if (rows.isEmpty()) {
                System.out.println("(no rows)");
                return;
            }

            printRow(head, width);
            StringBuilder line = new StringBuilder();
            for (int i = 0; i < cols; i++) {
                line.append("-".repeat(width[i]));
                if (i < cols - 1) line.append("-+-");
            }
            System.out.println(line);
            for (String[] row : rows) {
                printRow(row, width);
            }

        } catch (SQLException e) {
            System.err.println("Report failed: " + e.getMessage());
        }
    }

    private static void printRow(String[] cells, int[] width) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cells.length; i++) {
            sb.append(String.format("%-" + width[i] + "s", cells[i]));
            if (i < cells.length - 1) sb.append(" | ");
        }
        System.out.println(sb);
    }
}
