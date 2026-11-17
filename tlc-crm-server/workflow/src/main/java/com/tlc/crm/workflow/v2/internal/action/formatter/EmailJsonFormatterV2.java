package com.tlc.crm.workflow.v2.internal.action.formatter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.core.api.contact.manager.ContactManager;

import com.tlc.crm.entity.models.type.Email;

import com.tlc.crm.workflow.api.model.TriggerActionSource;
import com.tlc.crm.workflow.api.model.WorkflowField;
import com.tlc.crm.workflow.internal.trigger.action.field.ActionAttributes;
import com.tlc.crm.workflow.v2.field.WorkflowAttributesV2;
import com.tlc.crm.workflow.v2.internal.execution.NodeExecutionContext;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Formats the email action field mapping JSON for V2 execution.
 *
 * @author Thalaimalai Pandiyan T
 * @version 2.0
 */
public final class EmailJsonFormatterV2 extends AbstractJsonFormatterV2 {

    private final ContactManager contactManager;

    private EmailJsonFormatterV2() {
        this.contactManager = ContactManager.getInstance();
    }

    private static class Instance {
        private static final EmailJsonFormatterV2 EMAIL_JSON_FORMATTER = new EmailJsonFormatterV2();
    }

    public static EmailJsonFormatterV2 getInstance() {
        return Instance.EMAIL_JSON_FORMATTER;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId                Refers the organization id.
     * @param workflowActionId     Refers the workflow action id.
     * @param fieldMappingJson     Refers the {@link JsonArray} which holds information about the data pertaining to actions.
     * @param nodeExecutionContext Refers the {@link NodeExecutionContext}.
     * @param actionSource         Refers the {@code TriggerActionSource} for which the step options are retrieved.
     * @return The formatted {@link JsonObject}.
     */
    @Override
    public JsonObject format(final Long orgId, final Long workflowActionId, final JsonArray fieldMappingJson,
                             final NodeExecutionContext nodeExecutionContext, final TriggerActionSource actionSource) {
        final JsonObject emailJson = Json.object();
        final Function<JsonArray, Object> emailJsonFetcher = (valueArray) -> {
            final JsonArray emails = Json.array();
            final JsonArray contactEmails = Json.array();
            final JsonObject emailAddressJson = Json.object();

            for (int emailIndex = 0; emailIndex < valueArray.size(); emailIndex++) {
                final JsonObject valueJson = valueArray.getJsonObject(emailIndex);

                if (valueJson.containsKey(ActionAttributes.DEPENDS_ON)) {
                    final Object value = getStepOptionValue(valueJson, actionSource, nodeExecutionContext);

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
                case WorkflowAttributesV2.TO -> emailJson.put(ActionAttributes.TO_ADDRESS, emailJsonFetcher.apply(valueArray));
                case WorkflowAttributesV2.CC -> emailJson.put(ActionAttributes.CC_ADDRESS, emailJsonFetcher.apply(valueArray));
                case WorkflowAttributesV2.BCC -> emailJson.put(ActionAttributes.BCC_ADDRESS, emailJsonFetcher.apply(valueArray));
                case WorkflowAttributesV2.SUBJECT -> emailJson.put(ActionAttributes.SUBJECT, valueArray.getJsonObject(0).get(ActionAttributes.VALUE));
                case WorkflowAttributesV2.BODY -> emailJson.put(ActionAttributes.MESSAGE, valueArray.getJsonObject(0).get(ActionAttributes.VALUE));
                case WorkflowAttributesV2.TRACK_EMAILS -> emailJson.put(ActionAttributes.EMAIL_TRACKER_ENABLED, valueArray.getJsonObject(0).get(ActionAttributes.VALUE));
                case WorkflowAttributesV2.ATTACHMENTS -> {
                    final JsonArray attachmentIds = Json.array();

                    for (int index = 0; index < valueArray.size(); index++) {
                        final JsonObject valueJson = valueArray.getJsonObject(index);

                        attachmentIds.put(valueJson.getLong(ActionAttributes.VALUE));
                    }

                    if (attachmentIds.size() > 0) {
                        emailJson.put(ActionAttributes.ATTACHMENT_IDS, attachmentIds);
                    }
                }
            }
        }

        return emailJson;
    }
}
