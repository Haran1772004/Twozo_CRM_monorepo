package com.tlc.crm.common.service;

import com.tlc.crm.common.service.impl.PropertyDescriptorServiceImpl;

import java.beans.PropertyDescriptor;

/**
 * <p>
 * Provides the service to retrieves the property descriptor of the field.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface PropertyDescriptorService {

    static PropertyDescriptorService getInstance() {
        return PropertyDescriptorServiceImpl.getInstance();
    }

    /**
     * <p>
     * Fetches the {@link PropertyDescriptor} by using field and class
     * </p>
     *
     * @param fieldName  Represents the name of the field
     * @param modelClass Represents the class of the field
     * @return {@link PropertyDescriptor} holds the details of the property descriptor
     */
    PropertyDescriptor get(final String fieldName, final Class<?> modelClass);

    /**
     * <p>
     * Fetches the {@link PropertyDescriptor} by using field and class.
     * </p>
     *
     * @param fieldName      Represents the name of the field.
     * @param modelClass     Represents the class of the field.
     * @param readMethodName Refers the read method name of the field.
     * @return {@link PropertyDescriptor} holds the details of the property descriptor.
     */
    PropertyDescriptor get(final String fieldName, final Class<?> modelClass, final String readMethodName);

    /**
     * <p>
     * Fetches the {@link PropertyDescriptor} by using field and class
     * </p>
     *
     * @param fieldName       Represents the name of the field
     * @param modelClass      Represents the class of the field
     * @param readMethodName  Refers the read method name of the field
     * @param writeMethodName Refers the write method name of the field
     * @return {@link PropertyDescriptor} holds the details of the property descriptor.
     */
    PropertyDescriptor get(final String fieldName, final Class<?> modelClass, final String readMethodName, final String writeMethodName);
}
