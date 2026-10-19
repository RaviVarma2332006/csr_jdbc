-- ============================================================
-- Akshar Bharati CSR Prospect Database - MySQL Schema
-- ============================================================

CREATE DATABASE IF NOT EXISTS akshar_bharati_csr;
USE akshar_bharati_csr;

CREATE TABLE IF NOT EXISTS csr_companies (
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    company_name        VARCHAR(255) NOT NULL,
    region              VARCHAR(255),
    industry            VARCHAR(255),
    presence_location   TEXT,
    priority            ENUM('High','Medium-High','Medium','Low-Medium','Low') DEFAULT 'Medium',
    csr_focus           TEXT,
    potential_support   TEXT,
    csr_contact         TEXT,
    csr_email           TEXT,
    csr_phone           TEXT,
    website             VARCHAR(255),
    outreach_status     ENUM(
                            'Not Contacted','Researching','Email Sent','Call Made',
                            'Meeting Scheduled','Proposal Submitted','In Discussion',
                            'Funded/Partnered','Declined','On Hold'
                        ) DEFAULT 'Not Contacted',
    assigned_to         VARCHAR(150),
    last_contact_date   DATE,
    next_followup_date  DATE,
    meeting_notes       TEXT,
    employees_india     TEXT,
    also_in_master_db   BOOLEAN DEFAULT FALSE,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Helpful indexes for the queries you'll run most often
CREATE INDEX idx_priority ON csr_companies(priority);
CREATE INDEX idx_outreach_status ON csr_companies(outreach_status);
CREATE INDEX idx_region ON csr_companies(region);
CREATE INDEX idx_assigned_to ON csr_companies(assigned_to);
