package com.tlc.crm.common.cache;

import com.tlc.crm.common.cache.internal.MethodPropertiesCacheImpl;
import com.tlc.validator.TlcModel;

import java.lang.reflect.Method;

import java.util.Collection;

/**
 * <p>
 * Stores the meta data of the method
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public interface MethodPropertiesCache {

    /**
     * <p>
     * Gets the {@link MethodPropertiesCache} Instance
     * </p>
     *
     * @return {@link MethodPropertiesCache}
     */
    static MethodPropertiesCache getInstance() {
        return MethodPropertiesCacheImpl.getInstance();
    }

    /**
     * <p>
     * Fetches the annotated {@link Method} of the given class
     * </p>
     *
     * @param modelClass      Holds the metadata of the class
     * @return {@link Collection} {@link Method} holds the metadata of the methods
     */
    Collection<Method> getMethods(final Class<? extends TlcModel> modelClass);

    /**
     * <p>
     * Adds the {@link Method} of the model class into the cache
     * </p>
     *
     * @param modelClass Holds the details of the class
     * @param methods    {@link Method} holds the metadata of the methods
     */
    void add(final Class<? extends TlcModel> modelClass, final Collection<Method> methods);
}
