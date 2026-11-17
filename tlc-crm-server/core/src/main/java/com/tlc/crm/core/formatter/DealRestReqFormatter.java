package com.tlc.crm.core.formatter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.TypeChecker;

import com.tlc.crm.common.util.NumberUtils;

import com.tlc.crm.core.field.contact.CompanyFields;
import com.tlc.crm.core.field.deal.DealFields;

import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;

import com.tlc.crm.fields.models.FieldSubType;
import com.tlc.crm.fields.models.StoreSubType;

import com.tlc.i18n.I18nResolver;
import com.tlc.l10n.temporal.TemporalServiceProvider;

import java.util.Collection;
import java.util.Map;

/**
 * Formatter that converts an incoming REST request JSON for a Deal into the
 * internal request JSON structure used by the entity layer.
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
public final class DealRestReqFormatter extends EntityReqJsonFormatter {

    private final CompanyRestReqFormatter companyRestReqFormatter;
    private final ContactRestReqFormatter contactRestReqFormatter;

    private DealRestReqFormatter() {
        this.contactRestReqFormatter = ContactRestReqFormatter.getInstance();
        this.companyRestReqFormatter = CompanyRestReqFormatter.getInstance();
    }

    private static class Instance {
        private static final DealRestReqFormatter INSTANCE = new DealRestReqFormatter();
    }

    public static DealRestReqFormatter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId        Represents the organization ID for which the formatting is being done, allowing for organization-specific logic if necessary.
     * @param requestJson  Represents the incoming JSON object from the request that needs to be formatted into the internal entity JSON structure.
     * @param i18nResolver Represents the internationalization resolver that can be used to fetch localized strings or handle locale-specific formatting during the conversion process.
     */
    @Override
    public JsonObject format(final long orgId, final JsonObject requestJson, final I18nResolver i18nResolver) {
        final JsonObject dealJson = Json.object();
        final Collection<FieldInfo> fields = fieldService.getFieldsBySource(orgId, Source.DEAL.getName());

        for (final FieldInfo fieldInfo : fields) {
            final String internalName = i18nResolver.get(fieldInfo.getInternalName());

            if (fieldInfo.isPersistent()) {
                mapPersistentFieldToJson(orgId, fieldInfo, internalName, dealJson, requestJson, i18nResolver);
            } else {
                final String aliasName = fieldInfo.aliasName();

                switch (aliasName) {
                    case DealFields.TITLE, DealFields.DESCRIPTION -> {
                        if (requestJson.containsKey(internalName)) {
                            dealJson.put(fieldInfo.idAsString(), requestJson.getString(internalName));
                        }
                    }
                    case DealFields.PIPELINE, DealFields.PIPELINE_STAGE,
                         DealFields.SALES_OWNER, DealFields.TERRITORY -> {
                        if (requestJson.containsKey(internalName)) {
                            dealJson.put(fieldInfo.idAsString(), requestJson.getLong(internalName));
                        }
                    }
                    case DealFields.WON_REASON, DealFields.LOST_REASON, DealFields.TYPE, DealFields.PAYMENT_STATUS,
                         DealFields.DEAL_SOURCE -> {
                        if (requestJson.containsKey(internalName)) {
                            mapOptionField(orgId, requestJson.get(internalName), fieldInfo, dealJson, i18nResolver);
                        }
                    }
                    case DealFields.PRIMARY_CONTACT -> {
                        if (requestJson.containsKey(internalName)) {
                            final Object value = requestJson.get(internalName);

                            if (TypeChecker.isNonDecimalNumber(value)) {
                                dealJson.put(fieldInfo.idAsString(), value);
                            } else {
                                final JsonObject contactJson = contactRestReqFormatter.format(orgId, requestJson.getJsonObject(internalName), i18nResolver);

                                dealJson.put(fieldInfo.idAsString(), contactJson);
                            }
                        }
                    }
                    case DealFields.COMPANY -> {
                        if (requestJson.containsKey(internalName)) {
                            final Object value = requestJson.get(internalName);

                            if (TypeChecker.isNonDecimalNumber(value)) {
                                dealJson.put(fieldInfo.idAsString(), value);
                            } else {
                                final JsonObject companyJson = companyRestReqFormatter.format(orgId, requestJson.getJsonObject(internalName), i18nResolver);

                                dealJson.put(fieldInfo.idAsString(), companyJson);
                            }
                        }
                    }
                    case DealFields.ID -> {
                        if (requestJson.containsKey(internalName)) {
                            dealJson.put(DealFields.ID, requestJson.getLong(internalName));
                        }
                    }
                    case DealFields.DEAL_CLOSED_ON, DealFields.EXPECTED_CLOSE_DATE -> {
                        if (requestJson.containsKey(internalName)) {
                            dealJson.put(fieldInfo.idAsString(), TemporalServiceProvider.get().getISOOffsetTimeStamp(requestJson.getString(internalName)));
                        }
                    }
                    case DealFields.RELATED_CONTACTS -> {
                        if (requestJson.containsKey(internalName)) {
                            final JsonArray relatedContacts = Json.array();
                            final JsonArray jsonArray = requestJson.getJsonArray(internalName);
                            final int size = jsonArray.size();

                            for (int index = 0; index < size; index++) {
                                final Object value = jsonArray.get(index);

                                if (TypeChecker.isNonDecimalNumber(value)) {
                                    relatedContacts.put(NumberUtils.parseNumber(value).longValue());
                                } else {
                                    final JsonObject contactJson = contactRestReqFormatter.format(orgId, jsonArray.getJsonObject(index), i18nResolver);

                                    relatedContacts.put(contactJson);
                                }
                            }
                            dealJson.put(fieldInfo.idAsString(), relatedContacts);
                        }
                    }
                    case DealFields.DEAL_VALUE -> {
                        if (requestJson.containsKey(internalName)) {
                            final JsonObject jsonObject = requestJson.getJsonObject(internalName);
                            final JsonObject subFieldJson = Json.object();
                            final Map<StoreSubType, FieldSubType> fieldSubTypes = fieldTypeService.groupSubtypesByStoreSubType(fieldInfo.storeType());
                            final String productKey = StoreSubType.PRODUCT.getApiPropertyName();

                            if (jsonObject.containsKey(productKey)) {
                                subFieldJson.put(fieldSubTypes.get(StoreSubType.PRODUCT).idAsString(), jsonObject.opt(StoreSubType.PRODUCT.getApiPropertyName()));
                                subFieldJson.put(DealFields.IS_MANUALLY_ADDED, false);
                            } else {
                                subFieldJson.put(DealFields.IS_MANUALLY_ADDED, true);
                            }
                            subFieldJson.put(fieldSubTypes.get(StoreSubType.VALUE).idAsString(), jsonObject.opt(StoreSubType.VALUE.getApiPropertyName()));
                            final Object currencyValue = jsonObject.opt(StoreSubType.CURRENCY.getApiPropertyName());

                            if (TypeChecker.isNumber(currencyValue)) {
                                subFieldJson.put(fieldSubTypes.get(StoreSubType.CURRENCY).idAsString(), currencyValue);
                            } else {
                                subFieldJson.put(fieldSubTypes.get(StoreSubType.CURRENCY).idAsString(), resolveCurrency(orgId, (String) currencyValue, i18nResolver).getId());
                            }
                            dealJson.put(fieldInfo.idAsString(), subFieldJson);
                        }
                    }
                    case DealFields.TAGS -> {
                        if (requestJson.containsKey(internalName)) {
                            final JsonArray tags = Json.array();

                            requestJson.getJsonArray(internalName).forEach(value -> {
                                final JsonObject jsonObject = Json.object();

                                jsonObject.put(CompanyFields.ID, NumberUtils.parseNumber(value).longValue());
                                tags.put(jsonObject);
                            });

                            dealJson.put(fieldInfo.idAsString(), tags);
                        }
                    }
                }
            }
        }
        return dealJson;
    }

}
