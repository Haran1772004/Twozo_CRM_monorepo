package com.tlc.crm.fields.cache.settings;

import com.tlc.crm.fields.cache.RemoteCache;
import com.tlc.crm.fields.cache.internal.SourceSettingsCacheImpl;
import com.tlc.crm.fields.models.settings.SourceSettings;

import java.util.Map;

/**
 * Provides an abstract way to deal with {@link SourceSettings} cache.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public interface SourceSettingsCache extends RemoteCache {

    /**
     * <p>
     * Gets the instance of {@link SourceSettingsCache}.
     * </p>
     *
     * @return {@link SourceSettingsCache}.
     */
    static SourceSettingsCache getInstance() {
        return SourceSettingsCacheImpl.getInstance();
    }

    /**
     * <p>
     * Fetches source setting for the given source id.
     * </p>
     *
     * @param sourceId Represents the source id.
     * @return The {@link SourceSettings}
     */
    SourceSettings getDefaultSourceSettings(final Long sourceId);

    /**
     * <p>
     * Fetches the source settings grouped by source id for the given organization id
     * </p>
     *
     * @param orgId Represents an organization id
     * @return The {@link Map} containing key as org id value as  {@link SourceSettings}
     */
    Map<Long, SourceSettings> getOrgSourceSettings(final Long orgId);

    /**
     * <p>
     * Sets the default source settings details into cache.
     * </p>
     *
     * @param sourceSettings Represents the  {@link SourceSettings}
     */
    void setDefaultSourceSettings(final SourceSettings sourceSettings);

    /**
     * <p>
     * Sets the organization source settings details into cache
     * </p>
     *
     * @param sourceSettings Represents the  {@link SourceSettings}
     */
    void setOrgSourceSettings(final SourceSettings sourceSettings);
}