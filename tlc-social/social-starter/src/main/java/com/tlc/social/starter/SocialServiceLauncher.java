package com.tlc.social.starter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Launches the social service application.
 *
 * @author Maharaja S
 * @version 1.0
 */
@SpringBootApplication
public class SocialServiceLauncher {

    /**
     * Entry point for starting the social service application.
     *
     * @param args The command-line arguments passed to the application.
     */
    public static void main(final String[] args) {
        SpringApplication.run(SocialServiceActivator.class, args);
    }
}
