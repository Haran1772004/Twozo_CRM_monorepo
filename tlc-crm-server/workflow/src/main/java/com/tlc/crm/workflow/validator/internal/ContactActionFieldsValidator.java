package com.tlc.crm.workflow.validator.internal;

import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.validator.ValidatorContext;

import com.tlc.crm.workflow.api.model.TriggerActionEvent;
import com.tlc.crm.workflow.field.WorkflowAttributes;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Validates the contact action source fields entity and returns appropriate message.
 *
 * @author KarthickV
 * @version 1.0
 */
public final class ContactActionFieldsValidator extends WorkflowValidatorImpl {

    private static final String CONTACT_CREATE = "contactCreate";
    private static final String CONTACT_UPDATE = "contactUpdate";
    private static final String CONTACT = "Contact";
    private static final String EMAILS = "emails";
    private static final String PHONES = "phones";
    private final FieldService fieldService;

    public ContactActionFieldsValidator() {
        this.fieldService = FieldService.getInstance();
    }

    private static class Instance {
        private static final ContactActionFieldsValidator INSTANCE = new ContactActionFieldsValidator();
    }

    /**
     * Gets the {@link ContactActionFieldsValidator} instance.
     *
     * @return {@link ContactActionFieldsValidator}.
     */
    public static ContactActionFieldsValidator getInstance() {
        return ContactActionFieldsValidator.Instance.INSTANCE;
    }

    /**
     * <p>
     * Validates the fieldMapping {@link JsonArray} to ensure that required fields have non-empty values
     * </p>
     *
     * @param fieldMappingArray    The {@link JsonArray} containing field mappings to validate
     * @param actionEventAliasName Represents the alias name for  {@link TriggerActionEvent}
     * @param validatorContext     The {@link ValidatorContext} for adding constraint violation messages.
     */
    public void validateContactActionFields(final JsonArray fieldMappingArray, final String actionEventAliasName,
                                            final ValidatorContext validatorContext) {

        switch (actionEventAliasName) {
            case CONTACT_CREATE -> contactCreateValidation(fieldMappingArray, validatorContext);
            case CONTACT_UPDATE -> contactUpdateValidation(fieldMappingArray, validatorContext);
        }
    }

    /**
     * <p>
     * Validates contact create event based on field mapping array and validator context.
     * </p>
     *
     * @param fieldMappingArray The {@link JsonArray} containing field mappings to validate
     * @param validatorContext  The {@link ValidatorContext} for adding constraint violation messages.
     */
    private void contactCreateValidation(final JsonArray fieldMappingArray, final ValidatorContext validatorContext) {
        final int fieldMappingArraySize = fieldMappingArray.size();
        final Map<Long, FieldInfo> fieldInfoMap = fieldService.groupDefaultFieldsByAliasName(CONTACT, List.of(EMAILS, PHONES))
                .values().stream().collect(Collectors.toMap(FieldInfo::id, fieldInfo -> fieldInfo));
        boolean hasRequiredFieldValues = false;

        outerLoop:
        for (int fieldMappingArrayIndex = 0; fieldMappingArrayIndex < fieldMappingArraySize; fieldMappingArrayIndex++) {
            final JsonArray fieldArray = fieldMappingArray.getJsonArray(fieldMappingArrayIndex);
            final int fieldArraySize = fieldArray.size();

            for (int fieldArrayIndex = 0; fieldArrayIndex < fieldArraySize; fieldArrayIndex++) {
                final JsonObject fieldJson = fieldArray.getJsonObject(fieldArrayIndex);
                final boolean hasValues = fieldJson.containsKey(WorkflowAttributes.VALUES) &&
                        fieldJson.getJsonArray(WorkflowAttributes.VALUES).size() > 0;

                if (fieldJson.optBoolean(WorkflowAttributes.IS_REQUIRED) && !hasValues) {
                    addConstraintViolationMessage(validatorContext, "i18n.workflow.action.field.value.invalid");
                    break outerLoop;
                }

                if (fieldInfoMap.containsKey(fieldJson.getLong(WorkflowAttributes.FIELD_ID)) && hasValues) {
                    hasRequiredFieldValues = true;
                }
            }
        }

        if (!hasRequiredFieldValues) {
            addConstraintViolationMessage(validatorContext, "i18n.workflow.action.contact.email.or.phone.required");
        }
    }

    /**
     * <p>
     * Validates contact update event based on field mapping array and validator context.
     * </p>
     *
     * @param fieldMappingArray The {@link JsonArray} containing field mappings to validate
     * @param validatorContext  The {@link ValidatorContext} for adding constraint violation messages.
     */
    private void contactUpdateValidation(final JsonArray fieldMappingArray, final ValidatorContext validatorContext) {
        final int fieldMappingArraySize = fieldMappingArray.size();

        outerLoop:
        for (int fieldMappingArrayIndex = 0; fieldMappingArrayIndex < fieldMappingArraySize; fieldMappingArrayIndex++) {
            final JsonArray fieldArray = fieldMappingArray.getJsonArray(fieldMappingArrayIndex);
            final int fieldArraySize = fieldArray.size();

            for (int fieldArrayIndex = 0; fieldArrayIndex < fieldArraySize; fieldArrayIndex++) {
                final JsonObject fieldJson = fieldArray.getJsonObject(fieldArrayIndex);

                if (fieldJson.optBoolean(WorkflowAttributes.IS_REQUIRED) && !(fieldJson.containsKey(WorkflowAttributes.VALUES) &&
                        fieldJson.getJsonArray(WorkflowAttributes.VALUES).size() > 0)) {
                    addConstraintViolationMessage(validatorContext, "i18n.workflow.action.field.value.invalid");
                    break outerLoop;
                }
            }
        }
    }
}
