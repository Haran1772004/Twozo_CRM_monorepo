package com.tlc.crm.core.api.contact.model.converter;

import com.tlc.attachment.model.profilePicture.DisplayPicture;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.TypeChecker;
import com.tlc.crm.common.iam.api.model.converter.CrmUserConverter;
import com.tlc.crm.common.iam.api.model.converter.TerritoryConverter;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.Territory;
import com.tlc.crm.common.iam.service.UserService;

import com.tlc.crm.common.util.NumberUtils;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.summaryInfo.model.SummaryInfo;
import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.model.EmailSummary;
import com.tlc.crm.core.api.summaryInfo.model.converter.SummaryInfoConverter;
import com.tlc.crm.core.api.deal.model.converter.DealConverter;
import com.tlc.crm.core.features.api.activity.model.ActivitySummary;
import com.tlc.crm.core.field.contact.CompanyFields;
import com.tlc.crm.core.field.deal.DealFields;

import com.tlc.crm.entity.builder.ViewConverter;
import com.tlc.crm.entity.features.api.tag.model.Tag;
import com.tlc.crm.entity.features.api.tag.model.converter.TagConverter;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.type.converter.OptionConverter;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.entity.models.ValueHolder;
import com.tlc.crm.entity.models.type.Address;
import com.tlc.crm.entity.models.type.Monetary;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.entity.models.type.converter.AddressConverter;
import com.tlc.crm.entity.models.type.converter.MonetaryConverter;
import com.tlc.l10n.temporal.model.Date;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.l10n.temporal.resolver.DateUtil;
import com.tlc.l10n.temporal.resolver.DateTimeUtil;
import com.tlc.validator.TlcModel;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Provides different ways to convert the data from / into {@link Company}
 *
 * @author Selvakumar
 * @version 1.0
 */
public final class CompanyConverter extends ViewConverter<Company> {

    private final CompanyManager companyManager;
    private final MonetaryConverter monetaryConverter;
    private final AddressConverter addressConverter;
    private final UserService userService;
    private final CrmUserConverter crmUserConverter;
    private final TerritoryConverter territoryConverter;
    private final TagConverter tagConverter;
    private final SummaryInfoConverter summaryInfoConverter;
    private DealConverter dealConverter;
    private final DateTimeUtil dateTimeUtil;
    private final DateUtil dateUtil;
    private final OptionConverter optionConverter;

    private static class Instance {
        private static final CompanyConverter INSTANCE = new CompanyConverter();
    }

    private CompanyConverter() {
        companyManager = CompanyManager.getInstance();
        monetaryConverter = MonetaryConverter.getInstance();
        addressConverter = AddressConverter.getInstance();
        userService = UserService.getInstance();
        crmUserConverter = CrmUserConverter.getInstance();
        territoryConverter = TerritoryConverter.getInstance();
        tagConverter = TagConverter.getInstance();
        summaryInfoConverter = SummaryInfoConverter.getInstance();
        dateTimeUtil = DateTimeUtil.getInstance();
        dateUtil = DateUtil.getInstance();
        optionConverter = OptionConverter.getInstance();
    }

    public static CompanyConverter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Converts json attributes data into {@link Company}
     *
     * @param converterConfig Converter configuration settings
     * @param jsonObject      Holds the company attributes
     * @return {@link Company}
     */
    @Override
    public Company convert(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        if (converterConfig.isPartialData()) {
            return constructEntityByPartialData(converterConfig, jsonObject);
        } else {
            return constructEntity(converterConfig, jsonObject);
        }

    }

    /**
     * Converts json attributes into {@link Company}
     *
     * @param converterConfig Converter configuration settings
     * @param jsonObject      Holds the company attributes
     * @return {@link Company}
     */
    private Company constructEntity(ConverterConfig converterConfig, JsonObject jsonObject) {
        final Company company = new Company();
        final Long orgId = converterConfig.orgId();
        final String idKey = CompanyFields.ID;

        if (jsonObject.containsKey(idKey)) {
            final long id = jsonObject.getLong(idKey);

            company.setId(id);
            company.setSalesOwner(companyManager.partialGet(orgId, id).getSalesOwner());
        } else {
            company.setCreatedBy(userService.getUser(orgId, converterConfig.ownerId()));
        }
        setJsonDataToModel(converterConfig, company, jsonObject);

        return company;
    }

    /**
     * Converts partial data into {@link Company}
     *
     * @param converterConfig Converter configuration settings
     * @param jsonObject      Holds the company attributes
     * @return {@link Company}
     */
    private Company constructEntityByPartialData(ConverterConfig converterConfig, JsonObject jsonObject) {
        final Company company = companyManager.get(converterConfig.orgId(), jsonObject.getLong("id"));

        setJsonDataToModel(converterConfig, company, jsonObject);
        return company;
    }

