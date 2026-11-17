package com.tlc.crm.fields.resolver.internal;

import com.tlc.crm.fields.cache.settings.SourceSettingsCache;
import com.tlc.crm.fields.manager.SourceSettingsManager;
import com.tlc.crm.fields.models.settings.SourceSettings;
import com.tlc.crm.fields.resolver.settings.SourceSettingsResolver;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * Resolves the {@link SourceSettings} related details.
 *
 * @author Kishore J
 * @version 1.0
 * @see SourceSettingsResolver
 */
public class SourceSettingsResolverImpl implements SourceSettingsResolver {

    private final SourceSettingsCache sourceSettingsCache;
    private final SourceSettingsManager sourceSettingsManager;

    private SourceSettingsResolverImpl() {
        sourceSettingsCache = SourceSettingsCache.getInstance();
        sourceSettingsManager = SourceSettingsManager.getInstance();
    }

    private static class Instance {
        private static final SourceSettingsResolverImpl INSTANCE = new SourceSettingsResolverImpl();
    }

    /**
     * <p>
     * Gets the {@link SourceSettingsResolverImpl} instance.
     * </p>
     *
     * @return {@link SourceSettingsResolverImpl}.
     */
    public static SourceSettingsResolverImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param sourceId Represents the source ID
     * @return The {@link SourceSettings}
     */
    @Override
    public SourceSettings getDefaultSourceSettings(final Long sourceId) {
        final SourceSettings existingSourceSettings = sourceSettingsCache.getDefaultSourceSettings(sourceId);

        if (Objects.isNull(existingSourceSettings)) {
            final SourceSettings sourceSettings = sourceSettingsManager.getDefaultSourceSettings(sourceId);

            sourceSettingsCache.setDefaultSourceSettings(sourceSettings);

            return sourceSettings;
        }

        return existingSourceSettings;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents an organization ID
     * @return The {@link Map} containing key as source ID value as {@link SourceSettings}
     */
    public Map<Long, SourceSettings> getOrgSourceSettings(final Long orgId) {
        final Map<Long, SourceSettings> existingSourceSettings = sourceSettingsCache.getOrgSourceSettings(orgId);

        if (Objects.isNull(existingSourceSettings)) {
            final Collection<SourceSettings> sourceSettings = sourceSettingsManager.getOrgSourceSettings(orgId);

            if (Objects.nonNull(sourceSettings) && !sourceSettings.isEmpty()) {

                for (final SourceSettings sourceSetting : sourceSettings) {
                    sourceSettingsCache.setOrgSourceSettings(sourceSetting);
                }
            }

            return sourceSettingsCache.getOrgSourceSettings(orgId);
        }

        return existingSourceSettings;
    }

    /**
     * <p>
     * Adds the {@link SourceSettings} details into cache.
     * </p>
     *
     * @param sourceSettings Refers the {@link SourceSettings} details.
     */
    public void addSourceSettings(final SourceSettings sourceSettings) {
        loadSourceSettings(sourceSettings);
    }

    /**
     * <p>
     * Loads the {@link SourceSettings} details into cache.
     * </p>
     *
     * @param sourceSettings Refers the {@link SourceSettings} details.
     */
    private void loadSourceSettings(final SourceSettings sourceSettings) {
        final Long orgId = sourceSettings.orgId();
        final Long sourceId = sourceSettings.getSourceId();
        final SourceSettings settings = getDefaultSourceSettings(sourceId);

        if (Objects.nonNull(orgId) && Objects.nonNull(settings)) {
            final String config = settings.getConfig();

            if (Objects.isNull(sourceSettings.getConfig()) && Objects.nonNull(config)) {
                sourceSettings.setConfig(config);
            }
            sourceSettingsCache.setOrgSourceSettings(sourceSettings);
        }
    }
}