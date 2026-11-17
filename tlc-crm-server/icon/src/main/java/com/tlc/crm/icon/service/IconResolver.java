package com.tlc.crm.icon.service;

import com.tlc.crm.icon.internal.resolver.IconResolverImpl;
import com.tlc.crm.icon.model.Icon;

import java.util.Map;

/**
 * <p>
 * A Resolver that resolves the {@link Icon} related details
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public interface IconResolver {

    /**
     * <p>
     * Gets the instance of {@link IconResolver}.
     * </p>
     *
     * @return {@link IconResolver}.
     */
    static IconResolver getInstance() {
        return IconResolverImpl.getInstance();
    }

    /**
     * <p>
     * Fetches all icons grouped by their IDs. If not cached, fetches data from the database,
     * caches it, and returns the icons from the cache.
     * </p>
     *
     * @return The {@link Map} containing key as icon ID value as {@link Icon}.
     */
    Map<Long, Icon> getAll();

    /**
     * <p>
     * Fetches icons are grouped by their IDs for the provided icon type. If not cached,
     * fetches data from the database, caches it, and returns the icons from the cache.
     * </p>
     *
     * @param iconType Represents the {@link Icon.Type}.
     * @return The {@link Map} containing key as icon ID value as {@link Icon}.
     */
    Map<Long, Icon> getIconsByType(final String iconType);
}