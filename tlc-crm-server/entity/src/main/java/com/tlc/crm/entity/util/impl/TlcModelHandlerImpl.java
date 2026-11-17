package com.tlc.crm.entity.util.impl;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.entity.internal.status.EntityErrorCodes;
import com.tlc.crm.entity.models.Entity;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.ValueHolder;
import com.tlc.crm.common.service.FieldPropertiesService;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.common.service.PropertyDescriptorService;
import com.tlc.crm.entity.util.ValidatorUtil;
import com.tlc.crm.entity.util.TlcModelHandler;
import com.tlc.validator.TlcModel;

import java.beans.PropertyDescriptor;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Provides the implementations for the {@link TlcModelHandler} service for handling one or two TLCModels
 *
 * @author sundar
 * @version 1.0
 * @see TlcModelHandler
 */
public class TlcModelHandlerImpl implements TlcModelHandler {

    private static final String ID_KEY = "id";
    private static final String ORG_ID_KEY = "orgId";
    private static final String ID_PROPERTY_SETTER_METHOD_NAME = "setId";
    private static final String ORG_ID_PROPERTY_SETTER_METHOD_NAME = "setOrgId";

    private final FieldService fieldService;
    private final FieldPropertiesService fieldPropertiesService;
    private final PropertyDescriptorService propertyDescriptorService;

    private final Map<Class<? extends TlcModel>, Collection<String>> groupedFieldNames;
    private final Map<Class<? extends TlcModel>, Map<String, String>> groupedAliasNames;

    private static class Instance {
        private static final TlcModelHandler TLC_MODEL_HANDLER = new TlcModelHandlerImpl();
    }

    private TlcModelHandlerImpl() {
        this.fieldService = FieldService.getInstance();
        this.fieldPropertiesService = FieldPropertiesService.getInstance();
        this.propertyDescriptorService = PropertyDescriptorService.getInstance();
        this.groupedFieldNames = new ConcurrentHashMap<>();
        this.groupedAliasNames = new ConcurrentHashMap<>();
    }

    /**
     * Gets the {@link TlcModelHandler} instance
     *
     * @return {@link TlcModelHandler}
     */
    public static TlcModelHandler getInstance() {
        return Instance.TLC_MODEL_HANDLER;
    }

    /**
     * {@inheritDoc}
     *
     * @param primaryModel    Represents the primary model
     * @param secondaryModel  Represents the secondary model
     * @param isPartialUpdate A flag indicating whether the update action should be performed partially or not
     * @return {@link Collection} of strings represents the deferred value field name/ID
     */
    @Override
    public Collection<String> fetchUpdatedValueFields(final TlcModel primaryModel, final TlcModel secondaryModel, final boolean isPartialUpdate) {
        final Collection<String> changedFields = new HashSet<>();
        final Class<? extends TlcModel> modelClass = primaryModel.getClass();
        final BiConsumer<String, Function<String, String>> modifiedFieldValueLoader = (fieldName, fieldReferenceFetcher) -> {
            final Method readMethod = fetchPropertyDescriptor(modelClass, fieldName).getReadMethod();
            final Object primaryValue = readValue(readMethod, primaryModel);
            final Object secondaryValue = readValue(readMethod, secondaryModel);
            final String fieldKey = fieldReferenceFetcher.apply(fieldName);

            addModifiedValueField(primaryValue, secondaryValue, changedFields, fieldKey, isPartialUpdate);
        };

        if (primaryModel instanceof final Entity primaryEntity && secondaryModel instanceof final Entity secondaryEntity) {
            final ValueHolder primaryValueHolder = primaryEntity.getValueHolder();
            final ValueHolder secondaryValueHolder = secondaryEntity.getValueHolder();
            final String entityType = primaryEntity.getEntityType();
            final Collection<FieldInfo> fieldInfoCollection = fieldService.getFieldsBySource(primaryEntity.orgId(), entityType);
            final Map<String, String> fieldAliasNames = getFieldAliasNames(modelClass);
            final Map<String, FieldInfo> groupedDefaultFieldsByAliasName = fieldService.groupDefaultFieldsByAliasName(entityType);

            for (final FieldInfo field : fieldInfoCollection) {

                if (!field.isPersistent()) {
                    continue;
                }
                final Long fieldId = field.id();

                addModifiedValueField(primaryValueHolder.get(fieldId), secondaryValueHolder.get(fieldId),
                        changedFields, field.idAsString(), isPartialUpdate);
            }

            for (final Map.Entry<String, String> fieldAliasEntry : fieldAliasNames.entrySet()) {
                modifiedFieldValueLoader.accept(fieldAliasEntry.getKey(), fieldName ->
                        groupedDefaultFieldsByAliasName.get(fieldAliasNames.get(fieldName)).idAsString());
            }
        } else {

            for (final String fieldName : getFieldNames(modelClass)) {
                modifiedFieldValueLoader.accept(fieldName, Function.identity());
            }
        }
        return changedFields;
    }

