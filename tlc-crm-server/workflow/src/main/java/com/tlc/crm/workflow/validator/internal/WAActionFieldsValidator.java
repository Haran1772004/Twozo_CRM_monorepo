package com.tlc.crm.workflow.validator.internal;

import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.entity.validator.ValidatorContext;
import com.tlc.crm.workflow.field.WorkflowAttributes;

/**
 * Validates the workflow action source fields for whatsapp send action
 *
 * @author Maharaja S
 * @version 1.0
 */
public final class WAActionFieldsValidator extends WorkflowValidatorImpl {

    public WAActionFieldsValidator() {
    }

    private static final class InstanceHolder {
        private static final WAActionFieldsValidator WA_ACTION_FIELDS_VALIDATOR = new WAActionFieldsValidator();
    }

    /**
     * Gets the {@link WAActionFieldsValidator} instance.
     *
     * @return {@link WAActionFieldsValidator}.
     */
    public static WAActionFieldsValidator getInstance() {
        return InstanceHolder.WA_ACTION_FIELDS_VALIDATOR;
    }

    /**
     * <p>
     * Validates the fieldMapping {@link JsonArray} to ensure that required fields have non-empty values
     * </p>
     *
     * @param fieldMappingArray The {@link JsonArray} containing field mappings to validate
     * @param validatorContext  The {@link ValidatorContext} for adding constraint violation messages.
     */
    public void validateWAActionFields(final JsonArray fieldMappingArray, final ValidatorContext validatorContext) {
        final int fieldMappingArraySize = fieldMappingArray.size();

        for (int fieldMappingArrayIndex = 0; fieldMappingArrayIndex < fieldMappingArraySize; fieldMappingArrayIndex++) {
            final JsonObject fieldJson = fieldMappingArray.getJsonObject(fieldMappingArrayIndex);
            final boolean hasValues = fieldJson.containsKey(WorkflowAttributes.VALUES) &&
                    fieldJson.getJsonArray(WorkflowAttributes.VALUES).size() > 0;

            if (fieldJson.optBoolean(WorkflowAttributes.IS_REQUIRED) && !hasValues) {
                addConstraintViolationMessage(validatorContext, "i18n.workflow.action.field.value.invalid");
                break;
            }
        }
    }
}