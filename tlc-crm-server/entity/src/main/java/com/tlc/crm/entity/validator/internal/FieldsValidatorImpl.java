package com.tlc.crm.entity.validator.internal;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.entity.models.Entity;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.PresetConfig;
import com.tlc.crm.entity.models.ValueHolder;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.validator.EntityValidator;
import com.tlc.crm.entity.validator.ValidatorContext;
import com.tlc.crm.entity.validator.annotation.FieldsValidator;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.validator.TlcModel;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Collection;
import java.util.Objects;

/**
 * It validates each and every entity available for the CRM entity and returns appropriate message
 *
 * @author Selvakumar G
 * @version 1.0
 */
public class FieldsValidatorImpl extends EntityValidator implements ConstraintValidator<FieldsValidator, Entity> {

    private final FieldService fieldService = FieldService.getSecureInstance(true);

    /**
     * Checks whether the all the entity are having the valid values or not
     *
     * @param entity  Entity for which the field values has to be validated
     * @param context Validation context
     * @return If the given entity is valid, it will return true
     */
    @Override
    public boolean isValid(final Entity entity, final ConstraintValidatorContext context) {
        final ValueHolder valueHolder = Objects.isNull(entity.getValueHolder()) ? new ValueHolder() : entity.getValueHolder();
        final Collection<FieldInfo> fields = fieldService.getFieldsBySource(entity.orgId(), entity.getEntityType());
        final ValidatorContext validatorContext = new ValidatorContext(context);

        if (Objects.isNull(fields) || fields.isEmpty()) {
            return true;
        }

        for (FieldInfo field : fields) {
            if (field.isActive() && field.isPersistent()) {
                final String fieldName = field.fieldName();

                try {
                    final Long fieldId = field.id();
                    final StoreType storeTypeId = field.storeType();
                    final PresetConfig presetConfig = field.presetConfig();
                    final Object value = getValueByStoreType(storeTypeId, valueHolder, fieldId);
                    final boolean isValueNull = Objects.isNull(value);
                    final boolean isRequired = presetConfig.required();

                    if (isValueNull) {
                        if (isRequired) {
                            addConstraintViolationMessage(validatorContext, String.format("%s is required", fieldName), fieldName);
                        }
                        valueHolder.put(fieldId, presetConfig.defaultValue());
                    } else {
                        final Long orgId = entity.orgId();

                        switch (storeTypeId) {
                            case SINGLE_STRING -> validateStringField(field, isRequired, presetConfig, value, validatorContext, true);
                            case MULTI_SELECT -> validateMultiSelectField(orgId, field, isRequired, value, validatorContext);
                            case TEXT -> validateStringField(field, isRequired, presetConfig, value, validatorContext, false);
                            case SINGLE_NUMBER -> validateNumberField(field, isRequired, value, validatorContext, presetConfig);
                            case DROPDOWN -> validateDropdownOption(field, value, validatorContext);
                            case PHONE, DATE_TIME_RANGE, DATE_RANGE, TIME_RANGE, ADDRESS, EMAIL, MONETARY ->
                                    validateTlcModel(field, (TlcModel) value, validatorContext, presetConfig);
                            case COMPANY, CONTACT -> validateEntityType(orgId, value, field, validatorContext);
                            case OWNER -> validateOwnerField(orgId, value, field, isRequired, validatorContext);
                            case DATE_TIME, DATE, TIME -> validateTemporalField(field, value, validatorContext, presetConfig);
                        }
                    }
                } catch (ErrorCode e) {
                    addConstraintViolationMessage(validatorContext, "Error occurred while validating field : "
                            + fieldName + ", error : " + e.getMessage(), fieldName);
                }
            }
        }

        if (validatorContext.isErrorsFound()) {
            validatorContext.getContext().disableDefaultConstraintViolation();
        }

        return !validatorContext.isErrorsFound();
    }

    /**
     * Gets the value from {@link ValueHolder} by store type
     *
     * @param storeType   Represents the field data type
     * @param valueHolder which contains the organisation dependent fields value
     * @param fieldId     Field ID
     * @return the value by store type
     */
    private Object getValueByStoreType(final StoreType storeType, final ValueHolder valueHolder, final Long fieldId) {
        return switch (storeType) {
            case SINGLE_STRING, TEXT, SINGLE_NUMBER, TIME, DATE, DROPDOWN, CONTACT, COMPANY, OWNER, PHONE, EMAIL,
                    ADDRESS, MONETARY, TIME_RANGE, DATE_RANGE, DATE_TIME_RANGE, MULTI_SELECT, DATE_TIME ->
                    valueHolder.get(fieldId);
            default -> null;
        };
    }
}
