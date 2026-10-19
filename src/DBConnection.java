import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Handles the JDBC connection to the MySQL database.
 * Update the constants below to match your MySQL setup.
 */
public class DBConnection {

    private static final String URL =
        "jdbc:mysql://localhost:3306/akshar_bharati_csr?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String USERNAME = "root";       // <-- change this
    private static final String PASSWORD = "Asdflkj@2222"; // <-- change this

    public static Connection getConnection() throws SQLException {
        try {
            // Not strictly required with modern JDBC (auto-loaded via SPI),
            // but explicit loading avoids surprises on older setups.
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found. Add mysql-connector-j to your classpath.", e);
        }
        return DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }

    /** Quick standalone connectivity test: run this class directly. */
    public static void main(String[] args) {
        try (Connection conn = getConnection()) {
            System.out.println("Connected successfully to: " + conn.getCatalog());
        } catch (SQLException e) {
            System.err.println("Connection failed: " + e.getMessage());
        }
    }
}
