package com.tlc.crm.entity.validator;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.StringUtil;
import com.tlc.crm.common.service.FieldPropertiesService;
import com.tlc.crm.common.service.PropertyDescriptorService;
import com.tlc.crm.entity.internal.status.EntityErrorCodes;
import com.tlc.crm.entity.models.Entity;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.PresetConfig;
import com.tlc.crm.entity.models.ValueHolder;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.entity.service.OptionService;
import com.tlc.crm.entity.validator.annotation.Field;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.validator.TlcModel;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * Validates the default fields of {@link Entity}
 *
 * @author Sundar
 * version 1.0
 */
public abstract class DefaultValidator extends EntityValidator {

    private final OptionService optionService = OptionService.getInstance();
    private final PropertyDescriptorService propertyDescriptorService;
    private final FieldPropertiesService fieldPropertiesService;

    protected DefaultValidator() {
        this.propertyDescriptorService = PropertyDescriptorService.getInstance();
        this.fieldPropertiesService = FieldPropertiesService.getInstance();
    }

    /**
     * Validates the default fields value
     *
     * @param validatorContext which contains the error flag and validation context which contains the error flag and validation context
     * @param entity           Entity for which the field values has to be validated
     * @param fields           which contains the default fields of the entity
     */
    protected void validate(final ValidatorContext validatorContext, final Entity entity, final Map<String, FieldInfo> fields) {
        final Class<? extends Entity> entityClass = entity.getClass();
        final Collection<java.lang.reflect.Field> fieldList = fieldPropertiesService.getFields(entityClass);

        fieldList.forEach(field -> {
            final Field fieldAnnotation = field.getAnnotation(Field.class);

            if (Objects.nonNull(fieldAnnotation)) {
                final boolean autoValidate = fieldAnnotation.autoValidate();
                final FieldInfo fieldInfo = fields.get(fieldAnnotation.aliasName());

                try {
                    if (Objects.nonNull(fieldInfo) && autoValidate) {
                        final String fieldName = field.getName();
                        final PropertyDescriptor propertyDescriptor = propertyDescriptorService.get(fieldName, entityClass);
                        final Object value = propertyDescriptor.getReadMethod().invoke(entity);
                        final PresetConfig presetConfig = fieldInfo.presetConfig();
                        final boolean required = presetConfig.immutable() || presetConfig.required();
                        final boolean isValueNull = Objects.isNull(value);

                        if (isValueNull) {
                            if (required) {
                                addConstraintViolationMessage(validatorContext, String.format("%s is required",
                                        StringUtil.capitalizeFirstLetter(fieldName).orElseGet(() -> null)), fieldName);
                            } else {
                                validateByStoreType(fieldInfo, false, null, validatorContext, propertyDescriptor.getWriteMethod(), entity);
                            }
                        } else {
                            if (value instanceof final Option option) {
                                validateDropdownOption(fieldInfo, option, validatorContext);
                            } else if (value instanceof final TlcModel tlcModel) {
                                validateTlcModel(fieldInfo, tlcModel, validatorContext, presetConfig);
                            } else if (value instanceof final Collection<?> collection && doesContainTlcModel(collection)) {
                                validateTlcModels(fieldInfo, collection, validatorContext, presetConfig);
                            } else if (value instanceof final Map<?, ?> valueMap) {
                                final Collection<?> values = valueMap.values();

                                if (doesContainTlcModel(values)) {
                                    validateTlcModels(fieldInfo, values, validatorContext, presetConfig);
                                } else {
                                    validateByStoreType(fieldInfo, required, values, validatorContext, propertyDescriptor.getWriteMethod(), entity);
                                }
                            } else {
                                validateByStoreType(fieldInfo, required, value, validatorContext, propertyDescriptor.getWriteMethod(), entity);
                            }
                        }
                    }
                } catch (InvocationTargetException | IllegalAccessException e) {
                    throw ErrorCode.get(EntityErrorCodes.DATA_VALIDATION_FAILED);
                }
            }
        });
    }

    /**
     * Disables the default constraint violation of the {@link ValidatorContext}
     *
     * @param validatorContext which contains the error flag and validation context which contains the error flag and validation context
     */
    protected void disableDefaultConstraintViolation(final ValidatorContext validatorContext) {
        if (validatorContext.isErrorsFound()) {
            validatorContext.getContext().disableDefaultConstraintViolation();
        }
    }

    /**
     * <p>
     * Validates territory field.
     * </p>
     *
     * @param territoryField   Territory's {@link FieldInfo}.
     * @param isTerritoryNull  Flag indicating whether territory's null.
     * @param validatorContext {@link ValidatorContext}.
     */
    protected void validateTerritoryField(final FieldInfo territoryField, final boolean isTerritoryNull,
                                          final ValidatorContext validatorContext) {
        if (Objects.nonNull(territoryField) && (territoryField.presetConfig().required() && isTerritoryNull)) {
            addConstraintViolationMessage(validatorContext, "i18n.entity.field.territory.required", territoryField.aliasName());
        }
    }