    /**
     * Sets JSON data into {@link Company}
     *
     * @param converterConfig Converter configuration settings
     * @param company         A model to which the company attributes will be set
     * @param jsonObject      Holds the field values of company
     */
    public void setJsonDataToModel(final ConverterConfig converterConfig, final Company company,
                                   final JsonObject jsonObject) {
        final Long orgId = converterConfig.orgId();
        final Long ownerId = converterConfig.ownerId();
        final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(Company.ENTITY_TYPE);
        final String nameKey = fields.get(CompanyFields.NAME).idAsString();
        final String websiteKey = fields.get(CompanyFields.WEBSITE).idAsString();
        final String employeeCountKey = fields.get(CompanyFields.EMPLOYEE_COUNT).idAsString();
        final String facebookKey = fields.get(CompanyFields.FACEBOOK).idAsString();
        final String linkedInKey = fields.get(CompanyFields.LINKEDIN).idAsString();
        final String twitterKey = fields.get(CompanyFields.TWITTER).idAsString();
        final String parentCompanyKey = fields.get(CompanyFields.PARENT_COMPANY).idAsString();
        final String sicCodeKey = fields.get(CompanyFields.SIC_CODE).idAsString();
        final String annualRevenueKey = fields.get(CompanyFields.ANNUAL_REVENUE).idAsString();
        final String addressKey = fields.get(CompanyFields.ADDRESS).idAsString();
        final String salesOwnerKey = fields.get(CompanyFields.SALES_OWNER).idAsString();
        final String territoryKey = fields.get(CompanyFields.TERRITORY).idAsString();
        final String descriptionKey = fields.get(CompanyFields.DESCRIPTION).idAsString();
        final String tagKey = fields.get(CompanyFields.TAGS).idAsString();

        final FieldInfo businessTypeField = fields.get(CompanyFields.BUSINESS_TYPE);
        final FieldInfo organisationStatusField = fields.get(CompanyFields.ORGANIZATION_STATUS);
        final FieldInfo industryTypeField = fields.get(CompanyFields.INDUSTRY_TYPE);

        final String businessTypeKey = businessTypeField.idAsString();
        final String industryTypeKey = industryTypeField.idAsString();
        final String organisationStatusKey = organisationStatusField.idAsString();

        company.setName(jsonObject.containsKey(nameKey) ? jsonObject.getNullableString(nameKey) : company.getName());
        company.setOrgId(orgId);
        company.setWebsite(jsonObject.optNullableValue(websiteKey, company.getWebsite()));
        company.setEmployeeCount(jsonObject.optNullableValue(employeeCountKey, company.getEmployeeCount()));
        company.setFacebook(jsonObject.optNullableValue(facebookKey, company.getFacebook()));
        company.setLinkedIn(jsonObject.optNullableValue(linkedInKey, company.getLinkedIn()));
        company.setTwitter(jsonObject.optNullableValue(twitterKey, company.getTwitter()));
        company.setDescription(jsonObject.optNullableValue(descriptionKey, company.getDescription()));

        if (jsonObject.containsKey(parentCompanyKey)) {
            if (jsonObject.isKeyPresentWithNullValue(parentCompanyKey)) {
                company.setParentCompany(null);
            } else {
                buildParentCompany(parentCompanyKey, converterConfig, company, jsonObject);
            }
        }

        if (jsonObject.containsKey(sicCodeKey)) {
            company.setSicCode(jsonObject.getNullableValue(sicCodeKey));
        } else if (converterConfig.isPartialData()) {
            company.setSicCode(company.getSicCode());
        }

        setCompanyOptions(orgId, jsonObject, businessTypeKey, company::setBusinessType, businessTypeField.id());
        setCompanyOptions(orgId, jsonObject, industryTypeKey, company::setIndustryType, industryTypeField.id());
        setCompanyOptions(orgId, jsonObject, organisationStatusKey, company::setOrganizationStatus, organisationStatusField.id());

        if (jsonObject.containsKey(annualRevenueKey)) {
            if (jsonObject.isKeyPresentWithNullValue(annualRevenueKey)) {
                company.setAnnualRevenue(null);
            } else {
                if (jsonObject.get(annualRevenueKey) instanceof final Map<?, ?> monetaryMap) {
                    final JsonObject monetaryJson = Json.object((Map<String, ?>) monetaryMap);

                    if (converterConfig.isPartialData()) {
                        final Monetary oldAnnualRevenue = company.getAnnualRevenue();
                        final Monetary newMonetary = Objects.isNull(oldAnnualRevenue) ? new Monetary() : oldAnnualRevenue;

                        monetaryConverter.setDataToMonetary(converterConfig, newMonetary, monetaryJson);
                        company.setAnnualRevenue(newMonetary);
                    } else {
                        company.setAnnualRevenue(monetaryConverter.convert(converterConfig, monetaryJson));
                    }
                }
            }
        }

        setSalesOwner(orgId, salesOwnerKey, jsonObject, company, ownerId);
        company.setUpdatedBy(userService.getUser(orgId, ownerId));
        entityBuilder.buildValueHolder(company, converterConfig, jsonObject);

        if (jsonObject.containsKey(tagKey)) {
            company.setSummaryInfo(summaryInfoConverter.buildTags(tagKey, company.getSummaryInfo(), converterConfig, jsonObject));
        }

        if (jsonObject.containsKey(addressKey)) {
            if (jsonObject.isKeyPresentWithNullValue(addressKey)) {
                company.setAddress(null);
            } else {
                final Address companyAddress;
                final JsonObject addressJson = jsonObject.getJsonObject(addressKey);

                if (converterConfig.isPartialData()) {
                    final Address oldAddress = company.getAddress();
                    final Address address = Objects.isNull(oldAddress) ? new Address() : oldAddress;

                    addressConverter.setDataToAddress(converterConfig, address, addressJson);
                    companyAddress = address;
                } else {
                    companyAddress = addressConverter.convert(converterConfig, addressJson);
                }
                company.setAddress(companyAddress);
            }
        }

        if (jsonObject.containsKey(territoryKey)) {
            if (jsonObject.isKeyPresentWithNullValue(territoryKey)) {
                company.setTerritory(null);
            } else {
                final Territory territory = new Territory();

                territory.setId(jsonObject.getLong(territoryKey));
                company.setTerritory(territory);
            }
        }

        if (converterConfig.areReadOnlyFieldsRequired()) {
            final String createdAtKey = fields.get(CompanyFields.CREATED_AT).idAsString();

            if (jsonObject.containsKey(createdAtKey)) {
                company.setCreatedTime(new DateTime(jsonObject.getLong(createdAtKey)));
            }
        }
        defaultFieldValuesLoader.load(company);
    }

    /**
     * <p>
     * Builds the parent company from the request and set it to the company
     * </p>
     *
     * @param idAsString      Represents the field id as string
     * @param converterConfig Represents the config used to convert
     * @param company         Represents the {@link Company} to be updated
     * @param jsonObject      Represents the request json
     */
    private void buildParentCompany(final String idAsString, final ConverterConfig converterConfig, final Company company,
                                    final JsonObject jsonObject) {
        final Object value = jsonObject.opt(idAsString);
        final long orgId = converterConfig.orgId();
        final boolean isPartial = converterConfig.isPartialData();

        if (Objects.nonNull(value)) {
            if (TypeChecker.isNonDecimalNumber(value)) {
                final Company parentCompany = new Company();

                parentCompany.setId(NumberUtils.parseNumber(value).longValue());
                parentCompany.setOrgId(orgId);
                company.setParentCompany(parentCompany);
            } else if (value instanceof final Map<?, ?> companyMap) {
                converterConfig.setPartialData(false);
                company.setParentCompany(convert(converterConfig, Json.object((Map<String, ?>) companyMap)));
                converterConfig.setPartialData(isPartial);
            }
        }
    }

