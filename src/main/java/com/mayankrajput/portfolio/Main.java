package com.mayankrajput.portfolio;

import com.mayankrajput.portfolio.contact.ContactServlet;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;

public class Main {

    public static void main(String[] args) throws Exception {

        String port = System.getenv("PORT");
        if (port == null) port = "8080";

        Tomcat tomcat = new Tomcat();
        tomcat.setPort(Integer.parseInt(port));

        // Create context
        Context ctx = tomcat.addContext("", System.getProperty("java.io.tmpdir"));

        // ✅ REGISTER SERVLET MANUALLY
        Tomcat.addServlet(ctx, "contactServlet", new ContactServlet());
        ctx.addServletMappingDecoded("/contact", "contactServlet");

        tomcat.getConnector(); // trigger connector

        System.out.println("🚀 Tomcat started on port " + port);

        tomcat.start();
        tomcat.getServer().await();
    }
}