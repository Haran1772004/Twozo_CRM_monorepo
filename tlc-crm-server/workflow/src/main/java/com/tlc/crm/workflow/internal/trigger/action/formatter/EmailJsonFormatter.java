package com.tlc.crm.workflow.internal.trigger.action.formatter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.entity.models.type.Email;

import com.tlc.crm.workflow.api.model.TriggerActionSource;
import com.tlc.crm.workflow.api.model.WorkflowField;
import com.tlc.crm.workflow.internal.trigger.action.ActionExecutionContext;
import com.tlc.crm.workflow.internal.trigger.action.field.ActionAttributes;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Converting the JSON representation of action field properties to match
 * the JSON structure of mail entity.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class EmailJsonFormatter extends AbstractJsonFormatter {

    private final ContactManager contactManager;

    private EmailJsonFormatter() {
        this.contactManager = ContactManager.getInstance();
    }

    private static class Instance {
        private static final EmailJsonFormatter EMAIL_JSON_FORMATTER = new EmailJsonFormatter();
    }

    public static EmailJsonFormatter getInstance() {
        return Instance.EMAIL_JSON_FORMATTER;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId                  Refers the organization id.
     * @param workflowActionId       Refers the workflow action id.
     * @param fieldMappingJson       Refers the {@link JsonArray} which holds information about the data pertaining to actions.
     * @param actionExecutionContext Refers the {@link ActionExecutionContext}.
     * @param actionSource           Refers the {@code TriggerActionSource} for which the step options are retrieved.
     * @return The formatted {@link JsonObject}.
     */
    @Override
    public JsonObject format(final Long orgId, final Long workflowActionId, final JsonArray fieldMappingJson,
                             final ActionExecutionContext actionExecutionContext, final TriggerActionSource actionSource) {
        final JsonObject emailJson = Json.object();
        final Function<JsonArray, Object> emailJsonFetcher = (valueArray) -> {
            final JsonArray emails = Json.array();
            final JsonArray contactEmails = Json.array();
            final JsonObject emailAddressJson = Json.object();

            for (int emailIndex = 0; emailIndex < valueArray.size(); emailIndex++) {
                final JsonObject valueJson = valueArray.getJsonObject(emailIndex);

                if (valueJson.containsKey(ActionAttributes.DEPENDS_ON)) {
                    final Object value = getStepOptionValue(valueJson, actionSource, actionExecutionContext);

                    if (value instanceof final Email email) {
                        contactEmails.put(email.id());
                    } else {
                        emails.put(value);
                    }
                } else {
                    final String emailAddress = valueJson.getString(ActionAttributes.VALUE);
                    final Map<String, Long> existingContactEmailIds = contactManager.getContactEmailIds(orgId, List.of(emailAddress));

                    if (existingContactEmailIds.containsKey(emailAddress)) {
                        contactEmails.put(existingContactEmailIds.get(emailAddress));
                    } else {
                        emails.put(emailAddress);
                    }
                }
            }

            if (contactEmails.size() > 0) {
                emailAddressJson.put(ActionAttributes.CONTACT_EMAILS, contactEmails);
            }

            if (emails.size() > 0) {
                emailAddressJson.put(ActionAttributes.EMAILS, emails);
            }

            return emailAddressJson.size() > 0 ? emailAddressJson : null;
        };

        for (int fieldIndex = 0; fieldIndex < fieldMappingJson.size(); fieldIndex++) {
            final JsonObject fieldObject = fieldMappingJson.getJsonObject(fieldIndex);
            final JsonArray valueArray = fieldObject.optJsonArray(ActionAttributes.VALUES);
            final WorkflowField workflowField = workflowFieldService.get(fieldObject.getLong(ActionAttributes.FIELD_ID));

            switch (workflowField.getAliasName()) {
                case "to" -> emailJson.put(ActionAttributes.TO_ADDRESS, emailJsonFetcher.apply(valueArray));
                case "cc" -> emailJson.put(ActionAttributes.CC_ADDRESS, emailJsonFetcher.apply(valueArray));
                case "bcc" -> emailJson.put(ActionAttributes.BCC_ADDRESS, emailJsonFetcher.apply(valueArray));
                case "subject" ->
                        emailJson.put(ActionAttributes.SUBJECT, valueArray.getJsonObject(0).get(ActionAttributes.VALUE));
                case "body" ->
                        emailJson.put(ActionAttributes.MESSAGE, valueArray.getJsonObject(0).get(ActionAttributes.VALUE));
                case "trackEmails" ->
                        emailJson.put(ActionAttributes.EMAIL_TRACKER_ENABLED, valueArray.getJsonObject(0).get(ActionAttributes.VALUE));
            }
        }

        return emailJson;
    }
}
