package com.tlc.crm.common.service;

import com.tlc.crm.common.service.impl.FieldPropertiesServiceImpl;
import com.tlc.validator.TlcModel;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import java.util.Collection;

/**
 * <p>
 * Provides the meta data of the field.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface FieldPropertiesService {

    static FieldPropertiesService getInstance() {
        return FieldPropertiesServiceImpl.getInstance();
    }

    /**
     * <p>
     * Provides the declared {@link Field} of the given class.
     * </p>
     *
     * @param modelClass Holds the metadata of the class.
     * @return Collection of {@link Field} holds the details of the metadata of the fields.
     */
    Collection<Field> getFields(final Class<? extends TlcModel> modelClass);

    /**
     * <p>
     * Provides the annotated {@link Field} of the given class.
     * </p>
     *
     * @param modelClass      Holds the metadata of the class.
     * @param annotationClass Holds the metadata of the annotation class.
     * @return Collection of {@link Field} holds the details of the metadata of the fields.
     */
    Collection<Field> getAnnotatedFields(final Class<? extends TlcModel> modelClass, final Class<? extends Annotation> annotationClass);

    /**
     * <p>
     * Provides the fields of the given class that matches with the type given.
     * </p>
     *
     * @param modelClass Holds the metadata of the class.
     * @param typeClass  Represents the type of the class.
     * @return Registered {@link Annotation} annotation of the field.
     */
    Collection<Field> getFieldsByType(final Class<? extends TlcModel> modelClass, final Class<?> typeClass);

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
    Collection<Field> getFieldsByTypeAndName(final Class<? extends TlcModel> modelClass, final Class<?> typeClass, final Collection<String> fieldNames);
}