    /**
     * {@inheritDoc}
     *
     * @param currentModel    The model containing values fetched from the database.
     * @param comparableModel The model containing values equal to those in the currentModel or
     *                        representing the state before any updates.
     * @param previousModel   The model holding previously stored values,
     *                        to be merged with the currentModel if it shares similar values
     *                        with the comparableModel for a specific field.
     */
    @Override
    public void compareAndMerge(final TlcModel currentModel, final TlcModel comparableModel, final TlcModel previousModel) {
        if (currentModel instanceof final Entity primaryEntity && comparableModel instanceof final Entity comparableEntity
                && previousModel instanceof final Entity secondaryEntity) {
            compareAndMergeValueHolders(primaryEntity, comparableEntity, secondaryEntity);
        }
        final Collection<String> fieldNames = getFieldNames(currentModel.getClass());

        if (fieldNames.isEmpty()) {
            return;
        }
        final Class<? extends TlcModel> entityClass = currentModel.getClass();

        for (final String fieldName : fieldNames) {

            try {
                final PropertyDescriptor propertyDescriptor = propertyDescriptorService.get(fieldName, entityClass);

                if (Objects.nonNull(propertyDescriptor)) {
                    final Method readMethod = propertyDescriptor.getReadMethod();
                    final Object currentEntityValue = readValue(readMethod, currentModel);
                    final Object previousEntityValue = readValue(readMethod, previousModel);
                    final Object comparableEntityValue = readValue(readMethod, comparableModel);

                    if (Objects.equals(currentEntityValue, previousEntityValue)) {
                        continue;
                    }

                    if (Objects.nonNull(comparableEntityValue) && Objects.isNull(previousEntityValue) ) {
                        final Field declaredField = entityClass.getDeclaredField(fieldName);

                        declaredField.setAccessible(true);
                        declaredField.set(currentModel, null);
                    }

                    if (ensureMergeEligibility(comparableEntityValue, currentEntityValue, previousEntityValue)) {
                        propertyDescriptor.getWriteMethod().invoke(currentModel, previousEntityValue);
                    }
                }
            } catch (InvocationTargetException | IllegalAccessException | NoSuchFieldException e) {
                throw ErrorCode.get(EntityErrorCodes.ENTITY_FIELD_ACCESS_ERROR);
            }
        }
    }

    /**
     * Reads the value from the given model
     *
     * @param readMethod Represents the {@link Method} to be used for reading the value from the provided model
     * @param model      Represents the model from which the value needs to be read
     * @return the value read from the provided model
     */
    private Object readValue(final Method readMethod, final TlcModel model) {
        try {
            final Object value = readMethod.invoke(model);

            return (value instanceof final String stringValue && stringValue.isEmpty()) ? null : value;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw ErrorCode.get(EntityErrorCodes.ENTITY_FIELD_ACCESS_ERROR);
        }
    }

    /**
     * Retrieves the field alias names grouped by their corresponding field names
     *
     * @param modelClass Represents the class from which the fields will be obtained
     * @return A {@link Map} that contains the field names and their corresponding grouped alias names
     */
    private Map<String, String> getFieldAliasNames(final Class<? extends TlcModel> modelClass) {
        return groupedAliasNames.computeIfAbsent(modelClass, modelClassKey -> {
            final Class<com.tlc.crm.entity.validator.annotation.Field> fieldAnnotationClass = com.tlc.crm.entity.validator.annotation.Field.class;

            return getFields(modelClass).stream().filter(field -> field.isAnnotationPresent(fieldAnnotationClass))
                    .collect(Collectors.toMap(Field::getName, field -> field.getAnnotation(fieldAnnotationClass).aliasName()));
        });
    }

    /**
     * Gets the {@link Collection} of fields of the provided class
     *
     * @param modelClass Represents the class from which the fields will be obtained
     * @return {@link Collection} of fields
     */
    private Collection<Field> getFields(final Class<? extends TlcModel> modelClass) {
        final Collection<Field> fields = new HashSet<>();

        for (final Field field : fieldPropertiesService.getFields(modelClass)) {

            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            fields.add(field);
        }
        return fields;
    }

    /**
     * Gets the collection of instance field names of provided entity
     *
     * @param modelClass Represents the entity class
     * @return {@link Collection} of field names that contains null values
     */
    private Collection<String> getFieldNames(final Class<? extends TlcModel> modelClass) {
        return groupedFieldNames.computeIfAbsent(modelClass, modelClassKey -> {
            final Collection<String> fieldNames = new HashSet<>();

            for (final java.lang.reflect.Field field : getFields(modelClass)) {

                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                fieldNames.add(field.getName());
            }
            return fieldNames;
        });
    }

