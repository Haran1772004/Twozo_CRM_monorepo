package com.tlc.components.starter.initializer;

/**
 * Represents a module with essential metadata such as its ID, artifact ID, class name prefix, and name.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public interface Modules {

    /**
     * Retrieves the unique identifier of the module.
     *
     * @return the module ID as a short value.
     */
    short getId();

    /**
     * Retrieves the artifact ID of the module.
     *
     * @return the artifact ID as a string.
     */
    String getArtifactId();

    /**
     * Retrieves the class name prefix associated with the module.
     *
     * @return the class name prefix as a string.
     */
    String getClassNamePrefix();

    /**
     * Retrieves the name of the module.
     *
     * @return the module name as a string.
     */
    String getName();
}
