package com.tlc.file.starter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Launches the file service application.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@SpringBootApplication
public class FileServiceLauncher {

    /**
     * Entry point for starting the File service application.
     *
     * @param args The command-line arguments passed to the application.
     */
    public static void main(String[] args) {
        SpringApplication.run(FileServiceActivator.class, args);
    }

}
