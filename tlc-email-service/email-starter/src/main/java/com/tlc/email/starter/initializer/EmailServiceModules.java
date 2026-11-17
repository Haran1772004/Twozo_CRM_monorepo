package com.tlc.email.starter.initializer;

import com.tlc.components.starter.initializer.ModuleRegistry;
import com.tlc.components.starter.initializer.Modules;

/**
 * Enum representing the different modules within the email-service system.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public enum EmailServiceModules implements Modules {

    FILE_SERVER("file-server", "com.tlc.file.server"),
    EMAIL_CLIENT("email-client", "com.tlc.email.client"),
    EMAIL_COMMON("email-common", "com.tlc.email.common"),
    EMAIL_SERVER("email-server", "com.tlc.email.server");

    private final String artifactId;
    private final String classNamePrefix;
    private final short id;

    EmailServiceModules(final String artifactId, final String classNamePrefix) {
        this.artifactId = artifactId;
        this.classNamePrefix = classNamePrefix;
        this.id = ModuleRegistry.getInstance().getId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public short getId() {
        return id;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getArtifactId() {
        return artifactId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getClassNamePrefix() {
        return classNamePrefix;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return this.name();
    }
}
