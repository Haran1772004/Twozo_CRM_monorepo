package com.tlc.crm.core.workflow.internal.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.Territory;

import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.ValueHolder;
import com.tlc.crm.entity.models.type.Address;
import com.tlc.crm.entity.models.type.Monetary;
import com.tlc.crm.entity.models.type.Option;

import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.field.contact.CompanyFields;
import com.tlc.crm.core.field.summary.SummaryFields;
import com.tlc.l10n.temporal.resolver.DateTimeUtil;

import java.util.Map;
import java.util.Objects;

/**
 * The converter that facilitates different ways to convert data from/to {@link Company}.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class CompanyWorkflowConverter extends EntityWorkflowConverter<Company> {

    private final DateTimeUtil dateTimeUtil;

    private CompanyWorkflowConverter() {
        dateTimeUtil = DateTimeUtil.getInstance();
    }

    private static class Instance {
        private static final CompanyWorkflowConverter INSTANCE = new CompanyWorkflowConverter();
    }

    public static CompanyWorkflowConverter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param company Refers the model.
     * @return The converted json.
     */
    @Override
    public JsonObject convert(final Company company) {
        final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(Company.ENTITY_TYPE);
        final JsonObject jsonObject = Json.object();
        final ValueHolder valueHolder = company.getValueHolder();
        final Option businessType = company.getBusinessType();
        final Option industryType = company.getIndustryType();
        final Option organisationType = company.getOrganizationStatus();
        final Address address = company.getAddress();
        final Monetary annualRevenue = company.getAnnualRevenue();
        final CrmUser createdBy = company.getCreatedBy();
        final CrmUser updatedBy = company.getUpdatedBy();
        final Territory territory = company.getTerritory();
        final Company parentCompany = company.getParentCompany();

        jsonObject.put(CompanyFields.ID, company.id());
        jsonObject.put(CompanyFields.ORG_ID, company.orgId());
        jsonObject.put(fields.get(CompanyFields.NAME).idAsString(), company.getName());
        jsonObject.put(fields.get(CompanyFields.EMPLOYEE_COUNT).idAsString(), company.getEmployeeCount());
        jsonObject.put(fields.get(CompanyFields.FACEBOOK).idAsString(), company.getFacebook());
        jsonObject.put(fields.get(CompanyFields.LINKEDIN).idAsString(), company.getLinkedIn());
        jsonObject.put(fields.get(CompanyFields.TWITTER).idAsString(), company.getTwitter());
        jsonObject.put(fields.get(CompanyFields.SIC_CODE).idAsString(), company.getSicCode());
        jsonObject.put(fields.get(CompanyFields.WEBSITE).idAsString(), company.getWebsite());
        jsonObject.put(fields.get(CompanyFields.SALES_OWNER).idAsString(), company.getSalesOwner().id());
        jsonObject.put(fields.get(CompanyFields.DESCRIPTION).idAsString(), company.getDescription());
        jsonObject.put(CompanyFields.CREATED_TIME, dateTimeUtil.getTimeStamp(company.getCreatedTime()));
        jsonObject.put(CompanyFields.UPDATED_TIME, dateTimeUtil.getTimeStamp(company.getModifiedTime()));
        jsonObject.put(CompanyFields.RECENT_NOTE, company.getRecentNote());

        if (Objects.nonNull(address)) {
            jsonObject.put(fields.get(CompanyFields.ADDRESS).idAsString(), addressConverter.convert(address));
        }

        if (Objects.nonNull(annualRevenue)) {
            jsonObject.put(fields.get(CompanyFields.ANNUAL_REVENUE).idAsString(),
                    monetaryConverter.convert(annualRevenue));
        }

        if (Objects.nonNull(businessType)) {
            jsonObject.put(fields.get(CompanyFields.BUSINESS_TYPE).idAsString(), businessType.id());
        }

        if (Objects.nonNull(industryType)) {
            jsonObject.put(fields.get(CompanyFields.INDUSTRY_TYPE).idAsString(), industryType.id());
        }

        if (Objects.nonNull(organisationType)) {
            jsonObject.put(fields.get(CompanyFields.ORGANIZATION_STATUS).idAsString(), organisationType.id());
        }

        if (Objects.nonNull(valueHolder)) {
            valueHolder.mapToJson(jsonObject);
        }

        if (Objects.nonNull(createdBy)) {
            jsonObject.put(fields.get(CompanyFields.CREATED_BY).idAsString(), createdBy.id());
        }

        if (Objects.nonNull(updatedBy)) {
            jsonObject.put(fields.get(CompanyFields.UPDATED_BY).idAsString(), updatedBy.id());
        }

        if (Objects.nonNull(territory)) {
            jsonObject.put(fields.get(CompanyFields.TERRITORY).idAsString(), territory.id());
        }

        if (Objects.nonNull(parentCompany)) {
            jsonObject.put(fields.get(CompanyFields.PARENT_COMPANY).idAsString(), parentCompany.id());
        }
        final JsonObject summaryJson = summaryInfoConverter.convert(company.getSummaryInfo());

        if (summaryJson.size() > 0) {
            jsonObject.put(SummaryFields.SUMMARY, summaryJson);
        }

        return jsonObject;
    }

    /**
     * {@inheritDoc}
     *
     * @param jsonObject Refers the json object.
     * @return The {@link Company}.
     */
    @Override
    public Company convert(JsonObject jsonObject) {
        final Long orgId = jsonObject.getLong(CompanyFields.ORG_ID);
        final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(Company.ENTITY_TYPE);
        final String createdByKey = CompanyFields.CREATED_BY;
        final Long ownerId = jsonObject.getLong(fields.get(createdByKey).idAsString());
        final ConverterConfig converterConfig = new ConverterConfig(orgId, ownerId, null, false, jsonObject);
        final Company company = new Company();

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
        final FieldInfo businessTypeField = fields.get(CompanyFields.BUSINESS_TYPE);
        final FieldInfo organisationStatusField = fields.get(CompanyFields.ORGANIZATION_STATUS);
        final FieldInfo industryTypeField = fields.get(CompanyFields.INDUSTRY_TYPE);
        final String businessTypeKey = businessTypeField.idAsString();
        final String industryTypeKey = industryTypeField.idAsString();
        final String organisationStatusKey = organisationStatusField.idAsString();
        final Object annualRevenue = jsonObject.opt(annualRevenueKey);
        final Object address = jsonObject.opt(addressKey);
        final CrmUser user = userService.getUser(orgId, ownerId);

        company.setOrgId(orgId);
        company.setId(jsonObject.getLong(CompanyFields.ID));
        company.setCreatedTime(dateTimeUtil.getTemporal(jsonObject.getLong(CompanyFields.CREATED_TIME)));
        company.setModifiedTime(dateTimeUtil.getTemporal(jsonObject.getLong(CompanyFields.UPDATED_TIME)));
        company.setCreatedBy(user);
        company.setName(jsonObject.containsKey(nameKey) ? jsonObject.getString(nameKey)
                : company.getName());
        company.setWebsite(jsonObject.containsKey(websiteKey) ? jsonObject.getString(websiteKey)
                : company.getWebsite());
        company.setFacebook(jsonObject.containsKey(facebookKey) ? jsonObject.getString(facebookKey)
                : company.getFacebook());
        company.setLinkedIn(jsonObject.containsKey(linkedInKey) ? jsonObject.getString(linkedInKey)
                : company.getLinkedIn());
        company.setTwitter(jsonObject.containsKey(twitterKey) ? jsonObject.getString(twitterKey)
                : company.getTwitter());
        company.setBusinessType(jsonObject.containsKey(businessTypeKey)
                ? optionConverter.convert(orgId, businessTypeField.id(), jsonObject.getLong(businessTypeKey)) : company.getBusinessType());
        company.setIndustryType(jsonObject.containsKey(industryTypeKey)
                ? optionConverter.convert(orgId, industryTypeField.id(), jsonObject.getLong(industryTypeKey)) : company.getIndustryType());
        company.setOrganizationStatus(jsonObject.containsKey(organisationStatusKey)
                ? optionConverter.convert(orgId, organisationStatusField.id(), jsonObject.getLong(organisationStatusKey))
                : company.getOrganizationStatus());
        company.setAnnualRevenue(annualRevenue instanceof final Map<?, ?> monetaryMap
                ? monetaryConverter.convert(converterConfig, Json.object((Map<String, ?>) monetaryMap)) : company.getAnnualRevenue());
        company.setAddress(address instanceof final Map<?, ?> addressMap
                ? addressConverter.convert(converterConfig, Json.object((Map<String, ?>) addressMap)) : company.getAddress());
        company.setSalesOwner(jsonObject.containsKey(salesOwnerKey) ? userService.getUser(orgId, jsonObject.getLong(salesOwnerKey))
                : user);
        company.setUpdatedBy(user);
        company.setDescription(jsonObject.containsKey(descriptionKey) ? jsonObject.getString(descriptionKey) : company.getDescription());
        company.setRecentNote(jsonObject.optString(CompanyFields.RECENT_NOTE, null));
        entityBuilder.buildValueHolder(company, converterConfig, jsonObject);

        if (jsonObject.containsKey(parentCompanyKey)) {
            final Company parentCompany = new Company();

            parentCompany.setId((jsonObject.getLong(parentCompanyKey)));
            company.setParentCompany(parentCompany);
        }

        if (jsonObject.containsKey(sicCodeKey)) {
            company.setSicCode(jsonObject.getLong(sicCodeKey));
        }

        if (jsonObject.containsKey(territoryKey)) {
            final Territory territory = new Territory();

            territory.setId(jsonObject.getLong(territoryKey));
            company.setTerritory(territory);
        }

        if (jsonObject.containsKey(SummaryFields.SUMMARY)) {
            company.setSummaryInfo(summaryInfoConverter.convert(converterConfig, jsonObject.getJsonObject(SummaryFields.SUMMARY)));
        }

        if (jsonObject.containsKey(employeeCountKey)) {
            company.setEmployeeCount(jsonObject.getLong(employeeCountKey));
        }

        return company;
    }

}