    /**
     * <p>
     * Converts {@link Company} into {@link JsonObject}
     * </p>
     *
     * @param company A model that holds the attributes of company
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject convert(Company company) {
        final Map<String, FieldInfo> fields = secureFieldService.groupDefaultFieldsByAliasName(Company.ENTITY_TYPE);
        final JsonObject jsonObject = Json.object();
        final ValueHolder valueHolder = company.getValueHolder();
        final Option businessType = company.getBusinessType();
        final Option industryType = company.getIndustryType();
        final Option organisationStatus = company.getOrganizationStatus();
        final Address address = company.getAddress();
        final Monetary annualRevenue = company.getAnnualRevenue();
        final CrmUser createdBy = company.getCreatedBy();
        final CrmUser updatedBy = company.getUpdatedBy();
        final Territory territory = company.getTerritory();
        final Company parentCompany = company.getParentCompany();
        final SummaryInfo summaryInfo = company.getSummaryInfo();
        final CrmUser salesOwner = company.getSalesOwner();

        final String employeeCountAlias = CompanyFields.EMPLOYEE_COUNT;
        final String facebookAlias = CompanyFields.FACEBOOK;
        final String linkedinAlias = CompanyFields.LINKEDIN;
        final String twitterAlias = CompanyFields.TWITTER;
        final String sicCodeAlias = CompanyFields.SIC_CODE;
        final String websiteAlias = CompanyFields.WEBSITE;
        final String descriptionAlias = CompanyFields.DESCRIPTION;
        final String updatedAtAlias = CompanyFields.UPDATED_AT;
        final String createdAtAlias = CompanyFields.CREATED_AT;
        final String lastAssignedAtAlias = CompanyFields.LAST_ASSIGNED_AT;
        final String addressAlias = CompanyFields.ADDRESS;
        final String parentCompanyAlias = CompanyFields.PARENT_COMPANY;
        final String annualRevenueAlias = CompanyFields.ANNUAL_REVENUE;
        final String businessTypeAlias = CompanyFields.BUSINESS_TYPE;
        final String industryTypeAlias = CompanyFields.INDUSTRY_TYPE;
        final String organizationStatusAlias = CompanyFields.ORGANIZATION_STATUS;
        final String createdByAlias = CompanyFields.CREATED_BY;
        final String updatedByAlias = CompanyFields.UPDATED_BY;
        final String territoryAlias = CompanyFields.TERRITORY;
        final String salesOwnerAlias = CompanyFields.SALES_OWNER;

        jsonObject.put(CompanyFields.ID, company.id());
        jsonObject.put(CompanyFields.ORG_ID, company.orgId());
        jsonObject.put(fields.get(CompanyFields.NAME).idAsString(), company.getName());

        if (fields.containsKey(employeeCountAlias)) {
            jsonObject.put(fields.get(employeeCountAlias).idAsString(), company.getEmployeeCount());
        }

        if (fields.containsKey(facebookAlias)) {
            jsonObject.put(fields.get(facebookAlias).idAsString(), company.getFacebook());
        }

        if (fields.containsKey(linkedinAlias)) {
            jsonObject.put(fields.get(linkedinAlias).idAsString(), company.getLinkedIn());
        }

        if (fields.containsKey(twitterAlias)) {
            jsonObject.put(fields.get(twitterAlias).idAsString(), company.getTwitter());
        }

        if (fields.containsKey(sicCodeAlias)) {
            jsonObject.put(fields.get(sicCodeAlias).idAsString(), company.getSicCode());
        }

        if (fields.containsKey(websiteAlias)) {
            jsonObject.put(fields.get(websiteAlias).idAsString(), company.getWebsite());
        }

        if (fields.containsKey(descriptionAlias)) {
            jsonObject.put(fields.get(descriptionAlias).idAsString(), company.getDescription());
        }

        if (fields.containsKey(updatedAtAlias)) {
            jsonObject.put(fields.get(updatedAtAlias).idAsString(), dateTimeUtil.getTimeStamp(company.getModifiedTime()));
        }

        if (fields.containsKey(createdAtAlias)) {
            jsonObject.put(fields.get(createdAtAlias).idAsString(), dateTimeUtil.getTimeStamp(company.getCreatedTime()));
        }

        if (fields.containsKey(lastAssignedAtAlias)) {
            jsonObject.put(fields.get(lastAssignedAtAlias).idAsString(), dateTimeUtil.getTimeStamp(company.getLastAssignedAt()));
        }

        if (Objects.nonNull(address) && fields.containsKey(addressAlias)) {
            jsonObject.put(fields.get(addressAlias).idAsString(), addressConverter.convert(address));
        }

        if (Objects.nonNull(parentCompany) && fields.containsKey(parentCompanyAlias)) {
            jsonObject.put(fields.get(parentCompanyAlias).idAsString(), parentCompany.id());
        }

        if (Objects.nonNull(annualRevenue) && fields.containsKey(annualRevenueAlias)) {
            jsonObject.put(fields.get(annualRevenueAlias).idAsString(), monetaryConverter.convert(annualRevenue));
        }

        if (Objects.nonNull(businessType) && fields.containsKey(businessTypeAlias)) {
            jsonObject.put(fields.get(businessTypeAlias).idAsString(), businessType.id());
        }

        if (Objects.nonNull(industryType) && fields.containsKey(industryTypeAlias)) {
            jsonObject.put(fields.get(industryTypeAlias).idAsString(), industryType.id());
        }

        if (Objects.nonNull(organisationStatus) && fields.containsKey(organizationStatusAlias)) {
            jsonObject.put(fields.get(organizationStatusAlias).idAsString(), organisationStatus.id());
        }

        if (Objects.nonNull(valueHolder)) {
            valueHolder.mapToJson(jsonObject);
        }

        if (Objects.nonNull(createdBy) && fields.containsKey(createdAtAlias)) {
            jsonObject.put(fields.get(createdByAlias).idAsString(), createdBy.id());
        }

        if (Objects.nonNull(updatedBy) && fields.containsKey(updatedByAlias)) {
            jsonObject.put(fields.get(updatedByAlias).idAsString(), updatedBy.id());
        }

        if (Objects.nonNull(territory) && fields.containsKey(territoryAlias)) {
            jsonObject.put(fields.get(territoryAlias).idAsString(), territory.id());
        }

        if (Objects.nonNull(summaryInfo)) {
            final Collection<Tag> tags = summaryInfo.getTags();
            final String tagsAlias = CompanyFields.TAGS;

            if (Objects.nonNull(tags) && !tags.isEmpty() && fields.containsKey(tagsAlias)) {
                jsonObject.put(fields.get(tagsAlias).idAsString(), tagConverter.convertTags(tags));
            }
        }

        if (Objects.nonNull(salesOwner) && fields.containsKey(salesOwnerAlias)) {
            jsonObject.put(fields.get(salesOwnerAlias).idAsString(), salesOwner.id());
        }

        return jsonObject;
    }

    /**
     * Converts {@link Company} into {@link JsonObject}
     *
     * @param company         An entity that holds the attributes of company details
     * @param converterConfig Converter configuration settings
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject convert(final ConverterConfig converterConfig, final Company company) {
        final JsonObject jsonObject = Json.object();

        if (converterConfig.isIdRequiredOnKey()) {
            return convert(company);
        } else {
            final ValueHolder valueHolder = company.getValueHolder();
            final Option businessType = company.getBusinessType();
            final Option industryType = company.getIndustryType();
            final Option organisationType = company.getOrganizationStatus();
            final Address address = company.getAddress();
            final Monetary annualRevenue = company.getAnnualRevenue();
            final Company parentCompany = company.getParentCompany();

            jsonObject.put(CompanyFields.ID, company.id());
            jsonObject.put(CompanyFields.ORG_ID, company.orgId());
            jsonObject.put(CompanyFields.NAME, company.getName());
            jsonObject.put(CompanyFields.EMPLOYEE_COUNT, company.getEmployeeCount());
            jsonObject.put(CompanyFields.FACEBOOK, company.getFacebook());
            jsonObject.put(CompanyFields.LINKEDIN, company.getLinkedIn());
            jsonObject.put(CompanyFields.TWITTER, company.getTwitter());
            jsonObject.put(CompanyFields.SIC_CODE, company.getSicCode());
            jsonObject.put(CompanyFields.WEBSITE, company.getWebsite());
            jsonObject.put(CompanyFields.CREATED_AT, dateTimeUtil.getTimeStamp(company.getCreatedTime()));
            jsonObject.put(CompanyFields.UPDATED_AT, dateTimeUtil.getTimeStamp(company.getModifiedTime()));
            jsonObject.put(CompanyFields.LAST_ASSIGNED_AT, dateTimeUtil.getTimeStamp(company.getLastAssignedAt()));
            jsonObject.put(CompanyFields.DESCRIPTION, company.getDescription());

            if (Objects.nonNull(parentCompany)) {
                jsonObject.put(CompanyFields.PARENT_COMPANY, convertPartialData(parentCompany));
            }

            if (Objects.nonNull(address)) {
                jsonObject.put(CompanyFields.ADDRESS, addressConverter.convert(new ConverterConfig(false, false), address));
            }

            if (Objects.nonNull(annualRevenue)) {
                jsonObject.put(CompanyFields.ANNUAL_REVENUE, monetaryConverter.convert(new ConverterConfig(false, false), annualRevenue));
            }

            if (Objects.nonNull(businessType)) {
                jsonObject.put(CompanyFields.BUSINESS_TYPE, businessType.id());
            }

            if (Objects.nonNull(industryType)) {
                jsonObject.put(CompanyFields.INDUSTRY_TYPE, industryType.id());
            }

            if (Objects.nonNull(organisationType)) {
                jsonObject.put(CompanyFields.ORGANIZATION_STATUS, organisationType.id());
            }

            if (Objects.nonNull(valueHolder)) {
                valueHolder.mapToJson(jsonObject);
            }
        }

        return jsonObject;
    }

    /**
     * Converts {@link Company} into partial {@link JsonObject}
     *
     * @param company An entity that holds the attributes of company details
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject convertPartialData(final Company company) {
        final JsonObject companyJson = Json.object();

        if (Objects.nonNull(company)) {
            final Long id = company.id();
            final DisplayPicture thumbnail = company.getThumbnail();

            companyJson.put(CompanyFields.ID, company.id());
            companyJson.put(CompanyFields.NAME, company.getName());
            companyJson.put(CompanyFields.WEBSITE, company.getWebsite());

            if (Objects.nonNull(thumbnail)) {
                companyJson.put(FieldKeys.THUMBNAIL.getName(), convertThumbnailToJson(id, Company.ENTITY_TYPE, thumbnail));
            }
        }

        return companyJson;
    }

    /**
     * <p>
     * Converts {@link Company} into {@link JsonObject}
     * </p>
     *
     * @param company A model that holds the attributes of company
     * @return {@link JsonObject}
     */
    public JsonObject convertDuplicateCompanyToJson(final Company company) {
        final JsonObject jsonObject = Json.object();
        final CrmUser salesOwner = company.getSalesOwner();
        final DisplayPicture thumbnail = company.getThumbnail();
        final Long id = company.id();

        jsonObject.put(CompanyFields.ID, id);
        jsonObject.put(CompanyFields.NAME, company.getName());
        jsonObject.put(CompanyFields.SALES_OWNER, crmUserConverter.convert(salesOwner, true, true));
        jsonObject.put(CompanyFields.CREATED_ON, dateTimeUtil.getTimeStamp(company.getCreatedTime()));

        if (Objects.nonNull(thumbnail)) {
            jsonObject.put(CompanyFields.THUMBNAIL, convertThumbnailToJson(id, Company.ENTITY_TYPE, thumbnail));
        }

        return jsonObject;
    }

