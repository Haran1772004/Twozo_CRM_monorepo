package com.tlc.crm.core.api.deal.model.converter;

import com.tlc.attachment.model.profilePicture.DisplayPicture;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.iam.api.model.converter.CrmUserConverter;
import com.tlc.crm.common.iam.api.model.converter.TerritoryConverter;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.Territory;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.core.api.deal.manager.PipelineStageManager;
import com.tlc.crm.core.api.summaryInfo.model.converter.SummaryInfoConverter;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.forecastcategory.manager.ForecastCategoryManager;
import com.tlc.crm.entity.forecastcategory.model.ForecastCategory;
import com.tlc.crm.entity.models.PresetConfig;

import com.tlc.crm.entity.models.type.converter.OptionConverter;
import com.tlc.i18n.I18nKey;
import com.tlc.i18n.I18nResolver;
import com.tlc.crm.fields.service.settings.SourceSettingsService;
import com.tlc.l10n.model.currency.Currency;
import com.tlc.crm.core.api.summaryInfo.model.SummaryInfo;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.contact.model.converter.CompanyConverter;
import com.tlc.crm.core.api.contact.model.converter.ContactConverter;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.DealStatistics;
import com.tlc.crm.core.api.deal.model.DealValue;
import com.tlc.crm.core.api.deal.model.EmailSummary;
import com.tlc.crm.core.api.deal.model.Participant;
import com.tlc.crm.core.api.deal.model.Pipeline;
import com.tlc.crm.core.api.deal.model.PipelineStage;
import com.tlc.crm.core.api.deal.model.ProductDetail;
import com.tlc.crm.core.api.deal.model.ProductDetailsHolder;
import com.tlc.crm.core.features.api.activity.model.ActivitySummary;
import com.tlc.crm.core.features.api.deal.model.MergeType;
import com.tlc.crm.core.field.contact.CompanyFields;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.core.field.deal.DealFields;
import com.tlc.crm.core.field.deal.ProductDetailsHolderFields;
import com.tlc.crm.entity.builder.ViewConverter;
import com.tlc.crm.entity.currency.BaseCurrencyService;
import com.tlc.crm.entity.features.api.tag.model.Tag;
import com.tlc.crm.entity.features.api.tag.model.converter.TagConverter;
import com.tlc.crm.entity.features.field.tag.TagFields;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.fields.models.FieldSubType;
import com.tlc.crm.fields.models.StoreSubType;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.entity.models.ValueHolder;
import com.tlc.crm.entity.models.type.Monetary;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.entity.models.type.converter.MonetaryConverter;
import com.tlc.crm.fields.service.FieldTypeService;
import com.tlc.crm.common.util.NumberUtils;
import com.tlc.commons.util.TypeChecker;
import com.tlc.l10n.temporal.model.Date;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.l10n.temporal.resolver.DateUtil;
import com.tlc.l10n.temporal.resolver.DateTimeUtil;
import com.tlc.validator.TlcModel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Provides different ways to convert the data from / into {@link Deal}
 *
 * @author Selvakumar
 * @version 1.0
 */
public class DealConverter extends ViewConverter<Deal> {

    private static final String PRODUCT_NAME = Source.PRODUCT.getName();
    private static final int TEN_THOUSANDS = 10000;

    private final DealManager dealManager;
    private final ContactConverter contactConverter;
    private final CompanyConverter companyConverter;
    private final MonetaryConverter monetaryConverter;
    private final ProductDetailsHolderConverter productDetailsHolderConverter;
    private final PipelineStageConverter pipelineStageConverter;
    private final PipelineConverter pipelineConverter;
    private final UserService userService;
    private final CrmUserConverter crmUserConverter;
    private final TerritoryConverter territoryConverter;
    private final TagConverter tagConverter;
    private final BaseCurrencyService baseCurrencyService;
    private final DealValueConverter dealValueConverter;
    private final SummaryInfoConverter summaryInfoConverter;
    private final DateTimeUtil dateTimeUtil;
    private final DateUtil dateUtil;
    private final SourceSettingsService sourceSettingsService;
    private final OptionConverter optionConverter;
    private final ForecastCategoryManager forecastCategoryManager;

    private static class Instance {
        private static final DealConverter INSTANCE = new DealConverter();
    }

    private DealConverter() {
        this.dealManager = DealManager.getInstance();
        this.contactConverter = ContactConverter.getInstance();
        this.monetaryConverter = MonetaryConverter.getInstance();
        this.companyConverter = CompanyConverter.getInstance();
        this.productDetailsHolderConverter = ProductDetailsHolderConverter.getInstance();
        this.pipelineStageConverter = PipelineStageConverter.getInstance();
        this.pipelineConverter = PipelineConverter.getInstance();
        this.userService = UserService.getInstance();
        this.crmUserConverter = CrmUserConverter.getInstance();
        this.territoryConverter = TerritoryConverter.getInstance();
        this.tagConverter = TagConverter.getInstance();
        this.baseCurrencyService = BaseCurrencyService.getInstance();
        this.dealValueConverter = DealValueConverter.getInstance();
        this.summaryInfoConverter = SummaryInfoConverter.getInstance();
        this.dateTimeUtil = DateTimeUtil.getInstance();
        this.dateUtil = DateUtil.getInstance();
        this.sourceSettingsService = SourceSettingsService.getInstance();
        this.optionConverter = OptionConverter.getInstance();
        forecastCategoryManager =  ForecastCategoryManager.getInstance();
    }

    public static DealConverter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Converts the {@link JsonObject} into {@link Deal}
     *
     * @param converterConfig Converter Configuration
     * @param jsonObject      which contains the data
     * @return {@link Deal} deal entity
     */
    @Override
    public Deal convert(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        if (converterConfig.isPartialData()) {
            return buildDealByPartialData(converterConfig, jsonObject);
        } else {
            return buildDeal(converterConfig, jsonObject);
        }
    }

    /**
     * Converts the {@link JsonObject} into {@link Deal}
     *
     * @param converterConfig Converter Configuration
     * @param jsonObject      which contains the data
     * @return {@link Deal} deal entity
     */
    private Deal buildDeal(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final Deal deal = new Deal();
        final String idKey = DealFields.ID;
        final long orgId = converterConfig.orgId();

        if (jsonObject.containsKey(idKey)) {
            final long id = jsonObject.getLong(idKey);
            final Deal existingDeal = dealManager.get(orgId, id);

            deal.setId(id);
            deal.setSalesOwner(existingDeal.getSalesOwner());
            deal.setForecastCategory(existingDeal.getForecastCategory());
        } else {
            deal.setCreatedBy(userService.getUser(orgId, converterConfig.ownerId()));
        }

        setJsonToDeal(converterConfig, deal, jsonObject);
        return deal;
    }

    /**
     * Converts the partial json into the deal
     *
     * @param converterConfig Converter Configuration
     * @param jsonObject      which contains the partial deal data
     * @return {@link Deal} which is filled with the deal data
     */
    private Deal buildDealByPartialData(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final Deal deal = dealManager.get(converterConfig.orgId(), jsonObject.getLong(DealFields.ID));

        setJsonToDeal(converterConfig, deal, jsonObject);
        return deal;
    }

    /**
     * Sets the json data into the {@link Deal}
     *
     * @param converterConfig Converter Configuration
     * @param deal            to be filled with the data
     * @param jsonObject      which contains the data
     */
    public void setJsonToDeal(final ConverterConfig converterConfig, final Deal deal, final JsonObject jsonObject) {
        final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(Deal.ENTITY_TYPE);
        final Long orgId = converterConfig.orgId();
        final Long ownerId = converterConfig.ownerId();
        final String titleId = fields.get(DealFields.TITLE).idAsString();
        final String contact = fields.get(DealFields.PRIMARY_CONTACT).idAsString();
        final String company = fields.get(DealFields.COMPANY).idAsString();
        final String participants = fields.get(DealFields.RELATED_CONTACTS).idAsString();
        final String monetaryKey = fields.get(DealFields.DEAL_VALUE).idAsString();
        final String forecastCategoryKey = fields.get(DealFields.FORECAST_CATEGORY).idAsString();

        final FieldInfo stateField = fields.get(DealFields.STATUS);
        final FieldInfo typeField = fields.get(DealFields.TYPE);
        final FieldInfo sourceField = fields.get(DealFields.DEAL_SOURCE);
        final FieldInfo lostReasonField = fields.get(DealFields.LOST_REASON);
        final FieldInfo wonReasonField = fields.get(DealFields.WON_REASON);
        final FieldInfo paymentField = fields.get(DealFields.PAYMENT_STATUS);

        final String stateIdKey = stateField.idAsString();
        final String typeKey = typeField.idAsString();
        final String sourceKey = sourceField.idAsString();
        final String lostReasonKey = lostReasonField.idAsString();
        final String wonReasonKey = wonReasonField.idAsString();
        final String paymentKey = paymentField.idAsString();
        final String closeDateIdKey = fields.get(DealFields.EXPECTED_CLOSE_DATE).idAsString();
        final String closedTimeKey = fields.get(DealFields.DEAL_CLOSED_ON).idAsString();
        final String descriptionKey = fields.get(DealFields.DESCRIPTION).idAsString();
        final String salesOwnerKey = fields.get(DealFields.SALES_OWNER).idAsString();
        final String territoryKey = fields.get(DealFields.TERRITORY).idAsString();
        final String tagKey = fields.get(DealFields.TAGS).idAsString();
        final String pipelineStageKey = fields.get(DealFields.PIPELINE_STAGE).idAsString();
        final String recentNoteKey = fields.get(DealFields.RECENT_NOTE).idAsString();
        final String isArchivedKey = DealFields.IS_ARCHIVED;
        final boolean hasForecastCategory = jsonObject.containsKey(forecastCategoryKey);

        deal.setOrgId(orgId);
        setSalesOwner(orgId, salesOwnerKey, jsonObject, deal, ownerId);
        deal.setUpdatedBy(userService.getUser(orgId, ownerId));
        deal.setTitle(jsonObject.optNullableValue(titleId, deal.getTitle()));
        deal.setDescription(jsonObject.optNullableValue(descriptionKey, deal.getDescription()));
        setOptions(orgId, jsonObject, sourceKey, deal::setSource, sourceField.id(), true);
        setOptions(orgId, jsonObject, typeKey, deal::setType, typeField.id(), false);
        setOptions(orgId, jsonObject, paymentKey, deal::setPaymentStatus, paymentField.id(), false);

        if (jsonObject.containsKey(recentNoteKey)) {
            deal.setRecentNote(jsonObject.getString(recentNoteKey));
        }

        if (jsonObject.containsKey(closedTimeKey)) {
            deal.setClosedTime(dateTimeUtil.getTemporal(jsonObject.getNullableValue(closedTimeKey)));
        }

        if (jsonObject.containsKey(lostReasonKey) && !jsonObject.isKeyPresentWithNullValue(lostReasonKey)) {
            setOptions(orgId, jsonObject, lostReasonKey, deal::setLostReason, lostReasonField.id(), false);

            deal.setWonReason(null);
        } else if (jsonObject.containsKey(wonReasonKey) && !jsonObject.isKeyPresentWithNullValue(wonReasonKey)) {
            setOptions(orgId, jsonObject, wonReasonKey, deal::setWonReason, wonReasonField.id(), false);

            deal.setLostReason(null);
        } else if (converterConfig.isPartialData() && jsonObject.containsKey(pipelineStageKey)) {
            deal.setLostReason(null);
            deal.setWonReason(null);
        }

        if (jsonObject.containsKey(territoryKey)) {
            if (jsonObject.isKeyPresentWithNullValue(territoryKey)) {
                deal.setTerritory(null);
            } else {
                final Long territoryId = jsonObject.getLong(territoryKey);
                final Territory territory = new Territory();

                territory.setId(territoryId);
                deal.setTerritory(territory);
            }
        }

        if (hasForecastCategory) {
            if (jsonObject.isKeyPresentWithNullValue(forecastCategoryKey)) {
                deal.setForecastCategory(null);
            } else {
                final ForecastCategory forecastCategory = new ForecastCategory();

                forecastCategory.setId(jsonObject.getLong(forecastCategoryKey));
                deal.setForecastCategory(forecastCategory);
            }
        }

        if (jsonObject.containsKey(closeDateIdKey)) {
            if (jsonObject.isKeyPresentWithNullValue(closeDateIdKey)) {
                deal.setExpectedCloseDate(null);
            } else {
                deal.setExpectedCloseDate(dateUtil.getTemporal(jsonObject.getLong(closeDateIdKey)));

                if (!hasForecastCategory || Objects.isNull(deal.getForecastCategory())) {
                    deal.setForecastCategory(forecastCategoryManager.getDefaultCategory(orgId));
                }
            }
        }

        if (jsonObject.containsKey(contact) && jsonObject.isKeyPresentWithNullValue(contact)) {
            deal.setPrimaryContact(null);
        } else {
            buildContact(contact, converterConfig, deal, jsonObject);
        }

        if (jsonObject.containsKey(participants)) {
            if (jsonObject.isKeyPresentWithNullValue(participants)) {
                deal.setParticipants(null);
            } else {
                buildParticipants(orgId, converterConfig, deal, jsonObject.getJsonArray(participants));
            }
        }

        if (jsonObject.containsKey(company) && jsonObject.isKeyPresentWithNullValue(company)) {
            deal.setCompany(null);
        } else {
            buildCompany(company, converterConfig, deal, jsonObject);
        }

        buildPipeline(fields, deal, jsonObject);

        if (jsonObject.containsKey(stateIdKey)) {
            deal.setState(Deal.State.getStateById(jsonObject.getInt(stateIdKey)));
        } else {
            final PipelineStage pipelineStage = deal.getPipelineStage();

            if (Objects.nonNull(pipelineStage)) {
                deal.setState(getDealState(orgId, pipelineStage.id()));
            }
        }

        if (jsonObject.containsKey(monetaryKey) && jsonObject.isKeyPresentWithNullValue(monetaryKey)) {
            final Monetary defaultMonetary = getDefaultMonetary(orgId);

            deal.setDealValue(new DealValue(defaultMonetary.orgId(), defaultMonetary.id(), defaultMonetary.getCurrency(), defaultMonetary.getValue()));
            deal.setProductDetailsHolder(null);
        } else {
            buildMonetary(fields, orgId, deal, converterConfig, jsonObject);
        }

        if (jsonObject.containsKey(tagKey)) {
            deal.setSummaryInfo(summaryInfoConverter.buildTags(tagKey, deal.getSummaryInfo(), converterConfig, jsonObject));
        }

        if (jsonObject.containsKey(isArchivedKey) && jsonObject.getBoolean(isArchivedKey)) {
            deal.setStatus(Deal.Status.ARCHIVED.getId());
        }

        if (converterConfig.areReadOnlyFieldsRequired()) {
            final String createdAtKey = fields.get(DealFields.DEAL_CREATED).idAsString();

            if (jsonObject.containsKey(createdAtKey)) {
                deal.setCreatedTime(new DateTime(jsonObject.getLong(createdAtKey)));
            }
        }
        entityBuilder.buildValueHolder(deal, converterConfig, jsonObject);
        defaultFieldValuesLoader.load(deal);
    }

