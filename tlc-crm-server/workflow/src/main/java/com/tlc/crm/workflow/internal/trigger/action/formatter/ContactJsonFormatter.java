package com.tlc.crm.workflow.internal.trigger.action.formatter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.util.NumberUtils;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.fields.models.StoreSubType;
import com.tlc.crm.entity.models.type.Email;
import com.tlc.crm.entity.models.type.Phone;
import com.tlc.crm.fields.models.FieldSubType;

import com.tlc.crm.workflow.api.model.TriggerActionSource;
import com.tlc.crm.workflow.internal.trigger.action.ActionExecutionContext;
import com.tlc.crm.workflow.internal.trigger.action.field.ActionAttributes;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.HashSet;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Converting the JSON representation of action field properties to match
 * the JSON structure of contact entity.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class ContactJsonFormatter extends AbstractJsonFormatter {

    private final EntityJsonAggregator contactJsonAggregator;

    private ContactJsonFormatter() {
        this.contactJsonAggregator = ContactJsonAggregator.getInstance();
    }

    private static class Instance {
        private static final ContactJsonFormatter CONTACT_JSON_FORMATTER = new ContactJsonFormatter();
    }

    /**
     * Gets the instance of {@link ContactJsonFormatter}.
     *
     * @return {@link ContactJsonFormatter} instance.
     */
    public static ContactJsonFormatter getInstance() {
        return Instance.CONTACT_JSON_FORMATTER;
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
    public JsonObject format(final Long orgId, final Long workflowActionId, final JsonArray fieldMappingJson, final ActionExecutionContext actionExecutionContext,
                             final TriggerActionSource actionSource) {
        final JsonObject contactJson = Json.object();
        final Collection<FieldInfo> fields = fieldService.getFieldsBySource(orgId, ActionAttributes.CONTACT);
        final Function<JsonArray, Object> valueRetriever = (jsonArray) -> {
            final JsonObject jsonObject = jsonArray.getJsonObject(0);

            if (jsonObject.containsKey(ActionAttributes.DEPENDS_ON)) {
                return getStepOptionValue(jsonObject, actionSource, actionExecutionContext);
            } else {
                return jsonObject.get(ActionAttributes.VALUE);
            }
        };

        for (int index = 0; index < fieldMappingJson.size(); index++) {
            final JsonArray fieldArray = fieldMappingJson.getJsonArray(index);

            for (int fieldIndex = 0; fieldIndex < fieldArray.size(); fieldIndex++) {
                final JsonObject fieldObject = fieldArray.getJsonObject(fieldIndex);
                final Optional<FieldInfo> optionalField = fields.stream().filter(field ->
                        Objects.equals(fieldObject.getLong(ActionAttributes.FIELD_ID), field.id())).findFirst();

                if (optionalField.isPresent()) {
                    final FieldInfo fieldInfo = optionalField.get();
                    final JsonArray valueArray = fieldObject.optJsonArray(ActionAttributes.VALUES);

                    if (fieldInfo.isPersistent()) {
                        loadPersistentFieldValue(orgId, fieldInfo, valueArray, contactJson);
                    } else {
                        switch (fieldInfo.aliasName()) {
                            case "id" -> contactJson.put(ActionAttributes.ID, valueRetriever.apply(valueArray));
                            case "firstName", "lastName" ->
                                    contactJson.put(fieldInfo.idAsString(), valueRetriever.apply(valueArray));
                            case "company", "salesOwner", "lifeCycleStage", "lifeCycleStatus", "contactLostReason" -> {
                                final Object value = valueRetriever.apply(valueArray);

                                if (Objects.nonNull(value)) {
                                    contactJson.put(fieldInfo.idAsString(), NumberUtils.parseNumber(value).longValue());
                                }
                            }
                            case "emails" -> {
                                final JsonArray emailArray = Json.array();
                                final Map<StoreSubType, FieldSubType> fieldSubTypes = fieldTypeService.groupSubtypesByStoreSubType(fieldInfo.storeType());
                                final String emailValueKey = fieldSubTypes.get(StoreSubType.EMAIL_VALUE).idAsString();
                                final String emailTypeKey = fieldSubTypes.get(StoreSubType.EMAIL_TYPE).idAsString();
                                final int emailTypeId = Email.Type.WORK.getId();
                                final Collection<String> emails = new HashSet<>();

                                for (int emailIndex = 0; emailIndex < valueArray.size(); emailIndex++) {
                                    final JsonObject valueEmailJson = valueArray.getJsonObject(emailIndex);

                                    if (valueEmailJson.containsKey(ActionAttributes.DEPENDS_ON)) {
                                        final Object value = getStepOptionValue(valueEmailJson, actionSource, actionExecutionContext);

                                        emails.add(value instanceof final Email email ? email.getValue() : (String) value);
                                    } else {
                                        emails.add(valueEmailJson.getString(ActionAttributes.VALUE));
                                    }
                                }

                                for (final String value : emails) {
                                    final JsonObject emailJson = Json.object();

                                    emailJson.put(emailValueKey, value);
                                    emailJson.put(emailTypeKey, emailTypeId);

                                    emailArray.put(emailJson);
                                }
                                contactJson.put(fieldInfo.idAsString(), emailArray);
                            }
                            case "phones" -> {
                                final JsonArray phoneArray = Json.array();
                                final Map<StoreSubType, FieldSubType> fieldSubTypes = fieldTypeService.groupSubtypesByStoreSubType(fieldInfo.storeType());
                                final String phoneNumberKey = fieldSubTypes.get(StoreSubType.PHONE_NUMBER).idAsString();
                                final String phoneTypeKey = fieldSubTypes.get(StoreSubType.PHONE_TYPE).idAsString();
                                final int phoneTypeId = Phone.Type.WORK.getId();
                                final Collection<String> phones = new HashSet<>();

                                for (int phoneIndex = 0; phoneIndex < valueArray.size(); phoneIndex++) {
                                    final JsonObject valuePhoneJson = valueArray.getJsonObject(phoneIndex);

                                    phones.add(valuePhoneJson.containsKey(ActionAttributes.DEPENDS_ON)
                                            ? (String) getStepOptionValue(valuePhoneJson, actionSource, actionExecutionContext)
                                            : valuePhoneJson.getString(ActionAttributes.VALUE));
                                }

                                for (final String value : phones) {
                                    final JsonObject phoneJson = Json.object();

                                    phoneJson.put(phoneNumberKey, value);
                                    phoneJson.put(phoneTypeKey, phoneTypeId);

                                    phoneArray.put(phoneJson);
                                }
                                contactJson.put(fieldInfo.idAsString(), phoneArray);
                            }
                            case "tags" -> {
                                final Collection<Object> tagIds = valueArray.toList().stream()
                                        .map(json -> ((Map<String, ?>) json).get(ActionAttributes.VALUE)).collect(Collectors.toSet());
                                final JsonArray tagArray = Json.array();

                                for (final Object tagId : tagIds) {
                                    final JsonObject tagJson = Json.object();

                                    tagJson.put(ActionAttributes.VALUE, tagId);
                                    tagArray.put(tagJson);
                                }
                                contactJson.put(fieldInfo.idAsString(), tagArray);
                            }
                            case "marketingList" -> {
                                final int size = valueArray.size();
                                final Collection<Long> marketingListIds = new HashSet<>(size);

                                for (int valueIndex = 0; valueIndex < size; valueIndex++) {
                                    marketingListIds.add(valueArray.getJsonObject(valueIndex).getLong(ActionAttributes.VALUE));
                                }

                                contactJson.put(fieldInfo.idAsString(), marketingListIds);
                            }
                        }
                    }
                }
            }
        }
        contactJsonAggregator.merge(orgId, contactJson);

        return contactJson;
    }
}