    /**
     * {@inheritDoc}
     *
     * @param company         Holds the data of specific entity
     * @param converterConfig Represents the {@link ConverterConfig}
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject summary(final Company company, final ConverterConfig converterConfig) {
        final JsonObject json = Json.object();
        final JsonObject profileJson = convertProfileToJsonObject(company, converterConfig);
        final DisplayPicture thumbnail = company.getThumbnail();

        if (Objects.nonNull(thumbnail)) {
            profileJson.put(CompanyFields.THUMBNAIL, convertThumbnailToJson(company.id(), Company.ENTITY_TYPE, thumbnail));
        }
        json.put(CompanyFields.ID, company.id());
        json.put(CompanyFields.PROFILE, profileJson);
        json.put(CompanyFields.SUMMARY, convertSummaryToJsonArray(company, converterConfig));
        convertSubFieldToJson(company, converterConfig, json);

        if (converterConfig.isAccessConfigRequired()) {
            json.put(FieldKeys.IS_EDITABLE.getName(), company.isEditable());
            json.put(FieldKeys.IS_DELETABLE.getName(), company.isDeletable());
        }

        return json;
    }

    /**
     * {@inheritDoc}
     *
     * @param company         Represents the {@link Company}
     * @param converterConfig Represents the {@link ConverterConfig}
     * @return The {@link JsonObject} containing company summary details
     */
    public JsonObject summaryV2(final Long orgId, final Long userId, final Company company, final ConverterConfig converterConfig) {
        final JsonObject companyJson = Json.object();

        companyJson.put(CompanyFields.ID, company.id());
        loadSummaryFieldPreference(orgId, userId, Source.COMPANY.getId(), companyJson);
        loadSummaryFields(company, companyJson, converterConfig);
        convertSubFieldToJson(company, converterConfig, companyJson);

        if (converterConfig.isAccessConfigRequired()) {
            companyJson.put(FieldKeys.IS_EDITABLE.getName(), company.isEditable());
            companyJson.put(FieldKeys.IS_DELETABLE.getName(), company.isDeletable());
        }
        return companyJson;
    }

    /**
     * {@inheritDoc}
     *
     * @param company         Represents the {@link Company}
     * @param converterConfig Represents the {@link ConverterConfig}
     * @return The {@link JsonObject} containing company profile details
     */
    @Override
    public JsonObject profile(final Company company, final ConverterConfig converterConfig) {
        final JsonObject profileJson = convertProfileToJsonObject(company, converterConfig);
        final DisplayPicture thumbnail = company.getThumbnail();
        final DisplayPicture original = company.getOriginalPicture();

        if (Objects.nonNull(thumbnail) && Objects.nonNull(original)) {
            profileJson.put(FieldKeys.PROFILE_PIC.getName(), convertProfilePictureToJson(company.id(), Company.ENTITY_TYPE, thumbnail, original));
        }
        profileJson.put(CompanyFields.ID, company.id());

        if (converterConfig.isAccessConfigRequired()) {
            profileJson.put(FieldKeys.IS_EDITABLE.getName(), company.isEditable());
            profileJson.put(FieldKeys.IS_DELETABLE.getName(), company.isDeletable());
        }
        return profileJson;
    }

