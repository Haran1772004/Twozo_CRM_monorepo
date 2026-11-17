package com.tlc.crm.common.resolver.impl;

import com.tlc.crm.common.cache.internal.PropertyDescriptorCacheImpl;
import com.tlc.crm.common.resolver.PropertyDescriptorResolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.util.Objects;

/**
 * <p>
 * Resolves the property descriptor of the field.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class PropertyDescriptorResolverImpl implements PropertyDescriptorResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(PropertyDescriptorResolverImpl.class);

    private final PropertyDescriptorCacheImpl descriptorCache;

    private static class Instance {
        private static final PropertyDescriptorResolverImpl INSTANCE = new PropertyDescriptorResolverImpl();
    }

    public static PropertyDescriptorResolverImpl getInstance() {
        return Instance.INSTANCE;
    }

    private PropertyDescriptorResolverImpl() {
        descriptorCache = PropertyDescriptorCacheImpl.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param fieldName  Represents the name of the field
     * @param modelClass Represents the class of the field
     * @return
     */
    public PropertyDescriptor get(final String fieldName, final Class<?> modelClass) {
        return get(fieldName, modelClass, null, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PropertyDescriptor get(final String fieldName, final Class<?> modelClass, final String readMethodName) {
        return get(fieldName, modelClass, readMethodName, null);
    }

    /**
     * {@inheritDoc}
     *
     * @param fieldName       Represents the name of the field
     * @param modelClass      Represents the class of the field
     * @param readMethodName  Refers the read method name of the field
     * @param writeMethodName Refers the write method name of the field
     * @return
     */
    public PropertyDescriptor get(final String fieldName, final Class<?> modelClass, final String readMethodName,
                                  final String writeMethodName) {
        final PropertyDescriptor existingDescriptor = descriptorCache.get(fieldName, modelClass);

        if (Objects.isNull(existingDescriptor)) {
            final PropertyDescriptor propertyDescriptor = getDescriptor(fieldName, modelClass, readMethodName, writeMethodName);

            if (Objects.nonNull(propertyDescriptor)) {
                descriptorCache.add(fieldName, modelClass, propertyDescriptor);
            }
            return propertyDescriptor;
        }

        return existingDescriptor;
    }

    /**
     * <p>
     * Retrieves the {@link PropertyDescriptor} associated with the given class and field, using the provided read and write method names if available.
     * If the read and write method names are not null, the {@link PropertyDescriptor} will be obtained using the default read and write methods.
     * </p>
     *
     * @param fieldName       Represents the name of the field to be fetched
     * @param modelClass      Represents the class that contains the desired property
     * @param readMethodName  Represents the read method name of the field
     * @param writeMethodName Represents the write method name of the field
     * @return The {@link PropertyDescriptor} associated with the specified field
     */
    private PropertyDescriptor getDescriptor(final String fieldName, final Class<?> modelClass, final String readMethodName,
                                             final String writeMethodName) {

        try {
            return Objects.isNull(readMethodName) && Objects.isNull(writeMethodName) ? new PropertyDescriptor(fieldName, modelClass)
                    : new PropertyDescriptor(fieldName, modelClass, readMethodName, writeMethodName);
        } catch (IntrospectionException e) {
            LOGGER.debug(String.format("Failed to create property descriptor. Class - %s, Field - %s", modelClass.getName(), fieldName), e);
        }
        return null;
    }
}