    /**
     * <p>
     * Gets the deal state with the pipeline stage id.
     * </p>
     *
     * @param orgId           Represents the organization id.
     * @param pipelineStageId Represents the {@link PipelineStage} id.
     * @return The {@link Deal.State}.
     */
    private Deal.State getDealState(final Long orgId, final Long pipelineStageId) {
        final String pipelineStageName = PipelineStageManager.getInstance().getPipelineStageName(orgId, pipelineStageId);

        if (Objects.nonNull(pipelineStageName)) {
            return Deal.State.getStateByName(pipelineStageName);
        }
        return Deal.State.OPEN;
    }

    /**
     * Builds the collection of {@link Participant} entity from the given json objects and map it to deal
     *
     * @param converterConfig Converter configuration
     * @param deal            An entity that holds the attributes of deal
     * @param participants    Holds the field values of participants
     */
    private void buildParticipants(final Long orgId, final ConverterConfig converterConfig, final Deal deal, final JsonArray participants) {
        final boolean isPartial = converterConfig.isPartialData();

        if (Objects.nonNull(participants)) {
            if (isPartial) {
                deal.setParticipants(null);
            }

            for (int index = 0; index < participants.size(); index++) {
                final JsonObject contactJson = participants.optJsonObject(index);
                final Participant participant = new Participant();

                if (Objects.isNull(contactJson)) {
                    final long contactId = participants.getLong(index);
                    final Contact contact = new Contact();

                    contact.setId(contactId);
                    contact.setOrgId(orgId);
                    participant.setContact(contact);
                } else {
                    converterConfig.setPartialData(false);
                    participant.setContact(contactConverter.convert(converterConfig, contactJson));
                    converterConfig.setPartialData(isPartial);
                }
                deal.addParticipant(participant);
            }
        }
    }

    /**
     * <p>
     * Builds the {@link Monetary} entity from the given json objects.
     * </p>
     *
     * @param fields          Represents the monetary fields
     * @param orgId           Represents the organization id
     * @param converterConfig Converter configuration
     * @param deal            An entity that holds the attributes of deal
     * @param jsonObject      Holds the field values of deal
     */
    private void buildMonetary(final Map<String, FieldInfo> fields, final Long orgId, final Deal deal,
                               final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final String monetaryKey = fields.get(DealFields.DEAL_VALUE).idAsString();
        final DealValue dealValue;

        if (jsonObject.containsKey(monetaryKey)) {
            final JsonObject monetaryJson = jsonObject.getJsonObject(monetaryKey);
            final Map<StoreSubType, FieldSubType> dealValueSubTypes = FieldTypeService.getInstance().groupSubtypesByStoreSubType(StoreType.DEAL_VALUE);
            final String productsKey = dealValueSubTypes.get(StoreSubType.PRODUCT).idAsString();

            if (sourceSettingsService.isSourceEnabled(orgId, PRODUCT_NAME) && monetaryJson.containsKey(productsKey)) {
                final JsonObject productsJson = monetaryJson.optJsonObject(productsKey);

                if (productsJson.containsKey(ProductDetailsHolderFields.ITEMS.getName())) {
                    productsJson.put(ProductDetailsHolderFields.DEAL_ID.getName(), deal.id());
                    productsJson.put(ProductDetailsHolderFields.CURRENCY.getName(), monetaryJson.getLong(dealValueSubTypes.get(StoreSubType.CURRENCY).idAsString()));

                    final ProductDetailsHolder productDetailsHolder = productDetailsHolderConverter.convert(converterConfig, productsJson);
                    dealValue = new DealValue();

                    if (Objects.nonNull(productDetailsHolder)) {
                        if (monetaryJson.containsKey(FieldKeys.ID.getName())) {
                            dealValue.setId(monetaryJson.getLong(FieldKeys.ID.getName()));
                        }
                        dealValue.setOrgId(orgId);
                        dealValue.setCurrency(productDetailsHolder.getCurrency());
                        dealValue.setValue(BigDecimal.valueOf(productDetailsHolder.getTotalAmount(productDetailsHolder)));
                        deal.setProductDetailsHolder(productDetailsHolder);
                    }
                } else {
                    final Monetary monetary = monetaryConverter.convert(converterConfig, monetaryJson);
                    dealValue = new DealValue(monetary.orgId(), monetary.id(), monetary.getCurrency(), monetary.getValue());
                }
            } else {
                final Monetary monetary = monetaryConverter.convert(converterConfig, jsonObject.getJsonObject(monetaryKey));
                dealValue = new DealValue(monetary.orgId(), monetary.id(), monetary.getCurrency(), monetary.getValue());
            }
        } else if (!converterConfig.isPartialData()) {
            final Monetary defaultMonetary = getDefaultMonetary(orgId);
            dealValue = new DealValue(defaultMonetary.orgId(), defaultMonetary.id(), defaultMonetary.getCurrency(), defaultMonetary.getValue());
        } else {
            dealValue = deal.getDealValue();
        }

        deal.setDealValue(dealValue);
    }

    /**
     * Gets the default monetary
     *
     * @param orgId represents the organization id
     * @return {@link Monetary}
     */
    private Monetary getDefaultMonetary(final Long orgId) {
        final Monetary monetary = new Monetary();

        monetary.setOrgId(orgId);
        monetary.setCurrency(baseCurrencyService.getDefaultCurrency(orgId));
        monetary.setValue(BigDecimal.ZERO);

        return monetary;
    }

    /**
     * Builds the {@link Option} entity from the given field id
     *
     * @param orgId    Represents the organization id
     * @param fieldId  Represents the dropdown field id
     * @param id       Represents the option id
     * @param isStatic refers the option is static or not
     * @return {@link Option}
     */
    public Option buildOption(final Long orgId, final Long fieldId, final Long id, final boolean isStatic) {
        final Option option = new Option();

        option.setId(id);
        option.setStaticOption(isStatic);
        option.setFieldId(fieldId);
        option.setOrgId(orgId);

        return option;
    }

    /**
     * <p>
     * Builds the {@link Company} entity from the given json objects and map it to deal
     * </p>
     *
     * @param idAsString      Represents the company field id of Deal
     * @param converterConfig Converter configuration
     * @param deal            An entity that holds the attributes of deal
     * @param jsonObject      Holds the field values of deal
     */
    private void buildCompany(final String idAsString, final ConverterConfig converterConfig, final Deal deal,
                              final JsonObject jsonObject) {
        final Object value = jsonObject.opt(idAsString);
        final long orgId = converterConfig.orgId();
        final boolean isPartial = converterConfig.isPartialData();

        if (Objects.nonNull(value)) {
            if (TypeChecker.isNonDecimalNumber(value)) {
                final Company company = new Company();

                company.setId(NumberUtils.parseNumber(value).longValue());
                company.setOrgId(orgId);
                deal.setCompany(company);
            } else if (value instanceof final Map<?, ?> companyMap) {
                converterConfig.setPartialData(false);
                final Company company = companyConverter.convert(converterConfig, Json.object((Map<String, ?>) companyMap));

                company.setNewEntity(true);
                deal.setCompany(company);
                converterConfig.setPartialData(isPartial);
            }
        } else if (Objects.isNull(deal.id())) {
            final Contact contact = deal.getPrimaryContact();

            if (Objects.isNull(contact)) {
                deal.setCompany(null);
            } else {
                final Company contactCompany = contact.getCompany();

                if (Objects.nonNull(contactCompany)) {
                    contactCompany.setOrgId(orgId);
                    deal.setCompany(contactCompany);
                }
            }
        }
    }

    /**
     * Builds the {@link Contact} entity from the given json objects and map it to deal
     *
     * @param config     Converter configuration
     * @param deal       An entity that holds the attributes of deal
     * @param jsonObject Holds the field values of contact
     */
    private void buildContact(final String idAsString, final ConverterConfig config, final Deal deal,
                              final JsonObject jsonObject) {
        final Object value = jsonObject.opt(idAsString);
        final boolean isPartial = config.isPartialData();
        final Contact contact;

        if (Objects.nonNull(value)) {
            if (TypeChecker.isNonDecimalNumber(value)) {
                contact = new Contact();

                contact.setId(NumberUtils.parseNumber(value).longValue());
                contact.setOrgId(config.orgId());
            } else {
                config.setPartialData(false);
                contact = value instanceof final Map<?, ?> contactMap
                        ? contactConverter.convert(config, Json.object((Map<String, ?>) contactMap)) : null;

                if (Objects.nonNull(contact)) {
                    contact.setNewEntity(true);
                }
                config.setPartialData(isPartial);
            }
        } else {
            contact = deal.getPrimaryContact();

            if (Objects.nonNull(contact)) {
                contact.setOrgId(config.orgId());
            }
        }
        deal.setPrimaryContact(contact);
    }

    /**
     * Builds the {@link Pipeline} and {@link PipelineStage} from the given json objects and map it to deal
     *
     * @param fields     System Fields
     * @param deal       An entity that holds the attributes of deal
     * @param jsonObject Holds the field values of pipeline
     */
    private void buildPipeline(final Map<String, FieldInfo> fields, final Deal deal,
                               final JsonObject jsonObject) {
        final String pipelineKey = fields.get(DealFields.PIPELINE).idAsString();
        final String pipelineStageKey = fields.get(DealFields.PIPELINE_STAGE).idAsString();
        final Long orgId = deal.orgId();

        if (jsonObject.containsKey(pipelineKey)) {
            if (jsonObject.isKeyPresentWithNullValue(pipelineKey)) {
                deal.setPipeline(null);
            } else {
                final Pipeline pipeline = new Pipeline();

                pipeline.setId(jsonObject.getLong(pipelineKey));
                pipeline.setOrgId(orgId);

                deal.setPipeline(pipeline);
            }
        }

        if (jsonObject.containsKey(pipelineStageKey)) {
            if (jsonObject.isKeyPresentWithNullValue(pipelineStageKey)) {
                deal.setPipelineStage(null);
            } else {
                final PipelineStage pipelineStage = new PipelineStage();

                pipelineStage.setId(jsonObject.getLong(pipelineStageKey));
                pipelineStage.setOrgId(orgId);

                deal.setPipelineStage(pipelineStage);
            }
        }
    }

