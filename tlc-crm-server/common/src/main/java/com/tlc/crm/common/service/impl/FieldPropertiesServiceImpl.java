package com.tlc.crm.common.service.impl;

import com.tlc.crm.common.resolver.FieldPropertiesResolver;
import com.tlc.crm.common.service.FieldPropertiesService;

import com.tlc.validator.TlcModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>
 * Provides the meta data of the field.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class FieldPropertiesServiceImpl implements FieldPropertiesService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FieldPropertiesServiceImpl.class);

    private final FieldPropertiesResolver fieldPropertiesResolver;

    private static class Instance {
        private static final FieldPropertiesServiceImpl INSTANCE = new FieldPropertiesServiceImpl();
    }

    /**
     * <p>
     * Gets the {@link FieldPropertiesServiceImpl}.
     * </p>
     *
     * @return {@link FieldPropertiesServiceImpl}.
     */
    public static FieldPropertiesServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    private FieldPropertiesServiceImpl() {
        fieldPropertiesResolver = FieldPropertiesResolver.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Field> getFields(final Class<? extends TlcModel> modelClass) {
        return fieldPropertiesResolver.getFields(modelClass);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Field> getAnnotatedFields(final Class<? extends TlcModel> modelClass, final Class<? extends Annotation> annotationClass) {
        final Collection<Field> fields = getFields(modelClass);

        return fields.stream().filter(field -> field.isAnnotationPresent(annotationClass)).collect(Collectors.toSet());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Field> getFieldsByType(final Class<? extends TlcModel> modelClass, final Class<?> typeClass) {
        final Collection<Field> fields = getFields(modelClass);

        return fields.stream().filter(field -> Objects.equals(field.getType(), typeClass)).collect(Collectors.toSet());
    }

    /**
     * <p>
     * Provides the fields of the given names if matched with the type class.
     * </p>
     *
     * @param modelClass Holds the metadata of the class.
     * @param typeClass  Represents the type of the class.
     * @param fieldNames Field names.
     * @return Registered {@link Annotation} annotation of the field.
     */
    @Override
    public Collection<Field> getFieldsByTypeAndName(final Class<? extends TlcModel> modelClass, final Class<?> typeClass, final Collection<String> fieldNames) {
        final Collection<Field> existingFields = getFields(modelClass);
        final Collection<Field> fields = existingFields.stream().filter(field ->
                fieldNames.contains(field.getName()) && typeClass == field.getType()).collect(Collectors.toSet());

        if (fields.isEmpty()) {
            LOGGER.debug("Fields fetch: {} model does not have any fields matching the given type: {}, fields: {}", modelClass.getName(), typeClass.getName(), fieldNames);
        }

        return fields;
    }
}
