package com.tlc.crm.zapier.internal.action.formatter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.json.JsonArray;

import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.common.util.NumberUtils;

import com.tlc.crm.fields.models.FieldSubType;
import com.tlc.crm.fields.models.StoreSubType;

import com.tlc.crm.zapier.api.models.ZapAction;
import com.tlc.crm.zapier.fields.ZapAttributes;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.Map;

/**
 * Implementation of {@link CompanyZapActionJsonFormatter} for formatting
 * company-related data into a JSON structure suitable for Zapier actions.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CompanyZapActionJsonFormatter extends AbstractZapActionJsonFormatter {

    private static final class Instance {
        private static final CompanyZapActionJsonFormatter ZAP_ACTION_JSON_FORMATTER = new CompanyZapActionJsonFormatter();
    }

    public static CompanyZapActionJsonFormatter getInstance() {
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
        final JsonObject companyJson = Json.object();
        final Collection<FieldInfo> fields = fieldService.getFieldsBySource(orgId, Source.COMPANY.getName());

        for (final FieldInfo fieldInfo : fields) {

            if (fieldInfo.isPersistent()) {
                mapPersistentFieldToJson(orgId, fieldInfo, companyJson, actionJson);
            } else {
                final String aliasName = fieldInfo.aliasName();

                switch (aliasName) {
                    case "name", "website", "facebook", "twitter", "linkedIn", "description" -> {
                        if (actionJson.containsKey(aliasName)) {
                            companyJson.put(fieldInfo.idAsString(), actionJson.getString(aliasName));
                        }
                    }
                    case "address" -> {
                        if (actionJson.containsKey(aliasName)) {
                            final JsonObject subFieldJson = Json.object();
                            final Map<StoreSubType, FieldSubType> fieldSubTypes = fieldTypeService.groupSubtypesByStoreSubType(fieldInfo.storeType());

                            subFieldJson.put(fieldSubTypes.get(StoreSubType.ADDRESS_LINE1).idAsString(), actionJson.getString(aliasName));
                            companyJson.put(fieldInfo.idAsString(), subFieldJson);
                        }
                    }
                    case "salesOwner", "sicCode", "territory", "employeeCount", "parentCompany",
                         "organizationStatus", "industryType", "businessType" -> {
                        if (actionJson.containsKey(aliasName)) {
                            companyJson.put(fieldInfo.idAsString(), actionJson.getLong(aliasName));
                        }
                    }
                    case "id" -> {
                        if (actionJson.containsKey(aliasName)) {
                            companyJson.put(ZapAttributes.ID, actionJson.getLong(aliasName));
                        }
                    }
                    case "annualRevenue" -> {
                        if (actionJson.containsKey(aliasName)) {
                            final JsonObject subFieldJson = Json.object();
                            final Map<StoreSubType, FieldSubType> fieldSubTypes = fieldTypeService.groupSubtypesByStoreSubType(fieldInfo.storeType());

                            subFieldJson.put(fieldSubTypes.get(StoreSubType.VALUE).idAsString(), actionJson.getLong(aliasName));
                            subFieldJson.put(fieldSubTypes.get(StoreSubType.CURRENCY).idAsString(), baseCurrencyService.getDefaultCurrency(orgId).getId());

                            companyJson.put(fieldInfo.idAsString(), subFieldJson);
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

                            companyJson.put(fieldInfo.idAsString(), tags);
                        }
                    }
                }
            }
        }

        return companyJson;
    }


}
