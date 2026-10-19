import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

/**
 * A small web interface for the CSR tracker, built on the JDK's own HTTP server
 * (no extra jars, no Maven).
 *
 * Run from the csr_jdbc folder:
 *   javac -cp "lib/*" -d out src/*.java
 *   java -cp "out;lib/*" WebApp
 *
 * Then open http://localhost:8080 . Login: user "volunteer", password from the
 * CSR_APP_PASSWORD environment variable (default "changeme" - change it!).
 */
public class WebApp {

    private static final int PORT = 8080;
    private static final String USERNAME = "volunteer";
    private static final String PASSWORD = envOr("CSR_APP_PASSWORD", "changeme");
    private static final String HTML = "text/html; charset=utf-8";
    private static final String TEXT = "text/plain; charset=utf-8";

    private static final String COOKIE_NAME = "csr_session";
    private static final long SESSION_MS = 12L * 60 * 60 * 1000;   // stay signed in for 12 hours
    private static final Map<String, Long> SESSIONS = new ConcurrentHashMap<>();
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final String LOGIN_CSS =
        "*{box-sizing:border-box}"
        + "body{margin:0;min-height:100vh;display:flex;align-items:center;justify-content:center;"
        + "font-family:Arial,Helvetica,sans-serif;padding:20px;"
        + "background:linear-gradient(135deg,#143a5a 0%,#1F4E78 45%,#3a8dc9 100%)}"
        + ".login{background:#fff;width:100%;max-width:400px;border-radius:16px;padding:36px 32px 28px;"
        + "box-shadow:0 20px 50px rgba(0,0,0,.30)}"
        + ".logo{width:64px;height:64px;border-radius:50%;background:#1F4E78;color:#fff;font-size:24px;"
        + "font-weight:bold;display:flex;align-items:center;justify-content:center;margin:0 auto 14px;letter-spacing:1px}"
        + "h1{margin:0;text-align:center;font-size:24px;color:#14304a}"
        + ".sub{text-align:center;color:#667;font-size:14px;margin:6px 0 22px}"
        + "label{display:block;font-size:13px;font-weight:bold;color:#33475b;margin:14px 0 6px}"
        + "input[type=text],input[type=password]{width:100%;padding:12px 14px;border:1px solid #c5ced8;"
        + "border-radius:10px;font-size:15px;outline:none}"
        + "input[type=text]:focus,input[type=password]:focus{border-color:#1F4E78;box-shadow:0 0 0 3px rgba(31,78,120,.18)}"
        + ".show{display:flex;align-items:center;gap:8px;font-size:13px;color:#556;margin:12px 0 0;font-weight:normal}"
        + "button{width:100%;margin-top:22px;padding:13px;border:0;border-radius:10px;background:#1F4E78;"
        + "color:#fff;font-size:16px;font-weight:bold;cursor:pointer}"
        + "button:hover{background:#173d5d}"
        + ".error{background:#fde2e2;color:#8a1f1f;padding:10px 12px;border-radius:8px;font-size:14px;margin-bottom:6px}"
        + ".foot{text-align:center;color:#8a97a5;font-size:12px;margin:22px 0 0}";

    private static final String[] STATUSES = {
        "Not Contacted", "Researching", "Email Sent", "Call Made", "Meeting Scheduled",
        "Proposal Submitted", "In Discussion", "Funded/Partnered", "Declined", "On Hold"
    };
    private static final String[] PRIORITIES = {
        "High", "Medium-High", "Medium", "Low-Medium", "Low"
    };

    private static final String[] REGIONS = {
        "Hadapsar", "Kharadi", "Ramtekdi-Chakan"
    };

    private static final CSRCompanyDAO DAO = new CSRCompanyDAO();

