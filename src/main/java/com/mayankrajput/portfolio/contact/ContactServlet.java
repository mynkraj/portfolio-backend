package com.mayankrajput.portfolio.contact;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * ContactServlet
 *
 * - Reads mail configuration from environment variables:
 *     EMAIL_FROM      (sender email, e.g. yourgmail@gmail.com)
 *     EMAIL_PASSWORD  (app password for the sender)
 *     EMAIL_TO        (destination email where you receive messages)
 *
 * - Handles:
 *     GET    -> friendly status message
 *     OPTIONS -> CORS preflight
 *     POST   -> accepts form-urlencoded (name,email,message) and sends email
 *
 * - CORS: checks Origin header against a whitelist and responds accordingly.
 */
@WebServlet("/contact")
public class ContactServlet extends HttpServlet {

    // Allowed origins (add your Netlify URL here). For development you can allow localhost 5500.
    // In production, replace or extend this list with your real domain(s).
    private static final List<String> ALLOWED_ORIGINS = Arrays.asList(
            "http://127.0.0.1:5500",
            "http://localhost:5500",
         //   "http://localhost:5500",
           // "https://celebrated-alpaca-ce4636.netlify.app", // replace with your Netlify site
            "https://mayankdeveloper.netlify.app" // optional - add any of your deployed URLs
    );

    private void setCorsHeaders(HttpServletRequest req, HttpServletResponse res) {
        String origin = req.getHeader("Origin");
        if (origin != null && ALLOWED_ORIGINS.contains(origin)) {
            res.setHeader("Access-Control-Allow-Origin", origin);
        } else {
            // For local testing you can default to allow none or all.
            // WARNING: Allowing "*" is not safe for credentials; use it only in development.
            // res.setHeader("Access-Control-Allow-Origin", "*");
        }

        res.setHeader("Access-Control-Allow-Headers", "Content-Type");
        res.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
        res.setHeader("Vary", "Origin");
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse res) throws IOException {
        // Handle CORS preflight
        setCorsHeaders(req, res);
        res.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        setCorsHeaders(req, res);
        res.setContentType("application/json; charset=UTF-8");
        PrintWriter out = res.getWriter();
        out.write("{\"ok\":true, \"message\":\"Contact endpoint - POST form (name,email,message) to send an email.\"}");
        out.flush();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        // CORS
        setCorsHeaders(req, res);
        res.setContentType("application/json; charset=UTF-8");

        // Read form parameters (x-www-form-urlencoded)
        String name = req.getParameter("name");
        String email = req.getParameter("email");
        String message = req.getParameter("message");

        // Basic validation
        if (isBlank(name) || isBlank(email) || isBlank(message)) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            res.getWriter().write("{\"success\":false,\"error\":\"All fields are required\"}");
            return;
        }

        // Load email configuration from environment
        final String FROM = System.getenv("EMAIL_FROM");
        final String PASSWORD = System.getenv("EMAIL_PASSWORD");
        final String TO = System.getenv("EMAIL_TO");

        if (isBlank(FROM) || isBlank(PASSWORD) || isBlank(TO)) {
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            res.getWriter().write("{\"success\":false,\"error\":\"Mail configuration not found on server (EMAIL_FROM / EMAIL_PASSWORD / EMAIL_TO).\"}");
            // Also log to server console for debugging
            System.err.println("Mail env vars missing. Set EMAIL_FROM, EMAIL_PASSWORD and EMAIL_TO.");
            return;
        }

        try {
            sendEmail(FROM, PASSWORD, TO, name, email, message);

            res.setStatus(HttpServletResponse.SC_OK);
            res.getWriter().write("{\"success\":true,\"message\":\"Message sent\"}");
        } catch (Exception e) {
            // Log server-side full stacktrace for debugging (do not expose stacktrace to users)
            e.printStackTrace();
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            res.getWriter().write("{\"success\":false,\"error\":\"Failed to send email\"}");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private void sendEmail(String from, String password, String to,
                           String name, String fromEmail, String messageBody) throws MessagingException {

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props, new jakarta.mail.Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(from, password);
            }
        });

        Message msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(from));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        msg.setSubject("New Portfolio Contact from " + name);

        String content = "Name: " + name + "\n"
                + "Email: " + fromEmail + "\n\n"
                + messageBody;

        msg.setText(content);

        Transport.send(msg);
        System.out.println("ContactServlet: email sent successfully to " + to + " from " + from);
    }
}