    /**
     * Validates the entity based store type
     *
     * @param fieldInfo        Represents the field info
     * @param entity           Represents the entity
     * @param validatorContext Represents the validator context
     * @param presetConfig     Represents the preset config
     */
    protected void validateEntity(final FieldInfo fieldInfo, final Entity entity, final ValidatorContext validatorContext,
                                  final PresetConfig presetConfig) {
        final Long entityId = entity.id();

        if (Objects.isNull(entityId)) {
            validateTlcModel(fieldInfo, entity, validatorContext, presetConfig);
        } else {
            validateEntityType(entity.orgId(), entityId, fieldInfo, validatorContext);
        }
    }

    /**
     * Validates the value by {@link StoreType}
     *
     * @param field            Represents the field related information.
     * @param required         Flag to indicate whether the field value is required or not
     * @param value            to be validated
     * @param validatorContext which contains the error flag and validation context which contains the error flag and validation context
     * @param method           which invokes the setter method
     * @param entity           Entity for which the contact values has to be validated
     * @throws InvocationTargetException if any error raised while invoking a method
     * @throws IllegalAccessException    if this Method object is enforcing Java language access control and the underlying method is inaccessible
     */
    private void validateByStoreType(final FieldInfo field, final boolean required, final Object value,
                                     final ValidatorContext validatorContext, final Method method, final Entity entity)
            throws InvocationTargetException, IllegalAccessException {
        final ValueHolder valueHolder = Objects.isNull(entity.getValueHolder()) ? new ValueHolder() : entity.getValueHolder();
        final PresetConfig presetConfig = field.presetConfig();
        final StoreType storeType = field.storeType();
        final Object defaultValue = presetConfig.defaultValue();
        final boolean isValueNull = Objects.isNull(value);
        final boolean needDefaultValue = isValueNull && Objects.nonNull(defaultValue);

        if (needDefaultValue) {
            switch (storeType) {
                case SINGLE_STRING, TEXT, SINGLE_NUMBER -> method.invoke(entity, defaultValue);
                case DROPDOWN -> {
                    final Option defaultOption = optionService.getDefaultOption(entity.orgId(), field);

                    if (Objects.nonNull(defaultOption)) {
                        method.invoke(entity, defaultOption);
                        valueHolder.put(field.id(), defaultOption.id());
                    }
                }
                case MULTI_SELECT -> {
                    final Long orgId = entity.orgId();
                    final Collection<Option> defaultOptions = optionService.getDefaultOptions(orgId, field);

                    if (Objects.nonNull(defaultOptions)) {
                        method.invoke(entity, defaultOptions);
                    }
                }
            }
        } else {
            if (!isValueNull) {
                switch (storeType) {
                    case SINGLE_STRING -> validateStringField(field, required, presetConfig, value, validatorContext, true);
                    case TEXT -> validateStringField(field, required, presetConfig, value, validatorContext, false);
                    case SINGLE_NUMBER, OWNER -> validateNumberField(field, required, value, validatorContext, presetConfig);
                    case MULTI_SELECT -> validateMultiSelectField(entity.orgId(), field, required, value, validatorContext);
                    case DROPDOWN -> validateDropdownOption(field, value, validatorContext);
                    case DATE, TIME, DATE_TIME -> validateTemporalField(field, value, validatorContext, presetConfig);
                }
            }
        }
    }

    /**
     * Checks whether the {@link Collection} contains the {@link TlcModel} or not
     *
     * @param value collection be validated
     * @return true if the value represents the collection of {@link TlcModel}
     */
    private boolean doesContainTlcModel(final Collection<?> value) {
        return !value.isEmpty() && value.stream().filter(Objects::nonNull).anyMatch(TlcModel.class::isInstance);
    }

    /**
     * <p>
     * Validated the created time field value
     * </p>
     *
     * @param entity            Represents the {@link Entity}
     * @param validatorContext  Represents the {@link ValidatorContext}
     * @param createdTimeField  Represent the created time {@link FieldInfo}
     */
    protected void validateCreatedTime(final Entity entity, final ValidatorContext validatorContext, final FieldInfo createdTimeField) {
        final DateTime createdTime = entity.getCreatedTime();

        if (Objects.nonNull(createdTime) && createdTime.getTimeStamp() > Instant.now().toEpochMilli()) {
            addConstraintViolationMessage(validatorContext, "i18n.entity.field.created.invalid", createdTimeField.aliasName());
        }
    }
}