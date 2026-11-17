package com.tlc.crm.common.cache.internal;

import com.tlc.cache.Cache;
import com.tlc.cache.CacheManager;

import com.tlc.crm.common.cache.MethodPropertiesCache;
import com.tlc.validator.TlcModel;

import java.lang.reflect.Method;

import java.util.Collection;
import java.util.Collections;

/**
 * <p>
 * Stores the meta data of the method
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public class MethodPropertiesCacheImpl implements MethodPropertiesCache {

    private final Cache<Class<? extends TlcModel>, Collection<Method>> methodPropertiesCache;

    private static class Instance {
        private static final MethodPropertiesCacheImpl INSTANCE = new MethodPropertiesCacheImpl();
    }

    private MethodPropertiesCacheImpl() {
        methodPropertiesCache = CacheManager.getInstance().createCache();
    }

    /**
     * <p>
     * Gets the {@link MethodPropertiesCacheImpl} Instance
     * </p>
     *
     * @return {@link MethodPropertiesCacheImpl}
     */
    public static MethodPropertiesCacheImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param modelClass      Represents the model class
     * @return {@link Collection} {@link Method} holds the metadata of the methods
     */
    @Override
    public Collection<Method> getMethods(final Class<? extends TlcModel> modelClass) {
        return methodPropertiesCache.containsKey(modelClass) ? Collections.unmodifiableCollection(methodPropertiesCache.get(modelClass)) :
                Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     *
     * @param modelClass Holds the details of the class
     * @param methods    {@link Method} holds the metadata of the methods
     */
    public void add(final Class<? extends TlcModel> modelClass, final Collection<Method> methods) {
        methodPropertiesCache.put(modelClass, methods);
    }

    /**
     * <p>
     * Clears the method meta data from the cache
     * </p>
     */
    void clear() {
        methodPropertiesCache.clear();
    }
}
