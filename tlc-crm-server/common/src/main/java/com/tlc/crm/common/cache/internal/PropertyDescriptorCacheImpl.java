package com.tlc.crm.common.cache.internal;

import com.tlc.cache.Cache;
import com.tlc.cache.CacheConfig;
import com.tlc.cache.CacheManager;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.crm.common.cache.PropertyDescriptorCache;

import java.beans.PropertyDescriptor;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * Stores the property descriptor of the field.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class PropertyDescriptorCacheImpl implements PropertyDescriptorCache {

    private final Cache<Class<?>, Map<String, PropertyDescriptor>> descriptorCache;

    private static class Instance {
        private static final PropertyDescriptorCacheImpl INSTANCE = new PropertyDescriptorCacheImpl();
    }

    public static PropertyDescriptorCacheImpl getInstance() {
        return Instance.INSTANCE;
    }

    private PropertyDescriptorCacheImpl() {
        final CacheConfig<Class<?>, Map<String, PropertyDescriptor>> descriptorCacheConfig = new CacheConfig<>();

        descriptorCacheConfig.setExpireAfterAccess(TimeUnit.MINUTES.toMillis(60));
        descriptorCache = CacheManager.getInstance().createCache();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PropertyDescriptor get(final String fieldName, final Class<?> modelClass) {
        final Map<String, PropertyDescriptor> descriptors = descriptorCache.get(modelClass);

        return Objects.isNull(descriptors) ? null : descriptors.get(fieldName);
    }

    /**
     * <p>
     * Adds the {@link PropertyDescriptor} into the cache.
     * </p>
     *
     * @param fieldName          Represents the name of the field.
     * @param modelClass         Represents the class of the field.
     * @param propertyDescriptor {@link PropertyDescriptor} holds the details of the property descriptor.
     */
    public void add(final String fieldName, final Class<?> modelClass, final PropertyDescriptor propertyDescriptor) {
        descriptorCache.computeIfAbsent(modelClass, descriptors -> new HashMap<>()).put(fieldName, propertyDescriptor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void load() {
        throw ErrorCode.get(ErrorCodes.NOT_SUPPORTED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        descriptorCache.clear();
    }
}
