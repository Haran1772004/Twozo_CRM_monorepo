package com.tlc.crm.common.cache.internal;

import com.tlc.cache.Cache;
import com.tlc.cache.CacheConfig;
import com.tlc.cache.CacheManager;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.crm.common.cache.FieldPropertiesCache;
import com.tlc.validator.TlcModel;

import java.lang.reflect.Field;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * Stores the meta data of the field.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class FieldPropertiesCacheImpl implements FieldPropertiesCache {

    private final Cache<Class<? extends TlcModel>, Collection<Field>> fieldPropertiesCache;

    private static class Instance {
        private static final FieldPropertiesCacheImpl INSTANCE = new FieldPropertiesCacheImpl();
    }

    public static FieldPropertiesCacheImpl getInstance() {
        return Instance.INSTANCE;
    }

    private FieldPropertiesCacheImpl() {
        final CacheConfig<Class<? extends TlcModel>, Collection<Field>> fieldPropertiesCacheConfig = new CacheConfig<>();

        fieldPropertiesCacheConfig.setExpireAfterAccess(TimeUnit.MINUTES.toMillis(60));
        fieldPropertiesCache = CacheManager.getInstance().createCache(fieldPropertiesCacheConfig);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Field> getFields(final Class<? extends TlcModel> modelClass) {
        return fieldPropertiesCache.containsKey(modelClass) ? Collections.unmodifiableCollection(fieldPropertiesCache.get(modelClass)) :
                Collections.emptyList();
    }

    /**
     * <p>
     * Adds the {@link Field} of the model class into the cache.
     * </p>
     *
     * @param modelClass Holds the details of the class.
     * @param fields     {@link Field} holds the metadata of the fields.
     */
    public void add(final Class<? extends TlcModel> modelClass, final Collection<Field> fields) {
        fieldPropertiesCache.put(modelClass, fields);
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
        fieldPropertiesCache.clear();
    }
}
