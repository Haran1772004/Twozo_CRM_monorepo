package com.tlc.crm.zapier.internal.action.formatter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.type.Email;
import com.tlc.crm.entity.models.type.Phone;
import com.tlc.crm.common.util.NumberUtils;

import com.tlc.crm.fields.models.FieldSubType;
import com.tlc.crm.fields.models.StoreSubType;

import com.tlc.crm.zapier.api.models.ZapAction;
import com.tlc.crm.zapier.fields.ZapAttributes;

import com.tlc.l10n.temporal.TemporalServiceProvider;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.Map;

/**
 * Implementation of {@link ContactZapActionJsonFormatter} for formatting
 * contact-related data into a JSON structure suitable for Zapier actions.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ContactZapActionJsonFormatter extends AbstractZapActionJsonFormatter {

    private static final class Instance {
        private static final ContactZapActionJsonFormatter ZAP_ACTION_JSON_FORMATTER = new ContactZapActionJsonFormatter();
    }

    public static ContactZapActionJsonFormatter getInstance() {
        return Instance.ZAP_ACTION_JSON_FORMATTER;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId      The ID of the organization, used to apply organization-specific logic or formatting.
     * @param zapAction  The Zapier action containing data to be formatted.
     * @param actionJson The existing JSON object that will be populated with the formatted action data.
     * @return The formatted JSON object ready for sending as a Zapier action payload.
     */
    @Override
    public JsonObject format(final long orgId, final ZapAction zapAction, final JsonObject actionJson) {
        final JsonObject contactJson = Json.object();
        final Collection<FieldInfo> fields = fieldService.getFieldsBySource(orgId, Source.CONTACT.getName());

        for (final FieldInfo fieldInfo : fields) {

            if (fieldInfo.isPersistent()) {
                mapPersistentFieldToJson(orgId, fieldInfo, contactJson, actionJson);
            } else {
                final String aliasName = fieldInfo.aliasName();

                switch (aliasName) {
                    case "firstName", "lastName", "facebook", "twitter",
                         "linkedIn", "designation", "department", "otherUnsubscribeReason" -> {
                        if (actionJson.containsKey(aliasName)) {
                            contactJson.put(fieldInfo.idAsString(), actionJson.getString(aliasName));
                        }
                    }
                    case "company", "salesOwner", "contactSource", "contactLostReason",
                         "lifeCycleStage", "lifeCycleStatus", "timeZone",
                         "territory", "subscriptionStatus", "unsubscribeReason" -> {
                        if (actionJson.containsKey(aliasName)) {
                            contactJson.put(fieldInfo.idAsString(), actionJson.getLong(aliasName));
                        }
                    }
                    case "id" -> {
                        if (actionJson.containsKey(aliasName)) {
                            contactJson.put(ZapAttributes.ID, actionJson.getLong(aliasName));
                        }
                    }
                    case "emails" -> {
                        if (actionJson.containsKey(aliasName)) {
                            final JsonArray array = Json.array();
                            final Map<StoreSubType, FieldSubType> fieldSubTypes = fieldTypeService.groupSubtypesByStoreSubType(fieldInfo.storeType());
                            final String typeKey = fieldSubTypes.get(StoreSubType.EMAIL_TYPE).idAsString();
                            final String valueKey = fieldSubTypes.get(StoreSubType.EMAIL_VALUE).idAsString();

                            actionJson.getJsonArray(aliasName).forEach(value -> {
                                final JsonObject subFieldJson = Json.object();

                                subFieldJson.put(typeKey, Email.Type.WORK.getId());
                                subFieldJson.put(valueKey, (String) value);
                                array.put(subFieldJson);
                            });

                            contactJson.put(fieldInfo.idAsString(), array);
                        }
                    }
                    case "phones" -> {
                        if (actionJson.containsKey(aliasName)) {
                            final JsonArray array = Json.array();
                            final Map<StoreSubType, FieldSubType> fieldSubTypes = fieldTypeService.groupSubtypesByStoreSubType(fieldInfo.storeType());
                            final String typeKey = fieldSubTypes.get(StoreSubType.PHONE_TYPE).idAsString();
                            final String numberKey = fieldSubTypes.get(StoreSubType.PHONE_NUMBER).idAsString();

                            actionJson.getJsonArray(aliasName).forEach(value -> {
                                final JsonObject subFieldJson = Json.object();

                                subFieldJson.put(typeKey, Phone.Type.WORK.getId());
                                subFieldJson.put(numberKey, value);
                                array.put(subFieldJson);
                            });

                            contactJson.put(fieldInfo.idAsString(), array);
                        }
                    }
                    case "tags" -> {
                        if (actionJson.containsKey(aliasName)) {
                            final JsonArray tags = Json.array();

                            actionJson.getJsonArray(aliasName).forEach(value -> {
                                final JsonObject jsonObject = Json.object();

                                jsonObject.put(ZapAttributes.ID, NumberUtils.parseNumber(value).longValue());
                                tags.put(jsonObject);
                            });

                            contactJson.put(fieldInfo.idAsString(), tags);
                        }
                    }
                    case "dateOfBirth" -> {
                        if (actionJson.containsKey(aliasName)) {
                            contactJson.put(fieldInfo.idAsString(), TemporalServiceProvider.get().getISOOffsetTimeStamp(actionJson.getString(aliasName)));
                        }
                    }
                    case "address" -> {
                        if (actionJson.containsKey(aliasName)) {
                            final JsonObject subFieldJson = Json.object();
                            final Map<StoreSubType, FieldSubType> fieldSubTypes = fieldTypeService.groupSubtypesByStoreSubType(fieldInfo.storeType());

                            subFieldJson.put(fieldSubTypes.get(StoreSubType.ADDRESS_LINE1).idAsString(), actionJson.getString(aliasName));
                            contactJson.put(fieldInfo.idAsString(), subFieldJson);
                        }
                    }
                    case "subscriptionTypes" -> {
                        if (actionJson.containsKey(aliasName)) {
                            final JsonArray array = Json.array();

                            actionJson.getJsonArray(aliasName).forEach(value -> array.put(NumberUtils.parseNumber(value).longValue()));
                            contactJson.put(fieldInfo.idAsString(), array);
                        }
                    }
                }
            }
        }
        return contactJson;
    }
}
