package com.tlc.crm.common.cache;

import com.tlc.crm.common.cache.internal.FieldPropertiesCacheImpl;
import com.tlc.validator.TlcModel;

import java.lang.reflect.Field;

import java.util.Collection;

/**
 * <p>
 * Stores the meta data of the field.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface FieldPropertiesCache extends LocalCache {

    static FieldPropertiesCache getInstance() {
        return FieldPropertiesCacheImpl.getInstance();
    }

    /**
     * <p>
     * Fetches the declared {@link Field} of the given class.
     * </p>
     *
     * @param modelClass Holds the metadata of the class.
     * @return Collection of {@link Field} holds the metadata of the fields.
     */
    Collection<Field> getFields(final Class<? extends TlcModel> modelClass);
}
