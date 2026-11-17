package com.tlc.crm.common.cache;

import com.tlc.crm.common.cache.internal.PropertyDescriptorCacheImpl;

import java.beans.PropertyDescriptor;

/**
 * <p>
 * Stores the property descriptor of the field.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface PropertyDescriptorCache extends LocalCache {

    static PropertyDescriptorCache getInstance() {
        return PropertyDescriptorCacheImpl.getInstance();
    }

    /**
     * <p>
     * Fetches the {@link PropertyDescriptor} by using field and class.
     * </p>
     *
     * @param fieldName  Represents the name of the field.
     * @param modelClass Represents the class of the field.
     * @return {@link PropertyDescriptor} holds the details of the property descriptor.
     */
    PropertyDescriptor get(final String fieldName, final Class<?> modelClass);
}