    /**
     * Converts the {@link Deal} into the {@link JsonObject}
     *
     * @param deal contains the data
     * @return the {@link JsonObject} filled with data
     */
    @Override
    public JsonObject convert(final Deal deal) {
        final JsonObject dealJson = Json.object();
        final Map<String, FieldInfo> fields = secureFieldService.groupDefaultFieldsByAliasName(Deal.ENTITY_TYPE);
        final ValueHolder valueHolder = deal.getValueHolder();
        final Company company = deal.getCompany();
        final DealValue dealValue = deal.getDealValue();
        final Pipeline pipeline = deal.getPipeline();
        final PipelineStage stage = deal.getPipelineStage();
        final Collection<Participant> dealParticipants = deal.getParticipants();
        final Territory territory = deal.getTerritory();
        final Option lostReason = deal.getLostReason();
        final Option wonReason = deal.getWonReason();
        final Option paymentStatus = deal.getPaymentStatus();
        final Option dealSource = deal.getSource();
        final Option type = deal.getType();
        final TimeUnit timeUnit = TimeUnit.MILLISECONDS;
        final Long modifiedTime = dateTimeUtil.getTimeStamp(deal.getModifiedTime());
        final Long timeDifference = Objects.isNull(modifiedTime) ? null : timeUnit.toDays(Instant.now().toEpochMilli()) - timeUnit.toDays(modifiedTime);
        final CrmUser createdBy = deal.getCreatedBy();
        final CrmUser updatedBy = deal.getUpdatedBy();
        final JsonObject monetaryJson;
        final ProductDetailsHolder productDetailsHolder;
        final SummaryInfo summaryInfo = deal.getSummaryInfo();
        final Contact primaryContact = deal.getPrimaryContact();
        final DateTime createdTime = deal.getCreatedTime();
        final Deal.State dealState = deal.getState();
        final CrmUser salesOwner = deal.getSalesOwner();

        final String expectedCloseDateAlias = DealFields.EXPECTED_CLOSE_DATE;
        final String pipelineAlias = DealFields.PIPELINE;
        final String pipelineStageAlias = DealFields.PIPELINE_STAGE;
        final String dealClosedOnAlias = DealFields.DEAL_CLOSED_ON;
        final String descriptionAlias = DealFields.DESCRIPTION;
        final String statusAlias = DealFields.STATUS;
        final String salesOwnerAlias = DealFields.SALES_OWNER;
        final String primaryContactAlias = DealFields.PRIMARY_CONTACT;
        final String companyAlias = DealFields.COMPANY;
        final String relatedContactsAlias = DealFields.RELATED_CONTACTS;
        final String dealValueAlias = DealFields.DEAL_VALUE;
        final String dealSourceAlias = DealFields.DEAL_SOURCE;
        final String typeAlias = DealFields.TYPE;
        final String paymentStatusAlias = DealFields.PAYMENT_STATUS;
        final String lostReasonAlias = DealFields.LOST_REASON;
        final String wonReasonAlias = DealFields.WON_REASON;
        final String createdByAlias = DealFields.CREATED_BY;
        final String updatedByAlias = DealFields.UPDATED_BY;
        final String territoryAlias = DealFields.TERRITORY;
        final String tagsAlias = DealFields.TAGS;
        final String dealCreatedAlias = DealFields.DEAL_CREATED;

        if (Objects.isNull(dealValue)) {
            monetaryJson = null;
            productDetailsHolder = null;
        } else {
            monetaryJson = dealValueConverter.convert(dealValue);
            productDetailsHolder = deal.getProductDetailsHolder();
        }
        dealJson.put(DealFields.ID, deal.id());
        dealJson.put(fields.get(DealFields.TITLE).idAsString(), deal.getTitle());

        if (fields.containsKey(expectedCloseDateAlias)) {
            dealJson.put(fields.get(expectedCloseDateAlias).idAsString(), dateUtil.getTimeStamp(deal.getExpectedCloseDate()));
        }

        if (Objects.nonNull(pipeline) && fields.containsKey(pipelineAlias)) {
            final Short rottenDays = pipeline.getRottenDays();

            dealJson.put(fields.get(pipelineAlias).idAsString(), pipeline.id());

            if (Objects.nonNull(rottenDays)) {
                dealJson.put(DealFields.IS_ROTTEN, timeDifference >= rottenDays);
            }
        }

        if (Objects.nonNull(stage) && fields.containsKey(pipelineStageAlias)) {
            dealJson.put(fields.get(pipelineStageAlias).idAsString(), stage.id());
        }

        if (fields.containsKey(dealClosedOnAlias)) {
            dealJson.put(fields.get(dealClosedOnAlias).idAsString(), dateTimeUtil.getTimeStamp(deal.getClosedTime()));
        }

        if (fields.containsKey(descriptionAlias)) {
            dealJson.put(fields.get(descriptionAlias).idAsString(), deal.getDescription());
        }

        if (Objects.nonNull(dealState) && fields.containsKey(statusAlias)) {
            dealJson.put(fields.get(statusAlias).idAsString(), dealState.getId());
        }

        if (Objects.nonNull(salesOwner) && fields.containsKey(salesOwnerAlias)) {
            dealJson.put(fields.get(salesOwnerAlias).idAsString(), salesOwner.id());
        }

        if (Objects.nonNull(primaryContact) && fields.containsKey(primaryContactAlias)) {
            dealJson.put(fields.get(primaryContactAlias).idAsString(), primaryContact.id());
        }

        if (Objects.nonNull(company) && fields.containsKey(companyAlias)) {
            dealJson.put(fields.get(companyAlias).idAsString(), company.id());
        }
        dealJson.put(DealFields.TOTAL_PRODUCTS, deal.getTotalProductCount());

        if (Objects.nonNull(dealParticipants) && !dealParticipants.isEmpty() && fields.containsKey(relatedContactsAlias)) {
            final JsonArray participants = Json.array();

            for (final Participant participant : dealParticipants) {
                participants.put(participant.getContact().id());
            }
            dealJson.put(fields.get(relatedContactsAlias).idAsString(), participants);
        }

        if (Objects.nonNull(monetaryJson) && fields.containsKey(dealValueAlias)) {
            if (Objects.nonNull(productDetailsHolder)) {
                final Map<StoreSubType, FieldSubType> dealValueSubTypes = FieldTypeService.getInstance().groupSubtypesByStoreSubType(StoreType.DEAL_VALUE);

                productDetailsHolder.setManuallyAdded(false);
                monetaryJson.put(dealValueSubTypes.get(StoreSubType.PRODUCT).idAsString(), productDetailsHolderConverter.convert(productDetailsHolder));
            } else {
                monetaryJson.put(DealFields.IS_MANUALLY_ADDED, true);
            }
            dealJson.put(fields.get(dealValueAlias).idAsString(), monetaryJson);
        }

        if (Objects.nonNull(dealSource) && fields.containsKey(dealSourceAlias)) {
            dealJson.put(fields.get(dealSourceAlias).idAsString(), dealSource.id());
        }

        if (Objects.nonNull(type) && fields.containsKey(typeAlias)) {
            dealJson.put(fields.get(typeAlias).idAsString(), type.id());
        }

        if (Objects.nonNull(paymentStatus) && fields.containsKey(paymentStatusAlias)) {
            dealJson.put(fields.get(paymentStatusAlias).idAsString(), paymentStatus.id());
        }

        if (Objects.nonNull(lostReason) && fields.containsKey(lostReasonAlias)) {
            dealJson.put(fields.get(lostReasonAlias).idAsString(), lostReason.id());
        }

        if (Objects.nonNull(wonReason) && fields.containsKey(wonReasonAlias)) {
            dealJson.put(fields.get(wonReasonAlias).idAsString(), wonReason.id());
        }

        if (Objects.nonNull(createdBy) && fields.containsKey(createdByAlias)) {
            dealJson.put(fields.get(createdByAlias).idAsString(), createdBy.id());
        }

        if (Objects.nonNull(updatedBy) && fields.containsKey(updatedByAlias)) {
            dealJson.put(fields.get(updatedByAlias).idAsString(), updatedBy.id());
        }

        if (Objects.nonNull(territory) && fields.containsKey(territoryAlias)) {
            dealJson.put(fields.get(territoryAlias).idAsString(), territory.id());
        }

        if (Objects.nonNull(valueHolder)) {
            valueHolder.mapToJson(dealJson);
        }

        if (Objects.nonNull(summaryInfo)) {
            final Collection<Tag> tags = summaryInfo.getTags();

            if (Objects.nonNull(tags) && !tags.isEmpty() && fields.containsKey(tagsAlias)) {
                dealJson.put(fields.get(tagsAlias).idAsString(), tagConverter.convertTags(tags));
            }
        }

        if (Objects.nonNull(createdTime) && fields.containsKey(dealCreatedAlias)) {
            dealJson.put(fields.get(dealCreatedAlias).idAsString(), createdTime.getTimeStamp());
        }
        return dealJson;
    }

    /**
     * Converts {@link Deal} into {@link JsonObject}
     *
     * @param deal            An entity that holds the attributes of deal details
     * @param converterConfig Converter configuration settings
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject convert(final ConverterConfig converterConfig, final Deal deal) {
        final JsonObject dealJson = Json.object();

        if (converterConfig.isIdRequiredOnKey()) {
            return convert(deal);
        } else {
            final ValueHolder valueHolder = deal.getValueHolder();
            final Company company = deal.getCompany();
            final DealValue dealValue = deal.getDealValue();
            final Pipeline pipeline = deal.getPipeline();
            final PipelineStage stage = deal.getPipelineStage();
            final Option lostReason = deal.getLostReason();
            final Option wonReason = deal.getWonReason();
            final Option paymentStatus = deal.getPaymentStatus();
            final Option dealSource = deal.getSource();
            final Deal.State state = deal.getState();
            final Option type = deal.getType();
            final Territory territory = deal.getTerritory();
            final TimeUnit timeUnit = TimeUnit.MILLISECONDS;
            final Optional<Long> modifiedTime = Optional.ofNullable(deal.getModifiedTime()).map(DateTime::getTimeStamp);
            final long timeDifference = modifiedTime.map(time -> timeUnit.toDays(Instant.now().toEpochMilli()) - timeUnit.toDays(time)).orElseGet(() -> 0L);
            final CrmUser createdBy = deal.getCreatedBy();
            final CrmUser updatedBy = deal.getUpdatedBy();
            final Long orgId = deal.orgId();
            final Contact primaryContact = deal.getPrimaryContact();
            final Collection<Participant> participants = deal.getParticipants();
            final Long closedTime = dateTimeUtil.getTimeStamp(deal.getClosedTime());
            final Long stageUpdatedAtTime = dateTimeUtil.getTimeStamp(deal.getStageUpdatedAt());

            dealJson.put(DealFields.ID, deal.id());
            dealJson.put(DealFields.TITLE, deal.getTitle());
            dealJson.put(DealFields.EXPECTED_CLOSE_DATE, dateUtil.getTimeStamp(deal.getExpectedCloseDate()));
            dealJson.put(DealFields.PIPELINE, pipeline.getName());
            dealJson.put(DealFields.PIPELINE_STAGE, stage.getName());
            dealJson.put(DealFields.IS_ROTTEN, timeDifference >= pipeline.getRottenDays());
            dealJson.put(DealFields.AGE, timeDifference);
            dealJson.put(DealFields.STAGE_UPDATED_AT, stageUpdatedAtTime);
            dealJson.put(DealFields.CLOSE_DATE, closedTime);
            dealJson.put(DealFields.DESCRIPTION, deal.getDescription());
            dealJson.put(DealFields.SALES_OWNER, crmUserConverter.convert(deal.getSalesOwner(), true, false));

            if (Objects.nonNull(primaryContact)) {
                dealJson.put(DealFields.PRIMARY_CONTACT, primaryContact.id());
            }

            if (Objects.nonNull(company)) {
                dealJson.put(DealFields.COMPANY, company.getName());
            }
            dealJson.put(DealFields.OWNER_ID, deal.getOwnerId());
            dealJson.put(DealFields.ORG_ID, orgId);
            dealJson.put(DealFields.TOTAL_PRODUCTS, deal.getTotalProductCount());

            if (Objects.nonNull(participants) && !participants.isEmpty()) {

                final JsonArray participantsArray = Json.array();

                for (final Participant participant : participants) {
                    participantsArray.put(participant.getContact().id());
                }
                dealJson.put(DealFields.RELATED_CONTACTS, participantsArray);
            }

            if (Objects.nonNull(dealValue)) {
                final ProductDetailsHolder productDetailsHolder = deal.getProductDetailsHolder();
                final BigDecimal value = dealValue.getValue();

                dealValue.setValue(value.multiply(BigDecimal.valueOf(stage.getProbability() / 100)));
                final JsonObject monetaryJson = monetaryConverter.convert(new ConverterConfig(false, false), dealValue);

                if (Objects.nonNull(productDetailsHolder)) {
                    final Map<StoreSubType, FieldSubType> dealValueSubTypes = FieldTypeService.getInstance().groupSubtypesByStoreSubType(StoreType.DEAL_VALUE);

                    productDetailsHolder.setManuallyAdded(false);
                    monetaryJson.put(dealValueSubTypes.get(StoreSubType.PRODUCT).idAsString(), productDetailsHolderConverter.convert(productDetailsHolder));
                }
                dealJson.put(DealFields.DEAL_VALUE, monetaryJson);
                dealJson.put(DealFields.EXPECTED_DEAL_VALUE, baseCurrencyService.applyPrecision(orgId,
                        value.multiply(BigDecimal.valueOf(stage.getProbability()))));
            }
            if (Objects.nonNull(state)) {
                dealJson.put(DealFields.STATUS, state.getId());
            }

            if (Objects.nonNull(dealSource)) {
                dealJson.put(DealFields.DEAL_SOURCE, dealSource.id());
            }

            if (Objects.nonNull(type)) {
                dealJson.put(DealFields.TYPE, type.id());
            }

            if (Objects.nonNull(paymentStatus)) {
                dealJson.put(DealFields.PAYMENT_STATUS, paymentStatus.id());
            }

            if (Objects.nonNull(lostReason)) {
                dealJson.put(DealFields.LOST_REASON, lostReason.id());
                dealJson.put(DealFields.LOST_TIME, stageUpdatedAtTime);
            }

            if (Objects.nonNull(wonReason)) {
                dealJson.put(DealFields.WON_REASON, wonReason.id());
                dealJson.put(DealFields.WON_TIME, stageUpdatedAtTime);
            }

            if (Objects.nonNull(createdBy)) {
                dealJson.put(DealFields.CREATED_BY, crmUserConverter.convert(createdBy, true, false));
            }

            if (Objects.nonNull(updatedBy)) {
                dealJson.put(DealFields.UPDATED_BY, crmUserConverter.convert(updatedBy, true, false));
            }

            if (Objects.nonNull(territory)) {
                dealJson.put(DealFields.TERRITORY, territoryConverter.convertTerritoryToJson(territory));
            }

            if (Objects.nonNull(valueHolder)) {
                valueHolder.mapToJson(dealJson);
            }
        }

        return dealJson;
    }

    /**
     * Converts {@link Deal} into partial {@link JsonObject}
     *
     * @param deal An entity that holds the attributes of deal details
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject convertPartialData(final Deal deal) {
        final JsonObject dealJson = Json.object();

        if (Objects.nonNull(deal)) {
            dealJson.put(DealFields.ID, deal.id());
            dealJson.put(DealFields.TITLE, deal.getTitle());
        }

        return dealJson;
    }

    /**
     * Converts the {@link Deal} into the partial {@link JsonObject} for deal forecast
     *
     * @param deal Which contains the deal details.
     * @return the {@link JsonObject} filled with data.
     */
    public JsonObject convertPartialJsonForDealForecast(final Deal deal) {
        final JsonObject dealJson = Json.object();
        final Company company = deal.getCompany();
        final Collection<Participant> dealParticipants = deal.getParticipants();
        final Contact primaryContact = deal.getPrimaryContact();
        final JsonArray participants = Json.array();
        final Deal.State dealState = deal.getState();
        final boolean isPrimaryContactNonNull = Objects.nonNull(primaryContact);
        final PipelineStage pipelineStage = deal.getPipelineStage();

        dealJson.put(DealFields.ID, deal.id());
        dealJson.put(DealFields.ORG_ID, deal.orgId());
        dealJson.put(DealFields.TITLE, deal.getTitle());
        dealJson.put(DealFields.DEAL_VALUE, monetaryConverter.convert(new ConverterConfig(false, false), deal.getDealValue()));
        dealJson.put(DealFields.STATUS, deal.getState().getName());
        dealJson.put(DealFields.EXPECTED_CLOSE_DATE, dateUtil.getTimeStamp(deal.getExpectedCloseDate()));
        dealJson.put(FieldKeys.IS_EDITABLE.getName(), deal.isEditable());

        if (Objects.nonNull(pipelineStage)) {
            dealJson.put(DealFields.PIPELINE_STAGE, pipelineStage.getName());
        }

        if (isPrimaryContactNonNull) {
            dealJson.put(DealFields.PRIMARY_CONTACT, contactConverter.convertPartialData(primaryContact));
        }

        if ((Deal.State.WON != dealState) && (Deal.State.LOST != dealState)) {
            dealJson.put(DealFields.IS_ROTTEN, deal.isRotten());
        }

        for (final Participant participant : dealParticipants) {
            final JsonObject participantJson = contactConverter.convertPartialData(participant.getContact());

            if (isPrimaryContactNonNull && Objects.equals(primaryContact.id(), participant.getContact().id())) {
                participantJson.put(DealFields.IS_PRIMARY, true);
            }
            participants.put(participantJson);
        }
        dealJson.put(DealFields.RELATED_CONTACTS, participants);

        if (Objects.nonNull(company)) {
            dealJson.put(DealFields.COMPANY, companyConverter.convertPartialData(company));
        }
        final ForecastCategory forecastCategory = deal.getForecastCategory();

        dealJson.put(DealFields.IS_COMMITED, Objects.nonNull(forecastCategory) && forecastCategory.isCommittedCategory());

        return dealJson;
    }

