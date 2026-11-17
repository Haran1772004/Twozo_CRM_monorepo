package com.tlc.crm.core.api.analytics.cache.util;

/**
 * Utility for composing and decomposing org-scoped cache keys.
 * <p>
 * All org-dependent remote caches use composed keys of the form {@code "orgId:entityId"}
 * to ensure strict tenant isolation in a multi-tenant environment.
 * </p>
 *
 * @author Karthick V
 * @version 1.0
 */
public final class CacheKeyUtil {

    private static final char SEPARATOR = ':';

    private CacheKeyUtil() {
    }

    /**
     * Composes a cache key from orgId and entityId.
     *
     * @param orgId    the organization identifier
     * @param entityId the entity identifier (fieldId, moduleId, etc.)
     * @return composed key in the format {@code "orgId:entityId"}
     */
    public static String compose(final Long orgId, final Number entityId) {
        return orgId + String.valueOf(SEPARATOR) + entityId;
    }
}

