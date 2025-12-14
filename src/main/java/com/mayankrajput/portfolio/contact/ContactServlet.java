package com.mayankrajput.portfolio.contact;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

//@WebServlet("/contact")
public class ContactServlet extends HttpServlet {

    // ✅ Allowed frontend origins
    private static final List<String> ALLOWED_ORIGINS = Arrays.asList(
            "http://127.0.0.1:5500",
            "http://localhost:5500",
            "https://mayankdeveloper.netlify.app"
    );

    /* ===================== CORS ===================== */

    private void setCorsHeaders(HttpServletRequest req, HttpServletResponse res) {
        String origin = req.getHeader("Origin");

        if (origin != null && ALLOWED_ORIGINS.contains(origin)) {
            res.setHeader("Access-Control-Allow-Origin", origin);
        }

        res.setHeader("Access-Control-Allow-Headers", "Content-Type");
        res.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
        res.setHeader("Vary", "Origin");
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse res) {
        setCorsHeaders(req, res);
        res.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    /* ===================== GET ===================== */

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        setCorsHeaders(req, res);
        res.setContentType("application/json");
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());

        PrintWriter out = res.getWriter();
        out.write("{\"ok\":true,\"message\":\"Contact endpoint is running. Use POST with name, email, message.\"}");
        out.flush();
    }

    /* ===================== POST ===================== */

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        setCorsHeaders(req, res);
        res.setContentType("application/json");
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());

        String name = req.getParameter("name");
        String email = req.getParameter("email");
        String message = req.getParameter("message");

        if (isBlank(name) || isBlank(email) || isBlank(message)) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            res.getWriter().write("{\"success\":false,\"error\":\"All fields are required\"}");
            return;
        }

        String FROM = System.getenv("EMAIL_FROM");
        String PASSWORD = System.getenv("EMAIL_PASSWORD");
        String TO = System.getenv("EMAIL_TO");

        if (isBlank(FROM) || isBlank(PASSWORD) || isBlank(TO)) {
            System.err.println("❌ EMAIL env vars missing");
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            res.getWriter().write("{\"success\":false,\"error\":\"Mail configuration missing on server\"}");
            return;
        }

        try {
            sendEmail(FROM, PASSWORD, TO, name, email, message);

            res.setStatus(HttpServletResponse.SC_OK);
            res.getWriter().write("{\"success\":true,\"message\":\"Message sent successfully\"}");

        } catch (Exception e) {
            e.printStackTrace();
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            res.getWriter().write("{\"success\":false,\"error\":\"Failed to send email\"}");
        }
    }

    /* ===================== EMAIL ===================== */

    private void sendEmail(
            String from,
            String password,
            String to,
            String name,
            String fromEmail,
            String messageBody
    ) throws MessagingException {

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

        String body =
                "Name: " + name + "\n" +
                        "Email: " + fromEmail + "\n\n" +
                        "Message:\n" +
                        messageBody;

        msg.setText(body);
        Transport.send(msg);

        System.out.println("✅ Email sent successfully");
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}