package com.tlc.crm.common.cache;

import com.tlc.crm.common.cache.internal.I18nPlaceholderCacheImpl;

import java.util.Map;

/**
 * <p>
 * Stores the i18n place holder value in the cache
 * </p>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public interface I18nPlaceholderCache extends RemoteCache {

    /**
     * <p>
     * Gets the {@link I18nPlaceholderCache} instance.
     * </p>
     *
     * @return {@link I18nPlaceholderCache}.
     */
    static I18nPlaceholderCache get() {
        return I18nPlaceholderCacheImpl.getInstance();
    }

    /**
     * <p>
     * Gets the i18n value with key
     * </p>
     *
     * @param key  An i18n key.
     * @return The i18key value.
     */
    String get(final String key);

    /**
     * <p>
     * Adds the i18n key map with org Id
     * </p>
     *
     * @param key    An i18n key.
     * @param value The  i18n key value
     */
    void add(final String key, final String value);

    /**
     * <p>
     * Removes the i18n key from cache.
     * </p>
     *
     * @param key Represents the i18n key
     */
    void remove(final String key);
}