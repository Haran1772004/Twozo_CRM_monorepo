package com.tlc.iam.starter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Launches the IAM application.
 *
 * @author ThalaimalaiPandiyan
 * @version 1.0
 */
@SpringBootApplication
class IAMLauncher {

    /**
     * Entry point for starting the IAM application.
     *
     * @param args The command-line arguments passed to the application.
     */
    public static void main(String[] args) {
        SpringApplication.run(IAMActivator.class, args);
    }
}
