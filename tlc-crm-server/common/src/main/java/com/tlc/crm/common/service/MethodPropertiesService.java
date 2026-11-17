package com.tlc.crm.common.service;

import com.tlc.crm.common.service.impl.MethodPropertiesServiceImpl;
import com.tlc.validator.TlcModel;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;

/**
 * <p>
 * Provides the meta data of the method
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public interface MethodPropertiesService {

    /**
     * <p>
     * Gets the {@link MethodPropertiesService} Instance
     * </p>
     *
     * @return {@link MethodPropertiesService}
     */
    static MethodPropertiesService getInstance() {
        return MethodPropertiesServiceImpl.getInstance();
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
     * Provides the annotation of the given annotation class and method
     * </p>
     *
     * @param modelClass      Holds the metadata of the class
     * @param annotationClass Holds the metadata of the annotation class
     * @return Collection of {@link Method} holds the details of the metadata of the methods
     */
    Collection<Method> getAnnotatedMethods(final Class<? extends TlcModel> modelClass, final Class<? extends Annotation> annotationClass);
}