    /**
     * {@inheritDoc}
     *
     * @param deal            Holds the data of specific entity
     * @param converterConfig Represents the {@link ConverterConfig}
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject summary(final Deal deal, final ConverterConfig converterConfig) {
        final JsonObject json = Json.object();

        json.put(DealFields.ID, deal.id());
        json.put(DealFields.SUMMARY, convertSummaryToJsonArray(deal, converterConfig));
        convertSubFieldToJson(deal, converterConfig, json);

        return json;
    }

    /**
     * {@inheritDoc}
     *
     * @param deal            Represents the {@link Deal}
     * @param converterConfig Represents the {@link ConverterConfig}
     * @return The {@link JsonObject} containing deal profile details
     */
    @Override
    public JsonObject profile(final Deal deal, final ConverterConfig converterConfig) {
        final JsonObject profileJson = convertProfileToJsonObject(deal, converterConfig);
        final JsonObject dealAgeJson = Json.object();

        dealAgeJson.put(DealFields.AGE, deal.getAge());
        dealAgeJson.put(DealFields.INACTIVE_DAYS, deal.getInactiveDays());
        profileJson.put(DealFields.DEAL_AGE, dealAgeJson);
        profileJson.put(DealFields.ID, deal.id());
        profileJson.put(DealFields.IS_ARCHIVED, deal.isArchived());

        if (converterConfig.isAccessConfigRequired()) {
            profileJson.put(FieldKeys.IS_EDITABLE.getName(), deal.isEditable());
            profileJson.put(FieldKeys.IS_DELETABLE.getName(), deal.isDeletable());
        }
        convertSubFieldToJson(deal, converterConfig, profileJson);

        return profileJson;
    }