    /**
     * <p>
     * Converts the fields into json object
     * </p>
     *
     * @param fieldInfo Represents a data carrier that holds field details
     * @param company   A model that represents company
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject convertFieldInfoToJsonObject(final FieldInfo fieldInfo, final Company company, final ConverterConfig converterConfig) {
        final JsonObject jsonObject = Json.object();

        this.dealConverter = Objects.isNull(dealConverter) ? DealConverter.getInstance() : dealConverter;

        if (fieldInfo.isPersistent()) {
            setValuesToJson(company.getValueHolder().get(fieldInfo.id()), jsonObject, fieldInfo);
        } else {
            switch (fieldInfo.aliasName()) {
                case "annualRevenue" -> {
                    final Monetary annualRevenue = company.getAnnualRevenue();

                    if (Objects.nonNull(annualRevenue)) {
                        jsonObject.put(FieldKeys.VALUE.getName(), convertSubTypesToJson(StoreType.MONETARY, List.of(annualRevenue)));
                    }
                }
                case "industryType" -> {
                    final Option industryType = company.getIndustryType();

                    if (Objects.nonNull(industryType)) {
                        jsonObject.put(FieldKeys.VALUE.getName(), convertOptionToJson(industryType, converterConfig));
                    }
                }
                case "employeeCount" -> jsonObject.put(FieldKeys.VALUE.getName(), company.getEmployeeCount());
                case "parentCompany" -> {
                    final Company parentCompany = company.getParentCompany();

                    if (Objects.nonNull(parentCompany)) {
                        jsonObject.put(FieldKeys.VALUE.getName(), convertPartialData(company.getParentCompany()));
                    }
                }
                case "address" -> {
                    final Address address = company.getAddress();

                    if (Objects.nonNull(address)) {
                        jsonObject.put(FieldKeys.VALUE.getName(), convertSubTypesToJson(StoreType.ADDRESS, List.of(address)));
                    }
                }
                case "name" -> jsonObject.put(FieldKeys.VALUE.getName(), company.getName());
                case "website" -> jsonObject.put(FieldKeys.VALUE.getName(), company.getWebsite());
                case "facebook" -> jsonObject.put(FieldKeys.VALUE.getName(), company.getFacebook());
                case "twitter" -> jsonObject.put(FieldKeys.VALUE.getName(), company.getTwitter());
                case "linkedIn" -> jsonObject.put(FieldKeys.VALUE.getName(), company.getLinkedIn());
                case "organizationStatus" -> {
                    final Option organizationStatus = company.getOrganizationStatus();

                    if (Objects.nonNull(organizationStatus)) {
                        jsonObject.put(FieldKeys.VALUE.getName(), convertOptionToJson(organizationStatus, converterConfig));
                    }
                }
                case "sicCode" -> jsonObject.put(FieldKeys.VALUE.getName(), company.getSicCode());
                case "businessType" -> {
                    final Option businessType = company.getBusinessType();

                    if (Objects.nonNull(businessType)) {
                        jsonObject.put(FieldKeys.VALUE.getName(), convertOptionToJson(businessType, converterConfig));
                    }
                }
                case "salesOwner" -> {
                    final CrmUser salesOwner = company.getSalesOwner();

                    if (Objects.nonNull(salesOwner)) {
                        jsonObject.put(FieldKeys.VALUE.getName(), crmUserConverter.convert(salesOwner, true, true));
                    }
                }
                case "createdBy" -> {
                    final CrmUser createdBy = company.getCreatedBy();

                    if (Objects.nonNull(createdBy)) {
                        jsonObject.put(FieldKeys.VALUE.getName(), crmUserConverter.convert(createdBy, true, true));
                    }
                }
                case "updatedBy" -> {
                    final CrmUser updatedBy = company.getUpdatedBy();

                    if (Objects.nonNull(updatedBy)) {
                        jsonObject.put(FieldKeys.VALUE.getName(), crmUserConverter.convert(updatedBy, true, true));
                    }
                }
                case "territory" -> {
                    final Territory territory = company.getTerritory();

                    if (Objects.nonNull(territory)) {
                        jsonObject.put(FieldKeys.VALUE.getName(), territoryConverter.convertTerritoryToJson(territory));
                    }
                }
                case "tags" -> {
                    final Collection<Tag> tags = company.getSummaryInfo().getTags();

                    if (Objects.nonNull(tags) && !tags.isEmpty()) {
                        jsonObject.put(FieldKeys.VALUE.getName(), tagConverter.convertTagsForSummary(tags));
                    }
                }
                case "id" -> jsonObject.put(FieldKeys.VALUE.getName(), company.id());
                case "emailMessageCount" -> {
                    final EmailSummary emailSummary = company.getSummaryInfo().getEmailSummary();

                    if (Objects.nonNull(emailSummary)) {
                        final Long emailMessageCount = emailSummary.getEmailMessageCount();

                        if (Objects.nonNull(emailMessageCount) && emailMessageCount > 0) {
                            jsonObject.put(FieldKeys.VALUE.getName(), emailMessageCount);
                        }
                    }
                }
                case "lastEmailSent" -> {
                    final EmailSummary emailSummary = company.getSummaryInfo().getEmailSummary();

                    if (Objects.nonNull(emailSummary)) {
                        final Date lastEmailSent = emailSummary.getLastEmailSent();

                        if (Objects.nonNull(lastEmailSent)) {
                            jsonObject.put(FieldKeys.VALUE.getName(), lastEmailSent.getTimeStamp());
                        }
                    }
                }
                case "lastEmailReceived" -> {
                    final EmailSummary emailSummary = company.getSummaryInfo().getEmailSummary();

                    if (Objects.nonNull(emailSummary)) {
                        final Date lastEmailReceived = emailSummary.getLastEmailReceived();

                        if (Objects.nonNull(lastEmailReceived)) {
                            jsonObject.put(FieldKeys.VALUE.getName(), lastEmailReceived.getTimeStamp());
                        }
                    }
                }
                case "description" -> jsonObject.put(FieldKeys.VALUE.getName(), company.getDescription());
                case "createdAt" -> jsonObject.put(FieldKeys.VALUE.getName(), dateTimeUtil.getTimeStamp(company.getCreatedTime()));
                case "updatedAt" -> jsonObject.put(FieldKeys.VALUE.getName(), dateTimeUtil.getTimeStamp(company.getModifiedTime()));
                case "lastAssignedAt" -> jsonObject.put(FieldKeys.VALUE.getName(), dateTimeUtil.getTimeStamp(company.getLastAssignedAt()));
                case "deals" -> dealConverter.loadSummaryInfoToJson(company.getSummaryInfo(), Deal.State.OPEN.getName(), jsonObject);
                case "wonDeals" -> dealConverter.loadSummaryInfoToJson(company.getSummaryInfo(), Deal.State.WON.getName(), jsonObject);
                case "lostDeals" -> dealConverter.loadSummaryInfoToJson(company.getSummaryInfo(), Deal.State.LOST.getName(), jsonObject);
                case "closedDeals" -> dealConverter.loadSummaryInfoToJson(company.getSummaryInfo(), DealFields.CLOSED, jsonObject);
                case "lastActivityDate" -> jsonObject.put(FieldKeys.VALUE.getName(), dateUtil.getTimeStamp(company.getSummaryInfo().getActivitySummary().getLastActivityDate()));
                case "nextActivityDate" -> jsonObject.put(FieldKeys.VALUE.getName(), dateUtil.getTimeStamp(company.getSummaryInfo().getActivitySummary().getNextActivityDate()));
                case "totalActivities" -> {
                    final long totalActivities = company.getSummaryInfo().getActivitySummary().getTotalActivities();

                    if (totalActivities > 0) {
                        jsonObject.put(FieldKeys.VALUE.getName(), totalActivities);
                    }
                }
                case "upcomingActivities" -> {
                    final long upcomingActivities = company.getSummaryInfo().getActivitySummary().getUpcomingActivities();

                    if (upcomingActivities > 0) {
                        jsonObject.put(FieldKeys.VALUE.getName(), upcomingActivities);
                    }
                }
                case "doneActivities" -> {
                    final long doneActivities = company.getSummaryInfo().getActivitySummary().getDoneActivities();

                    if (doneActivities > 0) {
                        jsonObject.put(FieldKeys.VALUE.getName(), doneActivities);
                    }
                }
                case "lastActivityType" -> {
                    final String lastActivityType = company.getSummaryInfo().getActivitySummary().getLastActivityType();

                    if (Objects.nonNull(lastActivityType)) {
                        jsonObject.put(FieldKeys.VALUE.getName(), lastActivityType);
                    }
                }
                case "recentNote" -> jsonObject.put(FieldKeys.VALUE.getName(), company.getRecentNote());
            }
        }
        loadFieldsIntoJson(jsonObject, fieldInfo, converterConfig);

        return jsonObject;
    }

    /**
     * <p>
     * Converts the {@link Company} to {@link JsonObject}
     * </p>
     *
     * @param company Represents the {@link Company}
     * @return the {@link JsonObject}
     */
    public JsonObject convertCompanyWithThumbnail(final Company company) {
        final JsonObject companyJson = Json.object();

        if (Objects.nonNull(company)) {
            final DisplayPicture thumbnail = company.getThumbnail();
            final Long id = company.id();

            companyJson.put(CompanyFields.ID, id);
            companyJson.put(CompanyFields.NAME, company.getName());
            companyJson.put(CompanyFields.WEBSITE, company.getWebsite());

            if (Objects.nonNull(thumbnail)) {
                companyJson.put(CompanyFields.THUMBNAIL, convertThumbnailToJson(id, Company.ENTITY_TYPE, thumbnail));
            }
        }

        return companyJson;
    }

    /**
     * <p>
     * Converts the {@link Company} to a compact {@link JsonObject} containing
     * the company name, ID, and formatted address.
     * </p>
     *
     * @param includeFormattedAddress Represents whether to include formatted address or not.
     * @param company                 Represents the {@link Company}
     * @return the compact {@link JsonObject}
     */
    public JsonObject convertCompanyWithAddress(final boolean includeFormattedAddress, final Company company) {
        final JsonObject companyJson = Json.object();
        final Address address = company.getAddress();

        companyJson.put(CompanyFields.NAME, company.getName());
        companyJson.put(CompanyFields.VALUE, company.id());

        if (Objects.nonNull(address)) {

            if (includeFormattedAddress) {
                final String formattedAddress = Stream.of(address.getAddressLine1(), address.getAddressLine2(),
                                address.getCity(), address.getState(), address.getZipcode())
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(companyAddress -> !companyAddress.isEmpty())
                        .collect(Collectors.joining(", "));

                if (!formattedAddress.isEmpty()) {
                    companyJson.put(CompanyFields.ADDRESS, formattedAddress);
                }
            } else {
                companyJson.put(CompanyFields.ADDRESS, addressConverter.convert(new ConverterConfig(false, false),
                        company.getAddress()));
            }
        }

        return companyJson;
    }