    private static final String CSS =
        "body{font-family:Arial,Helvetica,sans-serif;margin:0;background:#f4f6f8;color:#1b2733}"
        + "header{background:#1F4E78;padding:14px 20px;display:flex;justify-content:space-between;align-items:center}"
        + "header a{color:#fff;text-decoration:none;font-size:18px;font-weight:bold}"
        + "header a.logout{font-size:14px;font-weight:normal;opacity:.9}"
        + "main{max-width:1100px;margin:0 auto;padding:16px}"
        + "h1{font-size:22px}h2{font-size:18px;margin-top:28px}"
        + ".cards{display:flex;flex-wrap:wrap;gap:10px}"
        + ".card{background:#fff;border-radius:8px;padding:10px 14px;text-decoration:none;color:inherit;"
        + "box-shadow:0 1px 3px rgba(0,0,0,.12);min-width:110px;display:flex;flex-direction:column}"
        + ".card b{font-size:22px;color:#1F4E78}.card span{font-size:12px;color:#556}"
        + "form.filters{display:flex;flex-wrap:wrap;gap:8px;margin:14px 0}"
        + "input,select,textarea{padding:8px;border:1px solid #bbb;border-radius:6px;font-size:14px;"
        + "font-family:inherit;box-sizing:border-box}"
        + "button{background:#1F4E78;color:#fff;border:0;border-radius:6px;padding:9px 16px;font-size:14px;cursor:pointer}"
        + ".wrap{overflow-x:auto}"
        + "table{border-collapse:collapse;width:100%;background:#fff;border-radius:8px;overflow:hidden}"
        + "th,td{padding:9px 10px;border-bottom:1px solid #e3e7eb;text-align:left;font-size:14px;vertical-align:top}"
        + "th{background:#e8eef5}"
        + "td a{color:#1F4E78;font-weight:bold;text-decoration:none}"
        + ".badge{padding:2px 8px;border-radius:10px;font-size:12px;white-space:nowrap;background:#e5e7eb}"
        + ".p-High{background:#d6e6ff}.p-Medium-High{background:#e1f2ff}"
        + ".due{background:#fff6d6;padding:10px;border-radius:8px}"
        + ".panel{background:#fff;border-radius:8px;padding:14px;margin:12px 0;box-shadow:0 1px 3px rgba(0,0,0,.12)}"
        + ".panel th{width:190px;background:none}"
        + ".ok{background:#dff5e3;padding:10px;border-radius:8px}.err{background:#fde2e2;padding:10px;border-radius:8px}"
        + "label{display:block;margin:10px 0 4px;font-weight:bold;font-size:13px}"
        + "input[type=text],input[type=date],select,textarea{width:100%;max-width:520px}"
        + "pre{white-space:pre-wrap;font-family:inherit;margin:0}"
        + ".muted{color:#667;font-size:13px}";

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/", WebApp::handleHome);
        server.createContext("/company", WebApp::handleCompany);
        server.createContext("/update", WebApp::handleUpdate);
        server.createContext("/login", WebApp::handleLogin);
        server.createContext("/logout", WebApp::handleLogout);
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();