    /**
     * <p>
     * Converts the fields into json object
     * </p>
     *
     * @param fieldInfo Represents a data carrier that holds field details
     * @param deal      A model that represents deal
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject convertFieldInfoToJsonObject(final FieldInfo fieldInfo, final Deal deal, final ConverterConfig converterConfig) {
        final JsonObject jsonObject = Json.object();
        final Long orgId = deal.orgId();

        if (fieldInfo.isPersistent()) {
            setValuesToJson(deal.getValueHolder().get(fieldInfo.id()), jsonObject, fieldInfo);
        } else {
            switch (fieldInfo.aliasName()) {
                case "type" -> {
                    final Option dealType = deal.getType();

                    if (Objects.nonNull(dealType)) {
                        jsonObject.put(FieldKeys.VALUE.getName(), convertOptionToJson(dealType, converterConfig));
                    }
                }
//                case "campaign" -> jsonObject.put(FieldKeys.VALUE.getName(), deal.getCampaign());
                case "paymentStatus" -> {
                    final Option paymentStatus = deal.getPaymentStatus();

                    if (Objects.nonNull(paymentStatus)) {
                        jsonObject.put(FieldKeys.VALUE.getName(), convertOptionToJson(paymentStatus, converterConfig));
                    }
                }
                case "company" -> {
                    final Company company = deal.getCompany();

                    if (Objects.nonNull(company)) {
                        jsonObject.put(FieldKeys.VALUE.getName(), companyConverter.convertPartialData(company));
                    }
                }
                case "dealSource" -> {
                    final Option source = deal.getSource();

                    if (Objects.nonNull(source)) {
                        jsonObject.put(FieldKeys.VALUE.getName(), convertOptionToJson(source, converterConfig));
                    }
                }
                case "dealValue" -> {
                    final Monetary monetary = deal.getDealValue();

                    if (Objects.nonNull(monetary)) {
                        jsonObject.put(FieldKeys.VALUE.getName(), convertSubTypesToJson(StoreType.DEAL_VALUE, List.of(monetary)));
                    }
                    jsonObject.put(DealFields.IS_MANUALLY_ADDED, Objects.isNull(deal.getProductDetailsHolder()));
                }
                case "expectedCloseDate" -> {
                    final Date expectedCloseDate = deal.getExpectedCloseDate();

                    if (Objects.nonNull(expectedCloseDate)) {
                        jsonObject.put(FieldKeys.VALUE.getName(), dateUtil.getTimeStamp(expectedCloseDate));
                    }
                }
                case "title" -> jsonObject.put(FieldKeys.VALUE.getName(), deal.getTitle());
                case "status" -> jsonObject.put(FieldKeys.VALUE.getName(), deal.getState().getName());
                case "pipelineStage" -> {
                    final PipelineStage pipelineStage = deal.getPipelineStage();

                    if (Objects.nonNull(pipelineStage)) {
                        jsonObject.put(FieldKeys.VALUE.getName(), pipelineStageConverter.convertPartialData(pipelineStage));
                    }
                }
                case "dealClosedOn" -> jsonObject.put(FieldKeys.VALUE.getName(), dateTimeUtil.getTimeStamp(deal.getClosedTime()));
                case "primaryContact" -> {
                    final Contact contact = deal.getPrimaryContact();

                    if (Objects.nonNull(contact)) {
                        jsonObject.put(FieldKeys.VALUE.getName(), contactConverter.convertPartialData(contact));
                    }
                }
                case "lostReason" -> {
                    final Option lostReason = deal.getLostReason();

                    if (Objects.nonNull(lostReason)) {
                        jsonObject.put(FieldKeys.VALUE.getName(), convertOptionToJson(lostReason, converterConfig));
                    }
                }
                case "pipeline" -> {
                    final Pipeline pipeline = deal.getPipeline();

                    if (Objects.nonNull(pipeline)) {
                        jsonObject.put(FieldKeys.VALUE.getName(), pipelineConverter.convertPartialData(pipeline));
                    }
                }
                case "wonReason" -> {
                    final Option wonReason = deal.getWonReason();

                    if (Objects.nonNull(wonReason)) {
                        jsonObject.put(FieldKeys.VALUE.getName(), convertOptionToJson(wonReason, converterConfig));
                    }
                }
                case "description" -> jsonObject.put(FieldKeys.VALUE.getName(), deal.getDescription());
                case "id" -> jsonObject.put(FieldKeys.VALUE.getName(), deal.getId());
                case "wonTime" -> jsonObject.put(FieldKeys.VALUE.getName(), dateTimeUtil.getTimeStamp(deal.getWonTime()));
                case "lostTime" -> jsonObject.put(FieldKeys.VALUE.getName(), dateTimeUtil.getTimeStamp(deal.getLostTime()));
                case "dealCreated" -> jsonObject.put(FieldKeys.VALUE.getName(), dateTimeUtil.getTimeStamp(deal.getCreatedTime()));
                case "weightedValue" -> {
                    final Monetary weightedRevenue = deal.getWeightedRevenue();

                    weightedRevenue.setCurrency(baseCurrencyService.getDefaultCurrency(deal.orgId()));
                    jsonObject.put(FieldKeys.VALUE.getName(), convertSubTypesToJson(StoreType.MONETARY, List.of(weightedRevenue)));
                }
                case "rottingDays" -> jsonObject.put(FieldKeys.VALUE.getName(), deal.getRottenDays());
                case "stageProbability" -> jsonObject.put(FieldKeys.VALUE.getName(), deal.getStageProbability());
                case "updatedAt" -> jsonObject.put(FieldKeys.VALUE.getName(), dateTimeUtil.getTimeStamp(deal.getModifiedTime()));
                case "lastActivityDate" -> jsonObject.put(FieldKeys.VALUE.getName(), dateUtil.getTimeStamp(deal.getSummaryInfo().getActivitySummary().getLastActivityDate()));
                case "nextActivityDate" -> jsonObject.put(FieldKeys.VALUE.getName(), dateUtil.getTimeStamp(deal.getSummaryInfo().getActivitySummary().getNextActivityDate()));
                case "totalActivities" -> {
                    final long totalActivities = deal.getSummaryInfo().getActivitySummary().getTotalActivities();

                    if (totalActivities > 0) {
                        jsonObject.put(FieldKeys.VALUE.getName(), totalActivities);
                    }
                }
                case "upcomingActivities" -> {
                    final long upcomingActivities = deal.getSummaryInfo().getActivitySummary().getUpcomingActivities();

                    if (upcomingActivities > 0) {
                        jsonObject.put(FieldKeys.VALUE.getName(), upcomingActivities);
                    }
                }
                case "doneActivities" -> {
                    final long doneActivities = deal.getSummaryInfo().getActivitySummary().getDoneActivities();

                    if (doneActivities > 0) {
                        jsonObject.put(FieldKeys.VALUE.getName(), doneActivities);
                    }
                }
                case "lastActivityType" -> {
                    final String lastActivityType = deal.getSummaryInfo().getActivitySummary().getLastActivityType();

                    if (Objects.nonNull(lastActivityType)) {
                        jsonObject.put(FieldKeys.VALUE.getName(), lastActivityType);
                    }
                }
                case "dealStageUpdatedAt" -> jsonObject.put(FieldKeys.VALUE.getName(), dateTimeUtil.getTimeStamp(deal.getStageUpdatedAt()));
                case "age" -> {
                    final Long age = deal.getAge();

                    jsonObject.put(FieldKeys.VALUE.getName(), Objects.isNull(age) ? null : age);
                }
                case "expectedDealValue" -> {
                    final Monetary expectedDealValue = deal.getExpectedDealValue();

                    jsonObject.put(FieldKeys.VALUE.getName(), Objects.isNull(expectedDealValue) ? null :
                            convertSubTypesToJson(StoreType.MONETARY, List.of(expectedDealValue)));
                }
                case "webFormName" -> jsonObject.put(FieldKeys.VALUE.getName(), deal.getSummaryInfo().getWebFormName());
                case "products" -> {
                    final ProductDetailsHolder productDetailsHolder = deal.getProductDetailsHolder();

                    if (Objects.nonNull(productDetailsHolder) && sourceSettingsService.isSourceEnabled(orgId, PRODUCT_NAME)) {
                        jsonObject.put(FieldKeys.VALUE.getName(), productDetailsHolderConverter.convertPartialDataToJson(productDetailsHolder));
                    }
                }
                case "salesOwner" -> {
                    final CrmUser salesOwner = deal.getSalesOwner();

                    if (Objects.nonNull(salesOwner)) {
                        jsonObject.put(FieldKeys.VALUE.getName(), crmUserConverter.convert(salesOwner, true, true));
                    }
                }
                case "createdBy" -> {
                    final CrmUser createdBy = deal.getCreatedBy();

                    if (Objects.nonNull(createdBy)) {
                        jsonObject.put(FieldKeys.VALUE.getName(), crmUserConverter.convert(createdBy, true, true));
                    }
                }
                case "updatedBy" -> {
                    final CrmUser updatedBy = deal.getUpdatedBy();

                    if (Objects.nonNull(updatedBy)) {
                        jsonObject.put(FieldKeys.VALUE.getName(), crmUserConverter.convert(updatedBy, true, true));
                    }
                }
                case "territory" -> {
                    final Territory territory = deal.getTerritory();

                    if (Objects.nonNull(territory)) {
                        jsonObject.put(FieldKeys.VALUE.getName(), territoryConverter.convertTerritoryToJson(territory));
                    }
                }
                case "tags" -> {
                    final Collection<Tag> tags = deal.getSummaryInfo().getTags();

                    if (Objects.nonNull(tags) && !tags.isEmpty()) {
                        jsonObject.put(FieldKeys.VALUE.getName(), tagConverter.convertTagsForSummary(tags));
                    }
                }
                case "emailMessageCount" -> {
                    final EmailSummary emailSummary = deal.getSummaryInfo().getEmailSummary();

                    if (Objects.nonNull(emailSummary)) {
                        final Long emailMessageCount = emailSummary.getEmailMessageCount();

                        if (Objects.nonNull(emailMessageCount) && emailMessageCount > 0) {
                            jsonObject.put(FieldKeys.VALUE.getName(), emailMessageCount);
                        }
                    }
                }
                case "lastEmailSent" -> {
                    final EmailSummary emailSummary = deal.getSummaryInfo().getEmailSummary();

                    if (Objects.nonNull(emailSummary)) {
                        final Date lastEmailSent = emailSummary.getLastEmailSent();

                        if (Objects.nonNull(lastEmailSent)) {
                            jsonObject.put(FieldKeys.VALUE.getName(), lastEmailSent.getTimeStamp());
                        }
                    }
                }
                case "lastEmailReceived" -> {
                    final EmailSummary emailSummary = deal.getSummaryInfo().getEmailSummary();

                    if (Objects.nonNull(emailSummary)) {
                        final Date lastEmailReceived = emailSummary.getLastEmailReceived();

                        if (Objects.nonNull(lastEmailReceived)) {
                            jsonObject.put(FieldKeys.VALUE.getName(), lastEmailReceived.getTimeStamp());
                        }
                    }
                }
                case "dealValueInBaseCurrency" -> jsonObject.put(FieldKeys.VALUE.getName(),
                        convertSubTypesToJson(StoreType.MONETARY, List.of(deal.getDealValueInBaseCurrency())));
                case "relatedContacts" -> {
                    final Collection<Participant> relatedContacts = deal.getParticipants();

                    if (Objects.nonNull(relatedContacts) && !relatedContacts.isEmpty()) {
                        final JsonArray jsonArray = Json.array();

                        for (final Participant participant : relatedContacts.stream().filter(relatedContact -> Objects.nonNull(relatedContact.getContact()))
                                .sorted(Comparator.comparing(participant -> participant.getContact().id())).toList()) {
                            jsonArray.put(contactConverter.convertPartialData(participant.getContact()));
                        }
                        jsonObject.put(FieldKeys.VALUE.getName(), jsonArray);
                    }
                }
                case "lastAssignedAt" -> jsonObject.put(FieldKeys.VALUE.getName(), dateTimeUtil.getTimeStamp(deal.getLastAssignedAt()));
                case "recentNote" -> jsonObject.put(FieldKeys.VALUE.getName(), deal.getRecentNote());
                case "productAmount" -> {
                    final Monetary productAmount = deal.getProductAmount();

                    jsonObject.put(FieldKeys.VALUE.getName(), Objects.nonNull(productAmount) ?
                            convertSubTypesToJson(StoreType.MONETARY, List.of(productAmount)) : null);
                }
                case "forecastCategory" -> {
                    final ForecastCategory forecastCategory = deal.getForecastCategory();

                    if (Objects.nonNull(forecastCategory)) {
                        jsonObject.put(FieldKeys.VALUE.getName(), forecastCategory.getName());
                    }
                }
                //TODO In future if any product field exists in the system or custom fields, the IS_PRODUCT_ENABLED configuration needs to be added to the JSON.
                //TODO In future if the field is auto-generated, the isSourceEnabled check should be applied.
            }
        }
        loadFieldsIntoJson(jsonObject, fieldInfo, converterConfig);

        return jsonObject;
    }

    /**
     * Converts the primary deal to {@link JsonObject}
     *
     * @param deal         An entity that holds the attributes of deal details
     * @param i18nResolver Refers the {@link I18nResolver} to resolve the i18nKey
     * @return The {@link JsonObject}
     */
    public JsonObject convertPrimaryDealToJson(final Deal deal, final I18nResolver i18nResolver) {
        final long orgId = deal.orgId();
        final ValueHolder valueHolder = deal.getValueHolder();
        final Company company = deal.getCompany();
        final Long closedTime = dateTimeUtil.getTimeStamp(deal.getClosedTime());
        final Long stageUpdatedAtTime = dateTimeUtil.getTimeStamp(deal.getStageUpdatedAt());
        final Option lostReason = deal.getLostReason();
        final Option wonReason = deal.getWonReason();
        final Option paymentStatus = deal.getPaymentStatus();
        final Option dealSource = deal.getSource();
        final Deal.State state = deal.getState();
        final Option type = deal.getType();
        final Territory territory = deal.getTerritory();
        final TimeUnit timeUnit = TimeUnit.MILLISECONDS;
        final Long createdTime = dateTimeUtil.getTimeStamp(deal.getCreatedTime());
        final Long age = Objects.isNull(createdTime) ? null : timeUnit.toDays(Instant.now().toEpochMilli() - createdTime);
        final JsonObject jsonObject = Json.object();
        final Monetary monetary = deal.getDealValue();
        final BigDecimal monetaryValue = monetary.getValue();
        final Currency currency = monetary.getCurrency();
        final BigDecimal exchangeRate = baseCurrencyService.getCurrencyExchangeRate(orgId, currency);
        final BigDecimal baseCurrencyValue = Objects.isNull(exchangeRate) ? monetaryValue :
                monetaryValue.divide(exchangeRate, NumericalConstant.FIVE, RoundingMode.HALF_EVEN);
        final String currencySymbol = currency.getSymbol();
        final Contact contact = deal.getPrimaryContact();
        final Collection<Participant> participants = deal.getParticipants();
        final SummaryInfo summaryInfo = deal.getSummaryInfo();
        final ActivitySummary activitySummary = summaryInfo.getActivitySummary();
        final EmailSummary emailSummary = summaryInfo.getEmailSummary();
        final Collection<Tag> tags = summaryInfo.getTags();
        final Monetary expectedDealValue = deal.getExpectedDealValue();
        final Monetary weightedRevenue = deal.getWeightedRevenue();
        final ProductDetailsHolder productDetailsHolder = deal.getProductDetailsHolder();
        final ForecastCategory forecastCategory = deal.getForecastCategory();

        jsonObject.put(DealFields.ID, deal.id());
        jsonObject.put(DealFields.TITLE, deal.getTitle());
        jsonObject.put(DealFields.PIPELINE, deal.getPipeline().getName());
        jsonObject.put(DealFields.PIPELINE_STAGE, deal.getPipelineStage().getName());
        jsonObject.put(DealFields.DEAL_CREATED, createdTime);
        jsonObject.put(DealFields.UPDATED_AT, dateTimeUtil.getTimeStamp(deal.getModifiedTime()));
        jsonObject.put(DealFields.NEXT_ACTIVITY_DATE, dateUtil.getTimeStamp(activitySummary.getNextActivityDate()));
        jsonObject.put(DealFields.LAST_ACTIVITY_DATE, dateUtil.getTimeStamp(activitySummary.getLastActivityDate()));
        jsonObject.put(DealFields.TOTAL_ACTIVITIES, activitySummary.getTotalActivities());
        jsonObject.put(DealFields.LAST_ACTIVITY_TYPE, activitySummary.getLastActivityType());
        jsonObject.put(DealFields.ACTIVITIES_TO_DO, activitySummary.getPendingActivities());
        jsonObject.put(DealFields.UPCOMING_ACTIVITIES, activitySummary.getUpcomingActivities());
        jsonObject.put(DealFields.DONE_ACTIVITIES, activitySummary.getDoneActivities());
        jsonObject.put(DealFields.EXPECTED_CLOSE_DATE, dateUtil.getTimeStamp(deal.getExpectedCloseDate()));
        jsonObject.put(DealFields.STAGE_UPDATED_AT, dateTimeUtil.getTimeStamp(deal.getStageUpdatedAt()));
        jsonObject.put(DealFields.STAGE_PROBABILITY, String.format("%s %s", deal.getPipelineStage().getProbability(), "%"));
        jsonObject.put(DealFields.CREATED_BY, deal.getCreatedBy().getFullName());
        jsonObject.put(DealFields.SALES_OWNER, deal.getSalesOwner().getFullName());
        jsonObject.put(DealFields.DEAL_VALUE, String.format("%s %s", currencySymbol, monetaryValue));
        jsonObject.put(DealFields.DEAL_VALUE_IN_BASE_CURRENCY, String.format("%s %s",
                baseCurrencyService.getDefaultCurrency(orgId).getSymbol(), baseCurrencyValue));
        jsonObject.put(DealFields.ROTTING_DAYS, deal.getRottenDays());
        jsonObject.put(DealFields.DEAL_CLOSED_ON, closedTime);
        jsonObject.put(DealFields.IS_ROTTEN, deal.isRotten());
        jsonObject.put(DealFields.AGE, age);
        jsonObject.put(DealFields.DESCRIPTION, deal.getDescription());

        if (sourceSettingsService.isSourceEnabled(orgId, PRODUCT_NAME)) {
            jsonObject.put(DealFields.PRODUCTS, deal.getTotalProductCount());
        }
        jsonObject.put(DealFields.UPDATED_BY, deal.getUpdatedBy().getFullName());
        jsonObject.put(DealFields.WEB_FORM, summaryInfo.getWebFormName());
        jsonObject.put(DealFields.CAMPAIGN, deal.getCampaign());
        jsonObject.put(DealFields.RECENT_NOTE, deal.getRecentNote());
        jsonObject.put(DealFields.LAST_ASSIGNED_AT, dateTimeUtil.getTimeStamp(deal.getLastAssignedAt()));
        jsonObject.put(DealFields.DEAL_STAGE_UPDATED_AT, dateTimeUtil.getTimeStamp(deal.getStageUpdatedAt()));

        if (Objects.nonNull(weightedRevenue)) {
            jsonObject.put(DealFields.WEIGHTED_VALUE, String.format("%s %s", currencySymbol, weightedRevenue.getValue()));
        }

        if (Objects.nonNull(expectedDealValue)) {
            jsonObject.put(DealFields.EXPECTED_DEAL_VALUE, String.format("%s %s", currencySymbol, expectedDealValue.getValue()));
        }

        if (Objects.nonNull(contact)) {
            final JsonObject contactJson = Json.object();

            contactJson.put(ContactFields.ID, contact.id());
            contactJson.put(ContactFields.FULL_NAME, contact.getFullName());
            jsonObject.put(DealFields.PRIMARY_CONTACT, contactJson);
        }

        if (Objects.nonNull(company)) {
            final JsonObject companyJson = Json.object();

            companyJson.put(CompanyFields.ID, company.id());
            companyJson.put(CompanyFields.NAME, company.getName());
            jsonObject.put(DealFields.COMPANY, companyJson);
        }

        if (Objects.nonNull(state)) {
            final JsonObject stateJson = Json.object();

            if (!Objects.equals(Deal.State.OPEN, state)) {
                stateJson.put(DealFields.MERGE_TYPE, Objects.equals(Deal.State.WON, state) ? MergeType.SUCCESS : MergeType.FAILURE);
            }
            stateJson.put(DealFields.NAME, state.getDisplayName());
            jsonObject.put(DealFields.STATUS, stateJson);
        }

        if (Objects.nonNull(participants)) {
            final JsonArray participantJsonArray = Json.array();

            for (final Participant participant : participants) {
                final Contact relatedContact = participant.getContact();
                final JsonObject contactJson = Json.object();

                contactJson.put(ContactFields.ID, relatedContact.id());
                contactJson.put(ContactFields.FULL_NAME, relatedContact.getFullName());
                participantJsonArray.put(contactJson);
            }
            jsonObject.put(DealFields.RELATED_CONTACTS, participantJsonArray);
        }

        if (Objects.nonNull(dealSource)) {
            jsonObject.put(DealFields.DEAL_SOURCE, i18nResolver.get(dealSource.getOption()));
        }

        if (Objects.nonNull(type)) {
            jsonObject.put(DealFields.TYPE, type.getOption());
        }

        if (Objects.nonNull(paymentStatus)) {
            jsonObject.put(DealFields.PAYMENT_STATUS, paymentStatus.getOption());
        }

        if (Objects.nonNull(lostReason)) {
            jsonObject.put(DealFields.LOST_REASON, lostReason.getOption());
            jsonObject.put(DealFields.LOST_TIME, stageUpdatedAtTime);
        }

        if (Objects.nonNull(wonReason)) {
            jsonObject.put(DealFields.WON_REASON, wonReason.getOption());
            jsonObject.put(DealFields.WON_TIME, stageUpdatedAtTime);
        }

        if (Objects.nonNull(territory)) {
            jsonObject.put(DealFields.TERRITORY, territory.getName());
        }

        if (Objects.nonNull(emailSummary)) {
            loadEmailSummaryFields(jsonObject,
                    DealFields.LAST_EMAIL_SENT, dateUtil.getTimeStamp(emailSummary.getLastEmailSent()),
                    DealFields.EMAIL_MESSAGE_COUNT, emailSummary.getEmailMessageCount(),
                    DealFields.LAST_EMAIL_RECEIVED, dateUtil.getTimeStamp(emailSummary.getLastEmailReceived()));
        }

        if (Objects.nonNull(tags)) {
            final JsonArray tagsJsonArray = Json.array();

            for (final Tag tag : tags) {
                final JsonObject tagJson = Json.object();

                tagJson.put(TagFields.ID.getName(), tag.id());
                tagJson.put(TagFields.TAG.getName(), tag.getName());
                tagsJsonArray.put(tagJson);
            }
            jsonObject.put(DealFields.TAGS, tagsJsonArray);
        }

        if (Objects.nonNull(productDetailsHolder)) {
            final Collection<ProductDetail> productDetails = productDetailsHolder.getProductDetails();

            if (Objects.nonNull(productDetails)) {
                final double productAmount = productDetails.stream()
                        .mapToDouble(productDetail -> productDetail.getQuantity() * productDetail.getPrice()).sum();

                jsonObject.put(DealFields.PRODUCT_AMOUNT, String.format("%s %s", currencySymbol, productAmount));
            }
        }

        if (Objects.nonNull(forecastCategory)) {
            jsonObject.put(DealFields.FORECAST_CATEGORY, forecastCategory.getName());
        }
        loadValueHolderToJson(jsonObject, valueHolder);
        loadPersistentFieldValues(deal.orgId(), Deal.ENTITY_TYPE, jsonObject);

        return jsonObject;
    }

