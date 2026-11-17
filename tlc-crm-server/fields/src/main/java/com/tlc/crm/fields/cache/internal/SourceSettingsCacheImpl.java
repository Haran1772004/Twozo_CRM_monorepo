package com.tlc.crm.fields.cache.internal;

import com.tlc.cache.Cache;
import com.tlc.cache.CacheManager;
import com.tlc.cache.remote.CacheConfig;
import com.tlc.cache.service.provider.CacheServiceProvider;
import com.tlc.crm.fields.cache.settings.SourceSettingsCache;
import com.tlc.crm.fields.manager.SourceSettingsManager;
import com.tlc.crm.fields.models.settings.SourceSettings;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Stores the source settings into the cache.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class SourceSettingsCacheImpl implements SourceSettingsCache {

    private final Cache<Long, SourceSettings> defaultSourceSettingsCache;
    private final Cache<Long, Map<Long, SourceSettings>> orgSourceSettingsCache;
    private final SourceSettingsManager sourceSettingsManager;

    private static class Instance {
        private static final SourceSettingsCacheImpl INSTANCE = new SourceSettingsCacheImpl();
    }

    private SourceSettingsCacheImpl() {
        final CacheConfig<Long, Map<Long, SourceSettings>> orgSourceSettingsConfig = new CacheConfig<>("orgSourceSettingsConfig");

        orgSourceSettingsConfig.setTtl((int) TimeUnit.MINUTES.toMillis(1));
        this.defaultSourceSettingsCache = CacheManager.getInstance().createCache();
        this.orgSourceSettingsCache = CacheServiceProvider.get().createOrOpenCache(orgSourceSettingsConfig);
        this.sourceSettingsManager = SourceSettingsManager.getInstance();
    }

    /**
     * Gets the {@link SourceSettingsCacheImpl} instance.
     *
     * @return The {@link SourceSettingsCacheImpl} instance.
     */
    public static SourceSettingsCacheImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void load() {
        final Collection<SourceSettings> sourceSettings = sourceSettingsManager.getAllSourceSettings();

        if (!sourceSettings.isEmpty()) {
            defaultSourceSettingsCache.putAll(groupedDefaultSourceSettings(sourceSettings));
        }
    }

    /**
     * <p>
     * Clears the default source settings cache
     * </p>
     */
    public void clearDefaultSourceSettings() {
        defaultSourceSettingsCache.clear();
    }

    /**
     * {@inheritDoc}
     *
     * @param sourceId Represents the source id
     * @return The {@link SourceSettings}
     */
    @Override
    public SourceSettings getDefaultSourceSettings(final Long sourceId) {
        return defaultSourceSettingsCache.get(sourceId);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents an organization id
     * @return The {@link Map} containing key as source id value as {@link SourceSettings}
     */
    public Map<Long, SourceSettings> getOrgSourceSettings(final Long orgId) {
        return orgSourceSettingsCache.get(orgId);
    }

    /**
     * {@inheritDoc}
     *
     * @param sourceSettings Represents {@link SourceSettings}
     */
    @Override
    public void setDefaultSourceSettings(final SourceSettings sourceSettings) {
        defaultSourceSettingsCache.put(sourceSettings.getSourceId(), sourceSettings);
    }

    /**
     * {@inheritDoc}
     *
     * @param sourceSettings Represents the  {@link SourceSettings}
     */
    @Override
    public void setOrgSourceSettings(final SourceSettings sourceSettings) {
        final Long orgId = sourceSettings.orgId();
        Map<Long, SourceSettings> sourceSettingsMap = orgSourceSettingsCache.computeIfAbsent(orgId, settingsMap -> new HashMap<>());

        if (Objects.nonNull(sourceSettingsMap) && !sourceSettingsMap.isEmpty()) {
            sourceSettingsMap.put(sourceSettings.getSourceId(), sourceSettings);
        } else {
            sourceSettingsMap = new HashMap<>();
            sourceSettingsMap.put(sourceSettings.getSourceId(), sourceSettings);
        }

        orgSourceSettingsCache.put(orgId, sourceSettingsMap);
    }

    /**
     * <p>
     * Groups a collection of source settings by their source IDs.
     * </p>
     *
     * @param sourceSettings Which contains the collection of {@link SourceSettings}
     * @return The {@link Map} containing key as source id vale as {@link SourceSettings}
     */
    private Map<Long, SourceSettings> groupedDefaultSourceSettings(final Collection<SourceSettings> sourceSettings) {
        final Map<Long, SourceSettings> sourceSettingsMap = new HashMap<>();

        for (final SourceSettings settings : sourceSettings) {
            sourceSettingsMap.put(settings.getSourceId(), settings);
        }

        return sourceSettingsMap;
    }
}