    /**
     * The primary and secondary values of the provided field identifier are compared. If the values vary, the field identifier is added into the field identifier collection
     *
     * @param primaryValue     Represents the primary value
     * @param secondaryValue   Represents the secondary value
     * @param fieldIdentifiers Which contains the collection of modified values field identifiers
     * @param fieldIdentifier  Represents the field identifier
     */
    private void addModifiedValueField(final Object primaryValue, final Object secondaryValue, final Collection<String> fieldIdentifiers,
                                       final String fieldIdentifier, final boolean isPartialUpdate) {
        final boolean secondaryValueIsNull = Objects.isNull(secondaryValue);

        if ((isPartialUpdate && secondaryValueIsNull) || (secondaryValueIsNull && Objects.isNull(primaryValue))) {
            return;
        }

        if (!ValidatorUtil.equals(primaryValue, secondaryValue)) {
            fieldIdentifiers.add(fieldIdentifier);
        }
    }

    /**
     * <p>
     * Compares the {@link ValueHolder} of the primaryEntity with the comparableEntity.
     * If both entities have the same values in a field in their respective value holders,
     * the primaryValueHolder is updated with the value from the secondaryEntity's {@link ValueHolder} for that field.
     * If they have different values, the primaryEntity's {@link ValueHolder} retains its own value for the field.
     * </p>
     *
     * @param primaryEntity    Represents the model that contains the values from DB
     * @param comparableEntity Represents the model that contains the values presents inside the Excel sheet
     * @param secondaryEntity  Represents the model that contains the previously contained values
     */
    private void compareAndMergeValueHolders(final Entity primaryEntity, final Entity comparableEntity, final Entity secondaryEntity) {
        final ValueHolder secondaryValueHolder = secondaryEntity.getValueHolder();

        if (Objects.nonNull(secondaryValueHolder)) {
            final ValueHolder existingValueHolder = primaryEntity.getValueHolder();
            final boolean existingValueHolderIsNull = Objects.isNull(existingValueHolder);
            final ValueHolder primaryValueHolder = existingValueHolderIsNull ? new ValueHolder() : existingValueHolder;
            final ValueHolder comparableValueHolder = comparableEntity.getValueHolder();

            for (final FieldInfo field : fieldService.getFieldsBySource(primaryEntity.orgId(), primaryEntity.getEntityType())) {

                if (!field.isPersistent()) {
                    continue;
                }
                final Long fieldId = field.id();
                final Object secondaryEntityValue = secondaryValueHolder.get(fieldId);
                final Object currentEntityValue = comparableValueHolder.get(fieldId);

                if (Objects.nonNull(currentEntityValue) && Objects.isNull(secondaryEntityValue)) {
                    primaryValueHolder.remove(fieldId);
                }

                if (ensureMergeEligibility(currentEntityValue, primaryValueHolder.get(fieldId), secondaryEntityValue)) {
                    primaryValueHolder.put(fieldId, secondaryEntityValue);
                }
            }

            if (existingValueHolderIsNull) {
                primaryEntity.setValueHolder(primaryValueHolder);
            }
        }
    }

    /**
     * <p>
     * Checks if the secondary value can replace the primary value of the main {@link TlcModel}.
     * This is valid if the primary value and the comparable value are equal, and the primary
     * value is not the same as the secondary value.
     * </p>
     *
     * @param comparableValue The value for comparison
     * @param currentValue    The current value of the main Model
     * @param previousValue   The previous value for potential replacement
     * @return True if replacement is eligible, otherwise false
     */
    private boolean ensureMergeEligibility(final Object comparableValue, final Object currentValue, final Object previousValue) {

        if (ValidatorUtil.equals(currentValue, comparableValue)) {
            return !ValidatorUtil.equals(currentValue, previousValue);
        } else if (currentValue instanceof final Collection<?> currentCollection
                && comparableValue instanceof final Collection<?> comparableCollection
                && previousValue instanceof final Collection<?> previousCollection) {

            return ValidatorUtil.equals(comparableCollection, currentCollection) && !ValidatorUtil.equals(previousCollection, currentCollection);
        }
        return false;
    }

    /**
     * Fetches the {@link PropertyDescriptor} for the specified class and field from the groupedPropertyDescriptors.
     * If the descriptor is not already present, it will be loaded and then returned
     *
     * @param entityClass The class that contains the desired property
     * @param fieldName   The name of the field to be fetched
     * @return The {@link PropertyDescriptor} associated with the specified field
     */
    private PropertyDescriptor fetchPropertyDescriptor(final Class<? extends TlcModel> entityClass, final String fieldName) {
        final PropertyDescriptor propertyDescriptor;

        if (Objects.equals(ID_KEY, fieldName)) {
            propertyDescriptor = propertyDescriptorService.get(fieldName, entityClass, ID_KEY, ID_PROPERTY_SETTER_METHOD_NAME);
        } else if (Objects.equals(ORG_ID_KEY, fieldName)) {
            propertyDescriptor = propertyDescriptorService.get(fieldName, entityClass, ORG_ID_KEY, ORG_ID_PROPERTY_SETTER_METHOD_NAME);
        } else {
            propertyDescriptor = propertyDescriptorService.get(fieldName, entityClass);
        }
        return propertyDescriptor;
    }
}