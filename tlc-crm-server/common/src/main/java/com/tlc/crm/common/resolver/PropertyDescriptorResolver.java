package com.tlc.crm.common.resolver;

import com.tlc.crm.common.resolver.impl.PropertyDescriptorResolverImpl;

import java.beans.PropertyDescriptor;

/**
 * <p>
 * Resolves the property descriptor of the field.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface PropertyDescriptorResolver {

    static PropertyDescriptorResolver getInstance() {
        return PropertyDescriptorResolverImpl.getInstance();
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
     * @return {@link PropertyDescriptor} holds the details of the property descriptor
     */
    PropertyDescriptor get(final String fieldName, final Class<?> modelClass, final String readMethodName, final String writeMethodName);
}
