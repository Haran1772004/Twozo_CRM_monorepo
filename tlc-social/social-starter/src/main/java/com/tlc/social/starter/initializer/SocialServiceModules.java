package com.tlc.social.starter.initializer;

import com.tlc.components.starter.initializer.ModuleRegistry;
import com.tlc.components.starter.initializer.Modules;

/**
 * Defines the names of various modules within the social application, along with their corresponding identifiers.
 *
 * @author Maharaja S
 * @version 1.0
 */
public enum SocialServiceModules implements Modules {

    FILE_SERVER("file-server", "com.tlc.file.server"),
    SOCIAL_CLIENT("social-client", "com.tlc.social.client"),
    SOCIAL_COMMON("social-common", "com.tlc.social.common"),
    SOCIAL_SERVER("social-server", "com.tlc.social.server");

    private final short id;
    private final String artifactId;
    private final String classNamePrefix;

    SocialServiceModules(final String artifactId, final String classNamePrefix) {
        this.artifactId = artifactId;
        this.classNamePrefix = classNamePrefix;
        this.id = ModuleRegistry.getInstance().getId();
    }

    /**
     * {@inheritDoc}
     *
     * @return the id
     */
    @Override
    public short getId() {
        return id;
    }

    /**
     * {@inheritDoc}
     *
     * @return the artifact is.
     */
    @Override
    public String getArtifactId() {
        return artifactId;
    }

    /**
     * {@inheritDoc}
     *
     * @return the class name prefix
     */
    @Override
    public String getClassNamePrefix() {
        return classNamePrefix;
    }

    /**
     * {@inheritDoc}
     *
     * @return the name.
     */
    @Override
    public String getName() {
        return this.name();
    }
}
