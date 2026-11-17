package com.tlc.crm.zapier.internal.event.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.iam.api.model.CrmUser;

import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;

import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.type.Address;
import com.tlc.crm.entity.models.type.Email;
import com.tlc.crm.entity.models.type.Monetary;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.entity.models.type.Phone;
import com.tlc.crm.entity.models.type.Range;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.service.OptionService;

import com.tlc.crm.fields.models.FieldSubType;
import com.tlc.crm.fields.models.StoreSubType;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.fields.service.FieldTypeService;

import com.tlc.crm.zapier.internal.event.ZapEventData;
import com.tlc.i18n.I18nResolver;
import com.tlc.i18n.I18nService;
import com.tlc.i18n.service.provider.I18nServiceProvider;

import com.tlc.iam.common.models.User;
import com.tlc.l10n.model.country.Country;
import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.l10n.temporal.model.ISOTemporalFormat;
import com.tlc.l10n.temporal.model.TimezoneInfo;
import com.tlc.l10n.temporal.model.Temporal;

import com.tlc.validator.TlcModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Abstract base class for converting model data into a JSON format
 * suitable for Zapier event payloads.
 *
 * @param <T> the type of {@link TlcModel} to be converted.
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
abstract class AbstractZapEventDataConverter<T extends TlcModel> {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractZapEventDataConverter.class);

    protected static final String NAME = "name";
    protected static final String ID = "id";

    protected final FieldService fieldService;
    protected final FieldTypeService fieldTypeService;
    protected final I18nService i18nService;
    protected final OptionService optionService;
    protected final ContactManager contactManager;
    protected final CompanyManager companyManager;

    protected AbstractZapEventDataConverter() {
        this.fieldService = FieldService.getInstance();
        this.fieldTypeService = FieldTypeService.getInstance();
        this.i18nService = I18nServiceProvider.get();
        this.optionService = OptionService.getInstance();
        this.contactManager = ContactManager.getInstance();
        this.companyManager = CompanyManager.getInstance();
    }

    /**
     * Converts the given model to the collection of {@link JsonObject} suitable for sending to Zapier.
     *
     * @param models       The specific models instance to convert.
     * @param zapEventData Refers the {@code ZapEventData}.
     * @return The collection of {@link JsonObject} representing the converted model data.
     */
    public final Collection<JsonObject> convert(final Collection<T> models, final ZapEventData zapEventData) {
        final Collection<JsonObject> eventJsonList = new ArrayList<>(models.size());

        for (final T model : models) {
            eventJsonList.add(convert(model, zapEventData));
        }

        return eventJsonList;
    }

    /**
     * Converts the given model to a {@link JsonObject} suitable for sending to Zapier.
     *
     * @param fieldInfo    Refers the {@code FieldInfo}.
     * @param value        Refers the value of the custom field.
     * @param jsonObject   Refers the json where the custom fields are mapped.
     * @param i18nResolver The resolver used for localization during conversion.
     * @param userMap      The grouped user map.
     */
    protected final void mapPersistentFieldToJson(final FieldInfo fieldInfo,
                                                  final Object value, final JsonObject jsonObject,
                                                  final I18nResolver i18nResolver, final Map<Long, User> userMap) {
        if (null != value) {
            final StoreType storeType = fieldInfo.storeType();
            final String fieldName = fieldInfo.fieldName();

            switch (storeType) {
                case SINGLE_STRING, TEXT, SINGLE_NUMBER -> jsonObject.put(fieldName, value);
                case CONTACT, COMPANY, OWNER -> {
                    final JsonObject json = Json.object();
                    if (value instanceof final Company company) {
                        json.put(NAME, company.getName());
                        json.put(ID, company.id());
                    } else if (value instanceof final Contact contact) {
                        json.put(NAME, contact.getFullName());
                        json.put(ID, contact.id());
                    } else if (value instanceof final CrmUser crmUser) {
                        json.put(NAME, userMap.get(crmUser.getIamId()));
                        json.put(ID, crmUser.id());
                    }
                    jsonObject.put(fieldName, json);
                }
                case DATE, TIME, DATE_TIME -> {
                    if (value instanceof final Temporal temporal) {
                        jsonObject.put(fieldName, TemporalServiceProvider.get().getISOOffsetDateTimeString(temporal.getTimeStamp(), TimezoneInfo.UTC, switch (storeType) {
                            case TIME -> ISOTemporalFormat.ISO_OFFSET_TIME;
                            case DATE -> ISOTemporalFormat.ISO_OFFSET_DATE;
                            default -> ISOTemporalFormat.ISO_OFFSET_DATE_TIME;
                        }));
                    }
                }
                case MONETARY, ADDRESS, EMAIL, PHONE, DATE_RANGE, TIME_RANGE, DATE_TIME_RANGE -> {
                    final JsonObject subTypeJson = Json.object();
                    final Map<StoreSubType, FieldSubType> fieldSubTypes = fieldTypeService.groupSubtypesByStoreSubType(storeType);

                    if (value instanceof final Monetary monetary) {
                        subTypeJson.put(i18nResolver.get(fieldSubTypes.get(StoreSubType.CURRENCY).name()),
                                i18nResolver.get(monetary.getCurrency().getI18nKey()));
                        subTypeJson.put(i18nResolver.get(fieldSubTypes.get(StoreSubType.VALUE).name()), monetary.getValue());
                    } else if (value instanceof final Address address) {
                        subTypeJson.put(i18nResolver.get(fieldSubTypes.get(StoreSubType.ADDRESS_LINE1).name()), address.getAddressLine1());
                        subTypeJson.put(i18nResolver.get(fieldSubTypes.get(StoreSubType.ADDRESS_LINE2).name()), address.getAddressLine2());
                        subTypeJson.put(i18nResolver.get(fieldSubTypes.get(StoreSubType.CITY).name()), address.getCity());
                        subTypeJson.put(i18nResolver.get(fieldSubTypes.get(StoreSubType.STATE).name()), address.getState());
                        subTypeJson.put(i18nResolver.get(fieldSubTypes.get(StoreSubType.COUNTRY).name()),
                                Optional.ofNullable(address.getCountry()).map(Country::getName).orElseGet(() -> null));
                        subTypeJson.put(i18nResolver.get(fieldSubTypes.get(StoreSubType.ZIPCODE).name()), address.getZipcode());
                    } else if (value instanceof final Email email) {
                        subTypeJson.put(i18nResolver.get(fieldSubTypes.get(StoreSubType.EMAIL_VALUE).name()), email.getValue());
                        subTypeJson.put(i18nResolver.get(fieldSubTypes.get(StoreSubType.EMAIL_TYPE).name()), email.getType());
                    } else if (value instanceof final Phone phone) {
                        subTypeJson.put(i18nResolver.get(fieldSubTypes.get(StoreSubType.PHONE_NUMBER).name()), phone.getNumber());
                        subTypeJson.put(i18nResolver.get(fieldSubTypes.get(StoreSubType.PHONE_TYPE).name()), phone.getType());
                    } else if (value instanceof final Range<?> range) {
                        final Long fromTimeStamp = range.getFrom().getTimeStamp();
                        final Long toTimeStamp = range.getTo().getTimeStamp();

                        subTypeJson.put(i18nResolver.get(fieldSubTypes.get(StoreSubType.FROM).name()), TemporalServiceProvider.get().getISOOffsetDateTimeString(fromTimeStamp, TimezoneInfo.UTC, switch (storeType) {
                            case TIME_RANGE -> ISOTemporalFormat.ISO_OFFSET_TIME;
                            case DATE_RANGE -> ISOTemporalFormat.ISO_OFFSET_DATE;
                            default -> ISOTemporalFormat.ISO_OFFSET_DATE_TIME;
                        }));
                        subTypeJson.put(i18nResolver.get(fieldSubTypes.get(StoreSubType.TO).name()), TemporalServiceProvider.get().getISOOffsetDateTimeString(toTimeStamp, TimezoneInfo.UTC, switch (storeType) {
                            case TIME_RANGE -> ISOTemporalFormat.ISO_OFFSET_TIME;
                            case DATE_RANGE -> ISOTemporalFormat.ISO_OFFSET_DATE;
                            default -> ISOTemporalFormat.ISO_OFFSET_DATE_TIME;
                        }));
                    }

                    jsonObject.put(fieldName, subTypeJson);
                }
                case DROPDOWN -> {
                    if (value instanceof final Option option) {
                        jsonObject.put(fieldName, i18nResolver.get(option.getOption()));
                    }
                }
                case MULTI_SELECT -> {
                    if (value instanceof final Collection<?> models) {
                        final JsonArray options = Json.array();

                        models.forEach(model -> {
                            final Option option = (Option) model;

                            options.put(i18nResolver.get(option.getOption()));
                        });

                        jsonObject.put(fieldName, options);
                    }
                }
            }
        }
    }

    /**
     * Converts the given model to a {@link JsonObject} suitable for sending to Zapier.
     *
     * @param model        The specific model instance to convert.
     * @param zapEventData Refers the {@code ZapEventData}.
     * @return {@link JsonObject} representing the converted model data.
     */
    abstract JsonObject convert(final T model, final ZapEventData zapEventData);
}
