package com.tlc.iam.starter.initializer;

import com.tlc.components.starter.initializer.ModuleRegistry;
import com.tlc.components.starter.initializer.Modules;

/**
 * Enum representing the different modules within the IAM-Server system.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public enum IAMModules implements Modules {

    IAM_COMMON("iam-common", "com.tlc.iam.common"),
    IAM_CLIENT("client", "com.tlc.iam.client"),
    IAM_SERVER("server", "com.tlc.iam.server");

    private final String artifactId;
    private final String classNamePrefix;
    private final short id;

    IAMModules(final String artifactId, final String classNamePrefix) {
        this.artifactId = artifactId;
        this.classNamePrefix = classNamePrefix;
        this.id = ModuleRegistry.getInstance().getId();
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
    public short getId() {
        return id;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return this.name();
    }

}
