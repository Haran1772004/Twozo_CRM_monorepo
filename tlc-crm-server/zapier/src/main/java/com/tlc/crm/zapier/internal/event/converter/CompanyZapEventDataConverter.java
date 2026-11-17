package com.tlc.crm.zapier.internal.event.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.Territory;

import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.summaryInfo.model.SummaryInfo;

import com.tlc.crm.entity.features.api.tag.model.Tag;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.ValueHolder;
import com.tlc.crm.entity.models.type.Address;
import com.tlc.crm.entity.models.type.Monetary;
import com.tlc.crm.entity.models.type.Option;

import com.tlc.crm.fields.models.FieldSubType;
import com.tlc.crm.fields.models.StoreSubType;

import com.tlc.crm.zapier.internal.event.ZapEventData;

import com.tlc.i18n.I18nResolver;

import com.tlc.l10n.temporal.model.ISOTemporalFormat;
import com.tlc.l10n.temporal.model.TimezoneInfo;
import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.l10n.model.country.Country;

import com.tlc.iam.common.models.User;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Handles the conversion of model data into a JSON format suitable for Zapier event payloads.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CompanyZapEventDataConverter extends AbstractZapEventDataConverter<Company> {

    private static final class Instance {
        private static final CompanyZapEventDataConverter EVENT_DATA_CONVERTER = new CompanyZapEventDataConverter();
    }

    public static CompanyZapEventDataConverter getInstance() {
        return Instance.EVENT_DATA_CONVERTER;
    }

    /**
     * {@inheritDoc}
     *
     * @param company      The specific model instance to convert.
     * @param zapEventData The {@code ZapEventData}.
     * @return {@link JsonObject} representing the converted model data.
     */
    @Override
    protected JsonObject convert(final Company company, final ZapEventData zapEventData) {
        final Collection<FieldInfo> fields = fieldService.getFieldsBySource(company.orgId(), Source.COMPANY.getName());
        final JsonObject companyJson = Json.object();
        final I18nResolver i18nResolver = i18nService.getResolver(zapEventData.getLocale());
        final Map<Long, User> userMap = zapEventData.getGroupedUsers();
        final ValueHolder valueHolder = company.getValueHolder();

        for (final FieldInfo fieldInfo : fields) {

            if (fieldInfo.isPersistent()) {
                mapPersistentFieldToJson(fieldInfo, valueHolder.get(fieldInfo.id()), companyJson, i18nResolver, userMap);
            } else {
                final String aliasName = fieldInfo.aliasName();

                switch (aliasName) {
                    case "name" -> companyJson.put(aliasName, company.getName());
                    case "website" -> companyJson.put(aliasName, company.getWebsite());
                    case "address" -> {
                        final Address address = company.getAddress();

                        if (Objects.nonNull(address)) {
                            final Map<StoreSubType, FieldSubType> fieldSubTypes = fieldTypeService.groupSubtypesByStoreSubType(fieldInfo.storeType());
                            final JsonObject addressJson = Json.object();

                            addressJson.put(i18nResolver.get(fieldSubTypes.get(StoreSubType.ADDRESS_LINE1).name()), address.getAddressLine1());
                            addressJson.put(i18nResolver.get(fieldSubTypes.get(StoreSubType.ADDRESS_LINE2).name()), address.getAddressLine2());
                            addressJson.put(i18nResolver.get(fieldSubTypes.get(StoreSubType.CITY).name()), address.getCity());
                            addressJson.put(i18nResolver.get(fieldSubTypes.get(StoreSubType.STATE).name()), address.getState());
                            addressJson.put(i18nResolver.get(fieldSubTypes.get(StoreSubType.COUNTRY).name()),
                                    Optional.ofNullable(address.getCountry()).map(Country::getName).orElseGet(() -> null));
                            addressJson.put(i18nResolver.get(fieldSubTypes.get(StoreSubType.ZIPCODE).name()), address.getZipcode());

                            companyJson.put(aliasName, addressJson);
                        }
                    }
                    case "salesOwner" -> {
                        final JsonObject saleOwnerJson = Json.object();
                        final CrmUser salesOwner = company.getSalesOwner();

                        saleOwnerJson.put(NAME, userMap.get(salesOwner.getIamId()).getFullName());
                        saleOwnerJson.put(ID, salesOwner.id());

                        companyJson.put(aliasName, saleOwnerJson);
                    }
                    case "facebook" -> companyJson.put(aliasName, company.getFacebook());
                    case "twitter" -> companyJson.put(aliasName, company.getTwitter());
                    case "linkedIn" -> companyJson.put(aliasName, company.getLinkedIn());
                    case "annualRevenue" -> {
                        final Monetary annualRevenue = company.getAnnualRevenue();

                        if (Objects.nonNull(annualRevenue)) {
                            final JsonObject subTypeJson = Json.object();
                            final Map<StoreSubType, FieldSubType> fieldSubTypes = fieldTypeService.groupSubtypesByStoreSubType(fieldInfo.storeType());

                            subTypeJson.put(i18nResolver.get(fieldSubTypes.get(StoreSubType.CURRENCY).name()),
                                    i18nResolver.get(annualRevenue.getCurrency().getI18nKey()));
                            subTypeJson.put(i18nResolver.get(fieldSubTypes.get(StoreSubType.VALUE).name()), annualRevenue.getValue());

                            companyJson.put(aliasName, subTypeJson);
                        }
                    }
                    case "sicCode" -> companyJson.put(aliasName, company.getSicCode());
                    case "territory" -> {
                        final Territory territory = company.getTerritory();

                        if (Objects.nonNull(territory)) {
                            final JsonObject territoryJson = Json.object();

                            territoryJson.put(NAME, territory.getName());
                            territoryJson.put(ID, territory.id());

                            companyJson.put(aliasName, territoryJson);
                        }
                    }
                    case "tags" -> {
                        final SummaryInfo companySummary = company.getSummaryInfo();

                        if (Objects.nonNull(companySummary)) {
                            final Collection<Tag> companyTags = companySummary.getTags();

                            if (Objects.nonNull(companyTags)) {
                                final JsonArray tags = Json.array();

                                for (final Tag tag : companyTags) {
                                    final JsonObject tagJson = Json.object();

                                    tagJson.put(NAME, tag.getName());
                                    tagJson.put(ID, tag.id());

                                    tags.put(tagJson);
                                }
                                companyJson.put(aliasName, tags);
                            }
                        }
                    }
                    case "employeeCount" -> companyJson.put(aliasName, company.getEmployeeCount());
                    case "parentCompany" -> {
                        final Company parentCompany = company.getParentCompany();

                        if (Objects.nonNull(parentCompany)) {
                            final JsonObject parentCompanyJson = Json.object();

                            parentCompanyJson.put(NAME, parentCompany.getName());
                            parentCompanyJson.put(ID, parentCompany.id());

                            companyJson.put(aliasName, parentCompanyJson);
                        }
                    }
                    case "organizationStatus" -> {
                        final Option organizationStatus = company.getOrganizationStatus();

                        if (Objects.nonNull(organizationStatus)) {
                            companyJson.put(aliasName, i18nResolver.get(organizationStatus.getOption()));
                        }
                    }
                    case "industryType" -> {
                        final Option industryType = company.getIndustryType();

                        if (Objects.nonNull(industryType)) {
                            companyJson.put(aliasName, i18nResolver.get(industryType.getOption()));
                        }
                    }
                    case "businessType" -> {
                        final Option businessType = company.getBusinessType();

                        if (Objects.nonNull(businessType)) {
                            companyJson.put(aliasName, i18nResolver.get(businessType.getOption()));
                        }
                    }
                    case "description" -> companyJson.put(aliasName, company.getDescription());
                    case "createdAt" ->
                            companyJson.put(aliasName, TemporalServiceProvider.get().getISOOffsetDateTimeString(company.getCreatedTime().getTimeStamp(), TimezoneInfo.UTC, ISOTemporalFormat.ISO_OFFSET_DATE_TIME));
                    case "updatedAt" ->
                            companyJson.put(aliasName, TemporalServiceProvider.get().getISOOffsetDateTimeString(company.getModifiedTime().getTimeStamp(), TimezoneInfo.UTC, ISOTemporalFormat.ISO_OFFSET_DATE_TIME));
                    case "lastAssignedAt" ->
                            companyJson.put(aliasName, TemporalServiceProvider.get().getISOOffsetDateTimeString(company.getLastAssignedAt().getTimeStamp(), TimezoneInfo.UTC, ISOTemporalFormat.ISO_OFFSET_DATE_TIME));
                    case "createdBy" -> {
                        final JsonObject creatorJson = Json.object();
                        final CrmUser creator = company.getCreatedBy();

                        creatorJson.put(NAME, userMap.get(creator.getIamId()).getFullName());
                        creatorJson.put(ID, creator.id());

                        companyJson.put(aliasName, creatorJson);
                    }
                    case "updatedBy" -> {
                        final JsonObject updaterJson = Json.object();
                        final CrmUser updatedBy = company.getUpdatedBy();

                        updaterJson.put(NAME, userMap.get(updatedBy.getIamId()).getFullName());
                        updaterJson.put(ID, updatedBy.id());

                        companyJson.put(aliasName, updaterJson);
                    }
                }
            }
        }

        return companyJson;
    }
}
