package com.tlc.crm.icon.service;

import com.tlc.crm.icon.internal.cache.IconCacheImpl;
import com.tlc.crm.icon.model.Icon;

import java.util.Map;

/**
 * Provides an abstract way to deal with {@link Icon}
 *
 * @author kowsalya
 * @version 1.0
 */
public interface IconCache {

    /**
     * <p>
     * Gets the instance of {@link IconCache}
     * </p>
     *
     * @return {@link IconCache}
     */
    static IconCache getInstance() {
        return IconCacheImpl.getInstance();
    }

    /**
     * <p>
     * Fetches all the icons from cache
     * </p>
     *
     * @return The {@link Map} containing key as icon ID and value as {@link Icon}
     */
    Map<Long, Icon> getAllIcons();

    /**
     * <p>
     * Fetches the icons organized by their IDs for the given icon type
     * </p>
     *
     * @param iconType Represents the {@link Icon.Type}
     * @return The {@link Map} containing the key as icon ID and value as {@link Icon}
     */
    Map<Long, Icon> getIconsByType(final String iconType);

    /**
     * <p>
     * Sets the icon details into the cache.
     * </p>
     *
     * @param iconType Represents the {@link Icon.Type}
     * @param icon     Represents the {@link Icon}
     */
    void setIcon(final String iconType, final Icon icon);
}
