package com.tlc.crm.server.starter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Launches the CRM server application.
 *
 * @author ThalaimalaiPandiyan
 * @version 1.0
 */
@SpringBootApplication
class CrmServerLauncher {

    /**
     * Entry point for starting the CRM Server application.
     *
     * @param args The command-line arguments passed to the application.
     */
    public static void main(final String[] args) {
        SpringApplication.run(CrmServerActivator.class, args);
    }
}
