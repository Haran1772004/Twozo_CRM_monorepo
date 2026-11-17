package com.tlc.crm.common.service.impl;

import com.tlc.crm.common.resolver.MethodPropertiesResolver;
import com.tlc.crm.common.service.MethodPropertiesService;
import com.tlc.validator.TlcModel;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * <p>
 * Provides the meta data of the method
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public class MethodPropertiesServiceImpl implements MethodPropertiesService {

    private final MethodPropertiesResolver methodPropertiesResolver;

    private static class Instance {
        private static final MethodPropertiesServiceImpl INSTANCE = new MethodPropertiesServiceImpl();
    }

    private MethodPropertiesServiceImpl() {
        methodPropertiesResolver = MethodPropertiesResolver.getInstance();
    }

    /**
     * <p>
     * Gets the {@link MethodPropertiesServiceImpl} Instance
     * </p>
     *
     * @return {@link MethodPropertiesServiceImpl}
     */
    public static MethodPropertiesServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param modelClass      Holds the metadata of the class
     * @return Registered {@link Annotation} annotation of the method
     */
    @Override
    public Collection<Method> getMethods(final Class<? extends TlcModel> modelClass) {
        return methodPropertiesResolver.getMethods(modelClass);
    }

    /**
     * {@inheritDoc}
     *
     * @param annotationClass Holds the metadata of the annotation class
     * @return Registered {@link Annotation} annotation of the method
     */
    @Override
    public Collection<Method> getAnnotatedMethods(final Class<? extends TlcModel> modelClass, final Class<? extends Annotation> annotationClass) {
        final Collection<Method> methods = getMethods(modelClass);

        return methods.stream().filter(method -> method.isAnnotationPresent(annotationClass)).collect(Collectors.toSet());
    }
}