    /**
     * Converts partial deal row to {@link JsonObject}
     *
     * @param deal Represents the deal
     * @return {@link JsonObject} of contact
     */
    public JsonObject convertDealToJson(final Deal deal) {
        final JsonObject dealJson = Json.object();

        if (Objects.nonNull(deal)) {
            dealJson.put(DealFields.TITLE, deal.getTitle());
            dealJson.put(DealFields.DEAL_VALUE, monetaryConverter.convert(new ConverterConfig(false, false), deal.getDealValue()));
            dealJson.put(DealFields.ORG_ID, deal.orgId());
            dealJson.put(DealFields.ID, deal.id());
            final PipelineStage pipelineStage = deal.getPipelineStage();

            if (Objects.nonNull(pipelineStage)) {
                dealJson.put(DealFields.PIPELINE_STAGE, pipelineStageConverter.convertPartialData(pipelineStage));
            }
        }
        return dealJson;
    }

    /**
     * Maps COMPANY and CONTACT custom-field references for deal merge/preview responses.
     */
    @Override
    protected JsonObject convertEntityToJson(final StoreType storeType, final TlcModel model) {
        return switch (storeType) {
            case COMPANY -> {
                final JsonObject companyJson = Json.object();
                final Company company = (Company) model;

                companyJson.put(FieldKeys.ID.getName(), company.id());
                companyJson.put(FieldKeys.NAME.getName(), company.getName());

                yield companyJson;
            }
            case CONTACT -> {
                final JsonObject contactJson = Json.object();
                final Contact contact = (Contact) model;

                contactJson.put(FieldKeys.ID.getName(), contact.id());
                contactJson.put(FieldKeys.NAME.getName(), contact.getFullName());

                yield contactJson;
            }
            default -> null;
        };
    }

    /**
     * Converts activity deal row to {@link JsonObject}
     *
     * @param deal Represents the deal
     * @return {@link JsonObject} of contact
     */
    public JsonObject convertDealToActivityJson(final Deal deal) {
        final JsonObject jsonObject = Json.object();
        final Monetary value = deal.getDealValue();
        final PipelineStage pipelineStage = deal.getPipelineStage();

        jsonObject.put(DealFields.TITLE, deal.getTitle());
        jsonObject.put(DealFields.ID, deal.id());
        jsonObject.put(DealFields.EXPECTED_CLOSE_DATE, dateUtil.getTimeStamp(deal.getExpectedCloseDate()));

        if (Objects.nonNull(pipelineStage)) {
            jsonObject.put(DealFields.PIPELINE_STAGE, pipelineStageConverter.convertPartialData(pipelineStage));
        }

        if (Objects.nonNull(value)) {
            jsonObject.put(DealFields.DEAL_VALUE, monetaryConverter.convert(new ConverterConfig(false, false), value));
        }

        return jsonObject;
    }

    /**
     * <p>
     * Converts deal to {@link JsonObject}
     * </p>
     *
     * @param deal Represents the entity of {@link Deal}.
     * @return {@link JsonObject} of deal
     */
    public JsonObject buildDealJsonObject(final Deal deal) {
        final Long id = deal.id();
        final JsonObject dealObject = Json.object();
        final Contact contact = deal.getPrimaryContact();
        final Company company = deal.getCompany();

        dealObject.put(DealFields.ID, id);
        dealObject.put(DealFields.TITLE, deal.getTitle());
        dealObject.put(DealFields.EXPECTED_CLOSE_DATE, dateUtil.getTimeStamp(deal.getExpectedCloseDate()));
        dealObject.put(DealFields.DEAL_VALUE, monetaryConverter.convertMonetaryToJson(deal.orgId(), deal.getDealValue()));
        dealObject.put(DealFields.PIPELINE_STAGE, deal.getPipelineStage().getName());
        dealObject.put(DealFields.IS_ARCHIVED, deal.isArchived());

        if (Objects.nonNull(contact)) {
            dealObject.put(DealFields.CONTACT_ID, contact.id());
        }

        if (Objects.nonNull(company)) {
            dealObject.put(DealFields.COMPANY_ID, company.id());
        }

        return dealObject;
    }

    /**
     * <p>
     * Gets the deal field info to json
     * </p>
     *
     * @param orgId Represents an organization id
     * @return The {@link JsonArray} details
     */
    public JsonArray getFieldInfoToJson(final Long orgId, final CrmResponse crmResponse) {
        final JsonArray jsonArray = Json.array();
        final Collection<FieldInfo> fieldsBySource = secureFieldService.getFieldsBySource(orgId, Deal.ENTITY_TYPE);

        for (final FieldInfo fieldInfo : fieldsBySource) {
            final JsonObject jsonObject = Json.object();

            jsonObject.put(DealFields.KEY, fieldInfo.isPersistent() ? fieldInfo.idAsString() : fieldInfo.aliasName());
            jsonObject.put(DealFields.NAME, crmResponse.resolveI18nPlaceholder(fieldInfo.i18nName()));
            jsonObject.put(DealFields.STORE_TYPE, fieldInfo.storeType().getName());
            jsonArray.put(jsonObject);
        }

        return jsonArray;
    }

    /**
     * <p>
     * Converts a {@link Deal} into a {@link JsonObject} representing a kanban view of the deal
     * </p>
     *
     * @param deal         Represents the {@link Deal}
     * @param numberFormat Represents the {@link NumberFormat}
     * @return The {@link JsonObject} the kanban view of the given Deal
     */
    public JsonObject convertDealForKanbanView(final Deal deal, final NumberFormat numberFormat) {
        final JsonObject dealJson = Json.object();
        final Company company = deal.getCompany();
        final Collection<Participant> participants = deal.getParticipants();
        final JsonArray participantsArray = Json.array();
        final Monetary monetary = deal.getDealValue();
        final Contact primaryContact = deal.getPrimaryContact();
        final Deal.State dealState = deal.getState();

        dealJson.put(DealFields.ID, deal.id());
        dealJson.put(DealFields.TITLE, deal.getTitle());
        dealJson.put(DealFields.EXPECTED_CLOSE_DATE, dateUtil.getTimeStamp(deal.getExpectedCloseDate()));

        if ((Deal.State.WON != dealState) && (Deal.State.LOST != dealState)) {
            dealJson.put(DealFields.IS_ROTTEN, deal.isRotten());
        }

        if (Objects.nonNull(monetary)) {
            final String valueKey = DealFields.VALUE;
            final JsonObject monetaryJson = monetaryConverter.convert(new ConverterConfig(false, false), monetary);
            final double value = monetaryJson.getDouble(valueKey);

            if (value >= TEN_THOUSANDS) {
                monetaryJson.put(valueKey, numberFormat.format(value));
            }
            dealJson.put(DealFields.DEAL_VALUE, monetaryJson);
        }

        if (Objects.nonNull(company)) {
            dealJson.put(DealFields.COMPANY, companyConverter.convertCompanyWithThumbnail(company));
        }

        if (Objects.nonNull(participants) && !participants.isEmpty()) {

            for (final Participant participant : participants) {
                participantsArray.put(contactConverter.convertContactWithThumbnail(participant.getContact()));
            }
        }

        if (Objects.nonNull(primaryContact)) {
            participantsArray.put(contactConverter.convertContactWithThumbnail(primaryContact).put(DealFields.IS_PRIMARY, true));
        }
        dealJson.put(DealFields.RELATED_CONTACTS, participantsArray);

        dealJson.put(FieldKeys.IS_EDITABLE.getName(), deal.isEditable());
        dealJson.put(FieldKeys.IS_DELETABLE.getName(), deal.isDeletable());

        return dealJson;
    }

