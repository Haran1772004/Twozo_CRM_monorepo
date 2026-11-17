package com.tlc.crm.fields.service;

import com.tlc.crm.fields.models.FieldStore;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.fields.service.internal.FieldStoreServiceImpl;

import java.util.Collection;

/**
 * <p>
 * Provides an abstract way to deal with {@link FieldStore}
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public interface FieldStoreService {

    /**
     * <p>
     * Gets the {@link FieldStoreService} instance.
     * </p>
     *
     * @return {@link FieldStoreService}.
     */
    static FieldStoreService getInstance() {
        return FieldStoreServiceImpl.getInstance();
    }

    /**
     * <p>
     * Fetches the field store details.
     * </p>
     *
     * @param id Represents the field store id.
     * @return {@link FieldStore}
     */
    FieldStore getFieldStoreById(final Long id);

    /**
     * <p>
     * Fetches the field store details for the given collection of field store ids.
     * </p>
     *
     * @param ids Represents the collection of field store ids.
     * @return {@link Collection} of {@link FieldStore}
     */
    Collection<FieldStore> getFieldStoresByIds(final Collection<Long> ids);

    /**
     * <p>
     * Fetches the field store given by source name and storetype
     * </p>
     *
     * @param sourceName Represents the source name
     * @param type       Represents the store type
     * @return {@link FieldStore}
     */
    FieldStore getFieldStoreByType(final String sourceName, final StoreType type);

    /**
     * <p>
     * Fetches the field store given by source name, store type, and persistence flag.
     * Use isPersistent=true for custom field (wide table) stores, false for default field (legacy table) stores.
     * </p>
     *
     * @param sourceName   Represents the source name
     * @param type         Represents the store type
     * @param isPersistent Whether to fetch the persistent or non-persistent store variant
     * @return {@link FieldStore}
     */
    FieldStore getFieldStoreByType(final String sourceName, final StoreType type, final boolean isPersistent);
}