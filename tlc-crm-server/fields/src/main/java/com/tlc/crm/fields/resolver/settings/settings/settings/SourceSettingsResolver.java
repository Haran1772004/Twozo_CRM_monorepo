package com.tlc.crm.fields.resolver.settings;

import com.tlc.crm.fields.models.settings.SourceSettings;
import com.tlc.crm.fields.resolver.internal.SourceSettingsResolverImpl;

import java.util.Map;

/**
 * A resolver that resolves the {@link SourceSettings} related details.
 *
 * @author Kishore J
 * @version 1.0
 */
public interface SourceSettingsResolver {

    /**
     * <p>
     * Gets the instance of {@link SourceSettingsResolver}.
     * </p>
     *
     * @return {@link SourceSettingsResolver}.
     */
    static SourceSettingsResolver getInstance() {
        return SourceSettingsResolverImpl.getInstance();
    }

    /**
     * <p>
     * Retrieves the default source setting for the given source ID. If the source settings are not cached,
     * fetches them from the database, caches the data, and returns the source settings from the cache.
     * </p>
     *
     * @param sourceId Represents the source ID
     * @return The {@link SourceSettings}
     */
    SourceSettings getDefaultSourceSettings(final Long sourceId);

    /**
     * <p>
     * Retrieves the org source settings grouped by their source id for the given organization.
     * If the source settings are not cached, fetches them from the database, caches the data,
     * and returns the source settings from the cache.
     * </p>
     *
     * @param orgId Represents an organization ID
     * @return The {@link Map} containing key as source ID value as {@link SourceSettings}
     */
    Map<Long, SourceSettings> getOrgSourceSettings(final Long orgId);
}