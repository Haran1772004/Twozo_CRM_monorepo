package com.tlc.crm.entity.builder;

import com.tlc.attachment.model.profilePicture.DisplayPicture;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;
import com.tlc.commons.util.RandomIdGenerator;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.converter.OptionJsonConverter;
import com.tlc.crm.entity.fields.source.Source;

import com.tlc.crm.fields.manager.SummaryFieldPreferenceManager;
import com.tlc.crm.fields.models.SummaryFieldPreference;
import com.tlc.i18n.I18nKey;
import com.tlc.i18n.I18nResolutionAdapter;
import com.tlc.i18n.service.provider.I18nServiceProvider;
import com.tlc.i18n.I18nResolver;
import com.tlc.l10n.model.country.Country;
import com.tlc.l10n.model.currency.Currency;

import com.tlc.crm.entity.fields.displayPicture.DisplayPictureFields;
import com.tlc.crm.entity.currency.BaseCurrencyService;
import com.tlc.crm.entity.loader.DefaultFieldValuesLoader;
import com.tlc.crm.entity.models.PresetConfig;
import com.tlc.crm.entity.models.PresetConfig.Configuration;
import com.tlc.crm.common.iam.api.model.converter.CrmUserConverter;
import com.tlc.crm.common.iam.api.model.CrmUser;

import com.tlc.crm.entity.models.type.StaticOption;
import com.tlc.crm.entity.service.OptionService;
import com.tlc.crm.fields.service.FieldSourceService;
import com.tlc.crm.fields.service.FieldTypeService;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.fields.FieldTypeKeys;
import com.tlc.crm.entity.internal.status.EntityErrorCodes;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.entity.models.Entity;
import com.tlc.crm.entity.models.EntityConverterHolder;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.ValueHolder;
import com.tlc.crm.fields.models.FieldSource;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.fields.models.FieldSubType;
import com.tlc.crm.fields.models.StoreSubType;
import com.tlc.crm.entity.models.type.Address;
import com.tlc.crm.entity.models.type.Email;
import com.tlc.crm.entity.models.type.Monetary;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.entity.models.type.Phone;
import com.tlc.crm.entity.models.type.Range;