        System.out.println("CSR tracker is running.");
        System.out.println("  On this computer:  http://localhost:" + PORT);
        System.out.println("  On the same Wi-Fi: http://<this-computer-IP>:" + PORT + "   (find the IP with: ipconfig)");
        System.out.println("  Login: " + USERNAME + " / (your CSR_APP_PASSWORD)");
        if (PASSWORD.equals("changeme")) {
            System.out.println("WARNING: using the default password 'changeme'. Set CSR_APP_PASSWORD before sharing this.");
        }
        System.out.println("Press Ctrl+C to stop.");
    }

    // ------------------------------------------------------------------
    // Handlers
    // ------------------------------------------------------------------
    private static void handleHome(HttpExchange ex) throws IOException {
        if (!authorized(ex)) return;
        if (!ex.getRequestURI().getPath().equals("/")) {
            send(ex, 404, TEXT, "Not found");
            return;
        }
        try {
            Map<String, String> q = parseForm(ex.getRequestURI().getRawQuery());
            send(ex, 200, HTML, homePage(q));
        } catch (Exception e) {
            serverError(ex, e);
        }
    }

    private static void handleCompany(HttpExchange ex) throws IOException {
        if (!authorized(ex)) return;
        try {
            Map<String, String> q = parseForm(ex.getRequestURI().getRawQuery());
            int id = parseInt(q.get("id"));
            CSRCompany c = DAO.getCompanyById(id);
            if (c == null) {
                send(ex, 404, TEXT, "Company not found");
                return;
            }
            send(ex, 200, HTML, companyPage(c, q));
        } catch (Exception e) {
            serverError(ex, e);
        }
    }

    private static void handleUpdate(HttpExchange ex) throws IOException {
        if (!authorized(ex)) return;
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
            redirect(ex, "/");
            return;
        }
        try {
            Map<String, String> f = parseForm(readBody(ex));
            int id = parseInt(f.get("id"));
            CSRCompany c = DAO.getCompanyById(id);
            if (c == null) {
                send(ex, 404, TEXT, "Company not found");
                return;
            }

            String status = nz(f.get("status"));
            if (!isValidStatus(status)) status = c.getOutreachStatus();

            String assigned = c.getAssignedTo(); // not editable in the web page; keep the stored value
            Date lastContact = parseDate(f.get("last_contact"));
            Date nextFollowup = parseDate(f.get("next_followup"));
            String who = nz(f.get("who")).trim();
            String note = nz(f.get("note")).trim();

            // Notes are appended with a date + name, never overwritten.
            String notes = nz(c.getMeetingNotes());
            if (!note.isEmpty()) {
                String stamp = LocalDate.now().toString() + (who.isEmpty() ? "" : " | " + who);
                String entry = "[" + stamp + "] " + note;
                notes = notes.isEmpty() ? entry : notes + "\n" + entry;
            }

            boolean ok = DAO.updateOutreachProgress(id, status, assigned, lastContact, nextFollowup, notes);
            redirect(ex, "/company?id=" + id + (ok ? "&saved=1" : "&error=1"));
        } catch (Exception e) {
            serverError(ex, e);
        }
    }

    // ------------------------------------------------------------------
    // Pages
    // ------------------------------------------------------------------
    private static String homePage(Map<String, String> q) {
        List<CSRCompany> all = DAO.getAllCompanies();
        String region = nz(q.get("region"));
        String status = nz(q.get("status"));
        String priority = nz(q.get("priority"));
        String text = nz(q.get("q")).trim().toLowerCase();
        LocalDate today = LocalDate.now();

        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String s : STATUSES) counts.put(s, 0);
        List<CSRCompany> due = new ArrayList<>();

        for (CSRCompany c : all) {
            String st = c.getOutreachStatus();
            if (st != null && counts.containsKey(st)) counts.put(st, counts.get(st) + 1);
            if (c.getNextFollowupDate() != null
                    && !c.getNextFollowupDate().toLocalDate().isAfter(today)
                    && !"Funded/Partnered".equals(st) && !"Declined".equals(st)) {
                due.add(c);
            }
        }

        List<CSRCompany> shown = new ArrayList<>();
        for (CSRCompany c : all) {
            if (!region.isEmpty() && !nz(c.getRegion()).contains(region)) continue;
            if (!status.isEmpty() && !status.equals(c.getOutreachStatus())) continue;
            if (!priority.isEmpty() && !priority.equals(c.getPriority())) continue;
            if (!text.isEmpty()) {
                String hay = (nz(c.getCompanyName()) + " " + nz(c.getIndustry())).toLowerCase();
                if (!hay.contains(text)) continue;
            }
            shown.add(c);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<h1>Outreach pipeline</h1><div class=\"cards\">");
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            sb.append("<a class=\"card\" href=\"/?status=").append(enc(e.getKey())).append("\"><b>")
              .append(e.getValue()).append("</b><span>").append(esc(e.getKey())).append("</span></a>");
        }
        sb.append("</div>");

        if (!due.isEmpty()) {
            sb.append("<h2>Follow-ups due (").append(due.size()).append(")</h2><div class=\"due\"><ul>");
            for (CSRCompany c : due) {
                sb.append("<li><a href=\"/company?id=").append(c.getId()).append("\">")
                  .append(esc(c.getCompanyName())).append("</a> &mdash; due ")
                  .append(c.getNextFollowupDate()).append("</li>");
            }
            sb.append("</ul></div>");
        }

        sb.append("<h2>Companies</h2>");
        sb.append("<form class=\"filters\" method=\"get\" action=\"/\">");
        sb.append("<select name=\"region\"><option value=\"\">All regions</option>");
        for (String r : REGIONS) sb.append(option(r, r, region));
        sb.append("</select>");
        sb.append("<select name=\"status\"><option value=\"\">Any status</option>");
        for (String s : STATUSES) sb.append(option(s, s, status));
        sb.append("</select>");
        sb.append("<select name=\"priority\"><option value=\"\">Any priority</option>");
        for (String p : PRIORITIES) sb.append(option(p, p, priority));
        sb.append("</select>");
        sb.append("<input type=\"text\" name=\"q\" placeholder=\"Search company name / industry\" value=\"")
          .append(esc(nz(q.get("q")))).append("\">");
        sb.append("<button type=\"submit\">Filter</button> <a href=\"/\" class=\"muted\" style=\"align-self:center\">Clear</a>");
        sb.append("</form>");

        sb.append("<p class=\"muted\">Showing ").append(shown.size()).append(" of ").append(all.size()).append("</p>");
        sb.append("<div class=\"wrap\"><table><tr><th>Company</th><th>Region</th><th>Priority</th>")
          .append("<th>Status</th><th>Next follow-up</th></tr>");
        for (CSRCompany c : shown) {
            sb.append("<tr><td><a href=\"/company?id=").append(c.getId()).append("\">")
              .append(esc(c.getCompanyName())).append("</a></td>")
              .append("<td>").append(esc(c.getRegion())).append("</td>")
              .append("<td><span class=\"badge p-").append(esc(c.getPriority())).append("\">")
              .append(esc(c.getPriority())).append("</span></td>")
              .append("<td>").append(esc(c.getOutreachStatus())).append("</td>")
              .append("<td>").append(c.getNextFollowupDate() == null ? "-" : c.getNextFollowupDate().toString()).append("</td></tr>");
        }
        sb.append("</table></div>");

        return page("CSR Tracker", sb.toString());
    }

    private static String companyPage(CSRCompany c, Map<String, String> q) {
        StringBuilder sb = new StringBuilder();
        sb.append("<p><a href=\"/\">&larr; Back to all companies</a></p>");
        sb.append("<h1>").append(esc(c.getCompanyName())).append("</h1>");
        if (q.containsKey("saved")) sb.append("<p class=\"ok\">Saved.</p>");
        if (q.containsKey("error")) sb.append("<p class=\"err\">Could not save. Check the server window for the error.</p>");

        sb.append("<div class=\"panel\"><table>");
        sb.append(row("Region", c.getRegion()));
        sb.append(row("Industry", c.getIndustry()));
        sb.append(row("Priority", c.getPriority()));
        sb.append(row("Presence / location", c.getPresenceLocation()));
        sb.append(row("Likely CSR focus", c.getCsrFocus()));
        sb.append(row("Why a good fit", c.getPotentialSupport()));
        sb.append(row("CSR contact", c.getCsrContact()));
        sb.append(row("CSR email", c.getCsrEmail()));
        sb.append(row("CSR phone", c.getCsrPhone()));
        sb.append(row("Employees (India)", c.getEmployeesIndia()));
        String site = nz(c.getWebsite()).trim();
        if (!site.isEmpty() && site.indexOf(' ') < 0) {
            String href = site.startsWith("http") ? site : "https://" + site;
            sb.append("<tr><th>Website</th><td><a href=\"").append(esc(href))
              .append("\" target=\"_blank\" rel=\"noopener\">").append(esc(site)).append("</a></td></tr>");
        } else {
            sb.append(row("Website", site));
        }
        sb.append("</table></div>");

        String last = c.getLastContactDate() != null ? c.getLastContactDate().toString() : LocalDate.now().toString();
        String next = c.getNextFollowupDate() != null ? c.getNextFollowupDate().toString() : "";

        sb.append("<h2>Log a call / update status</h2><div class=\"panel\">");
        sb.append("<form method=\"post\" action=\"/update\">");
        sb.append("<input type=\"hidden\" name=\"id\" value=\"").append(c.getId()).append("\">");

        sb.append("<label>Status</label><select name=\"status\">");
        for (String s : STATUSES) sb.append(option(s, s, nz(c.getOutreachStatus())));
        sb.append("</select>");

        sb.append("<label>Last contact date</label><input type=\"date\" name=\"last_contact\" value=\"").append(last).append("\">");
        sb.append("<label>Next follow-up date</label><input type=\"date\" name=\"next_followup\" value=\"").append(next).append("\">");
        sb.append("<label>Your name</label><input type=\"text\" name=\"who\" placeholder=\"Who is logging this?\">");
        sb.append("<label>Add a note (what happened, who you spoke to, what's next)</label>")
          .append("<textarea name=\"note\" rows=\"4\"></textarea>");
        sb.append("<p><button type=\"submit\">Save</button></p></form></div>");

        sb.append("<h2>Notes so far</h2><div class=\"panel\">");
        String notes = nz(c.getMeetingNotes()).trim();
        sb.append(notes.isEmpty() ? "<span class=\"muted\">No notes yet.</span>" : "<pre>" + esc(notes) + "</pre>");
        sb.append("</div>");

        return page(c.getCompanyName(), sb.toString());
    }

    private static String page(String title, String body) {
        return "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"utf-8\">"
            + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
            + "<title>" + esc(title) + "</title><style>" + CSS + "</style></head><body>"
            + "<header><a href=\"/\">Akshar Bharati &mdash; CSR Tracker</a>"
            + "<a class=\"logout\" href=\"/logout\">Log out</a></header>"
            + "<main>" + body + "</main></body></html>";
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------
    private static String row(String label, String value) {
        return "<tr><th>" + esc(label) + "</th><td>" + esc(nz(value).isEmpty() ? "-" : value) + "</td></tr>";
    }

    private static String option(String value, String label, String selected) {
        return "<option value=\"" + esc(value) + "\"" + (value.equals(selected) ? " selected" : "")
            + ">" + esc(label) + "</option>";
    }

    // ------------------------------------------------------------------
    // Login (page + session cookie)
    // ------------------------------------------------------------------
    private static boolean authorized(HttpExchange ex) throws IOException {
        if (isLoggedIn(ex)) return true;
        redirect(ex, "/login");
        return false;
    }

    private static void handleLogin(HttpExchange ex) throws IOException {
        try {
            if ("POST".equalsIgnoreCase(ex.getRequestMethod())) {
                Map<String, String> f = parseForm(readBody(ex));
                boolean userOk = safeEquals(nz(f.get("username")).trim(), USERNAME);
                boolean passOk = safeEquals(nz(f.get("password")), PASSWORD);
                if (userOk && passOk) {
                    long now = System.currentTimeMillis();
                    SESSIONS.values().removeIf(expiry -> expiry < now);   // tidy up old sessions
                    String token = newToken();
                    SESSIONS.put(token, now + SESSION_MS);
                    ex.getResponseHeaders().add("Set-Cookie", COOKIE_NAME + "=" + token
                        + "; Path=/; HttpOnly; SameSite=Lax; Max-Age=" + (SESSION_MS / 1000));
                    redirect(ex, "/");
                    return;
                }
                sleepQuietly(700);   // slows down password guessing
                send(ex, 401, HTML, loginPage("Incorrect username or password."));
                return;
            }
            if (isLoggedIn(ex)) {
                redirect(ex, "/");
                return;
            }
            send(ex, 200, HTML, loginPage(null));
        } catch (Exception e) {
            serverError(ex, e);
        }
    }

    private static void handleLogout(HttpExchange ex) throws IOException {
        String token = sessionToken(ex);
        if (token != null) SESSIONS.remove(token);
        ex.getResponseHeaders().add("Set-Cookie", COOKIE_NAME + "=; Path=/; HttpOnly; SameSite=Lax; Max-Age=0");
        redirect(ex, "/login");
    }

    private static boolean isLoggedIn(HttpExchange ex) {
        String token = sessionToken(ex);
        if (token == null) return false;
        Long expires = SESSIONS.get(token);
        if (expires == null) return false;
        if (expires < System.currentTimeMillis()) {
            SESSIONS.remove(token);
            return false;
        }
        return true;
    }

    private static String sessionToken(HttpExchange ex) {
        List<String> cookies = ex.getRequestHeaders().get("Cookie");
        if (cookies == null) return null;
        for (String header : cookies) {
            for (String part : header.split(";")) {
                String p = part.trim();
                if (p.startsWith(COOKIE_NAME + "=")) {
                    return p.substring(COOKIE_NAME.length() + 1);
                }
            }
        }
        return null;
    }

    private static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private static boolean safeEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    private static void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String loginPage(String error) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"utf-8\">");
        sb.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
        sb.append("<title>Sign in - CSR Tracker</title><style>").append(LOGIN_CSS).append("</style></head><body>");
        sb.append("<div class=\"login\"><div class=\"logo\">AB</div>");
        sb.append("<h1>CSR Tracker</h1><p class=\"sub\">Akshar Bharati &middot; sign in to continue</p>");
        if (error != null && !error.isEmpty()) {
            sb.append("<div class=\"error\">").append(esc(error)).append("</div>");
        }
        sb.append("<form method=\"post\" action=\"/login\">");
        sb.append("<label for=\"u\">Username</label>");
        sb.append("<input type=\"text\" id=\"u\" name=\"username\" autocomplete=\"username\" autofocus required>");
        sb.append("<label for=\"p\">Password</label>");
        sb.append("<input type=\"password\" id=\"p\" name=\"password\" autocomplete=\"current-password\" required>");
        sb.append("<label class=\"show\"><input type=\"checkbox\" ");
        sb.append("onclick=\"document.getElementById('p').type = this.checked ? 'text' : 'password'\"> Show password</label>");
        sb.append("<button type=\"submit\">Sign in</button></form>");
        sb.append("<p class=\"foot\">Volunteer access only</p></div></body></html>");
        return sb.toString();
    }

    private static void send(HttpExchange ex, int code, String type, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", type);
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void redirect(HttpExchange ex, String location) throws IOException {
        ex.getResponseHeaders().set("Location", location);
        ex.sendResponseHeaders(303, -1);
        ex.close();
    }

    private static void serverError(HttpExchange ex, Exception e) throws IOException {
        e.printStackTrace();
        send(ex, 500, TEXT, "Something went wrong. Check the server window for details.");
    }

    private static String readBody(HttpExchange ex) throws IOException {
        try (InputStream in = ex.getRequestBody()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static Map<String, String> parseForm(String s) {
        Map<String, String> map = new HashMap<>();
        if (s == null || s.isEmpty()) return map;
        for (String pair : s.split("&")) {
            int i = pair.indexOf('=');
            String k = i >= 0 ? pair.substring(0, i) : pair;
            String v = i >= 0 ? pair.substring(i + 1) : "";
            map.put(URLDecoder.decode(k, StandardCharsets.UTF_8), URLDecoder.decode(v, StandardCharsets.UTF_8));
        }
        return map;
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static int parseInt(String s) {
        try {
            return Integer.parseInt(nz(s).trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static Date parseDate(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        try {
            return Date.valueOf(s.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static boolean isValidStatus(String s) {
        for (String st : STATUSES) {
            if (st.equals(s)) return true;
        }
        return false;
    }

    private static String envOr(String name, String fallback) {
        String v = System.getenv(name);
        return (v == null || v.isEmpty()) ? fallback : v;
    }
}