    /**
     * Converts the primary company to {@link JsonObject} for merge preview/get APIs.
     *
     * @param company The primary {@link Company} entity
     * @return {@link JsonObject} representation of the company
     */
    public JsonObject convertPrimaryCompanyToJson(final Company company) {
        final JsonObject jsonObject = Json.object();
        final ValueHolder valueHolder = company.getValueHolder();
        final Option businessType = company.getBusinessType();
        final Option industryType = company.getIndustryType();
        final Option organizationStatus = company.getOrganizationStatus();
        final Address address = company.getAddress();
        final Monetary annualRevenue = company.getAnnualRevenue();
        final CrmUser createdBy = company.getCreatedBy();
        final CrmUser updatedBy = company.getUpdatedBy();
        final Territory territory = company.getTerritory();
        final Company parentCompany = company.getParentCompany();
        final SummaryInfo summaryInfo = company.getSummaryInfo();
        final CrmUser salesOwner = company.getSalesOwner();
        final EmailSummary emailSummary = Objects.nonNull(summaryInfo) ? summaryInfo.getEmailSummary() : null;
        final ActivitySummary activitySummary = summaryInfo.getActivitySummary();

        jsonObject.put(CompanyFields.ID, company.id());
        jsonObject.put(CompanyFields.ORG_ID, company.orgId());
        jsonObject.put(CompanyFields.NAME, company.getName());
        jsonObject.put(CompanyFields.EMPLOYEE_COUNT, company.getEmployeeCount());
        jsonObject.put(CompanyFields.FACEBOOK, company.getFacebook());
        jsonObject.put(CompanyFields.TWITTER, company.getTwitter());
        jsonObject.put(CompanyFields.LINKEDIN, company.getLinkedIn());
        jsonObject.put(CompanyFields.SIC_CODE, company.getSicCode());
        jsonObject.put(CompanyFields.WEBSITE, company.getWebsite());
        jsonObject.put(CompanyFields.DESCRIPTION, company.getDescription());
        jsonObject.put(CompanyFields.CREATED_AT, dateTimeUtil.getTimeStamp(company.getCreatedTime()));
        jsonObject.put(CompanyFields.UPDATED_AT, dateTimeUtil.getTimeStamp(company.getModifiedTime()));
        jsonObject.put(CompanyFields.LAST_ASSIGNED_AT, dateTimeUtil.getTimeStamp(company.getLastAssignedAt()));
        jsonObject.put(CompanyFields.CREATED_BY, createdBy.getFullName());
        jsonObject.put(CompanyFields.UPDATED_BY, updatedBy.getFullName());
        jsonObject.put(CompanyFields.SALES_OWNER, salesOwner.getFullName());
        jsonObject.put(CompanyFields.RECENT_NOTE, company.getRecentNote());

        if (Objects.nonNull(address)) {
            jsonObject.put(CompanyFields.ADDRESS, addressConverter.convert(address));
        }

        if (Objects.nonNull(parentCompany)) {
            final JsonObject parentJson = Json.object();

            parentJson.put(CompanyFields.ID, parentCompany.id());
            parentJson.put(CompanyFields.NAME, parentCompany.getName());
            jsonObject.put(CompanyFields.PARENT_COMPANY, parentJson);
        }

        if (Objects.nonNull(annualRevenue)) {
            jsonObject.put(CompanyFields.ANNUAL_REVENUE, String.format("%s %s", annualRevenue.getCurrency().getSymbol(), annualRevenue.getValue()));
        }

        if (Objects.nonNull(businessType)) {
            jsonObject.put(CompanyFields.BUSINESS_TYPE, businessType.getOption());
        }

        if (Objects.nonNull(industryType)) {
            jsonObject.put(CompanyFields.INDUSTRY_TYPE, industryType.getOption());
        }

        if (Objects.nonNull(organizationStatus)) {
            jsonObject.put(CompanyFields.ORGANIZATION_STATUS, organizationStatus.getOption());
        }

        if (Objects.nonNull(territory)) {
            jsonObject.put(CompanyFields.TERRITORY, territory.getName());
        }

        final JsonArray tagsJsonArray = Json.array();

        if (Objects.nonNull(summaryInfo)) {
            final Collection<Tag> tags = summaryInfo.getTags();

            if (Objects.nonNull(tags)) {
                for (final Tag tag : tags) {
                    final JsonObject tagJson = Json.object();

                    tagJson.put(DealFields.ID, tag.id());
                    tagJson.put(DealFields.TAG, tag.getName());
                    tagsJsonArray.put(tagJson);
                }
            }

            if (Objects.nonNull(activitySummary)) {
                jsonObject.put(CompanyFields.NEXT_ACTIVITY_DATE, dateUtil.getTimeStamp(activitySummary.getNextActivityDate()));
                jsonObject.put(CompanyFields.LAST_ACTIVITY_DATE, dateUtil.getTimeStamp(activitySummary.getLastActivityDate()));
                jsonObject.put(CompanyFields.TOTAL_ACTIVITIES, activitySummary.getTotalActivities());
                jsonObject.put(CompanyFields.LAST_ACTIVITY_TYPE, activitySummary.getLastActivityType());
                jsonObject.put(CompanyFields.UPCOMING_ACTIVITIES, activitySummary.getUpcomingActivities());
                jsonObject.put(CompanyFields.DONE_ACTIVITIES, activitySummary.getDoneActivities());
            }

            this.dealConverter = Objects.isNull(dealConverter) ? DealConverter.getInstance() : dealConverter;
            dealConverter.loadSummaryInfoToJson(summaryInfo, Deal.State.OPEN.getName(), jsonObject);
            dealConverter.loadSummaryInfoToJson(summaryInfo, Deal.State.WON.getName(), jsonObject);
            dealConverter.loadSummaryInfoToJson(summaryInfo, Deal.State.LOST.getName(), jsonObject);
            dealConverter.loadSummaryInfoToJson(summaryInfo, DealFields.CLOSED, jsonObject);
        }
        jsonObject.put(CompanyFields.TAGS, tagsJsonArray);

        if (Objects.nonNull(emailSummary)) {
            loadEmailSummaryFields(jsonObject,
                    DealFields.LAST_EMAIL_SENT, dateUtil.getTimeStamp(emailSummary.getLastEmailSent()),
                    DealFields.EMAIL_MESSAGE_COUNT, emailSummary.getEmailMessageCount(),
                    DealFields.LAST_EMAIL_RECEIVED, dateUtil.getTimeStamp(emailSummary.getLastEmailReceived()));
        }

        loadValueHolderToJson(jsonObject, valueHolder);
        loadPersistentFieldValues(company.orgId(), Company.ENTITY_TYPE, jsonObject);

        return jsonObject;
    }

    /**
     * Maps COMPANY and CONTACT custom-field references for company merge/preview responses.
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
     * <p>
     * Sets the options for the specific field of an entity
     * </p>
     *
     * @param orgId               The organization ID to which the entity belongs
     * @param jsonObject          Represents {@link JsonObject}
     * @param fieldKey            The key in the {@link JsonObject} representing the field to set
     * @param fieldOptionConsumer A {@link Consumer} that accepts an {@link Option} to set the value for the field.
     * @param fieldId             The ID of the field for which the option is being set
     */
    private void setCompanyOptions(final long orgId, final JsonObject jsonObject, final String fieldKey,
                                   final Consumer<Option> fieldOptionConsumer, final Long fieldId) {
        setOptions(orgId, jsonObject, fieldKey, fieldOptionConsumer, fieldId, true);
    }

