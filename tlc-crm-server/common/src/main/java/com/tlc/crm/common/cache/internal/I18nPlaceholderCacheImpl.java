package com.tlc.crm.common.cache.internal;

import com.tlc.cache.Cache;
import com.tlc.cache.remote.CacheConfig;
import com.tlc.cache.service.provider.CacheServiceProvider;
import com.tlc.crm.common.cache.I18nPlaceholderCache;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * Implements the {@link I18nPlaceholderCache}
 * </p>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public class I18nPlaceholderCacheImpl implements I18nPlaceholderCache {

    private final Cache<String, String> i18nPlaceholderCache;

    private I18nPlaceholderCacheImpl() {
        final CacheConfig<String, String> cacheConfig = new CacheConfig<>("i18nPlaceholderCache");

        cacheConfig.setTtl((int) TimeUnit.DAYS.toMillis(2));
        i18nPlaceholderCache = CacheServiceProvider.get().createOrOpenCache(cacheConfig);
    }

    private static class Instance {
        private static final I18nPlaceholderCacheImpl I18N_PLACEHOLDER_CACHE = new I18nPlaceholderCacheImpl();
    }

    /**
     * <p>
     * Gets the {@link I18nPlaceholderCacheImpl} instance.
     * </p>
     *
     * @return {@link I18nPlaceholderCacheImpl}.
     */
    public static I18nPlaceholderCacheImpl getInstance() {
        return I18nPlaceholderCacheImpl.Instance.I18N_PLACEHOLDER_CACHE;
    }

    /**
     * {@inheritDoc}
     *
     * @param key  An i18n key.
     * @return The i18key value.
     */
    @Override
    public String get(final String key) {
        return i18nPlaceholderCache.get(key);
    }

    /**
     * {@inheritDoc}
     *
     * @param key    An i18n key.
     * @param value The  i18n key value
     */
    @Override
    public void add(final String key, final String value) {
        i18nPlaceholderCache.put(key, value);
    }

    /**
     * {@inheritDoc}
     *
     * @param key Represents the i18n key
     */
    @Override
    public void remove(final String key) {
        i18nPlaceholderCache.remove(key);
    }

    @Override
    public void load() {
    }
}