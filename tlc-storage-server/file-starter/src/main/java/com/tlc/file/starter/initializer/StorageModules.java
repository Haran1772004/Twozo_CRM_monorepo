package com.tlc.file.starter.initializer;

import com.tlc.components.starter.initializer.ModuleRegistry;
import com.tlc.components.starter.initializer.Modules;

/**
 * Enum representing the different modules within the storage-server system.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public enum StorageModules implements Modules {

    FILE_CLIENT("file-client", "com.tlc.file.client"),
    FILE_SERVER("file-server", "com.tlc.file.server");

    private final String artifactId;
    private final String classNamePrefix;
    private final short id;

    StorageModules(final String artifactId, final String classNamePrefix) {
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
