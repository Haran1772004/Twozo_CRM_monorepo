package com.tlc.commons.service.registrar.model;

import com.tlc.commons.service.registrar.ResourceRegistrar;

/**
 * <p>
 * A model that holds the data of the {@link ResourceRegistrar} which is needed for registering the resources/actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public abstract class RegistrarData {

    private int moduleId;
    private String moduleName;
    private long lastModifiedTime;

    public int getModuleId() {
        return moduleId;
    }

    public void setModuleId(final int moduleId) {
        this.moduleId = moduleId;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(final String moduleName) {
        this.moduleName = moduleName;
    }

    public long getLastModifiedTime() {
        return lastModifiedTime;
    }

    public void setLastModifiedTime(final long lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
    }
}
