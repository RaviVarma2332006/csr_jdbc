import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * Reads the "Hadapsar Cluster - MASTER" sheet of the CSR prospect Excel file
 * and inserts every company row into the csr_companies MySQL table via
 * CSRCompanyDAO.addCompany().
 *
 * Expected column layout (header row = row 4, data starts row 5):
 *   1  Sr. No.
 *   2  Company Name
 *   3  Region(s)
 *   4  Industry
 *   5  Presence / Location(s)
 *   6  Priority
 *   7  Likely CSR Focus
 *   8  Potential Support for Akshar Bharati
 *   9  CSR Contact
 *   10 CSR Email
 *   11 CSR Phone
 *   12 Website
 *   13 Outreach Status
 *   14 Assigned To
 *   15 Last Contact Date      (usually blank on first import)
 *   16 Next Follow-up Date    (usually blank on first import)
 *   17 Meeting Notes / Call Log (usually blank on first import)
 *   18 No. of Employees (India)
 *   19 Also in Master DB?     ("Yes" / "No")
 */
public class ExcelImporter {

    private static final int HEADER_ROW_INDEX = 3;   // 0-based -> Excel row 4
    private static final int FIRST_DATA_ROW_INDEX = 4; // 0-based -> Excel row 5
    private static final String SHEET_NAME = "Hadapsar Cluster - MASTER";

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java ExcelImporter <path-to-xlsx>");
            System.err.println("Example: java ExcelImporter Akshar_Bharati_CSR_Prospect_List.xlsx");
            return;
        }
        String filePath = args[0];

        CSRCompanyDAO dao = new CSRCompanyDAO();
        int inserted = 0;
        int skipped = 0;

        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheet(SHEET_NAME);
            if (sheet == null) {
                System.err.println("Sheet '" + SHEET_NAME + "' not found in " + filePath);
                System.err.println("Available sheets:");
                for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                    System.err.println("  - " + workbook.getSheetName(i));
                }
                return;
            }

            DataFormatter formatter = new DataFormatter(); // safely converts any cell type to String

            for (int rowIndex = FIRST_DATA_ROW_INDEX; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) continue;

                String companyName = getCell(row, 1, formatter);
                if (companyName == null || companyName.trim().isEmpty()) {
                    continue; // skip blank rows
                }

                CSRCompany c = new CSRCompany();
                c.setCompanyName(companyName.trim());
                c.setRegion(getCell(row, 2, formatter));
                c.setIndustry(getCell(row, 3, formatter));
                c.setPresenceLocation(getCell(row, 4, formatter));
                c.setPriority(normalizePriority(getCell(row, 5, formatter)));
                c.setCsrFocus(getCell(row, 6, formatter));
                c.setPotentialSupport(getCell(row, 7, formatter));
                c.setCsrContact(getCell(row, 8, formatter));
                c.setCsrEmail(getCell(row, 9, formatter));
                c.setCsrPhone(getCell(row, 10, formatter));
                c.setWebsite(getCell(row, 11, formatter));
                c.setOutreachStatus(normalizeStatus(getCell(row, 12, formatter)));
                c.setAssignedTo(getCell(row, 13, formatter));
                // columns 14 (Last Contact) and 15 (Next Follow-up) are usually
                // blank on first import - addCompany() doesn't set dates anyway,
                // use updateCompany() later if you need to backfill them.
                c.setEmployeesIndia(getCell(row, 17, formatter));

                String alsoInMaster = getCell(row, 18, formatter);
                c.setAlsoInMasterDb(alsoInMaster != null && alsoInMaster.trim().equalsIgnoreCase("Yes"));

                int newId = dao.addCompany(c);
                if (newId != -1) {
                    inserted++;
                    System.out.println("Imported [" + newId + "] " + c.getCompanyName());
                } else {
                    skipped++;
                    System.err.println("Failed to import row " + (rowIndex + 1) + ": " + companyName);
                }
            }

        } catch (IOException e) {
            System.err.println("Could not read Excel file: " + e.getMessage());
            return;
        }

        System.out.println("\nImport complete. Inserted: " + inserted + " | Failed: " + skipped);
    }

    /** Reads a cell by 0-based column index and returns its display text, or null if empty. */
    private static String getCell(Row row, int colIndex, DataFormatter formatter) {
        Cell cell = row.getCell(colIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;
        String value = formatter.formatCellValue(cell).trim();
        return value.isEmpty() ? null : value;
    }

    /** Maps free-text priority values onto the ENUM values used in the database. */
    private static String normalizePriority(String raw) {
        if (raw == null) return "Medium";
        String p = raw.trim();
        switch (p) {
            case "High":
            case "Medium-High":
            case "Medium":
            case "Low-Medium":
            case "Low":
                return p;
            default:
                return "Medium"; // safe fallback for anything unexpected
        }
    }

    /** Maps free-text outreach status onto the ENUM values used in the database. */
    private static String normalizeStatus(String raw) {
        if (raw == null || raw.trim().isEmpty()) return "Not Contacted";
        String s = raw.trim();
        switch (s) {
            case "Not Contacted":
            case "Researching":
            case "Email Sent":
            case "Call Made":
            case "Meeting Scheduled":
            case "Proposal Submitted":
            case "In Discussion":
            case "Funded/Partnered":
            case "Declined":
            case "On Hold":
                return s;
            default:
                return "Not Contacted";
        }
    }
}
