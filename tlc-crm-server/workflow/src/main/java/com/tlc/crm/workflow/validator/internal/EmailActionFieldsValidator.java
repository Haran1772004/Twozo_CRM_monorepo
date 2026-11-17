package com.tlc.crm.workflow.validator.internal;

import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.entity.validator.ValidatorContext;

import com.tlc.crm.workflow.cache.service.WorkflowFieldService;
import com.tlc.crm.workflow.field.WorkflowAttributes;

/**
 * Validates the email action source fields entity and returns appropriate message.
 *
 * @author KarthickV
 * @version 1.0
 */
public final class EmailActionFieldsValidator extends WorkflowValidatorImpl {

    private static final String TO = "to";
    private static final String CC = "cc";
    private static final String BCC = "bcc";
    private final WorkflowFieldService workflowFieldService;

    public EmailActionFieldsValidator() {
        this.workflowFieldService = WorkflowFieldService.getInstance();
    }

    private static class Instance {
        private static final EmailActionFieldsValidator INSTANCE = new EmailActionFieldsValidator();
    }

    /**
     * Gets the {@link EmailActionFieldsValidator} instance.
     *
     * @return {@link EmailActionFieldsValidator}.
     */
    public static EmailActionFieldsValidator getInstance() {
        return EmailActionFieldsValidator.Instance.INSTANCE;
    }

    /**
     * <p>
     * Validates the fieldMapping {@link JsonArray} to ensure that required fields have non-empty values
     * </p>
     *
     * @param fieldMappingArray The {@link JsonArray} containing field mappings to validate
     * @param validatorContext  The {@link ValidatorContext} for adding constraint violation messages.
     */
    public void validateEmailActionFields(final JsonArray fieldMappingArray, final ValidatorContext validatorContext) {
        final int fieldMappingArraySize = fieldMappingArray.size();
        boolean hasRecipientsValue = false;

        for (int fieldMappingArrayIndex = 0; fieldMappingArrayIndex < fieldMappingArraySize; fieldMappingArrayIndex++) {
            final JsonObject fieldJson = fieldMappingArray.getJsonObject(fieldMappingArrayIndex);
            final boolean hasValues = fieldJson.containsKey(WorkflowAttributes.VALUES) &&
                    fieldJson.getJsonArray(WorkflowAttributes.VALUES).size() > 0;

            if (fieldJson.optBoolean(WorkflowAttributes.IS_REQUIRED) && !hasValues) {
                addConstraintViolationMessage(validatorContext, "i18n.workflow.action.field.value.invalid");
                break;
            }

            if (hasValues) {
                switch (workflowFieldService.get(fieldJson.getLong(WorkflowAttributes.FIELD_ID)).getAliasName()) {
                    case TO, BCC, CC -> hasRecipientsValue = true;
                }
            }
        }

        if (!hasRecipientsValue) {
            addConstraintViolationMessage(validatorContext, "i18n.workflow.action.field.value.invalid");
        }
    }
}
