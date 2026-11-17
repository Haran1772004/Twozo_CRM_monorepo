package com.tlc.email.starter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * <p>
 * Serves as the entry point for the email service application. This class starts the
 * spring boot application and sets up the email service.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
@SpringBootApplication
public class EmailServiceLauncher {

    public static void main(String[] args) {
        SpringApplication.run(Activator.class, args);
    }

}
