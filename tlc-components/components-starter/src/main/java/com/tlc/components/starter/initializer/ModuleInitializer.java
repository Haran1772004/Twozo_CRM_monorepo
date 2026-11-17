package com.tlc.components.starter.initializer;

/**
 * Interface outlines the process of initializing modules within the context of an application.
 * By adhering to this interface, classes responsible for module initialization can provide a consistent and standardized
 * approach across the application, promoting maintainability and scalability.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public interface ModuleInitializer {

    /**
     * Responsible for starting the initialization of the module, ensuring that all necessary
     * configurations are properly loaded and initialized.
     *
     * @throws Exception If an error occurs during the initialization of the module, such as
     *                   failure to read configuration files or access resources.
     */
    void initialize() throws Exception;
}
