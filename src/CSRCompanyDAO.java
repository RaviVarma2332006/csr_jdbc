import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object: all CRUD operations for the csr_companies table
 * live here. Uses PreparedStatement everywhere to prevent SQL injection.
 */
public class CSRCompanyDAO {

    // ---------------------------------------------------------
    // CREATE
    // ---------------------------------------------------------
    public int addCompany(CSRCompany c) {
        String sql = "INSERT INTO csr_companies " +
                "(company_name, region, industry, presence_location, priority, " +
                " csr_focus, potential_support, csr_contact, csr_email, csr_phone, " +
                " website, outreach_status, assigned_to, employees_india, also_in_master_db) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, c.getCompanyName());
            ps.setString(2, c.getRegion());
            ps.setString(3, c.getIndustry());
            ps.setString(4, c.getPresenceLocation());
            ps.setString(5, c.getPriority());
            ps.setString(6, c.getCsrFocus());
            ps.setString(7, c.getPotentialSupport());
            ps.setString(8, c.getCsrContact());
            ps.setString(9, c.getCsrEmail());
            ps.setString(10, c.getCsrPhone());
            ps.setString(11, c.getWebsite());
            ps.setString(12, c.getOutreachStatus());
            ps.setString(13, c.getAssignedTo());
            ps.setString(14, c.getEmployeesIndia());
            ps.setBoolean(15, c.isAlsoInMasterDb());

            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) return keys.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("addCompany failed: " + e.getMessage());
        }
        return -1;
    }

    // ---------------------------------------------------------
    // READ - all companies
    // ---------------------------------------------------------
    public List<CSRCompany> getAllCompanies() {
        List<CSRCompany> list = new ArrayList<>();
        String sql = "SELECT * FROM csr_companies ORDER BY " +
                "FIELD(priority,'High','Medium-High','Medium','Low-Medium','Low'), company_name";

        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("getAllCompanies failed: " + e.getMessage());
        }
        return list;
    }

    // ---------------------------------------------------------
    // READ - by id
    // ---------------------------------------------------------
    public CSRCompany getCompanyById(int id) {
        String sql = "SELECT * FROM csr_companies WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            System.err.println("getCompanyById failed: " + e.getMessage());
        }
        return null;
    }

    // ---------------------------------------------------------
    // READ - filtered (region / priority / outreach status / assigned_to)
    // Pass null for any filter you don't want to apply.
    // ---------------------------------------------------------
    public List<CSRCompany> searchCompanies(String region, String priority,
                                             String outreachStatus, String assignedTo) {
        List<CSRCompany> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM csr_companies WHERE 1=1");
        List<String> params = new ArrayList<>();

        if (region != null)          { sql.append(" AND region = ?");           params.add(region); }
        if (priority != null)        { sql.append(" AND priority = ?");         params.add(priority); }
        if (outreachStatus != null)  { sql.append(" AND outreach_status = ?");  params.add(outreachStatus); }
        if (assignedTo != null)      { sql.append(" AND assigned_to = ?");      params.add(assignedTo); }
        sql.append(" ORDER BY FIELD(priority,'High','Medium-High','Medium','Low-Medium','Low')");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setString(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("searchCompanies failed: " + e.getMessage());
        }
        return list;
    }

    // ---------------------------------------------------------
    // UPDATE - full record update
    // ---------------------------------------------------------
    public boolean updateCompany(CSRCompany c) {
        String sql = "UPDATE csr_companies SET " +
                "company_name=?, region=?, industry=?, presence_location=?, priority=?, " +
                "csr_focus=?, potential_support=?, csr_contact=?, csr_email=?, csr_phone=?, " +
                "website=?, outreach_status=?, assigned_to=?, last_contact_date=?, " +
                "next_followup_date=?, meeting_notes=?, employees_india=?, also_in_master_db=? " +
                "WHERE id=?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, c.getCompanyName());
            ps.setString(2, c.getRegion());
            ps.setString(3, c.getIndustry());
            ps.setString(4, c.getPresenceLocation());
            ps.setString(5, c.getPriority());
            ps.setString(6, c.getCsrFocus());
            ps.setString(7, c.getPotentialSupport());
            ps.setString(8, c.getCsrContact());
            ps.setString(9, c.getCsrEmail());
            ps.setString(10, c.getCsrPhone());
            ps.setString(11, c.getWebsite());
            ps.setString(12, c.getOutreachStatus());
            ps.setString(13, c.getAssignedTo());
            ps.setDate(14, c.getLastContactDate());
            ps.setDate(15, c.getNextFollowupDate());
            ps.setString(16, c.getMeetingNotes());
            ps.setString(17, c.getEmployeesIndia());
            ps.setBoolean(18, c.isAlsoInMasterDb());
            ps.setInt(19, c.getId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("updateCompany failed: " + e.getMessage());
            return false;
        }
    }

    // ---------------------------------------------------------
    // UPDATE - quick helper just for outreach tracking
    // (the fields your volunteers will update most often)
    // ---------------------------------------------------------
    public boolean updateOutreachProgress(int id, String outreachStatus, String assignedTo,
                                           Date lastContactDate, Date nextFollowupDate,
                                           String meetingNotes) {
        String sql = "UPDATE csr_companies SET outreach_status=?, assigned_to=?, " +
                "last_contact_date=?, next_followup_date=?, meeting_notes=? WHERE id=?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, outreachStatus);
            ps.setString(2, assignedTo);
            ps.setDate(3, lastContactDate);
            ps.setDate(4, nextFollowupDate);
            ps.setString(5, meetingNotes);
            ps.setInt(6, id);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("updateOutreachProgress failed: " + e.getMessage());
            return false;
        }
    }

    // ---------------------------------------------------------
    // DELETE
    // ---------------------------------------------------------
    public boolean deleteCompany(int id) {
        String sql = "DELETE FROM csr_companies WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("deleteCompany failed: " + e.getMessage());
            return false;
        }
    }

    // ---------------------------------------------------------
    // Helper: map one ResultSet row -> CSRCompany object
    // ---------------------------------------------------------
    private CSRCompany mapRow(ResultSet rs) throws SQLException {
        CSRCompany c = new CSRCompany();
        c.setId(rs.getInt("id"));
        c.setCompanyName(rs.getString("company_name"));
        c.setRegion(rs.getString("region"));
        c.setIndustry(rs.getString("industry"));
        c.setPresenceLocation(rs.getString("presence_location"));
        c.setPriority(rs.getString("priority"));
        c.setCsrFocus(rs.getString("csr_focus"));
        c.setPotentialSupport(rs.getString("potential_support"));
        c.setCsrContact(rs.getString("csr_contact"));
        c.setCsrEmail(rs.getString("csr_email"));
        c.setCsrPhone(rs.getString("csr_phone"));
        c.setWebsite(rs.getString("website"));
        c.setOutreachStatus(rs.getString("outreach_status"));
        c.setAssignedTo(rs.getString("assigned_to"));
        c.setLastContactDate(rs.getDate("last_contact_date"));
        c.setNextFollowupDate(rs.getDate("next_followup_date"));
        c.setMeetingNotes(rs.getString("meeting_notes"));
        c.setEmployeesIndia(rs.getString("employees_india"));
        c.setAlsoInMasterDb(rs.getBoolean("also_in_master_db"));
        return c;
    }
}
