# QUICK START (Windows / VS Code)

1. In VS Code: File > Open Folder > choose this `csr_jdbc` folder (not its parent).
2. Terminal > New Terminal, then run:
   `powershell -ExecutionPolicy Bypass -File download_jars.ps1`
   This fills the `lib` folder with the MySQL driver and all Apache POI jars.
3. Open `src/DBConnection.java` and set `USERNAME` and `PASSWORD` to your MySQL login.
4. Ctrl+Shift+P > "Java: Clean Java Language Server Workspace" > Restart and delete.
5. In MySQL run `TRUNCATE TABLE csr_companies;` if you've inserted test rows.
6. Open Run and Debug (left sidebar), pick **Launch ExcelImporter** in the dropdown, press the green arrow.
   Do NOT use the Code Runner button (it compiles without the jars and gives "JDBC Driver not found").

---

# CSR Prospect Database — JDBC CRUD App

A minimal Java + MySQL setup to store and manage your Akshar Bharati CSR
prospect list (the same columns as your working Excel database), with full
Create / Read / Update / Delete support.

## Files

| File | Purpose |
|---|---|
| `schema.sql` | Creates the `akshar_bharati_csr` database and `csr_companies` table |
| `src/CSRCompany.java` | Plain Java object (POJO) matching one row |
| `src/DBConnection.java` | Opens the JDBC connection to MySQL |
| `src/CSRCompanyDAO.java` | All CRUD methods (add, get, search, update, delete) |
| `src/Main.java` | Example usage of every CRUD operation |
| `src/ExcelImporter.java` | Bulk-imports your `Akshar_Bharati_CSR_Prospect_List.xlsx` "Hadapsar Cluster - MASTER" sheet into MySQL |

## Setup steps

### 1. Install MySQL and create the database
```bash
mysql -u root -p < schema.sql
```
This creates the database, table, indexes, and one sample row.

### 2. Get the MySQL JDBC driver
Download `mysql-connector-j` (the JDBC driver .jar) from:
https://dev.mysql.com/downloads/connector/j/

If you're using **Maven**, add this to `pom.xml` instead of a manual jar:
```xml
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <version>8.4.0</version>
</dependency>
```

### 3. Set your credentials
Open `src/DBConnection.java` and update:
```java
private static final String USERNAME = "root";
private static final String PASSWORD = "your_password";
```

### 4. Compile and run

**Without Maven** (manual classpath):
```bash
javac -d out src/*.java
java -cp "out:mysql-connector-j-8.4.0.jar" Main
```
(On Windows, use `;` instead of `:` in the classpath.)

**With Maven**, just run `Main.java` from your IDE, or:
```bash
mvn compile exec:java -Dexec.mainClass="Main"
```

### 5. Test the connection alone (optional)
```bash
java -cp "out:mysql-connector-j-8.4.0.jar" DBConnection
```
This just opens and closes a connection, printing success or the error.

## What each CRUD method does

- **`addCompany(CSRCompany c)`** → inserts a new prospect, returns its new `id`
- **`getAllCompanies()`** → returns every company, sorted High → Low priority (matches your Excel sort)
- **`getCompanyById(int id)`** → fetch one company
- **`searchCompanies(region, priority, outreachStatus, assignedTo)`** → filter by any combination; pass `null` to skip a filter
- **`updateCompany(CSRCompany c)`** → full record overwrite
- **`updateOutreachProgress(...)`** → quick helper for the fields your volunteers update most (status, assigned to, dates, notes) — used after every call/visit
- **`deleteCompany(int id)`** → removes a record

## Importing your existing Excel data (ExcelImporter.java)

This reads directly from your `Akshar_Bharati_CSR_Prospect_List.xlsx` file —
specifically the **"Hadapsar Cluster - MASTER"** sheet (your 85 deduplicated,
priority-sorted companies) — no manual CSV export needed.

### 1. Get the Apache POI library

**Maven** — add to `pom.xml`:
```xml
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.5</version>
</dependency>
```

**Manual jars** (if not using Maven) — download these from
https://poi.apache.org/download.html and put them on your classpath:
- `poi-5.2.5.jar`
- `poi-ooxml-5.2.5.jar`
- `poi-ooxml-lite-5.2.5.jar`
- `xmlbeans-5.2.0.jar`
- `commons-collections4-4.4.jar`
- `commons-io-2.15.1.jar`
- `commons-compress-1.26.0.jar`   (needed to open .xlsx files - easy to miss!)
- `log4j-api-2.20.0.jar`

