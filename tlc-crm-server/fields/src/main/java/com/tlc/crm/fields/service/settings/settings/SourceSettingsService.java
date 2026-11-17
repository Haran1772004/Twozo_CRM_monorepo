package com.tlc.crm.fields.service.settings;

import com.tlc.crm.fields.models.settings.SourceSettings;
import com.tlc.crm.fields.service.internal.SourceSettingsServiceImpl;

import java.util.Collection;
import java.util.Map;

/**
 * An interface which provides an abstract way to deal with {@link SourceSettings}
 *
 * @author Kishore J
 * @version 1.0
 */
public interface SourceSettingsService {

    /**
     * Gets the instance of {@link SourceSettingsService}.
     *
     * @return {@link SourceSettingsService}.
     */
    static SourceSettingsService getInstance() {
        return SourceSettingsServiceImpl.getInstance();
    }

    /**
     * <p>
     * Gets all the source settings of an organization.
     * </p>
     *
     * @param orgId Refers the organization id.
     * @return {@link Collection} of {@link SourceSettings}.
     */
    Collection<SourceSettings> getAllSourceSettings(final Long orgId);

    /**
     * <p>
     * Gets a collection of common source settings of an organization.
     * </p>
     *
     * @param orgId Represents an organization id.
     * @return {@link Collection} of {@link SourceSettings}.
     */
    Collection<SourceSettings> getCommonSourceSettings(final Long orgId);

    /**
     * <p>
     * Gets a collection of import source settings of an organization.
     * </p>
     *
     * @param orgId Represents an organization id.
     * @return {@link Collection} of {@link SourceSettings}.
     */
    Collection<SourceSettings> getImportSourceSettings(final Long orgId);

    /**
     * <p>
     * Gets the {@link SourceSettings} of an organization by source id.
     * </p>
     *
     * @param orgId    Refers the organization id.
     * @param sourceId Refers the source id.
     * @return The source settings details.
     */
    SourceSettings getSourceSettings(final Long orgId, final Long sourceId);

    /**
     * <p>
     * Gets the {@link SourceSettings} of an organization by source id.
     * </p>
     *
     * @param orgId  Refers the organization id.
     * @param source Refers the source name.
     * @return The source settings details.
     */
    SourceSettings getSourceSettingsBySource(final Long orgId, final String source);

    /**
     * <p>
     * Checks whether the entity source of an organization is enabled or not
     * </p>
     *
     * @param orgId  Refers the organization id.
     * @param source Refers the source name.
     * @return True if enabled.
     */
    boolean isSourceEnabled(final Long orgId, final String source);

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