    /**
     * <p>
     * Converts {@link Company} into {@link JsonObject}
     * </p>
     *
     * @param company         The {@link Company} entity containing the company-related information to convert.
     * @param converterConfig The {@link ConverterConfig} providing configuration settings.
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject convertModelToJson(final Company company, final ConverterConfig converterConfig) {
        final Map<String, FieldInfo> fields = secureFieldService.groupDefaultFieldsByAliasName(Company.ENTITY_TYPE);
        final JsonObject jsonObject = Json.object();
        final ValueHolder valueHolder = company.getValueHolder();
        final Option businessType = company.getBusinessType();
        final Option industryType = company.getIndustryType();
        final Option organisationStatus = company.getOrganizationStatus();
        final Address address = company.getAddress();
        final Monetary annualRevenue = company.getAnnualRevenue();
        final CrmUser createdBy = company.getCreatedBy();
        final CrmUser updatedBy = company.getUpdatedBy();
        final Territory territory = company.getTerritory();
        final Company parentCompany = company.getParentCompany();
        final SummaryInfo summaryInfo = company.getSummaryInfo();
        final CrmUser salesOwner = company.getSalesOwner();

        final String employeeCountAlias = CompanyFields.EMPLOYEE_COUNT;
        final String facebookAlias = CompanyFields.FACEBOOK;
        final String linkedinAlias = CompanyFields.LINKEDIN;
        final String twitterAlias = CompanyFields.TWITTER;
        final String sicCodeAlias = CompanyFields.SIC_CODE;
        final String websiteAlias = CompanyFields.WEBSITE;
        final String descriptionAlias = CompanyFields.DESCRIPTION;
        final String updatedAtAlias = CompanyFields.UPDATED_AT;
        final String createdAtAlias = CompanyFields.CREATED_AT;
        final String lastAssignedAtAlias = CompanyFields.LAST_ASSIGNED_AT;
        final String addressAlias = CompanyFields.ADDRESS;
        final String parentCompanyAlias = CompanyFields.PARENT_COMPANY;
        final String annualRevenueAlias = CompanyFields.ANNUAL_REVENUE;
        final String businessTypeAlias = CompanyFields.BUSINESS_TYPE;
        final String industryTypeAlias = CompanyFields.INDUSTRY_TYPE;
        final String organizationStatusAlias = CompanyFields.ORGANIZATION_STATUS;
        final String createdByAlias = CompanyFields.CREATED_BY;
        final String updatedByAlias = CompanyFields.UPDATED_BY;
        final String territoryAlias = CompanyFields.TERRITORY;
        final String salesOwnerAlias = CompanyFields.SALES_OWNER;

        jsonObject.put(CompanyFields.ID, company.id());
        jsonObject.put(CompanyFields.ORG_ID, company.orgId());
        jsonObject.put(fields.get(CompanyFields.NAME).idAsString(), company.getName());

        if (fields.containsKey(employeeCountAlias)) {
            jsonObject.put(fields.get(employeeCountAlias).idAsString(), company.getEmployeeCount());
        }

        if (fields.containsKey(facebookAlias)) {
            jsonObject.put(fields.get(facebookAlias).idAsString(), company.getFacebook());
        }

        if (fields.containsKey(linkedinAlias)) {
            jsonObject.put(fields.get(linkedinAlias).idAsString(), company.getLinkedIn());
        }

        if (fields.containsKey(twitterAlias)) {
            jsonObject.put(fields.get(twitterAlias).idAsString(), company.getTwitter());
        }

        if (fields.containsKey(sicCodeAlias)) {
            jsonObject.put(fields.get(sicCodeAlias).idAsString(), company.getSicCode());
        }

        if (fields.containsKey(websiteAlias)) {
            jsonObject.put(fields.get(websiteAlias).idAsString(), company.getWebsite());
        }

        if (fields.containsKey(descriptionAlias)) {
            jsonObject.put(fields.get(descriptionAlias).idAsString(), company.getDescription());
        }

        if (fields.containsKey(updatedAtAlias)) {
            jsonObject.put(fields.get(updatedAtAlias).idAsString(), dateTimeUtil.getTimeStamp(company.getModifiedTime()));
        }

        if (fields.containsKey(createdAtAlias)) {
            jsonObject.put(fields.get(createdAtAlias).idAsString(), dateTimeUtil.getTimeStamp(company.getCreatedTime()));
        }

        if (fields.containsKey(lastAssignedAtAlias)) {
            jsonObject.put(fields.get(lastAssignedAtAlias).idAsString(), dateTimeUtil.getTimeStamp(company.getLastAssignedAt()));
        }

        if (Objects.nonNull(address) && fields.containsKey(addressAlias)) {
            jsonObject.put(fields.get(addressAlias).idAsString(), addressConverter.convertModelToJson(address, converterConfig));
        }

        if (Objects.nonNull(parentCompany) && fields.containsKey(parentCompanyAlias)) {
            jsonObject.put(fields.get(parentCompanyAlias).idAsString(), convertModelToOptionJson(parentCompany));
        }

        if (Objects.nonNull(annualRevenue) && fields.containsKey(annualRevenueAlias)) {
            jsonObject.put(fields.get(annualRevenueAlias).idAsString(), monetaryConverter.convertModelToJson(annualRevenue, converterConfig));
        }

        if (Objects.nonNull(businessType) && fields.containsKey(businessTypeAlias)) {
            jsonObject.put(fields.get(businessTypeAlias).idAsString(), optionConverter.convertModelToOptionJson(businessType, converterConfig));
        }

        if (Objects.nonNull(industryType) && fields.containsKey(industryTypeAlias)) {
            jsonObject.put(fields.get(industryTypeAlias).idAsString(), optionConverter.convertModelToOptionJson(industryType, converterConfig));
        }

        if (Objects.nonNull(organisationStatus) && fields.containsKey(organizationStatusAlias)) {
            jsonObject.put(fields.get(organizationStatusAlias).idAsString(), optionConverter.convertModelToOptionJson(organisationStatus, converterConfig));
        }

        if (Objects.nonNull(valueHolder)) {
            valueHolder.mapToJson(jsonObject, converterConfig);
        }

        if (Objects.nonNull(createdBy) && fields.containsKey(createdByAlias)) {
            jsonObject.put(fields.get(createdByAlias).idAsString(), createdBy.id());
        }

        if (Objects.nonNull(updatedBy) && fields.containsKey(updatedByAlias)) {
            jsonObject.put(fields.get(updatedByAlias).idAsString(), updatedBy.id());
        }

        if (Objects.nonNull(territory) && fields.containsKey(territoryAlias)) {
            jsonObject.put(fields.get(territoryAlias).idAsString(), territoryConverter.convertModelToOptionJson(territory));
        }

        if (Objects.nonNull(summaryInfo)) {
            final Collection<Tag> tags = summaryInfo.getTags();
            final String tagsAlias = CompanyFields.TAGS;

            if (Objects.nonNull(tags) && !tags.isEmpty() && fields.containsKey(tagsAlias)) {
                jsonObject.put(fields.get(tagsAlias).idAsString(), tagConverter.convertModelToJson(tags));
            }
        }

        if (Objects.nonNull(salesOwner) && fields.containsKey(salesOwnerAlias)) {
            jsonObject.put(fields.get(salesOwnerAlias).idAsString(), crmUserConverter.convertModelToOptionJson(salesOwner));
        }

        return jsonObject;
    }

    /**
     * {@inheritDoc}
     *
     * @param company         The {@link Company} entity containing the company-related information to convert.
     * @param converterConfig The {@link ConverterConfig} providing configuration settings.
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject convertToRestJson(final Company company, final ConverterConfig converterConfig) {
        final Map<String, FieldInfo> fields = secureFieldService.groupDefaultFieldsByAliasName(Company.ENTITY_TYPE);
        final JsonObject jsonObject = Json.object();
        final ValueHolder valueHolder = company.getValueHolder();
        final Option businessType = company.getBusinessType();
        final Option industryType = company.getIndustryType();
        final Option organisationStatus = company.getOrganizationStatus();
        final Address address = company.getAddress();
        final Monetary annualRevenue = company.getAnnualRevenue();
        final CrmUser createdBy = company.getCreatedBy();
        final CrmUser updatedBy = company.getUpdatedBy();
        final Territory territory = company.getTerritory();
        final Company parentCompany = company.getParentCompany();
        final SummaryInfo summaryInfo = company.getSummaryInfo();
        final CrmUser salesOwner = company.getSalesOwner();

        final String employeeCountAlias = CompanyFields.EMPLOYEE_COUNT;
        final String facebookAlias = CompanyFields.FACEBOOK;
        final String linkedinAlias = CompanyFields.LINKEDIN;
        final String twitterAlias = CompanyFields.TWITTER;
        final String sicCodeAlias = CompanyFields.SIC_CODE;
        final String websiteAlias = CompanyFields.WEBSITE;
        final String descriptionAlias = CompanyFields.DESCRIPTION;
        final String updatedAtAlias = CompanyFields.UPDATED_AT;
        final String createdAtAlias = CompanyFields.CREATED_AT;
        final String lastAssignedAtAlias = CompanyFields.LAST_ASSIGNED_AT;
        final String addressAlias = CompanyFields.ADDRESS;
        final String parentCompanyAlias = CompanyFields.PARENT_COMPANY;
        final String annualRevenueAlias = CompanyFields.ANNUAL_REVENUE;
        final String businessTypeAlias = CompanyFields.BUSINESS_TYPE;
        final String industryTypeAlias = CompanyFields.INDUSTRY_TYPE;
        final String organizationStatusAlias = CompanyFields.ORGANIZATION_STATUS;
        final String createdByAlias = CompanyFields.CREATED_BY;
        final String updatedByAlias = CompanyFields.UPDATED_BY;
        final String territoryAlias = CompanyFields.TERRITORY;
        final String salesOwnerAlias = CompanyFields.SALES_OWNER;

        jsonObject.put(CompanyFields.ID, company.id());
        jsonObject.put(fields.get(CompanyFields.NAME).getInternalName(), company.getName());

        if (fields.containsKey(employeeCountAlias)) {
            jsonObject.put(fields.get(employeeCountAlias).getInternalName(), company.getEmployeeCount());
        }

        if (fields.containsKey(facebookAlias)) {
            jsonObject.put(fields.get(facebookAlias).getInternalName(), company.getFacebook());
        }

        if (fields.containsKey(linkedinAlias)) {
            jsonObject.put(fields.get(linkedinAlias).getInternalName(), company.getLinkedIn());
        }

        if (fields.containsKey(twitterAlias)) {
            jsonObject.put(fields.get(twitterAlias).getInternalName(), company.getTwitter());
        }

        if (fields.containsKey(sicCodeAlias)) {
            jsonObject.put(fields.get(sicCodeAlias).getInternalName(), company.getSicCode());
        }

        if (fields.containsKey(websiteAlias)) {
            jsonObject.put(fields.get(websiteAlias).getInternalName(), company.getWebsite());
        }

        if (fields.containsKey(descriptionAlias)) {
            jsonObject.put(fields.get(descriptionAlias).getInternalName(), company.getDescription());
        }

        if (fields.containsKey(updatedAtAlias)) {
            jsonObject.put(fields.get(updatedAtAlias).getInternalName(), dateTimeUtil.getTimeStamp(company.getModifiedTime()));
        }

        if (fields.containsKey(createdAtAlias)) {
            jsonObject.put(fields.get(createdAtAlias).getInternalName(), dateTimeUtil.getTimeStamp(company.getCreatedTime()));
        }

        if (fields.containsKey(lastAssignedAtAlias)) {
            jsonObject.put(fields.get(lastAssignedAtAlias).getInternalName(), dateTimeUtil.getTimeStamp(company.getLastAssignedAt()));
        }

        if (Objects.nonNull(address) && fields.containsKey(addressAlias)) {
            jsonObject.put(fields.get(addressAlias).getInternalName(), addressConverter.convertToRestJson(address, converterConfig));
        }

        if (Objects.nonNull(parentCompany) && fields.containsKey(parentCompanyAlias)) {
            jsonObject.put(fields.get(parentCompanyAlias).getInternalName(), convertModelToOptionJson(parentCompany));
        }

        if (Objects.nonNull(annualRevenue) && fields.containsKey(annualRevenueAlias)) {
            jsonObject.put(fields.get(annualRevenueAlias).getInternalName(), monetaryConverter.convertToRestJson(annualRevenue, converterConfig));
        }

        if (Objects.nonNull(businessType) && fields.containsKey(businessTypeAlias)) {
            jsonObject.put(fields.get(businessTypeAlias).getInternalName(), optionConverter.convertModelToOptionJson(businessType, converterConfig));
        }

        if (Objects.nonNull(industryType) && fields.containsKey(industryTypeAlias)) {
            jsonObject.put(fields.get(industryTypeAlias).getInternalName(), optionConverter.convertModelToOptionJson(industryType, converterConfig));
        }

        if (Objects.nonNull(organisationStatus) && fields.containsKey(organizationStatusAlias)) {
            jsonObject.put(fields.get(organizationStatusAlias).getInternalName(), optionConverter.convertModelToOptionJson(organisationStatus, converterConfig));
        }

        if (Objects.nonNull(valueHolder)) {
            valueHolder.mapToJson(jsonObject, converterConfig, true);
        }

        if (Objects.nonNull(createdBy) && fields.containsKey(createdByAlias)) {
            jsonObject.put(fields.get(createdByAlias).getInternalName(), createdBy.id());
        }

        if (Objects.nonNull(updatedBy) && fields.containsKey(updatedByAlias)) {
            jsonObject.put(fields.get(updatedByAlias).getInternalName(), updatedBy.id());
        }

        if (Objects.nonNull(territory) && fields.containsKey(territoryAlias)) {
            jsonObject.put(fields.get(territoryAlias).getInternalName(), territoryConverter.convertModelToOptionJson(territory));
        }

        if (Objects.nonNull(summaryInfo)) {
            final Collection<Tag> tags = summaryInfo.getTags();
            final String tagsAlias = CompanyFields.TAGS;

            if (Objects.nonNull(tags) && !tags.isEmpty() && fields.containsKey(tagsAlias)) {
                jsonObject.put(fields.get(tagsAlias).getInternalName(), tagConverter.convertModelToJson(tags));
            }
        }

        if (Objects.nonNull(salesOwner) && fields.containsKey(salesOwnerAlias)) {
            jsonObject.put(fields.get(salesOwnerAlias).getInternalName(), crmUserConverter.convertModelToOptionJson(salesOwner));
        }

        return jsonObject;
    }

    /**
     * Converts {@link Company} into {@link JsonObject}
     *
     * @param company The {@link Company} entity containing basic company information.
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject convertModelToOptionJson(final Company company) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(CompanyFields.VALUE, company.id());
        jsonObject.put(CompanyFields.NAME, company.getName());

        return jsonObject;
    }

    /**
     * {@inheritDoc}
     *
     * @param company         Holds the data of specific entity
     * @param converterConfig Represents the {@link ConverterConfig}
     * @param isHidden        A boolean flag indicating whether to display empty fields or not
     * @return {@link JsonObject}
     */
    public JsonObject convertSummaryIntoJson(final Company company, final ConverterConfig converterConfig, final boolean isHidden) {
        final JsonObject json = Json.object();
        final JsonObject profileJson = convertProfileToJson(company, converterConfig);
        final DisplayPicture thumbnail = company.getThumbnail();
        final Long companyId = company.id();

        if (Objects.nonNull(thumbnail)) {
            profileJson.put(CompanyFields.THUMBNAIL, convertThumbnailToJson(companyId, Company.ENTITY_TYPE, thumbnail));
        }
        json.put(CompanyFields.ID, companyId);
        json.put(CompanyFields.PROFILE, profileJson);
        json.put(CompanyFields.SUMMARY, convertSummaryToJsonArray(company, isHidden, converterConfig));
        convertSubFieldToJson(company, converterConfig, json);

        if (converterConfig.isAccessConfigRequired()) {
            json.put(FieldKeys.IS_EDITABLE.getName(), company.isEditable());
            json.put(FieldKeys.IS_DELETABLE.getName(), company.isDeletable());
        }

        return json;
    }

    /**
     * <p>
     * Converts the social profile for the given company
     * </p>
     *
     * @param company Represent the {@link Company}
     * @return {@link JsonObject}
     */
    public JsonObject convertSocialProfile(final Company company, final ConverterConfig converterConfig) {
        final JsonObject jsonObject = Json.object();
        final Map<String, FieldInfo> fieldsMap = getSocialProfileFields(company);

        for (final Map.Entry<String, FieldInfo> fieldEntry : fieldsMap.entrySet()) {
            jsonObject.put(fieldEntry.getKey(), convertFieldInfoToJsonObject(fieldEntry.getValue(), company, converterConfig));
        }
        return jsonObject;
    }

    /**
     * <p>
     * Gets the social profile fields
     * </p>
     *
     * @param company Represent the {@link Company}
     * @return All the {@link FieldInfo} of the social profile fields
     */
    private Map<String, FieldInfo> getSocialProfileFields(final Company company) {
        return secureFieldService.groupDefaultFieldsByAliasName(company.orgId(), company.getEntityType(), List.of(CompanyFields.LINKEDIN,
                CompanyFields.TWITTER, CompanyFields.FACEBOOK));
    }
}