### 2. Run the importer

**Maven:**
```bash
mvn compile exec:java -Dexec.mainClass="ExcelImporter" \
    -Dexec.args="Akshar_Bharati_CSR_Prospect_List.xlsx"
```

**Manual classpath** (adjust the jar list/paths to what you downloaded):
```bash
javac -cp "poi-5.2.5.jar:poi-ooxml-5.2.5.jar:poi-ooxml-lite-5.2.5.jar:xmlbeans-5.2.0.jar:mysql-connector-j-8.4.0.jar" \
    -d out src/*.java

java -cp "out:poi-5.2.5.jar:poi-ooxml-5.2.5.jar:poi-ooxml-lite-5.2.5.jar:xmlbeans-5.2.0.jar:commons-collections4-4.4.jar:commons-io-2.15.1.jar:commons-compress-1.26.0.jar:log4j-api-2.20.0.jar:mysql-connector-j-8.4.0.jar" \
    ExcelImporter Akshar_Bharati_CSR_Prospect_List.xlsx
```

### 3. What it does

- Opens the **"Hadapsar Cluster - MASTER"** sheet specifically (fails with a
  clear error + lists available sheet names if that tab isn't found)
- Starts reading from row 5 (row 4 is the header), skips any blank rows
- Maps free-text Priority/Outreach Status values onto the database's ENUM
  values, falling back to a safe default (`Medium` / `Not Contacted`) if a
  cell has an unexpected value instead of crashing the import
- Leaves `last_contact_date` / `next_followup_date` blank on import — these
  get filled in later via `updateOutreachProgress()` as your volunteers make
  calls
- Prints a running log of every row imported, plus a final count of
  successes/failures

### 4. Before running twice

The importer always **inserts new rows** — running it twice will duplicate
every company. If you need to re-import after fixing the spreadsheet, either
`TRUNCATE TABLE csr_companies;` first, or extend the importer to check for an
existing `company_name` before inserting (ask me if you want that added).


## Troubleshooting (issues we hit while setting this up)

| Error | Cause | Fix |
|---|---|---|
| `Public Key Retrieval is not allowed` | MySQL 8 default auth | Already fixed: `allowPublicKeyRetrieval=true` is in the URL in `DBConnection.java` |
| `Access denied for user 'root'@'localhost'` | Wrong password in `DBConnection.java` | Put your real MySQL password in `PASSWORD`, or create a dedicated app user |
| `ClassNotFoundException: ExcelImporter` | Ran a placeholder classpath (`"..."`) | Use VS Code's Run button with the `Launch ExcelImporter` config instead |
| `The import org.apache cannot be resolved` | POI jars missing from `lib/` | Download the POI jars listed above into `lib/` |
| `ClassNotFoundException: ...commons.compress...ZipFile` | `commons-compress` jar missing | Add `commons-compress-1.26.0.jar` to `lib/` |
| Output looks like the `Main` demo, not the import | Wrong run configuration selected | Pick `Launch ExcelImporter` in the Run and Debug dropdown |

After adding any jar: Ctrl+Shift+P -> "Java: Clean Java Language Server Workspace" -> restart.

Before importing, run `TRUNCATE TABLE csr_companies;` if you've already run `Main.java`, otherwise you'll get a stray test row.


## Reports and the web page

Compile once after any change:
```
javac -cp "lib/*" -d out src/*.java
```

**Reports (text, in the terminal):**
```
java -cp "out;lib/*" Reports
```

**Web page for volunteers:**
```
$env:CSR_APP_PASSWORD = "pick-a-strong-password"
java -cp "out;lib/*" WebApp
```
Open http://localhost:8080 (user: `volunteer`). Other devices on the same Wi-Fi can use
http://YOUR-COMPUTER-IP:8080 (find the IP with `ipconfig`). Allow Java through the Windows
firewall if it asks. The page stops when you close the terminal or press Ctrl+C.

Do not run `Main.java` again: its demo inserts another "Whirlpool of India" row each time.

## Housekeeping
- Duplicate check: `SELECT company_name, COUNT(*) FROM csr_companies GROUP BY company_name HAVING COUNT(*) > 1;`
- Weekly backup (normal terminal, not the MySQL prompt): `mysqldump -u root -p akshar_bharati_csr > csr_backup.sql`
