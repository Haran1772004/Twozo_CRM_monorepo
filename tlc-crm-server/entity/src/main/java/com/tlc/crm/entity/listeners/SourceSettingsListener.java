package com.tlc.crm.entity.listeners;

import com.tlc.crm.common.cache.I18nPlaceholderCache;
import com.tlc.crm.common.models.I18nPlaceholderResolverRegistry;
import com.tlc.crm.entity.cache.search.SearchIndexCache;
import com.tlc.crm.fields.manager.SourceSettingsManager;
import com.tlc.crm.fields.models.settings.SourceSettings;

import com.tlc.crm.fields.resolver.internal.SourceSettingsResolverImpl;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.listeners.CompleteRowListener;

import java.util.Collection;
import java.util.Objects;

/**
 * A Listener that listens create/update/ actions of {@link SourceSettings}.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class SourceSettingsListener implements CompleteRowListener {

    private final static String PLACEHOLDER_PATTERN = "%d_%s";

    private final SourceSettingsResolverImpl settingsResolver;
    private final SearchIndexCache searchIndexCache;
    private final I18nPlaceholderCache i18nPlaceholderCache;
    private final I18nPlaceholderResolverRegistry i18nPlaceholderResolverRegistry;

    private final static class InstanceHolder {
        private static final SourceSettingsListener INSTANCE = new SourceSettingsListener();
    }

    public static SourceSettingsListener getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private SourceSettingsListener() {
        this.settingsResolver = SourceSettingsResolverImpl.getInstance();
        searchIndexCache = SearchIndexCache.getInstance();
        i18nPlaceholderCache = I18nPlaceholderCache.get();
        i18nPlaceholderResolverRegistry = I18nPlaceholderResolverRegistry.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param rows Refers the collection of {@link SourceSettings} rows.
     */
    @Override
    public void addRows(final Collection<Row> rows) {
        addSourceSettingsToCache(rows);
    }

    /**
     * {@inheritDoc}
     *
     * @param rows Refers the collection of {@link SourceSettings} rows.
     */
    @Override
    public void updateRows(final Collection<Row> rows) {
        addSourceSettingsToCache(rows);
        updateI18nPlaceholderCache(rows);
    }

    /**
     * {@inheritDoc}
     *
     * @param rows Refers the collection of {@link SourceSettings} rows.
     */
    @Override
    public void deleteRows(final Collection<Row> rows) {

    }

    /**
     * Adds the collection of {@link SourceSettings} rows data into cache.
     *
     * @param rows Refers the collection of {@link SourceSettings} rows.
     */
    private void addSourceSettingsToCache(final Collection<Row> rows) {
        final SourceSettingsManager settingsManager = SourceSettingsManager.getInstance();

        if (Objects.nonNull(rows) && !rows.isEmpty()) {

            for (final Row row : rows) {
                final SourceSettings sourceSettings = settingsManager.convertRowToModel(row);

                settingsResolver.addSourceSettings(sourceSettings);

                i18nPlaceholderResolverRegistry.getPlaceholders().forEach(placeholder ->
                        i18nPlaceholderCache.remove(String.format(PLACEHOLDER_PATTERN, row.getOrgId(), placeholder)));

                searchIndexCache.addSourceSettings(sourceSettings);
            }
        }
    }

    /**
     * Updates the rows data into {@link I18nPlaceholderCache}.
     *
     * @param rows Refers the collection of rows.
     */
    private void updateI18nPlaceholderCache(final Collection<Row> rows) {

        rows.stream().findFirst().ifPresent(row -> {
            final Long orgId = row.getOrgId();

            i18nPlaceholderResolverRegistry.getPlaceholders().forEach(placeholder ->
                i18nPlaceholderCache.remove(String.format(PLACEHOLDER_PATTERN, orgId, placeholder)));
        });
    }
}
