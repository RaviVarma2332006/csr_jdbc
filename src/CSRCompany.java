import java.sql.Date;

/**
 * Plain Java object representing one row in the csr_companies table.
 */
public class CSRCompany {
    private int id;
    private String companyName;
    private String region;
    private String industry;
    private String presenceLocation;
    private String priority;
    private String csrFocus;
    private String potentialSupport;
    private String csrContact;
    private String csrEmail;
    private String csrPhone;
    private String website;
    private String outreachStatus;
    private String assignedTo;
    private Date lastContactDate;
    private Date nextFollowupDate;
    private String meetingNotes;
    private String employeesIndia;
    private boolean alsoInMasterDb;

    public CSRCompany() {
    }

    // Convenience constructor for creating a new company (no id yet)
    public CSRCompany(String companyName, String region, String industry,
                       String presenceLocation, String priority, String csrFocus,
                       String potentialSupport, String csrContact, String csrEmail,
                       String csrPhone, String website, String outreachStatus,
                       String assignedTo, String employeesIndia, boolean alsoInMasterDb) {
        this.companyName = companyName;
        this.region = region;
        this.industry = industry;
        this.presenceLocation = presenceLocation;
        this.priority = priority;
        this.csrFocus = csrFocus;
        this.potentialSupport = potentialSupport;
        this.csrContact = csrContact;
        this.csrEmail = csrEmail;
        this.csrPhone = csrPhone;
        this.website = website;
        this.outreachStatus = outreachStatus;
        this.assignedTo = assignedTo;
        this.employeesIndia = employeesIndia;
        this.alsoInMasterDb = alsoInMasterDb;
    }

    // ---------- Getters & Setters ----------
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getIndustry() { return industry; }
    public void setIndustry(String industry) { this.industry = industry; }

    public String getPresenceLocation() { return presenceLocation; }
    public void setPresenceLocation(String presenceLocation) { this.presenceLocation = presenceLocation; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getCsrFocus() { return csrFocus; }
    public void setCsrFocus(String csrFocus) { this.csrFocus = csrFocus; }

    public String getPotentialSupport() { return potentialSupport; }
    public void setPotentialSupport(String potentialSupport) { this.potentialSupport = potentialSupport; }

    public String getCsrContact() { return csrContact; }
    public void setCsrContact(String csrContact) { this.csrContact = csrContact; }

    public String getCsrEmail() { return csrEmail; }
    public void setCsrEmail(String csrEmail) { this.csrEmail = csrEmail; }

    public String getCsrPhone() { return csrPhone; }
    public void setCsrPhone(String csrPhone) { this.csrPhone = csrPhone; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }

    public String getOutreachStatus() { return outreachStatus; }
    public void setOutreachStatus(String outreachStatus) { this.outreachStatus = outreachStatus; }

    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }

    public Date getLastContactDate() { return lastContactDate; }
    public void setLastContactDate(Date lastContactDate) { this.lastContactDate = lastContactDate; }

    public Date getNextFollowupDate() { return nextFollowupDate; }
    public void setNextFollowupDate(Date nextFollowupDate) { this.nextFollowupDate = nextFollowupDate; }

    public String getMeetingNotes() { return meetingNotes; }
    public void setMeetingNotes(String meetingNotes) { this.meetingNotes = meetingNotes; }

    public String getEmployeesIndia() { return employeesIndia; }
    public void setEmployeesIndia(String employeesIndia) { this.employeesIndia = employeesIndia; }

    public boolean isAlsoInMasterDb() { return alsoInMasterDb; }
    public void setAlsoInMasterDb(boolean alsoInMasterDb) { this.alsoInMasterDb = alsoInMasterDb; }

    @Override
    public String toString() {
        return String.format(
            "[%d] %-30s | Region: %-15s | Priority: %-12s | Status: %-18s | Assigned: %s",
            id, companyName, region, priority, outreachStatus,
            (assignedTo == null || assignedTo.isEmpty()) ? "-" : assignedTo
        );
    }
}
