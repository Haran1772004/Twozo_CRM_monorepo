package com.tlc.crm.common.service.impl;

import com.tlc.crm.common.resolver.PropertyDescriptorResolver;
import com.tlc.crm.common.service.PropertyDescriptorService;

import java.beans.PropertyDescriptor;

/**
 * <p>
 * Provides the service to retrieves the property descriptor of the field.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class PropertyDescriptorServiceImpl implements PropertyDescriptorService {

    private final PropertyDescriptorResolver descriptorResolver;

    private static class Instance {
        private static final PropertyDescriptorServiceImpl INSTANCE = new PropertyDescriptorServiceImpl();
    }

    public static PropertyDescriptorServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    private PropertyDescriptorServiceImpl() {
        descriptorResolver = PropertyDescriptorResolver.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param fieldName  Represents the name of the field
     * @param modelClass Represents the class of the field
     * @return {@link PropertyDescriptor} holds the details of the property descriptor
     */
    @Override
    public PropertyDescriptor get(final String fieldName, final Class<?> modelClass) {
        return descriptorResolver.get(fieldName, modelClass);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PropertyDescriptor get(final String fieldName, final Class<?> modelClass, final String readMethodName) {
        return descriptorResolver.get(fieldName, modelClass, readMethodName);
    }

    /**
     * {@inheritDoc}
     *
     * @param fieldName       Represents the name of the field
     * @param modelClass      Represents the class of the field
     * @param readMethodName  Refers the read method name of the field
     * @param writeMethodName Refers the write method name of the field
     * @return {@link PropertyDescriptor} holds the details of the property descriptor
     */
    @Override
    public PropertyDescriptor get(final String fieldName, final Class<?> modelClass, final String readMethodName, final String writeMethodName) {
        return descriptorResolver.get(fieldName, modelClass, readMethodName, writeMethodName);
    }
}
