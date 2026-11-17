package com.tlc.crm.zapier.internal.cache;

import com.tlc.crm.common.cache.LocalCache;

import com.tlc.crm.zapier.api.models.ZapierField;

import java.util.Collection;

/**
 * Provides an abstract way to stores the {@link ZapierField} into cache.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public interface ZapierFieldCache extends LocalCache {

    /**
     * Retrieves the instance of {@link ZapierFieldCache}.
     *
     * @return The {@link ZapierFieldCache} instance.
     */
    static ZapierFieldCache getInstance() {
        return ZapierFieldCacheImpl.getInstance();
    }

    /**
     * Retrieves a collection of ZapierField objects associated with a specific action ID.
     *
     * @param id The action ID for which the fields are to be retrieved.
     * @return A collection of ZapierField objects for the specified action ID.
     */
    Collection<ZapierField> getFieldsByAction(final Short id);

    /**
     * Sets or updates a collection of ZapierField for a specific action ID.
     *
     * @param id     The action ID to which the fields are associated.
     * @param fields A collection of ZapierField objects to set or update for the given action ID.
     */
    void setFields(final Short id, final Collection<ZapierField> fields);
}