    /**
     * <p>
     * Loads the {@link SummaryInfo} according to its state into a {@link JsonObject}.
     * </p>
     *
     * @param summaryInfo Represents the {@link SummaryInfo}
     * @param stateKey    Represents the state of deal
     * @param json        Represents the response {@link JsonObject}
     */
    public void loadSummaryInfoToJson(final SummaryInfo summaryInfo, final String stateKey, final JsonObject json) {
        final Collection<Deal.State> state = Objects.equals(DealFields.CLOSED, stateKey)
                ? List.of(Deal.State.WON, Deal.State.LOST) : List.of(Deal.State.getState(stateKey));
        final Collection<Deal> deals = summaryInfo.getDealsByStates(state);

        if (deals.size() > 0) {
            final DealStatistics statistics = summaryInfo.getDealStatistics();
            final Long count = switch (stateKey.toLowerCase()) {
                case "lost" -> statistics.getLostDealsCount();
                case "won" -> statistics.getWonDealsCount();
                case "open" -> statistics.getOpenDealsCount();
                case "closed" -> statistics.getClosedDealsCount();
                default -> 0L;
            };
            final JsonObject dealJson = Json.object();
            final JsonArray dealArray = Json.array();

            for (final Deal deal : deals) {
                dealArray.put(convertDealToJson(deal));
            }

            dealJson.put(DealFields.DEALS, dealArray);
            dealJson.put(DealFields.COUNT, count);
            dealJson.put(DealFields.STATE, stateKey);

            json.put(FieldKeys.VALUE.getName(), dealJson);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param deal Represents the {@link Deal}
     * @return the summary fields
     */
    @Override
    protected final Map<Integer, List<FieldInfo>> getSummaryFields(final Deal deal) {
        final Collection<FieldInfo> fields = secureFieldService.getFieldsBySource(deal.orgId(), deal.getEntityType());

        return fields.stream()
                .filter(field -> {
                    final PresetConfig presetConfig = field.presetConfig();

                    return !presetConfig.showOnProfile() && !presetConfig.hidden()
                            && !Objects.equals(DealFields.RELATED_CONTACTS, field.aliasName());
                })
                .sorted(Comparator.comparing(field -> field.presetConfig().orderOnView()))
                .collect(Collectors.groupingBy(field -> field.presetConfig().rowIndexOnView()));
    }

    /**
     * <p>

    /**
     * Converts the {@link Deal} into the {@link JsonObject}
     *
     * @param deal            The {@link Deal} entity containing the data to be converted.
     * @param converterConfig The {@link ConverterConfig} that provides configuration options.
     * @return the {@link JsonObject} filled with data
     */
    @Override
    public JsonObject convertModelToJson(final Deal deal, final ConverterConfig converterConfig) {
        final long orgId = deal.orgId();
        final JsonObject dealJson = Json.object();
        final Map<String, FieldInfo> fields = secureFieldService.groupDefaultFieldsByAliasName(Deal.ENTITY_TYPE);
        final ValueHolder valueHolder = deal.getValueHolder();
        final Company company = deal.getCompany();
        final DealValue dealValue = deal.getDealValue();
        final Pipeline pipeline = deal.getPipeline();
        final PipelineStage stage = deal.getPipelineStage();
        final Collection<Participant> dealParticipants = deal.getParticipants();
        final Territory territory = deal.getTerritory();
        final Option lostReason = deal.getLostReason();
        final Option wonReason = deal.getWonReason();
        final Option paymentStatus = deal.getPaymentStatus();
        final Option dealSource = deal.getSource();
        final Option type = deal.getType();
        final CrmUser createdBy = deal.getCreatedBy();
        final CrmUser updatedBy = deal.getUpdatedBy();
        final JsonObject monetaryJson;
        final ProductDetailsHolder productDetailsHolder;
        final SummaryInfo summaryInfo = deal.getSummaryInfo();
        final Contact primaryContact = deal.getPrimaryContact();
        final CrmUser salesOwner = deal.getSalesOwner();
        final ForecastCategory forecastCategory = deal.getForecastCategory();

        final String expectedCloseDateAlias = DealFields.EXPECTED_CLOSE_DATE;
        final String pipelineAlias = DealFields.PIPELINE;
        final String pipelineStageAlias = DealFields.PIPELINE_STAGE;
        final String dealClosedOnAlias = DealFields.DEAL_CLOSED_ON;
        final String descriptionAlias = DealFields.DESCRIPTION;
        final String statusAlias = DealFields.STATUS;
        final String salesOwnerAlias = DealFields.SALES_OWNER;
        final String primaryContactAlias = DealFields.PRIMARY_CONTACT;
        final String companyAlias = DealFields.COMPANY;
        final String relatedContactsAlias = DealFields.RELATED_CONTACTS;
        final String dealValueAlias = DealFields.DEAL_VALUE;
        final String dealSourceAlias = DealFields.DEAL_SOURCE;
        final String typeAlias = DealFields.TYPE;
        final String paymentStatusAlias = DealFields.PAYMENT_STATUS;
        final String lostReasonAlias = DealFields.LOST_REASON;
        final String wonReasonAlias = DealFields.WON_REASON;
        final String createdByAlias = DealFields.CREATED_BY;
        final String updatedByAlias = DealFields.UPDATED_BY;
        final String territoryAlias = DealFields.TERRITORY;
        final String tagsAlias = DealFields.TAGS;
        final String forecastCategoryAlias = DealFields.FORECAST_CATEGORY;

        if (Objects.isNull(dealValue)) {
            monetaryJson = null;
            productDetailsHolder = null;
        } else {
            monetaryJson = dealValueConverter.convertModelToJson(dealValue, converterConfig);
            productDetailsHolder = deal.getProductDetailsHolder();
        }
        dealJson.put(DealFields.ID, deal.id());
        dealJson.put(fields.get(DealFields.TITLE).idAsString(), deal.getTitle());
        dealJson.put(DealFields.IS_ARCHIVED, deal.isArchived());

        if (fields.containsKey(expectedCloseDateAlias)) {
            dealJson.put(fields.get(expectedCloseDateAlias).idAsString(), dateUtil.getTimeStamp(deal.getExpectedCloseDate()));
        }

        if (Objects.nonNull(pipeline) && fields.containsKey(pipelineAlias)) {
            dealJson.put(fields.get(pipelineAlias).idAsString(), pipelineConverter.convertModelToOptionJson(pipeline));
        }

        if (Objects.nonNull(stage) && fields.containsKey(pipelineStageAlias)) {
            dealJson.put(fields.get(pipelineStageAlias).idAsString(), pipelineStageConverter.convertModelToOptionJson(stage));
        }

        if (fields.containsKey(dealClosedOnAlias)) {
            dealJson.put(fields.get(dealClosedOnAlias).idAsString(), dateTimeUtil.getTimeStamp(deal.getClosedTime()));
        }

        if (fields.containsKey(descriptionAlias)) {
            dealJson.put(fields.get(descriptionAlias).idAsString(), deal.getDescription());
        }

        if (Objects.nonNull(salesOwner) && fields.containsKey(salesOwnerAlias)) {
            dealJson.put(fields.get(salesOwnerAlias).idAsString(), crmUserConverter.convertModelToOptionJson(salesOwner));
        }

        if (Objects.nonNull(primaryContact) && fields.containsKey(primaryContactAlias)) {
            dealJson.put(fields.get(primaryContactAlias).idAsString(), contactConverter.convertModelToOptionJson(primaryContact));
        }

        if (Objects.nonNull(company) && fields.containsKey(companyAlias)) {
            dealJson.put(fields.get(companyAlias).idAsString(), companyConverter.convertModelToOptionJson(company));
        }
        dealJson.put(DealFields.TOTAL_PRODUCTS, deal.getTotalProductCount());

        if (Objects.nonNull(dealParticipants) && !dealParticipants.isEmpty() && fields.containsKey(relatedContactsAlias)) {
            final JsonArray participants = Json.array();

            for (final Participant participant : dealParticipants) {
                final Contact relatedContact = participant.getContact();

                participants.put(contactConverter.convertModelToOptionJson(relatedContact));
            }
            dealJson.put(fields.get(relatedContactsAlias).idAsString(), participants);
        }

        if (Objects.nonNull(monetaryJson) && fields.containsKey(dealValueAlias)) {

            if (sourceSettingsService.isSourceEnabled(orgId, PRODUCT_NAME) && Objects.nonNull(productDetailsHolder)) {
                final Map<StoreSubType, FieldSubType> dealValueSubTypes = FieldTypeService.getInstance().groupSubtypesByStoreSubType(StoreType.DEAL_VALUE);

                monetaryJson.put(DealFields.IS_MANUALLY_ADDED, false);
                monetaryJson.put(dealValueSubTypes.get(StoreSubType.PRODUCT).idAsString(), productDetailsHolderConverter.convertModelToJson(productDetailsHolder, converterConfig));
            } else {
                monetaryJson.put(DealFields.IS_MANUALLY_ADDED, true);
            }
            dealJson.put(fields.get(dealValueAlias).idAsString(), monetaryJson);
        }

        if (Objects.nonNull(dealSource) && fields.containsKey(dealSourceAlias)) {
            dealJson.put(fields.get(dealSourceAlias).idAsString(), optionConverter.convertModelToOptionJson(dealSource, converterConfig));
        }

        if (Objects.nonNull(type) && fields.containsKey(typeAlias)) {
            dealJson.put(fields.get(typeAlias).idAsString(), optionConverter.convertModelToOptionJson(type, converterConfig));
        }

        if (Objects.nonNull(paymentStatus) && fields.containsKey(paymentStatusAlias)) {
            dealJson.put(fields.get(paymentStatusAlias).idAsString(), optionConverter.convertModelToOptionJson(paymentStatus, converterConfig));
        }

        if (Objects.nonNull(lostReason) && fields.containsKey(lostReasonAlias)) {
            dealJson.put(fields.get(lostReasonAlias).idAsString(), optionConverter.convertModelToOptionJson(lostReason, converterConfig));
        }

        if (Objects.nonNull(wonReason) && fields.containsKey(wonReasonAlias)) {
            dealJson.put(fields.get(wonReasonAlias).idAsString(), optionConverter.convertModelToOptionJson(wonReason, converterConfig));
        }

        if (Objects.nonNull(createdBy) && fields.containsKey(createdByAlias)) {
            dealJson.put(fields.get(createdByAlias).idAsString(), createdBy.id());
        }

        if (Objects.nonNull(updatedBy) && fields.containsKey(updatedByAlias)) {
            dealJson.put(fields.get(updatedByAlias).idAsString(), updatedBy.id());
        }

        if (Objects.nonNull(territory) && fields.containsKey(territoryAlias)) {
            dealJson.put(fields.get(territoryAlias).idAsString(), territoryConverter.convertModelToOptionJson(territory));
        }

        if (Objects.nonNull(forecastCategory) && fields.containsKey(forecastCategoryAlias)) {
            final JsonObject jsonObject = Json.object();
            final I18nKey displayName = forecastCategory.getDisplayName();
            final String name = Objects.nonNull(displayName) && !forecastCategory.isBestCase() ?
                    converterConfig.getI18nPlaceholderResolver().get(displayName) : forecastCategory.getName();

            jsonObject.put(DealFields.VALUE, forecastCategory.id());
            jsonObject.put(DealFields.NAME, name);

            dealJson.put(fields.get(forecastCategoryAlias).idAsString(), jsonObject);
        }

        if (Objects.nonNull(valueHolder)) {
            valueHolder.mapToJson(dealJson, converterConfig);
        }

        if (Objects.nonNull(summaryInfo)) {
            final Collection<Tag> tags = summaryInfo.getTags();

            if (Objects.nonNull(tags) && !tags.isEmpty() && fields.containsKey(tagsAlias)) {
                dealJson.put(fields.get(tagsAlias).idAsString(), tagConverter.convertModelToJson(tags));
            }
        }
        return dealJson;
    }

    /**
     * {@inheritDoc}
     *
     * @param deal            The {@link Deal} entity containing the data to be converted.
     * @param converterConfig The {@link ConverterConfig} that provides configuration options.
     * @return the {@link JsonObject} filled with data
     */
    @Override
    public JsonObject convertToRestJson(final Deal deal, final ConverterConfig converterConfig) {
        final long orgId = deal.orgId();
        final JsonObject dealJson = Json.object();
        final Map<String, FieldInfo> fields = secureFieldService.groupDefaultFieldsByAliasName(Deal.ENTITY_TYPE);
        final ValueHolder valueHolder = deal.getValueHolder();
        final Company company = deal.getCompany();
        final DealValue dealValue = deal.getDealValue();
        final Pipeline pipeline = deal.getPipeline();
        final PipelineStage stage = deal.getPipelineStage();
        final Collection<Participant> dealParticipants = deal.getParticipants();
        final Territory territory = deal.getTerritory();
        final Option lostReason = deal.getLostReason();
        final Option wonReason = deal.getWonReason();
        final Option paymentStatus = deal.getPaymentStatus();
        final Option dealSource = deal.getSource();
        final Option type = deal.getType();
        final CrmUser createdBy = deal.getCreatedBy();
        final CrmUser updatedBy = deal.getUpdatedBy();
        final JsonObject monetaryJson;
        final ProductDetailsHolder productDetailsHolder;
        final SummaryInfo summaryInfo = deal.getSummaryInfo();
        final Contact primaryContact = deal.getPrimaryContact();
        final CrmUser salesOwner = deal.getSalesOwner();
        final ForecastCategory forecastCategory = deal.getForecastCategory();

        final String expectedCloseDateAlias = DealFields.EXPECTED_CLOSE_DATE;
        final String pipelineAlias = DealFields.PIPELINE;
        final String pipelineStageAlias = DealFields.PIPELINE_STAGE;
        final String dealClosedOnAlias = DealFields.DEAL_CLOSED_ON;
        final String descriptionAlias = DealFields.DESCRIPTION;
        final String statusAlias = DealFields.STATUS;
        final String salesOwnerAlias = DealFields.SALES_OWNER;
        final String primaryContactAlias = DealFields.PRIMARY_CONTACT;
        final String companyAlias = DealFields.COMPANY;
        final String relatedContactsAlias = DealFields.RELATED_CONTACTS;
        final String dealValueAlias = DealFields.DEAL_VALUE;
        final String dealSourceAlias = DealFields.DEAL_SOURCE;
        final String typeAlias = DealFields.TYPE;
        final String paymentStatusAlias = DealFields.PAYMENT_STATUS;
        final String lostReasonAlias = DealFields.LOST_REASON;
        final String wonReasonAlias = DealFields.WON_REASON;
        final String createdByAlias = DealFields.CREATED_BY;
        final String updatedByAlias = DealFields.UPDATED_BY;
        final String territoryAlias = DealFields.TERRITORY;
        final String tagsAlias = DealFields.TAGS;
        final String forecastCategoryAlias = DealFields.FORECAST_CATEGORY;

        if (Objects.isNull(dealValue)) {
            monetaryJson = null;
            productDetailsHolder = null;
        } else {
            monetaryJson = dealValueConverter.convertToRestJson(dealValue, converterConfig);
            productDetailsHolder = deal.getProductDetailsHolder();
        }
        dealJson.put(DealFields.ID, deal.id());
        dealJson.put(fields.get(DealFields.TITLE).getInternalName(), deal.getTitle());
        dealJson.put(DealFields.IS_ARCHIVED, deal.isArchived());

        if (fields.containsKey(expectedCloseDateAlias)) {
            dealJson.put(fields.get(expectedCloseDateAlias).getInternalName(), dateUtil.getTimeStamp(deal.getExpectedCloseDate()));
        }

        if (Objects.nonNull(pipeline) && fields.containsKey(pipelineAlias)) {
            dealJson.put(fields.get(pipelineAlias).getInternalName(), pipelineConverter.convertModelToOptionJson(pipeline));
        }

        if (Objects.nonNull(stage) && fields.containsKey(pipelineStageAlias)) {
            dealJson.put(fields.get(pipelineStageAlias).getInternalName(), pipelineStageConverter.convertModelToOptionJson(stage));
        }

        if (fields.containsKey(dealClosedOnAlias)) {
            dealJson.put(fields.get(dealClosedOnAlias).getInternalName(), dateTimeUtil.getTimeStamp(deal.getClosedTime()));
        }

        if (fields.containsKey(descriptionAlias)) {
            dealJson.put(fields.get(descriptionAlias).getInternalName(), deal.getDescription());
        }

        if (Objects.nonNull(salesOwner) && fields.containsKey(salesOwnerAlias)) {
            dealJson.put(fields.get(salesOwnerAlias).getInternalName(), crmUserConverter.convertModelToOptionJson(salesOwner));
        }

        if (Objects.nonNull(primaryContact) && fields.containsKey(primaryContactAlias)) {
            dealJson.put(fields.get(primaryContactAlias).getInternalName(), contactConverter.convertModelToOptionJson(primaryContact));
        }

        if (Objects.nonNull(company) && fields.containsKey(companyAlias)) {
            dealJson.put(fields.get(companyAlias).getInternalName(), companyConverter.convertModelToOptionJson(company));
        }
        dealJson.put(DealFields.TOTAL_PRODUCTS, deal.getTotalProductCount());

        if (Objects.nonNull(dealParticipants) && !dealParticipants.isEmpty() && fields.containsKey(relatedContactsAlias)) {
            final JsonArray participants = Json.array();

            for (final Participant participant : dealParticipants) {
                final Contact relatedContact = participant.getContact();

                participants.put(contactConverter.convertModelToOptionJson(relatedContact));
            }
            dealJson.put(fields.get(relatedContactsAlias).getInternalName(), participants);
        }

        if (Objects.nonNull(monetaryJson) && fields.containsKey(dealValueAlias)) {

            if (sourceSettingsService.isSourceEnabled(orgId, PRODUCT_NAME) && Objects.nonNull(productDetailsHolder)) {
                monetaryJson.put(DealFields.IS_MANUALLY_ADDED, false);
                monetaryJson.put(StoreSubType.PRODUCT.getApiPropertyName(), productDetailsHolderConverter.convertModelToJson(productDetailsHolder, converterConfig));
            } else {
                monetaryJson.put(DealFields.IS_MANUALLY_ADDED, true);
            }
            dealJson.put(fields.get(dealValueAlias).getInternalName(), monetaryJson);
        }

        if (Objects.nonNull(dealSource) && fields.containsKey(dealSourceAlias)) {
            dealJson.put(fields.get(dealSourceAlias).getInternalName(), optionConverter.convertModelToOptionJson(dealSource, converterConfig));
        }

        if (Objects.nonNull(type) && fields.containsKey(typeAlias)) {
            dealJson.put(fields.get(typeAlias).getInternalName(), optionConverter.convertModelToOptionJson(type, converterConfig));
        }

        if (Objects.nonNull(paymentStatus) && fields.containsKey(paymentStatusAlias)) {
            dealJson.put(fields.get(paymentStatusAlias).getInternalName(), optionConverter.convertModelToOptionJson(paymentStatus, converterConfig));
        }

        if (Objects.nonNull(lostReason) && fields.containsKey(lostReasonAlias)) {
            dealJson.put(fields.get(lostReasonAlias).getInternalName(), optionConverter.convertModelToOptionJson(lostReason, converterConfig));
        }

        if (Objects.nonNull(wonReason) && fields.containsKey(wonReasonAlias)) {
            dealJson.put(fields.get(wonReasonAlias).getInternalName(), optionConverter.convertModelToOptionJson(wonReason, converterConfig));
        }

        if (Objects.nonNull(createdBy) && fields.containsKey(createdByAlias)) {
            dealJson.put(fields.get(createdByAlias).getInternalName(), createdBy.id());
        }

        if (Objects.nonNull(updatedBy) && fields.containsKey(updatedByAlias)) {
            dealJson.put(fields.get(updatedByAlias).getInternalName(), updatedBy.id());
        }

        if (Objects.nonNull(territory) && fields.containsKey(territoryAlias)) {
            dealJson.put(fields.get(territoryAlias).getInternalName(), territoryConverter.convertModelToOptionJson(territory));
        }

        if (Objects.nonNull(forecastCategory) && fields.containsKey(forecastCategoryAlias)) {
            final JsonObject jsonObject = Json.object();
            final I18nKey displayName = forecastCategory.getDisplayName();
            final String name = Objects.nonNull(displayName) && !forecastCategory.isBestCase() ?
                    converterConfig.getI18nPlaceholderResolver().get(displayName) : forecastCategory.getName();

            jsonObject.put(DealFields.VALUE, forecastCategory.id());
            jsonObject.put(DealFields.NAME, name);

            dealJson.put(fields.get(forecastCategoryAlias).getInternalName(), jsonObject);
        }

        if (Objects.nonNull(valueHolder)) {
            valueHolder.mapToJson(dealJson, converterConfig, true);
        }

        if (Objects.nonNull(summaryInfo)) {
            final Collection<Tag> tags = summaryInfo.getTags();

            if (Objects.nonNull(tags) && !tags.isEmpty() && fields.containsKey(tagsAlias)) {
                dealJson.put(fields.get(tagsAlias).getInternalName(), tagConverter.convertModelToJson(tags));
            }
        }
        return dealJson;
    }

    /**
     * <p>
     * Converts the {@link Deal} to {@link Json}
     * </p>
     *
     * @param deal Represents the {@link Deal}
     * @return {@link JsonObject}
     */
    public JsonObject convertDealIntoJson(final Deal deal) {
        final PipelineStage pipelineStage = deal.getPipelineStage();
        final Company company = deal.getCompany();
        final Collection<Participant> participants = deal.getParticipants();
        final JsonObject dealJson = Json.object();
        final JsonObject pipelineStageJson = Json.object();
        final Pipeline pipeline = deal.getPipeline();
        final String pipelineStageName = pipelineStage.getName();
        final Date expectedCloseDate = deal.getExpectedCloseDate();
        final Contact primaryContact = deal.getPrimaryContact();

        if (Objects.nonNull(participants) && !participants.isEmpty()) {
            final JsonArray participantsArray = Json.array();

            for (final Participant participant : participants) {
                final JsonObject participantsJson = Json.object();
                final Contact contact = participant.getContact();
                final Long contactId = contact.id();
                final DisplayPicture thumbnail = contact.getThumbnail();

                participantsJson.put(DealFields.ID, contactId);
                participantsJson.put(DealFields.NAME, contact.getFullName());

                if (Objects.nonNull(thumbnail)) {
                    participantsJson.put(DealFields.PROFILE, convertThumbnailToJson(contactId, Contact.ENTITY_TYPE, thumbnail));
                }

                if (Objects.equals(primaryContact, contact)) {
                    participantsJson.put(DealFields.IS_PRIMARY, true);
                }

                participantsArray.put(participantsJson);
            }

            dealJson.put(DealFields.CONTACTS, participantsArray);
        }

        if (Objects.nonNull(company)) {
            final JsonObject companyJson = Json.object();
            final Long companyId = company.id();
            final DisplayPicture thumbnail = company.getThumbnail();

            companyJson.put(DealFields.ID, companyId);
            companyJson.put(DealFields.NAME, company.getName());

            if (Objects.nonNull(thumbnail)) {
                companyJson.put(DealFields.PROFILE, convertThumbnailToJson(companyId, Company.ENTITY_TYPE, thumbnail));
            }
            dealJson.put(DealFields.COMPANY, companyJson);
        }

        if (Objects.nonNull(expectedCloseDate)) {
            final Long expectedCloseDateTimeStamp = expectedCloseDate.getTimeStamp();

            dealJson.put(DealFields.EXPECTED_CLOSE_DATE, expectedCloseDateTimeStamp);
            dealJson.put(DealFields.IS_OVERDUE, Instant.now().toEpochMilli() > expectedCloseDateTimeStamp);
        }

        pipelineStageJson.put(DealFields.ID, pipelineStage.id());
        pipelineStageJson.put(DealFields.NAME, pipelineStage.getName());
        dealJson.put(DealFields.PIPELINE_STAGE, pipelineStageJson);
        dealJson.put(DealFields.TITLE, deal.getTitle());
        dealJson.put(DealFields.DEAL_COMPLETION_PERCENTAGE, Objects.equals(Deal.State.WON.getDisplayName(), pipelineStageName) ? 100 : ((double) pipelineStage.getOrder() / pipeline.getTotalStagesCount()) * 100);
        dealJson.put(DealFields.ID, deal.id());
        dealJson.put(DealFields.PIPELINE_ID, pipeline.id());
        dealJson.put(DealFields.ORG_ID, deal.orgId());
        dealJson.put(DealFields.DEAL_VALUE, monetaryConverter.convert(new ConverterConfig(false, false), deal.getDealValue()));

        return dealJson;
    }

    /**
     * {@inheritDoc}
     *
     * @param deal            Holds the data of specific entity
     * @param converterConfig Represents the {@link ConverterConfig}
     * @param isHidden        A boolean flag indicating whether to display empty fields or not
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject convertSummaryIntoJson(final Deal deal, final ConverterConfig converterConfig, final boolean isHidden) {
        final JsonObject json = Json.object();
        final JsonObject profileJson = convertProfileToJson(deal, converterConfig);

        profileJson.put(DealFields.IS_ARCHIVED, deal.isArchived());

        json.put(DealFields.ID, deal.id());
        json.put(DealFields.PROFILE, profileJson);
        json.put(DealFields.SUMMARY, convertSummaryToJsonArray(deal, isHidden, converterConfig));
        convertSubFieldToJson(deal, converterConfig, json);

        if (converterConfig.isAccessConfigRequired()) {
            json.put(FieldKeys.IS_EDITABLE.getName(), deal.isEditable());
            json.put(FieldKeys.IS_DELETABLE.getName(), deal.isDeletable());
        }

        return json;
    }

    /**
     * {@inheritDoc}
     *
     * @param deal            Represents the {@link Deal}
     * @param converterConfig Represents the {@link ConverterConfig}
     * @return The {@link JsonObject} containing summary details
     */
    @Override
    public JsonObject summaryV2(final Long orgId, final Long userId, final Deal deal, final ConverterConfig converterConfig) {
        final JsonObject dealJson = Json.object();

        dealJson.put(DealFields.ID, deal.id());
        loadSummaryFieldPreference(orgId, userId, Source.DEAL.getId(), dealJson);
        loadSummaryFields(deal, dealJson, converterConfig);
        convertSubFieldToJson(deal, converterConfig, dealJson);
        dealJson.put(DealFields.IS_ARCHIVED, deal.isArchived());

        if (converterConfig.isAccessConfigRequired()) {
            dealJson.put(FieldKeys.IS_EDITABLE.getName(), deal.isEditable());
            dealJson.put(FieldKeys.IS_DELETABLE.getName(), deal.isDeletable());
        }
        return dealJson;
    }
}