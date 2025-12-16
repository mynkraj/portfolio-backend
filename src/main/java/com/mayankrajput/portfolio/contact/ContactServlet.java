package com.mayankrajput.portfolio.contact;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class ContactServlet extends HttpServlet {

    private static final List<String> ALLOWED_ORIGINS = Arrays.asList(
            "http://localhost:5500",
            "https://mayankdeveloper.netlify.app",
            "https://mayanks.world",
            "https://www.mayanks.world"
    );

    private void setCors(HttpServletRequest req, HttpServletResponse res) {
        String origin = req.getHeader("Origin");
        if (origin != null && ALLOWED_ORIGINS.contains(origin)) {
            res.setHeader("Access-Control-Allow-Origin", origin);
        }
        res.setHeader("Access-Control-Allow-Headers", "Content-Type");
        res.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
        res.setHeader("Vary", "Origin");
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse res) {
        setCors(req, res);
        res.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        setCors(req, res);
        res.setContentType("application/json");
        res.getWriter().write(
                "{\"ok\":true,\"message\":\"Contact endpoint running. Use POST.\"}"
        );
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        setCors(req, res);
        res.setContentType("application/json");

        String name = req.getParameter("name");
        String email = req.getParameter("email");
        String message = req.getParameter("message");

        if (isBlank(name) || isBlank(email) || isBlank(message)) {
            res.setStatus(400);
            res.getWriter().write("{\"success\":false,\"error\":\"All fields required\"}");
            return;
        }

        String apiKey = System.getenv("RESEND_API_KEY");
        if (isBlank(apiKey)) {
            res.setStatus(500);
            res.getWriter().write("{\"success\":false,\"error\":\"Resend API key missing\"}");
            return;
        }

        boolean sent = sendViaResend(apiKey, name, email, message);

        if (sent) {
            res.getWriter().write("{\"success\":true,\"message\":\"Message sent\"}");
        } else {
            res.setStatus(500);
            res.getWriter().write("{\"success\":false,\"error\":\"Email failed\"}");
        }
    }

    private boolean sendViaResend(String apiKey, String name, String email, String msg) {
        try {
            URL url = new URL("https://api.resend.com/emails");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();

            con.setRequestMethod("POST");
            con.setRequestProperty("Authorization", "Bearer " + apiKey);
            con.setRequestProperty("Content-Type", "application/json");
            con.setDoOutput(true);

            String json =
                    "{"
                            + "\"from\":\"Portfolio <onboarding@resend.dev>\","
                            + "\"to\":[\"mayankrajput187@gmail.com\"],"
                            + "\"subject\":\"New Portfolio Contact\","
                            + "\"html\":\""
                            + "<p><b>Name:</b> " + escapeHtml(name) + "</p>"
                            + "<p><b>Email:</b> " + escapeHtml(email) + "</p>"
                            + "<p>" + escapeHtml(msg) + "</p>"
                            + "\""
                            + "}";

            byte[] body = json.getBytes(StandardCharsets.UTF_8);
            OutputStream os = con.getOutputStream();
            os.write(body);
            os.flush();

            return con.getResponseCode() == 200;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
    private String escapeHtml(String input) {
        if (input == null) return "";
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }
}