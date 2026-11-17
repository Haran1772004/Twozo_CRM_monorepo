package com.tlc.crm.fields.service.internal;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.fields.models.FieldSource;
import com.tlc.crm.fields.models.settings.SourceSettings;
import com.tlc.crm.fields.resolver.settings.SourceSettingsResolver;
import com.tlc.crm.fields.service.FieldSourceService;
import com.tlc.crm.fields.service.settings.SourceSettingsService;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * Implements the {@link SourceSettingsService} provides functionality for accessing {@link SourceSettings}
 *
 * @author Kishore J
 * @version 1.0
 * @see SourceSettingsService
 */
public class SourceSettingsServiceImpl implements SourceSettingsService {

    private final SourceSettingsResolver sourceSettingResolver;

    private SourceSettingsServiceImpl() {
        sourceSettingResolver = SourceSettingsResolver.getInstance();
    }

    private static class Instance {
        private static final SourceSettingsServiceImpl INSTANCE = new SourceSettingsServiceImpl();
    }

    /**
     * Gets the instance of {@link SourceSettingsServiceImpl}.
     *
     * @return {@link SourceSettingsServiceImpl}.
     */
    public static SourceSettingsServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Refers the organization id.
     * @return {@link Collection} of {@link SourceSettings}.
     */
    @Override
    public Collection<SourceSettings> getAllSourceSettings(final Long orgId) {
        final Collection<FieldSource> sources = FieldSourceService.getInstance().getSources();

        return sources.stream().map(source -> getSourceSettings(orgId, source.id())).filter(Objects::nonNull).toList();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents an organization id.
     * @return {@link Collection} of {@link SourceSettings}.
     */
    @Override
    public Collection<SourceSettings> getCommonSourceSettings(final Long orgId) {
        final Collection<FieldSource> sources = FieldSourceService.getInstance().getSources();

        return sources.stream().filter(FieldSource::isCommon).map(source -> getSourceSettings(orgId, source.id()))
                .filter(Objects::nonNull).toList();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents an organization id.
     * @return {@link Collection} of {@link SourceSettings}.
     */
    @Override
    public Collection<SourceSettings> getImportSourceSettings(final Long orgId) {
        final Collection<FieldSource> sources = FieldSourceService.getInstance().getSources();

        return sources.stream().filter(FieldSource::isImportable).map(source -> getSourceSettings(orgId, source.id()))
                .filter(Objects::nonNull).toList();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Refers the organization id.
     * @param sourceId Refers the source id.
     * @return The source settings details.
     */
    @Override
    public SourceSettings getSourceSettings(final Long orgId, final Long sourceId) {
        final SourceSettings defaultSourceSettings = sourceSettingResolver.getDefaultSourceSettings(sourceId);
        final Map<Long, SourceSettings> orgSourceSettings = sourceSettingResolver.getOrgSourceSettings(orgId);

        if (Objects.nonNull(orgSourceSettings) && !orgSourceSettings.isEmpty()) {
            final SourceSettings orgDependentSourceSettings = orgSourceSettings.get(sourceId);

            if (Objects.nonNull(orgDependentSourceSettings)) {
                final String defaultConfig = defaultSourceSettings.getConfig();
                final String orgDependentConfig = orgDependentSourceSettings.getConfig();
                final JsonObject defaultConfigJson = Objects.nonNull(defaultConfig) ? Json.object(defaultConfig) : Json.object();
                final JsonObject orgConfigJson = Objects.nonNull(orgDependentConfig) ? Json.object(orgDependentConfig) : Json.object();

                orgConfigJson.forEach(defaultConfigJson::put);
                orgDependentSourceSettings.setConfig(defaultConfigJson.size() > 0 ? defaultConfigJson.toString() : null);

                return orgDependentSourceSettings;
            } else {
                return defaultSourceSettings;
            }
        } else {
            return defaultSourceSettings;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Refers the organization id.
     * @param source Refers the source name.
     * @return The source settings details.
     */
    @Override
    public SourceSettings getSourceSettingsBySource(final Long orgId, final String source) {
        return getSourceSettings(orgId, FieldSourceService.getInstance().getSource(source).id());
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Refers the organization id.
     * @param source Refers the source name.
     * @return True if enabled.
     */
    @Override
    public boolean isSourceEnabled(final Long orgId, final String source) {
        final SourceSettings sourceSettings = getSourceSettings(orgId, FieldSourceService.getInstance().getSource(source).id());

        return sourceSettings.isEnabled();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents an organization ID
     * @return The {@link Map} containing key as source ID value as {@link SourceSettings}
     */
    @Override
    public Map<Long, SourceSettings> getOrgSourceSettings(final Long orgId) {
        return sourceSettingResolver.getOrgSourceSettings(orgId);
    }
}
