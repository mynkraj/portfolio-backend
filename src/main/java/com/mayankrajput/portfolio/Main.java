package com.mayankrajput.portfolio;

import org.apache.catalina.startup.Tomcat;

public class Main {

    public static void main(String[] args) throws Exception {

        String port = System.getenv("PORT");
        if (port == null) port = "8080";

        Tomcat tomcat = new Tomcat();
        tomcat.setPort(Integer.parseInt(port));

        tomcat.getConnector(); // trigger connector

        tomcat.addWebapp("", System.getProperty("java.io.tmpdir"));

        System.out.println("Starting Tomcat on port " + port);

        tomcat.start();
        tomcat.getServer().await();
    }
}