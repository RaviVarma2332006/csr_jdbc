import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

/**
 * Demonstrates all four CRUD operations against the csr_companies table.
 * Run this after: 1) creating the DB with schema.sql, 2) setting your
 * credentials in DBConnection.java, 3) adding mysql-connector-j to classpath.
 */
public class Main {
    public static void main(String[] args) {
        CSRCompanyDAO dao = new CSRCompanyDAO();

        // ---------- CREATE ----------
        CSRCompany newCompany = new CSRCompany(
                "Whirlpool of India",
                "Ramtekdi-Chakan",
                "Home Appliances Manufacturing",
                "Plot No. A-4, MIDC Ranjangaon, Pune",
                "High",
                "Skill Development, community development near factories",
                "NSDC-aligned skill program, explicit factory-community commitment",
                "Whirlpool of India CSR Team",
                "helpdeskindia@whirlpool.in",
                "1800 208 1800",
                "corporate.whirlpool.in",
                "Not Contacted",
                "Volunteer 3 (Chakan Lead)",
                "~2,000",
                false
        );
        int newId = dao.addCompany(newCompany);
        System.out.println("Inserted new company with id = " + newId);

        // ---------- READ (all) ----------
        System.out.println("\n--- All companies (sorted by priority) ---");
        List<CSRCompany> all = dao.getAllCompanies();
        for (CSRCompany c : all) {
            System.out.println(c);
        }

        // ---------- READ (filtered) ----------
        System.out.println("\n--- High priority companies in Hadapsar ---");
        List<CSRCompany> filtered = dao.searchCompanies("Hadapsar", "High", null, null);
        for (CSRCompany c : filtered) {
            System.out.println(c);
        }

        // ---------- UPDATE (outreach progress after a call) ----------
        if (newId != -1) {
            boolean updated = dao.updateOutreachProgress(
                    newId,
                    "Call Made",
                    "Volunteer 3 (Chakan Lead)",
                    Date.valueOf(LocalDate.now()),
                    Date.valueOf(LocalDate.now().plusDays(14)),
                    "Spoke with CSR desk; asked to send a formal proposal by email."
            );
            System.out.println("\nOutreach progress updated: " + updated);

            CSRCompany refreshed = dao.getCompanyById(newId);
            System.out.println("After update: " + refreshed);
        }

        // ---------- DELETE ----------
        // Uncomment to test deletion:
        // boolean deleted = dao.deleteCompany(newId);
        // System.out.println("\nDeleted test row: " + deleted);
    }
}