import com.tlc.i18n.I18nService;
import com.tlc.l10n.temporal.model.Date;
import com.tlc.l10n.temporal.model.Temporal;
import com.tlc.validator.TlcModel;
import com.tlc.web.HttpUtil;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * <p>
 * Converts an entity from json object required for profile and summary view and vice vera.
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public abstract class ViewConverter<T extends Entity> extends EntityConverter<T> implements OptionJsonConverter<T> {

    protected static final JsonObject JSON_OBJECT = Json.object();

    private static final String EMPTY_STRING = "";

    private final FieldTypeService fieldTypeService;
    private final I18nService i18nService;
    private final FieldSourceService fieldSourceService;
    private final EntityConverterHolder entityConverterHolder;
    private final RandomIdGenerator randomIdGenerator;
    private final FieldConstraintBuilder fieldConstraintBuilder;
    protected final BaseCurrencyService baseCurrencyService;
    protected final OptionService optionService;
    protected final DefaultFieldValuesLoader defaultFieldValuesLoader;
    protected final EntitySalesOwnerHandler entitySalesOwnerHandler;
    protected final String profilePicRedirectionUrl;
    private final SummaryFieldPreferenceManager summaryFieldPreferenceManager;

    protected ViewConverter() {
        fieldTypeService = FieldTypeService.getInstance();
        i18nService = I18nServiceProvider.get();
        fieldSourceService = FieldSourceService.getInstance();
        baseCurrencyService = BaseCurrencyService.getInstance();
        entityConverterHolder = EntityConverterHolder.getInstance();
        optionService = OptionService.getInstance();
        defaultFieldValuesLoader = DefaultFieldValuesLoader.getInstance();
        entitySalesOwnerHandler = EntitySalesOwnerHandler.getInstance();
        randomIdGenerator = RandomIdGenerator.getInstance();
        fieldConstraintBuilder = FieldConstraintBuilder.getInstance();
        final Configurations configurations = ConfHolder.getInstance().getProperties("profile-picture").getNestedConfigs("redirectUrl");
        this.profilePicRedirectionUrl = configurations.getConfig("source");
        summaryFieldPreferenceManager = SummaryFieldPreferenceManager.getInstance();
    }

    /**
     * <p>
     * Converts the {@link StoreSubType} to json.
     * </p>
     *
     * @param tlcModel     Represents a model the holds the required data.
     * @param storeSubType Represents the store subtypes of the fields.
     * @param fieldSubType Represents the field subtype.
     * @return {@link JsonObject}
     */
    protected JsonObject convertSubType(final TlcModel tlcModel, final StoreSubType storeSubType, final FieldSubType fieldSubType) {
        final JsonObject jsonObject = Json.object();
        final String valueKey = FieldKeys.VALUE.getName();

        if (tlcModel instanceof final Email email) {
            switch (storeSubType) {
                case EMAIL_TYPE -> jsonObject.put(valueKey, email.getType().getId());
                case EMAIL_VALUE -> jsonObject.put(valueKey, email.getValue());
                default -> throw ErrorCode.get(EntityErrorCodes.INVALID_CRM_FIELD_STORE_SUBTYPE);
            }
        } else if (tlcModel instanceof final Phone phone) {
            switch (storeSubType) {
                case PHONE_TYPE -> jsonObject.put(valueKey, phone.getType().getId());
                case PHONE_NUMBER -> jsonObject.put(valueKey, phone.getNumber());
                case COUNTRY_CODE -> {
                    final Country country = phone.getCountry();

                    if (Objects.nonNull(country)) {
                        jsonObject.put(valueKey, country.getCode());
                    }
                }
                default -> throw ErrorCode.get(EntityErrorCodes.INVALID_CRM_FIELD_STORE_SUBTYPE);
            }
        } else if (tlcModel instanceof final Monetary monetary) {
            switch (storeSubType) {
                case VALUE -> jsonObject.put(valueKey, baseCurrencyService
                        .applyPrecision(monetary.orgId(), monetary.getValue()));
                case CURRENCY -> {
                    final Currency currency = monetary.getCurrency();

                    if (Objects.nonNull(currency)) {
                        jsonObject.put(valueKey, currency.getId());
                        jsonObject.put(FieldKeys.NAME.getName(), currency);
                        jsonObject.put(FieldKeys.SYMBOL.getName(), currency.getSymbol());
                    }
                }
                case PRODUCT -> {
                    return jsonObject;
                }
                default -> throw ErrorCode.get(EntityErrorCodes.INVALID_CRM_FIELD_STORE_SUBTYPE);
            }
        } else if (tlcModel instanceof final Address address) {
            switch (storeSubType) {
                case CITY -> jsonObject.put(valueKey, address.getCity());
                case STATE -> jsonObject.put(valueKey, address.getState());
                case COUNTRY -> {
                    final Country country = address.getCountry();

                    if (Objects.nonNull(country)) {
                        jsonObject.put(valueKey, country.getId());
                    }
                }
                case ZIPCODE -> jsonObject.put(valueKey, address.getZipcode());
                case ADDRESS_LINE1 -> jsonObject.put(valueKey, address.getAddressLine1());
                case ADDRESS_LINE2 -> jsonObject.put(valueKey, address.getAddressLine2());
                case LATITUDE -> jsonObject.put(valueKey, address.getLatitude());
                case LONGITUDE -> jsonObject.put(valueKey, address.getLongitude());
                default -> throw ErrorCode.get(EntityErrorCodes.INVALID_CRM_FIELD_STORE_SUBTYPE);
            }
        } else if (tlcModel instanceof final Range range) {
            switch (storeSubType) {
                case FROM -> jsonObject.put(valueKey, range.getFrom().getTimeStamp());
                case TO -> jsonObject.put(valueKey, range.getTo().getTimeStamp());
                default -> throw ErrorCode.get(EntityErrorCodes.INVALID_CRM_FIELD_STORE_SUBTYPE);
            }
        }
        jsonObject.put(FieldKeys.ID.getName(), fieldSubType.id());

        return jsonObject;
    }

    /**
     * <p>
     * Gets the fields for summary view by using view order and view index of each fields.
     * </p>
     *
     * @param entity Holds the data of specific entity given.
     * @return All the {@link FieldInfo} of the given fields.
     */
    protected Map<Integer, List<FieldInfo>> getSummaryFields(final T entity) {
        final Collection<FieldInfo> fields = secureFieldService.getFieldsBySource(entity.orgId(), entity.getEntityType());
        return fields.stream()
                .filter(field -> {
                    final PresetConfig presetConfig = field.presetConfig();

                    return !presetConfig.showOnProfile() && !presetConfig.hidden();
                })
                .sorted(Comparator.comparing(field -> field.presetConfig().orderOnView()))
                .collect(Collectors.groupingBy(field -> field.presetConfig().rowIndexOnView()));
    }

    /**
     * <p>
     * Gets the fields for summary view by using view order and view index of each fields.
     * </p>
     *
     * @param entity Holds the data of specific entity given.
     * @return All the {@link FieldInfo} of the given fields.
     */
    public Map<Integer, List<FieldInfo>> getSummaryFieldsV2(final T entity) {
        final Collection<FieldInfo> fields = secureFieldService.getFieldsBySource(entity.orgId(), entity.getEntityType());

        return fields.stream().filter(fieldInfo -> {
                    final PresetConfig presetConfig = fieldInfo.presetConfig();

                    return Objects.nonNull(presetConfig) && presetConfig.hasSummaryConfig();
                }).sorted(Comparator.comparing(field -> field.presetConfig().getOrderOnViewV2()))
                .collect(Collectors.groupingBy(field -> field.presetConfig().getRowIndexOnViewV2()));
    }

    /**
     * <p>
     * Gets the fields for summary view by using view order and view index of each fields.
     * </p>
     *
     * @param entity Holds the data of specific entity given.
     * @return All the {@link FieldInfo} of the given fields.
     */
    public Map<Integer, List<FieldInfo>> getMobileSummaryFieldsV2(final T entity) {
        final Collection<FieldInfo> fields = secureFieldService.getFieldsBySource(entity.orgId(), entity.getEntityType());

        return fields.stream().filter(fieldInfo -> {
                    final PresetConfig presetConfig = fieldInfo.presetConfig();

                    return Objects.nonNull(presetConfig) && presetConfig.hasMobileSummaryConfig();
                }).sorted(Comparator.comparing(field -> field.presetConfig().getMobileOrderOnView()))
                .collect(Collectors.groupingBy(field -> field.presetConfig().getMobileRowIndexOnView()));
    }

    /**
     * <p>
     * Gets the fields by using the profile view.
     * </p>
     *
     * @param entity Holds the data of specific entity.
     * @return All the {@link FieldInfo} of the given fields.
     */
    private Collection<FieldInfo> getProfileFields(final T entity) {
        final Collection<FieldInfo> fields = secureFieldService.getFieldsBySource(entity.orgId(), entity.getEntityType());

        return fields.stream()
                .filter(field -> {
                            final PresetConfig presetConfig = field.presetConfig();

                            return !field.isPersistent() && presetConfig.showOnProfile();
                        }
                ).toList();
    }

    /**
     * <p>
     * Converts the {@link FieldSource} into json.
     * </p>
     *
     * @param orgId           Represents the organization id.
     * @param source          Represents the {@link FieldSource}.
     * @param converterConfig {@link ConverterConfig}.
     * @return {@link JsonObject}
     */
    private JsonObject getFieldSubTypesByGroup(final Long orgId, final FieldSource source, final ConverterConfig converterConfig) {
        final JsonObject jsonObject = Json.object();
        final Collection<FieldInfo> fields = secureFieldService.getFieldsBySource(orgId, source.getName());

        fields.forEach(field -> {
            final StoreType storeType = field.fieldStore().storeType();
            final JsonArray jsonArray = Json.array();
            final Collection<FieldSubType> fieldSubTypes = fieldTypeService.getSubTypesByStoreType(storeType);

            for (final FieldSubType fieldSubType : fieldSubTypes) {
                jsonArray.put(convertFieldSubTypeIntoJson(fieldSubType, converterConfig));
            }

            if (jsonArray.size() > 0) {
                jsonObject.put(storeType.getName(), jsonArray);
            }
        });

        return jsonObject;
    }

    /**
     * <p>
     * Converts the {@link FieldSubType} into json.
     * </p>
     *
     * @param subType         which contains the field subtype data.
     * @param converterConfig {@link ConverterConfig}.
     * @return {@link JsonObject} which contains the field subtype data.
     */
    private JsonObject convertFieldSubTypeIntoJson(final FieldSubType subType, final ConverterConfig converterConfig) {
        final JsonObject fieldSubTypeJson = Json.object();
        final String config = subType.config();
        final I18nResolver i18nResolver = i18nService.getResolver(converterConfig.locale());

        fieldSubTypeJson.put(FieldTypeKeys.ID.getName(), subType.id());
        fieldSubTypeJson.put(FieldTypeKeys.NAME.getName(), subType.name());
        fieldSubTypeJson.put(FieldTypeKeys.TYPE.getName(), subType.fieldType().storeType().getName());
        fieldSubTypeJson.put(FieldTypeKeys.DISPLAY_NAME.getName(), i18nResolver.get(subType.displayName()));

        if (Objects.nonNull(config)) {
            fieldSubTypeJson.put(FieldTypeKeys.CONFIG.getName(), fieldConstraintBuilder.buildSubFields(Json.object(config), i18nResolver));
        }
        fieldSubTypeJson.put(FieldTypeKeys.INPUT_TYPE.getName(), subType.inputType());
        fieldSubTypeJson.put(FieldTypeKeys.PLACEHOLDER.getName(), i18nResolver.get(subType.placeholder()));

        return fieldSubTypeJson;
    }

    /**
     * <p>
     * Summarizes the view of fields that are present for each entity.
     * </p>
     *
     * @param entity          Holds the data of specific entity.
     * @param converterConfig Represents the {@link ConverterConfig}.
     * @return {@link JsonObject}
     */
    protected abstract JsonObject summary(final T entity, final ConverterConfig converterConfig);

    /**
     * <p>
     * Summarizes the view of fields that are present for each entity.
     * </p>
     *
     * @param entity          Holds the data of specific entity.
     * @param converterConfig Represents the {@link ConverterConfig}.
     * @return {@link JsonObject}
     */
    protected abstract JsonObject summaryV2(final Long orgId, final Long userId, final T entity, final ConverterConfig converterConfig);

    /**
     * <p>
     * Converts the profile details for the {@link T} entity.
     * </p>
     *
     * @param entity          Holds the data of specific entity.
     * @param converterConfig Represents the {@link ConverterConfig}.
     * @return The {@link JsonObject} containing profile details.
     */
    protected abstract JsonObject profile(final T entity, final ConverterConfig converterConfig);

    /**
     * <p>
     * Coverts the subtypes to {@link JsonArray} with collection of models.
     * </p>
     *
     * @param storeType Represents the {@link StoreType}.
     * @param models    Represents the models that has to converted into json.
     * @return {@link JsonArray}
     */
    protected final Object convertSubTypesToJson(final StoreType storeType, final Collection<? extends TlcModel> models) {
        final Map<StoreSubType, FieldSubType> subTypes = fieldTypeService.groupSubtypesByStoreSubType(storeType);

        switch (storeType) {
            case PHONES, EMAILS, CONTACTS_ONBOARD, PRICE_TAGS, OWNERS -> {
                final JsonArray array = Json.array();

                for (final TlcModel model : models) {
                    final JsonObject json = Json.object();

                    json.put(FieldKeys.ID.getName(), model.id());
                    subTypes.forEach((key, value) -> json.put(key.getName(), convertSubType(model, key, value)));
                    array.put(json);
                }
                return array;
            }
            case DROPDOWN -> {
                final JsonObject jsonObject = Json.object();
                final TlcModel tlcModel = models.stream().findFirst().orElseGet(() -> null);

                if (tlcModel instanceof final Option option) {
                    jsonObject.put(FieldKeys.ID.getName(), option.id());
                    jsonObject.put(FieldKeys.NAME.getName(), option.getOption());
                }
                return jsonObject;
            }
            case MULTI_SELECT -> {
                final JsonArray jsonArray = Json.array();
                final String idKey = FieldKeys.ID.getName();
                final String nameKey = FieldKeys.NAME.getName();

                for (final TlcModel model : models) {

                    if (model instanceof final Option option) {
                        final JsonObject json = Json.object();

                        json.put(idKey, option.id());
                        json.put(nameKey, option.getOption());
                        jsonArray.put(json);
                    }
                }
                return jsonArray;
            }
            case COMPANY, CONTACT, COMPANY_ONBOARD, CONTACT_ONBOARD -> {
                final TlcModel tlcModel = models.stream().findFirst().orElseGet(() -> null);
                final EntityConverter<TlcModel> converter = (EntityConverter<TlcModel>) entityConverterHolder
                        .getConverter(storeType);

                return Objects.isNull(converter) ? null : converter.convertPartialData(tlcModel);
            }
            case OWNER -> {
                final TlcModel model = models.stream().findFirst().orElse(null);

                return Objects.isNull(model) ? null : CrmUserConverter.getInstance().convert((CrmUser) model, true, false);
            }
            case MONETARY, DEAL_VALUE -> {
                final JsonObject jsonObject = Json.object();
                final TlcModel tlcModel = models.stream().findFirst().orElseGet(() -> null);

                if (Objects.nonNull(tlcModel)) {
                    jsonObject.put(FieldKeys.ID.getName(), tlcModel.id());

                    for (final Map.Entry<StoreSubType, FieldSubType> subtype : subTypes.entrySet()) {
                        final StoreSubType key = subtype.getKey();
                        final String keyName = key.getName();
                        final String value = keyName.equalsIgnoreCase(FieldKeys.VALUE.name()) ? FieldKeys.PRICE.name() : keyName;

                        jsonObject.put(value, convertSubType(tlcModel, key, subtype.getValue()));
                    }
                }
                return jsonObject;
            }
            default -> {
                final JsonObject jsonObject = Json.object();

                models.stream().findFirst().ifPresent(model -> {
                    jsonObject.put(FieldKeys.ID.getName(), model.id());
                    subTypes.forEach((key, value) -> jsonObject.put(key.getName(), convertSubType(model, key, value)));
                });
                return jsonObject;
            }
        }
    }

    /**
     * <p>
     * Converts the {@link Option} data into {@link JsonObject}.
     * </p>
     *
     * @param option          Represents the {@link Option}.
     * @param converterConfig Represents the {@link ConverterConfig}.
     * @return {@link JsonObject} of {@link Option}.
     */
    protected final JsonObject convertOptionToJson(final Option option, final ConverterConfig converterConfig) {
        final JsonObject jsonObject = Json.object();

        if (option.isEditable()) {
            jsonObject.put(FieldKeys.NAME.getName(), option.getOption());
        } else {
            jsonObject.put(FieldKeys.NAME.getName(), ((StaticOption) option).getOptionName(i18nService.getResolver(converterConfig.locale())));
        }

        jsonObject.put(FieldKeys.ID.getName(), option.id());

        return jsonObject;
    }

    /**
     * <p>
     * Sets the fields to {@link JsonObject}.
     * </p>
     *
     * @param value      Holds the value of fields.
     * @param jsonObject Represents the {@link JsonObject}.
     * @param fieldInfo  Represents the {@link FieldInfo}.
     */
    protected final void setValuesToJson(final Object value, final JsonObject jsonObject, final FieldInfo fieldInfo) {
        if (Objects.nonNull(value)) {

            if (value instanceof Collection) {
                jsonObject.put(FieldKeys.VALUE.getName(), convertSubTypesToJson(fieldInfo.storeType(), (Collection<? extends TlcModel>) value));
            } else if (value instanceof final TlcModel model) {
                jsonObject.put(FieldKeys.VALUE.getName(), convertSubTypesToJson(fieldInfo.storeType(), List.of(model)));
            } else if (value instanceof final Temporal temporal) {
                jsonObject.put(FieldKeys.VALUE.getName(), temporal.getTimeStamp());
            } else {
                jsonObject.put(FieldKeys.VALUE.getName(), value);
            }
        }
    }

    /**
     * <p>
     * Loads the field data to json.
     * </p>
     *
     * @param jsonObject      Represents the {@link JsonObject}.
     * @param fieldInfo       Represents the {@link FieldInfo}.
     * @param converterConfig Refers the {@link ConverterConfig} attributes.
     */
    protected void loadFieldsIntoJson(final JsonObject jsonObject, final FieldInfo fieldInfo, final ConverterConfig converterConfig) {
        final PresetConfig presetConfig = fieldInfo.presetConfig();
        final I18nKey placeHolder = fieldInfo.getPlaceHolder();
        final String summaryFieldType = presetConfig.getSummaryFieldType();
        final StoreType storeType = fieldInfo.storeType();
        final BiConsumer<Source, String> relatedFieldConsumer = (source, fieldAliasName) ->
                fieldService.getDefaultFieldByAliasName(source, fieldAliasName).ifPresent(relatedField -> {
                    final JsonObject relatedFieldJson = Json.object();

                    relatedFieldJson.put(FieldKeys.ID.getName(), relatedField.id());
                    relatedFieldJson.put(FieldKeys.SOURCE.getName(), source.getName());
                    jsonObject.put(FieldKeys.RELATED_FIELD.getName(), relatedFieldJson);
                });

        switch (storeType) {
            case CONTACT_ONBOARD, CONTACTS_ONBOARD -> relatedFieldConsumer.accept(Source.CONTACT, FieldKeys.FIRST_NAME.getName());
            case COMPANY_ONBOARD -> relatedFieldConsumer.accept(Source.COMPANY, FieldKeys.NAME.getName());
        }

        jsonObject.put(FieldKeys.FIELD_ID.getName(), fieldInfo.id());
        jsonObject.put(FieldKeys.ALIAS_NAME.getName(), fieldInfo.aliasName());
        jsonObject.put(FieldKeys.FIELD_TYPE.getName(), Objects.isNull(summaryFieldType) ? storeType.getName() : summaryFieldType);
        jsonObject.put(FieldKeys.FIELD_NAME.getName(), converterConfig.getI18nPlaceholderResolver().get(fieldInfo.i18nName()));
        jsonObject.put(FieldKeys.CONFIG.getName(), getConfig(fieldInfo, converterConfig.getI18nPlaceholderResolver()));

        if (Objects.nonNull(placeHolder)) {
            jsonObject.put(FieldTypeKeys.PLACEHOLDER.getName(), i18nService.getResolver(converterConfig.locale()).get(placeHolder));
        }

        if (presetConfig.contains(PresetConfig.Configuration.ROW_INDEX_ON_VIEW)) {
            jsonObject.put(FieldKeys.INDEX.getName(), fieldInfo.presetConfig().rowIndexOnView());
        }
    }

    /**
     * <p>
     * Converts the sub fields to {@link JsonObject}.
     * </p>
     *
     * @param entity          Holds the attributes of given entity.
     * @param converterConfig Represents the {@link ConverterConfig}.
     * @param jsonObject      Represents the {@link JsonObject}.
     */
    public final void convertSubFieldToJson(final T entity, final ConverterConfig converterConfig,
                                            final JsonObject jsonObject) {
        final FieldSource fieldSource = fieldSourceService.getSource(entity.getEntityType());
        final JsonObject fieldSubTypeJson = getFieldSubTypesByGroup(entity.orgId(), fieldSource, converterConfig);

        if (fieldSubTypeJson.size() > 0) {
            jsonObject.put(FieldKeys.SUB_FIELDS.getName(), fieldSubTypeJson);
        }
    }

    /**
     * <p>
     * Converts the summary view if fields into {@link JsonArray}.
     * </p>
     *
     * @param entity          Holds the attributes of given entity.
     * @param converterConfig Represents the {@link ConverterConfig}.
     * @return {@link JsonArray}
     */
    protected final JsonArray convertSummaryToJsonArray(final T entity, final ConverterConfig converterConfig) {
        final JsonArray jsonArray = Json.array();
        final Map<Integer, List<FieldInfo>> fieldData = getSummaryFields(entity);
        final Collection<FieldInfo> fieldList = fieldData.remove(-1);

        for (final Map.Entry<Integer, List<FieldInfo>> field : fieldData.entrySet()) {
            final Collection<FieldInfo> fieldInfoList = field.getValue();
            final JsonArray array = Json.array();

            for (final FieldInfo fieldInfo : fieldInfoList) {
                final JsonObject fieldJson = convertFieldInfoToJsonObject(fieldInfo, entity, converterConfig);

                if (fieldJson.size() > 0 && isDependableFieldAllowed(fieldInfo, fieldJson)) {
                    array.put(fieldJson);
                }
            }
            jsonArray.put(array);
        }
        final JsonArray firstRow = Objects.requireNonNullElseGet(jsonArray.optJsonArray(0), Json::array);
        final JsonArray secondRow = Objects.requireNonNullElseGet(jsonArray.optJsonArray(1), Json::array);

        if (Objects.nonNull(fieldList) && !fieldList.isEmpty()) {

            for (final FieldInfo fieldInfo : fieldList) {
                if (firstRow.size() > secondRow.size()) {
                    final JsonObject secondRowJson = convertFieldInfoToJsonObject(fieldInfo, entity, converterConfig);

                    if (secondRowJson.size() > 0 && isDependableFieldAllowed(fieldInfo, secondRowJson)) {
                        secondRow.put(secondRowJson);
                    }
                } else {
                    final JsonObject firstRowJson = convertFieldInfoToJsonObject(fieldInfo, entity, converterConfig);

                    if (firstRowJson.size() > 0 && isDependableFieldAllowed(fieldInfo, firstRowJson)) {
                        firstRow.put(firstRowJson);
                    }
                }
            }
            jsonArray.clear();
            jsonArray.put(firstRow);
            jsonArray.put(secondRow);
        }

        return jsonArray;
    }

    /**
     * <p>
     * Converts the profile view if fields into {@link JsonObject}.
     * </p>
     *
     * @param entity          Holds the attributes of given entity.
     * @param converterConfig Represents the {@link ConverterConfig}.
     * @return {@link JsonObject}
     */
    public final JsonObject convertProfileToJsonObject(final T entity, final ConverterConfig converterConfig) {
        final JsonObject jsonObject = Json.object();
        final Collection<FieldInfo> fieldList = getProfileFields(entity);

        for (final FieldInfo fieldInfo : fieldList) {
            jsonObject.put(fieldInfo.aliasName(), convertFieldInfoToJsonObject(fieldInfo, entity, converterConfig));
        }

        return jsonObject;
    }

    /**
     * <p>
     * Converts a profile picture for a given entity into a JSON representation.
     * </p>
     *
     * @param id         The unique identifier of the entity.
     * @param entityType The type of the entity.
     * @param thumbnail  Represents the thumbnail {@link DisplayPicture}.
     * @param original   Represents the original {@link DisplayPicture}.
     * @return A {@link JsonObject} containing the profile picture details.
     */
    public final JsonObject convertProfilePictureToJson(final Long id, final String entityType,
                                                        final DisplayPicture thumbnail, final DisplayPicture original) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(DisplayPictureFields.THUMBNAIL, convertThumbnailToJson(id, entityType, thumbnail));
        jsonObject.put(DisplayPictureFields.ORIGINAL, convertOriginalPictureToJson(id, entityType, original));

        return jsonObject;
    }

    /**
     * <p>
     * Converts the thumbnail to {@link JsonObject}.
     * </p>
     *
     * @param id               Represents the entity id.
     * @param entityType       Represents the entity type.
     * @param thumbnailPicture Represents the {@link DisplayPicture}.
     * @return the {@link JsonObject}.
     */
    public final JsonObject convertThumbnailToJson(final Long id, final String entityType, final DisplayPicture thumbnailPicture) {
        final JsonObject profileJson = Json.object();
        final String extension = thumbnailPicture.extension().getName();
        final String imageType = thumbnailPicture.type().getName();
        final JsonObject json = Json.object();

        json.put(DisplayPictureFields.SOURCE, entityType.toLowerCase());
        json.put(DisplayPictureFields.ENTITY_ID, id);
        json.put(DisplayPictureFields.ID, randomIdGenerator.generateUniqueId());

        profileJson.put(DisplayPictureFields.NAME, imageType);
        profileJson.put(DisplayPictureFields.EXTENSION, extension);
        profileJson.put(DisplayPictureFields.SIZE, thumbnailPicture.size());
        profileJson.put(DisplayPictureFields.URL, HttpUtil.buildAbsoluteUrl(String.format("%s%s%s%s%s%s", profilePicRedirectionUrl,
                Base64.getEncoder().encodeToString(json.getBytes()), "/", imageType, ".", extension)));

        return profileJson;
    }

    /**
     * <p>
     * Converts the original picture to {@link JsonObject}.
     * </p>
     *
     * @param id              Represents the entity id.
     * @param entityType      Represents the entity type.
     * @param originalPicture Represents the {@link DisplayPicture}.
     * @return the {@link JsonObject}.
     */
    private JsonObject convertOriginalPictureToJson(final Long id, final String entityType, final DisplayPicture originalPicture) {
        final JsonObject profileJson = Json.object();
        final String extension = originalPicture.extension().getName();
        final String imageType = originalPicture.type().getName();
        final JsonObject json = Json.object();

        json.put(DisplayPictureFields.SOURCE, entityType.toLowerCase());
        json.put(DisplayPictureFields.ENTITY_ID, id);
        json.put(DisplayPictureFields.ID, randomIdGenerator.generateUniqueId());

        profileJson.put(DisplayPictureFields.NAME, imageType);
        profileJson.put(DisplayPictureFields.EXTENSION, extension);
        profileJson.put(DisplayPictureFields.SIZE, originalPicture.size());
        profileJson.put(DisplayPictureFields.URL, HttpUtil.buildAbsoluteUrl(String.format("%s%s%s%s%s%s", profilePicRedirectionUrl,
                Base64.getEncoder().encodeToString(json.getBytes()), "/", imageType, ".", extension)));

        return profileJson;
    }

    /**
     * <p>
     * Converts the {@link FieldInfo} into {@link JsonObject}.
     * </p>
     *
     * @param fieldInfo       Represents the {@link FieldInfo}.
     * @param entity          Holds the attributes of given entity.
     * @param converterConfig Represents the {@link ConverterConfig}.
     * @return {@link JsonObject}
     */
    protected abstract JsonObject convertFieldInfoToJsonObject(final FieldInfo fieldInfo, final T entity, final ConverterConfig converterConfig);

    /**
     * <p>
     * Gets the config of the provided field in json format.
     * </p>
     *
     * @param fieldInfo             Represents the {@link FieldInfo}.
     * @param i18nResolutionAdapter Refers the {@link I18nResolutionAdapter}.
     * @return the config in json.
     */
    protected JsonObject getConfig(final FieldInfo fieldInfo, final I18nResolutionAdapter i18nResolutionAdapter) {
        final PresetConfig presetConfig = fieldInfo.presetConfig();
        final JsonObject jsonObject = Json.object();
        final String maxDateKey = FieldKeys.MAX_DATE.getName();

        jsonObject.put(Configuration.ADD_VIEW.getName(), presetConfig.addView());
        jsonObject.put(Configuration.READ_ONLY.getName(), presetConfig.readOnly());
        jsonObject.put(Configuration.IS_HIDDEN.getName(), presetConfig.hidden());
        jsonObject.put(Configuration.IS_IMMOVABLE.getName(), presetConfig.isImmovable());
        jsonObject.put(Configuration.IMMUTABLE.getName(), presetConfig.immutable());
        jsonObject.put(Configuration.IS_FIELD_ENABLED.getName(), presetConfig.isFieldEnabled());
        jsonObject.put(Configuration.IS_FILTERABLE.getName(), presetConfig.isFilterable());
        jsonObject.put(Configuration.IS_DEFAULT_FIELD.getName(), presetConfig.isDefaultField());
        jsonObject.put(Configuration.IS_DEFAULT_SYSTEM_FIELD.getName(), presetConfig.isDefaultSystemField());
        jsonObject.put(Configuration.EDITABLE_VIA_LIST.getName(), presetConfig.isEditableViaList());
        jsonObject.put(Configuration.SHOW_ON_PROFILE.getName(), presetConfig.showOnProfile());
        jsonObject.put(Configuration.DROPDOWN.getName(), presetConfig.dropdown());
        jsonObject.put(Configuration.DROPDOWN_TYPE.getName(), presetConfig.dropdownTypeName());
        jsonObject.put(Configuration.DEPENDS_ON.getName(), presetConfig.dependsOn());
        jsonObject.put(Configuration.ORDER.getName(), presetConfig.order());
        jsonObject.put(Configuration.GROUP_ID.getName(), presetConfig.getGroupId());
        jsonObject.put(Configuration.IS_GROUPED_FIELD.getName(), presetConfig.isGroupedField());

        if (StoreType.AUTO_NUMBER == fieldInfo.storeType()) {
            jsonObject.put(Configuration.PREFIX.getName(), presetConfig.getPrefix());
            jsonObject.put(Configuration.NO_OF_DIGITS.getName(), presetConfig.getNoOfDigits());
            jsonObject.put(Configuration.START_NUMBER.getName(), presetConfig.getStartNumber());
        }
        final JsonObject presetConfigJson = presetConfig.presetConfigAsJson();

        presetConfig.getMaxValue().ifPresent(value -> {
            jsonObject.put(maxDateKey, value);
            presetConfigJson.put(maxDateKey, value);
        });

        jsonObject.put(FieldKeys.VALIDATIONS.getName(), fieldConstraintBuilder.build(fieldInfo, presetConfigJson, i18nResolutionAdapter));

        return jsonObject;
    }

    /**
     * <p>
     * Checks whether the dependable field is allowed in the summary.
     * </p>
     *
     * @param fieldInfo Represents the {@link FieldInfo}.
     * @param field     Represents the field in json format.
     * @return {@code true} if the dependable field is allowed, {@code false} otherwise.
     */
    public boolean isDependableFieldAllowed(final FieldInfo fieldInfo, final JsonObject field) {
        final String configKey = FieldKeys.CONFIG.getName();

        if (field.containsKey(configKey)) {
            final JsonObject config = field.getJsonObject(configKey);
            final int groupId = (int) config.get(Configuration.GROUP_ID.getName());

            if (NumericalConstant.ZERO == groupId) {
                return true;
            }

            return field.containsKey(FieldKeys.VALUE.getName()) || fieldInfo.presetConfig().isDependableFieldAlwaysVisible();
        }
        return false;
    }

    /**
     * <p>
     * Sets the sales owner for the entities.
     * </p>
     *
     * @param orgId          Represents the organization id.
     * @param salesOwnerKey  Represents the sales owner field key.
     * @param jsonObject     Represents the json data.
     * @param entity         Represents the {@link Entity}.
     * @param currentOwnerId Represents the current sales owner id.
     */
    protected void setSalesOwner(final long orgId, final String salesOwnerKey, final JsonObject jsonObject, final Entity entity, final Long currentOwnerId) {
        if (jsonObject.containsKey(salesOwnerKey)) {
            entitySalesOwnerHandler.assignOwner(orgId, jsonObject.getLong(salesOwnerKey), entity);
        } else if (Objects.isNull(entity.getSalesOwner())) {
            entitySalesOwnerHandler.assignOwner(orgId, currentOwnerId, entity);
        } else if (Objects.isNull(entity.getOwnerId())) {
            entity.setOwnerId(currentOwnerId);
        }
    }

    /**
     * <p>
     * Converts the mobile summary view fields into {@link JsonArray}.
     * </p>
     *
     * @param entity          Holds the attributes of given entity.
     * @param isHidden        A boolean flag indicating whether to display empty fields or not.
     * @param converterConfig Represents the {@link ConverterConfig}.
     * @return {@link JsonArray}
     */
    protected JsonArray convertSummaryToJsonArray(final T entity, final boolean isHidden, final ConverterConfig converterConfig) {
        final Map<Integer, JsonArray> dependableFields = new HashMap<>();
        final JsonArray summary = Json.array();
        final Collection<FieldInfo> fields = secureFieldService.getFieldsBySource(entity.orgId(), entity.getEntityType());
        final Collection<JsonArray> fieldsArray = new ArrayList<>();

        for (final FieldInfo fieldInfo : fields) {
            final PresetConfig presetConfig = fieldInfo.presetConfig();

            if (!presetConfig.hidden()) {
                final int groupId = presetConfig.getGroupId();

                if (0 < groupId) {
                    final JsonArray array = dependableFields.computeIfAbsent(groupId, field -> {
                        final JsonArray dependableFieldsArray = Json.array();

                        fieldsArray.add(dependableFieldsArray);

                        return dependableFieldsArray;
                    });
                    final JsonObject fieldJson = convertFieldInfoToJsonObject(fieldInfo, entity, converterConfig);

                    if (fieldJson.containsKey(FieldKeys.VALUE.getName())) {
                        array.put(fieldJson);
                    }
                } else {
                    final JsonObject jsonObject = convertFieldInfoToJsonObject(fieldInfo, entity, converterConfig);

                    if (jsonObject.size() > 0) {
                        final JsonArray array = Json.array();

                        array.put(jsonObject);
                        fieldsArray.add(array);
                    }
                }
            }
        }

        fieldsArray.forEach(summary::put);

        return summary;
    }

    /**
     * <p>
     * Summarizes the view of fields that are present for each entity.
     * </p>
     *
     * @param entity          Holds the data of specific entity.
     * @param converterConfig Represents the {@link ConverterConfig}.
     * @param isHidden        A boolean flag indicating whether to display empty fields or not.
     * @return {@link JsonObject}
     */
    protected abstract JsonObject convertSummaryIntoJson(final T entity, final ConverterConfig converterConfig, final boolean isHidden);

    /**
     * <p>
     * Gets the fields by using the profile view.
     * </p>
     *
     * @param entity Holds the data of specific entity.
     * @return All the {@link FieldInfo} of the given fields.
     */
    private Collection<FieldInfo> getMobileProfileFields(final T entity) {
        final Collection<FieldInfo> fields = secureFieldService.getFieldsBySource(entity.orgId(), entity.getEntityType());

        return fields.stream()
                .filter(field -> {
                            final PresetConfig presetConfig = field.presetConfig();

                            return !field.isPersistent() && presetConfig.showOnProfile() && !presetConfig.isHiddenForMobile();
                        }
                ).toList();
    }

    /**
     * <p>
     * Converts the profile view if fields into {@link JsonObject}.
     * </p>
     *
     * @param entity          Holds the attributes of given entity.
     * @param converterConfig Represents the {@link ConverterConfig}.
     * @return {@link JsonObject}
     */
    protected final JsonObject convertProfileToJson(final T entity, final ConverterConfig converterConfig) {
        final JsonObject jsonObject = Json.object();
        final Collection<FieldInfo> fieldList = getMobileProfileFields(entity);

        for (final FieldInfo fieldInfo : fieldList) {
            jsonObject.put(fieldInfo.aliasName(), convertFieldInfoToJsonObject(fieldInfo, entity, converterConfig));
        }

        return jsonObject;
    }

    /**
     * <p>
     * Loads the entity summary fields.
     * </p>
     *
     * @param entity            Represents the {@link T}.
     * @param entitySummaryJson Represents the {@link JsonObject} that contains the entity summary data.
     * @param converterConfig   Represents the {@link ConverterConfig}.
     */
    public void loadSummaryFields(final T entity, final JsonObject entitySummaryJson, final ConverterConfig converterConfig) {
        final I18nResolver resolver = i18nService.getResolver(converterConfig.locale());
        final boolean isMobileClient = converterConfig.isMobileClient();
        final Map<Integer, List<FieldInfo>> summaryFields = isMobileClient ? getMobileSummaryFieldsV2(entity) : getSummaryFieldsV2(entity);
        final Map<String, List<JsonObject>> defaultfieldsJsonMap = new HashMap<>();
        final List<JsonObject> customFieldsJsonList = new ArrayList<>();
        final Map<String, Map<String, List<JsonObject>>> subCategoryFieldsMap = new HashMap<>();

        for (final List<FieldInfo> fieldInfoList : summaryFields.values()) {

            for (final FieldInfo fieldInfo : fieldInfoList) {
                final JsonObject fieldJson = convertFieldInfoToJsonObject(fieldInfo, entity, converterConfig);
                final PresetConfig presetConfig = fieldInfo.presetConfig();

                if (Objects.nonNull(presetConfig)) {

                    if (presetConfig.isDefaultField()) {

                        if (isMobileClient && presetConfig.hasMobileSummaryConfig()) {
                            final String category = presetConfig.getMobileCategory();

                            if (fieldJson.size() > 0 && isDependableFieldAllowed(fieldInfo, fieldJson)) {
                                final String subCategory = presetConfig.getMobileSubCategory();

                                if (Objects.nonNull(subCategory)) {
                                    subCategoryFieldsMap.computeIfAbsent(category, subCategoryMap -> new LinkedHashMap<>())
                                            .computeIfAbsent(subCategory, fields -> new LinkedList<>()).add(fieldJson);
                                }
                            }
                        } else if (!isMobileClient && presetConfig.hasSummaryConfig()) {
                            final String category = presetConfig.getCategory();

                            if (fieldJson.size() > 0 && isDependableFieldAllowed(fieldInfo, fieldJson)) {
                                defaultfieldsJsonMap.computeIfAbsent(category, fieldJsons -> new ArrayList<>()).add(fieldJson);

                                final String subCategory = presetConfig.getSubCategory();

                                if (Objects.nonNull(subCategory)) {
                                    subCategoryFieldsMap.computeIfAbsent(category, subCategoryMap -> new HashMap<>())
                                            .computeIfAbsent(subCategory, fields -> new ArrayList<>()).add(fieldJson);
                                }
                            }
                        }
                    } else {
                        customFieldsJsonList.add(fieldJson);
                    }
                }
            }
        }

        if (isMobileClient) {
            convertMobileSummary(entitySummaryJson, subCategoryFieldsMap, customFieldsJsonList);
        } else {
            convertWebSummary(entitySummaryJson, defaultfieldsJsonMap, subCategoryFieldsMap, resolver, customFieldsJsonList);
        }
    }

    /**
     * <p>
     * Converts the summary response for the web.
     * </p>
     *
     * @param entitySummaryJson    The entity summary response json.
     * @param defaultFieldsJsonMap The map containing the default fields json.
     * @param subCategoryFieldsMap The map containing the subcategory fields.
     * @param resolver             The {@link I18nResolver}.
     * @param customFieldsJsonList The collection containing custom fields json.
     */
    private void convertWebSummary(final JsonObject entitySummaryJson, final Map<String, List<JsonObject>> defaultFieldsJsonMap,
            final Map<String, Map<String, List<JsonObject>>> subCategoryFieldsMap, final I18nResolver resolver,
            final List<JsonObject> customFieldsJsonList) {
        final String tagsKey = FieldKeys.TAGS.getName();
        final String basicInformationKey = FieldKeys.BASIC_INFORMATION.getName();
        final List<JsonObject> tagFields = defaultFieldsJsonMap.get(tagsKey);
        final List<JsonObject> basicInfoFieldsList = defaultFieldsJsonMap.get(basicInformationKey);

        if (Objects.nonNull(tagFields) && !tagFields.isEmpty()) {
            entitySummaryJson.put(tagsKey, tagFields.get(0));
        }

        if (Objects.nonNull(basicInfoFieldsList) && !basicInfoFieldsList.isEmpty()) {
            final JsonObject basicInformationJson = Json.object();
            final JsonArray basicInformationFieldArray = Json.array();

            basicInfoFieldsList.forEach(basicInformationFieldArray::put);
            basicInformationJson.put(FieldKeys.VALUES.getName(), basicInformationFieldArray);
            entitySummaryJson.put(basicInformationKey, basicInformationJson);
        }

        if (subCategoryFieldsMap.containsKey(FieldKeys.SYSTEM_FIELDS.getName())) {
            loadDefaultFields(entitySummaryJson, subCategoryFieldsMap, resolver);
        }

        if (!customFieldsJsonList.isEmpty()) {
            final JsonArray customFieldArray = Json.array();

            customFieldsJsonList.forEach(customFieldArray::put);
            entitySummaryJson.put(FieldKeys.CUSTOM_FIELDS.getName(), customFieldArray);
        }
    }

    /**
     * <p>
     * Convert the response for the mobile summary.
     * </p>
     *
     * @param entitySummaryJson    The entity summary response json.
     * @param subCategoryFieldsMap The map containing the subcategory fields.
     * @param customFieldsJsonList The collection containing custom fields json.
     */
    private void convertMobileSummary(final JsonObject entitySummaryJson, final Map<String, Map<String, List<JsonObject>>> subCategoryFieldsMap,
            final List<JsonObject> customFieldsJsonList) {
        final String basicInformationKey = FieldKeys.BASIC_INFORMATION.getName();
        final Map<String, List<JsonObject>> basicInfoFieldMap = subCategoryFieldsMap.get(basicInformationKey);

        if (Objects.nonNull(basicInfoFieldMap) && !basicInfoFieldMap.isEmpty()) {
            final JsonArray basicInformationFieldArray = Json.array();

            for (final List<JsonObject> basicInfoFieldList : basicInfoFieldMap.values()) {
                final JsonArray fieldArray = Json.array();

                basicInfoFieldList.forEach(fieldJson -> fieldArray.put(fieldJson));
                basicInformationFieldArray.put(fieldArray);
            }
            entitySummaryJson.put(basicInformationKey, basicInformationFieldArray);
        }

        if (subCategoryFieldsMap.containsKey(FieldKeys.SYSTEM_FIELDS.getName())) {
            loadMobileSummaryFields(entitySummaryJson, subCategoryFieldsMap);
        }

        if (!customFieldsJsonList.isEmpty()) {
            final JsonArray customFieldArray = Json.array();

            customFieldsJsonList.forEach(customFieldArray::put);
            entitySummaryJson.put(FieldKeys.CUSTOM_FIELDS.getName(), customFieldArray);
        }
    }

    /**
     * <p>
     * Load the system fields for the entity summary.
     * </p>
     *
     * @param entitySummaryJson    Represents {@link JsonObject} contains the entity summary details.
     * @param subcategoryFieldsMap Represents the map containing the system fields data.
     */
    private void loadMobileSummaryFields(final JsonObject entitySummaryJson, final Map<String, Map<String, List<JsonObject>>> subcategoryFieldsMap) {
        final String systemFieldsKey = FieldKeys.SYSTEM_FIELDS.getName();
        final String locationKey = FieldKeys.LOCATION.getName();
        final String systemInformationKey = FieldKeys.SYSTEM_INFORMATION.getName();
        final Map<String, List<JsonObject>> systemFieldsMap = subcategoryFieldsMap.get(systemFieldsKey);

        if (Objects.nonNull(systemFieldsMap) && !systemFieldsMap.isEmpty()) {
            final JsonArray editableFieldArray = Json.array();

            systemFieldsMap.values().forEach(fields -> {
                final JsonArray fieldArray = Json.array();

                fields.forEach(value -> fieldArray.put(value));
                editableFieldArray.put(fieldArray);
            });
            entitySummaryJson.put(systemFieldsKey, editableFieldArray);
        }
        final Map<String, List<JsonObject>> locationFields = subcategoryFieldsMap.get(locationKey);

        if (Objects.nonNull(locationFields) && !locationFields.isEmpty()) {
            final JsonArray locationFieldArray = Json.array();

            locationFields.values().forEach(fields -> fields.forEach(field -> locationFieldArray.put(field)));
            entitySummaryJson.put(locationKey, locationFieldArray);
        }
        final Map<String, List<JsonObject>> systemInformationFields = subcategoryFieldsMap.get(FieldKeys.SYSTEM_INFORMATION.getName());

        if (Objects.nonNull(systemInformationFields) && !systemInformationFields.isEmpty()) {
            final JsonArray immutableFieldArray = Json.array();

            systemInformationFields.values().forEach(fields -> fields.forEach(field -> immutableFieldArray.put(field)));
            entitySummaryJson.put(systemInformationKey, immutableFieldArray);
        }
    }

    /**
     * <p>
     * Load the system fields for the entity summary.
     * </p>
     *
     * @param entitySummaryJson  Represents {@link JsonObject} contains the entity summary details.
     * @param systemFieldsMap    Represents the map containing the system fields data.
     * @param resolver           Represents the {@link I18nResolver}.
     */
    private void loadDefaultFields(final JsonObject entitySummaryJson, final Map<String, Map<String, List<JsonObject>>> systemFieldsMap,
            final I18nResolver resolver) {
        final String nameKey = FieldKeys.NAME.getName();
        final String valuesKey = FieldKeys.VALUES.getName();
        final String systemFieldsKey = FieldKeys.SYSTEM_FIELDS.getName();
        final String tagsKey = FieldKeys.TAGS.getName();
        final JsonArray systemInfoJson = Json.array();
        final Map<String, List<JsonObject>> subFieldMap = systemFieldsMap.get(systemFieldsKey);
        final List<JsonObject> editableFields = subFieldMap.get(FieldKeys.EDITABLE_FIELDS.getName());

        if (Objects.nonNull(editableFields) && !editableFields.isEmpty()) {
            final JsonObject editableFieldsJson = Json.object();
            final JsonArray editableFieldArray = Json.array();

            if (entitySummaryJson.containsKey(tagsKey)) {
                editableFields.add(entitySummaryJson.getJsonObject(tagsKey));
            }
            editableFields.forEach(editableFieldArray::put);
            editableFieldsJson.put(valuesKey, editableFieldArray);
            systemInfoJson.put(editableFieldsJson);
        }
        final List<JsonObject> locationFields = subFieldMap.get(FieldKeys.LOCATION.getName());

        if (Objects.nonNull(locationFields) && !locationFields.isEmpty()) {
            final JsonObject locationJson = Json.object();
            final JsonArray locationFieldArray = Json.array();

            locationFields.forEach(locationFieldArray::put);
            locationJson.put(nameKey, resolver.get("i18n.entity.field.location"));
            locationJson.put(valuesKey, locationFieldArray);
            systemInfoJson.put(locationJson);
        }
        final List<JsonObject> socialProfileFields = subFieldMap.get(FieldKeys.SOCIAL_PROFILES.getName());

        if (Objects.nonNull(socialProfileFields) && !socialProfileFields.isEmpty()) {
            final JsonObject socialProfilesJson = Json.object();
            final JsonArray socialProfileFieldArray = Json.array();

            socialProfileFields.forEach(socialProfileFieldArray::put);
            socialProfilesJson.put(nameKey, resolver.get("i18n.entity.field.socialProfiles"));
            socialProfilesJson.put(valuesKey, socialProfileFieldArray);
            systemInfoJson.put(socialProfilesJson);
        }
        final List<JsonObject> immutableFields = subFieldMap.get(FieldKeys.IMMUTABLE_FIELDS.getName());

        if (Objects.nonNull(immutableFields) && !immutableFields.isEmpty()) {
            final JsonObject immutableFieldsJson = Json.object();
            final JsonArray immutableFieldArray = Json.array();

            immutableFields.forEach(immutableFieldArray::put);
            immutableFieldsJson.put(nameKey, resolver.get("i18n.entity.field.systemInformation"));
            immutableFieldsJson.put(valuesKey, immutableFieldArray);
            systemInfoJson.put(immutableFieldsJson);
        }
        entitySummaryJson.put(systemFieldsKey, systemInfoJson);
    }

    /**
     * <p>
     * Appends a value to the StringBuilder if it's not empty, ensuring no trailing comma.
     * </p>
     *
     * @param builder Represents the {@link StringBuilder}.
     * @param value   Represents the string value to be appended.
     */
    protected void appendIfNotEmpty(final StringBuilder builder, final String value) {
        if (!value.isBlank()) {
            if (!builder.isEmpty()) {
                builder.append(", ");
            }
            builder.append(value.trim());
        }
    }

    /**
     * <p>
     * Writes common email summary values into the response json.
     * </p>
     *
     * @param jsonObject           Represents the {@link JsonObject}.
     * @param lastEmailSentKey     Represents the key for last email sent.
     * @param lastEmailSent        Represents the last email sent timestamp.
     * @param emailMessageCountKey Represents the key for email message count.
     * @param emailMessageCount    Represents the email message count.
     * @param lastEmailReceivedKey Represents the key for last email received.
     * @param lastEmailReceived    Represents the last email received timestamp.
     */
    protected void loadEmailSummaryFields(final JsonObject jsonObject, final String lastEmailSentKey, final Long lastEmailSent,
                                          final String emailMessageCountKey, final Long emailMessageCount,
                                          final String lastEmailReceivedKey, final Long lastEmailReceived) {
        jsonObject.put(lastEmailSentKey, lastEmailSent);
        jsonObject.put(emailMessageCountKey, emailMessageCount);
        jsonObject.put(lastEmailReceivedKey, lastEmailReceived);
    }

    /**
     * <p>
     * Converts the {@link TlcModel} into json object.
     * </p>
     *
     * @param fieldId represents the field ID.
     * @param model   represents the {@link TlcModel} which contains the data.
     * @return the converted value.
     */
    protected Object convertModelToValue(final Long fieldId, final TlcModel model) {
        final FieldInfo fieldInfo = secureFieldService.getField(model.orgId(), fieldId);

        if (Objects.nonNull(fieldInfo)) {
            final StoreType storeType = fieldInfo.storeType();

            return switch (storeType) {
                case DROPDOWN, MULTI_SELECT -> {
                    final Option option = (Option) model;

                    yield option.getOption();
                }
                case OWNER -> {
                    final CrmUser owner = (CrmUser) model;

                    yield owner.getFullName();
                }
                case COMPANY, CONTACT -> {
                    final JsonObject entityJson = convertEntityToJson(storeType, model);

                    if (Objects.nonNull(entityJson)) {
                        yield entityJson;
                    }

                    final EntityConverter<T> converter = (EntityConverter<T>) entityConverterHolder.getConverter(storeType);

                    yield Objects.isNull(converter) ? null : converter.convertPartialData((T) model);
                }
                case TIME_RANGE, DATE_RANGE, DATE_TIME_RANGE -> {
                    final Range range = (Range) model;
                    final Map<StoreSubType, FieldSubType> groupedSubtypes = fieldTypeService.groupSubtypesByStoreSubType(storeType);
                    final JsonObject jsonObject = Json.object();

                    if (Objects.nonNull(groupedSubtypes) && !groupedSubtypes.isEmpty()) {
                        jsonObject.put(StoreSubType.FROM.getName(), range.getFrom().getTimeStamp());
                        jsonObject.put(StoreSubType.TO.getName(), range.getTo().getTimeStamp());
                    }

                    yield jsonObject;
                }
                default -> {
                    final EntityConverter<TlcModel> converter = (EntityConverter<TlcModel>) EntityConverterHolder.getInstance().getConverter(storeType);

                    yield Objects.isNull(converter) ? null : converter.convert(model);
                }
            };
        }

        return null;
    }

    /**
     * <p>
     * Allows child converters to define entity-specific reference mapping for COMPANY/CONTACT store types.
     * </p>
     *
     * @param storeType Represents the {@link StoreType}.
     * @param model     Represents the {@link TlcModel}.
     * @return the mapped reference value, or {@code null} if no mapping is defined.
     */
    protected JsonObject convertEntityToJson(final StoreType storeType, final TlcModel model) {
        return null;
    }

    /**
     * <p>
     * Converts a value holder map into response json using field-aware value conversion.
     * </p>
     *
     * @param jsonObject  Represents the {@link JsonObject}.
     * @param valueHolder Represents the {@link ValueHolder}.
     */
    protected void loadValueHolderToJson(final JsonObject jsonObject, final ValueHolder valueHolder) {
        if (Objects.nonNull(valueHolder)) {
            valueHolder.toMap().forEach((key, value) -> {

                if (value instanceof final TlcModel model) {
                    jsonObject.put(key.toString(), convertModelToValue(key, model));
                } else if (value instanceof final Collection<?> models) {
                    final JsonArray jsonArray = Json.array();

                    for (final Object model : models) {
                        jsonObject.put(key.toString(), model instanceof TlcModel tlcModel
                                ? jsonArray.put(convertModelToValue(key, tlcModel)) : value);
                    }
                } else if (value instanceof final Date date) {
                    jsonObject.put(key.toString(), date.getTimeStamp());
                } else {
                    jsonObject.put(key.toString(), value);
                }
            });
        }
    }

    /**
     * <p>
     * Flattens persistent complex field values (address/monetary/email/phone) into displayable strings.
     * </p>
     *
     * @param orgId      Represents the organization id.
     * @param sourceType Represents the source type.
     * @param jsonObject Represents the {@link JsonObject}.
     */
    protected void loadPersistentFieldValues(final long orgId, final String sourceType, final JsonObject jsonObject) {
        final Collection<FieldInfo> persistentFieldsBySource = secureFieldService.getPersistentFieldsBySource(orgId, sourceType);

        persistentFieldsBySource.forEach(fieldInfo -> {
            final String fieldId = fieldInfo.id().toString();
            final StoreType storeType = fieldInfo.storeType();
            final String idAsString = fieldInfo.idAsString();

            switch (storeType) {
                case ADDRESS -> {
                    final JsonObject addressJson = jsonObject.optJsonObject(fieldId);

                    if (Objects.nonNull(addressJson)) {
                        final Map<StoreSubType, FieldSubType> addressSubTypes = fieldTypeService.groupSubtypesByStoreSubType(StoreType.ADDRESS);
                        final String countrySubFieldKey = addressSubTypes.get(StoreSubType.COUNTRY).idAsString();
                        final StringBuilder addressBuilder = new StringBuilder();

                        appendIfNotEmpty(addressBuilder, addressJson.optString(addressSubTypes.get(StoreSubType.ADDRESS_LINE1).idAsString(), EMPTY_STRING));
                        appendIfNotEmpty(addressBuilder, addressJson.optString(addressSubTypes.get(StoreSubType.ADDRESS_LINE2).idAsString(), EMPTY_STRING));
                        appendIfNotEmpty(addressBuilder, addressJson.optString(addressSubTypes.get(StoreSubType.CITY).idAsString(), EMPTY_STRING));
                        appendIfNotEmpty(addressBuilder, addressJson.optString(addressSubTypes.get(StoreSubType.STATE).idAsString(), EMPTY_STRING));
                        appendIfNotEmpty(addressBuilder, addressJson.containsKey(countrySubFieldKey)
                                ? Country.getCountryById(addressJson.getInt(countrySubFieldKey)).getName() : EMPTY_STRING);
                        appendIfNotEmpty(addressBuilder, addressJson.optString(addressSubTypes.get(StoreSubType.ZIPCODE).idAsString(), EMPTY_STRING));

                        jsonObject.put(idAsString, addressBuilder.toString());
                    }
                }
                case MONETARY -> {
                    final JsonObject monetaryJson = jsonObject.optJsonObject(fieldId);

                    if (Objects.nonNull(monetaryJson)) {
                        final Map<StoreSubType, FieldSubType> monetarySubTypes = fieldTypeService.groupSubtypesByStoreSubType(StoreType.MONETARY);
                        final String currencyId = monetaryJson.getString(monetarySubTypes.get(StoreSubType.CURRENCY).idAsString());

                        jsonObject.put(idAsString, String.format("%s %s", Currency.getCurrencyById(Short.parseShort(currencyId)).getName(),
                                monetaryJson.getString(monetarySubTypes.get(StoreSubType.VALUE).idAsString())));
                    }
                }
                case EMAIL -> {
                    final JsonObject emailJson = jsonObject.optJsonObject(fieldId);

                    if (Objects.nonNull(emailJson)) {
                        final Map<StoreSubType, FieldSubType> emailSubTypes = fieldTypeService.groupSubtypesByStoreSubType(StoreType.EMAIL);

                        jsonObject.put(idAsString, emailJson.getString(emailSubTypes.get(StoreSubType.EMAIL_VALUE).idAsString()));
                    }
                }
                case PHONE -> {
                    final JsonObject phoneJson = jsonObject.optJsonObject(fieldId);

                    if (Objects.nonNull(phoneJson)) {
                        final Map<StoreSubType, FieldSubType> phoneSubTypes = fieldTypeService.groupSubtypesByStoreSubType(StoreType.PHONE);

                        jsonObject.put(idAsString, phoneJson.getString(phoneSubTypes.get(StoreSubType.PHONE_NUMBER).idAsString()));
                    }
                }
            }
        });
    }

    /**
     * <p>
     * Builds an {@link Option} with the provided attributes.
     * </p>
     *
     * @param orgId    Represents the organization id.
     * @param fieldId  Represents the field id.
     * @param id       Represents the option id.
     * @param isStatic Represents whether the option is static or not.
     * @return {@link Option}
     */
    protected Option buildOption(final Long orgId, final Long fieldId, final Long id, final boolean isStatic) {
        final Option option = new Option();

        option.setId(id);
        option.setStaticOption(isStatic);
        option.setFieldId(fieldId);
        option.setOrgId(orgId);

        return option;
    }

    /**
     * <p>
     * Sets the options for the specific field of an entity.
     * </p>
     *
     * @param orgId               The organization ID to which the entity belongs.
     * @param jsonObject          Represents {@link JsonObject}.
     * @param fieldKey            The key in the {@link JsonObject} representing the field to set.
     * @param fieldOptionConsumer A {@link java.util.function.Consumer} that accepts an {@link Option} to set the value for the field.
     * @param fieldId             The ID of the field for which the option is being set.
     * @param isStatic            A boolean flag indicating whether the option is static or not.
     */
    protected void setOptions(final long orgId, final JsonObject jsonObject, final String fieldKey,
                            final java.util.function.Consumer<Option> fieldOptionConsumer, final Long fieldId, final boolean isStatic) {
        if (jsonObject.containsKey(fieldKey)) {
            if (jsonObject.isKeyPresentWithNullValue(fieldKey)) {
                fieldOptionConsumer.accept(null);
            } else {
                fieldOptionConsumer.accept(buildOption(orgId, fieldId, jsonObject.getLong(fieldKey), isStatic));
            }
        }
    }

    /**
     * <p>
     * Loads the summary field preference for a specific user and source into the provided JSON object.
     * </p>
     *
     * @param orgId             Represents the organization ID.
     * @param userId            Represents the user ID.
     * @param sourceId          Represents the source ID.
     * @param entitySummaryJson Represents the summary json
     */
    protected void loadSummaryFieldPreference(final Long orgId, final Long userId, final int sourceId, final JsonObject entitySummaryJson) {
        final SummaryFieldPreference summaryFieldPreference = summaryFieldPreferenceManager.get(orgId, userId, sourceId);

        if (Objects.nonNull(summaryFieldPreference)) {
            entitySummaryJson.put(FieldKeys.FIELD_PREFERENCE.getName(), summaryFieldPreference.getPreferenceAsJson());
        }
    }
}