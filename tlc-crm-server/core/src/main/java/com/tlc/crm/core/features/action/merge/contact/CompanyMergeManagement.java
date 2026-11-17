package com.tlc.crm.core.features.action.merge.contact;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.converter.CompanyConverter;
import com.tlc.crm.core.features.action.merge.EntityMergeManagement;
import com.tlc.crm.core.features.api.contact.manager.CompanyMergeManager;
import com.tlc.crm.core.field.EntityMergeFields;
import com.tlc.crm.core.field.contact.CompanyFields;
import com.tlc.crm.core.field.deal.DealFields;
import com.tlc.crm.core.internal.contact.status.ContactErrorCodes;
import com.tlc.crm.entity.manager.EntityConfigManager;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.service.internal.SecureFieldService;
import com.tlc.crm.security.permission.annotation.authorizer.SourceAuthorizer;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.web.WebAction;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * Handles company merge actions by reusing duplicate merge validations.
 * </p>
 *
 * @author EswariNivethaVU.
 * @version 1.0.
 */
@SourceAuthorizer(subCategory = SubCategory.Type.COMPANIES)
@WebAction(path = "/contact/company/merge/mgmt")
public class CompanyMergeManagement extends EntityMergeManagement<Company> {

    private final CompanyManager companyManager;
    private final CompanyMergeManager companyMergeManager;
    private final CompanyConverter companyConverter;
    private final SecureFieldService secureFieldService;

    public CompanyMergeManagement() {
        companyManager = CompanyManager.getInstance();
        companyMergeManager = CompanyMergeManager.getInstance();
        companyConverter = CompanyConverter.getInstance();
        secureFieldService = SecureFieldService.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request  the received request.
     * @param response the response to be sent.
     */
    @Override
    protected void handle(final CrmRequest request, final CrmResponse response) {
        final JsonObject requestJson = request.getRequestJson();
        final JsonObject data = requestJson.getJsonObject(CompanyFields.DATA);
        final long orgId = request.orgId();
        final long userId = request.userId();
        final String type = requestJson.getString(CompanyFields.TYPE);

        switch (type) {
            case EntityMergeFields.PREVIEW -> getCompanyPreview(orgId, data, response);
            case EntityMergeFields.MERGE -> mergeCompanies(orgId, userId, data, response);
            case EntityMergeFields.GET_COMPANY_FIELDS -> response.put(EntityMergeFields.FIELDS, getCompanyFields(orgId, response));
            case EntityMergeFields.GET_COMPANY -> get(orgId, data, response);
        }
    }

    @Override
    protected EntityConfigManager<Company> getEntityManager() {
        return companyManager;
    }

    /**
     * <p>
     * Gets the company based on the company id.
     * </p>
     *
     * @param orgId       Represents an organization id.
     * @param data        Holds the {@link JsonObject} details.
     * @param crmResponse Represents the {@link CrmResponse} details.
     */
    private void get(final Long orgId, final JsonObject data, final CrmResponse crmResponse) {
        final Long id = data.getLong(CompanyFields.ID);

        if (!companyManager.exists(orgId, id)) {
            throw ErrorCode.get(ContactErrorCodes.COMPANY_NOT_FOUND, "i18n.company.not.found");
        }
        final Company company = companyMergeManager.get(orgId, id);

        crmResponse.put(CompanyFields.PREVIEW_RESULT, companyConverter.convertPrimaryCompanyToJson(company));
    }

    /**
     * <p>
     * Gets the company fields list based on the organization id.
     * </p>
     *
     * @param orgId       Represents an organization id.
     * @param crmResponse Represents the {@link CrmResponse} details.
     * @return The {@link JsonArray} details.
     */
    private JsonArray getCompanyFields(final Long orgId, final CrmResponse crmResponse) {
        final JsonArray jsonArray = Json.array();
        final Collection<FieldInfo> fieldsBySource = secureFieldService.getFieldsBySource(orgId, Company.ENTITY_TYPE);

        for (final FieldInfo fieldInfo : fieldsBySource) {
            final JsonObject jsonObject = Json.object();

            jsonObject.put(DealFields.KEY, fieldInfo.isPersistent() ? fieldInfo.idAsString() : fieldInfo.aliasName());
            jsonObject.put(DealFields.NAME, crmResponse.resolveI18nPlaceholder(fieldInfo.i18nName()));
            jsonObject.put(DealFields.STORE_TYPE, fieldInfo.storeType());
            jsonArray.put(jsonObject);
        }

        return jsonArray;
    }

    /**
     * <p>
     * Gets the company preview details.
     * </p>
     *
     * @param orgId       Represents the organization id.
     * @param data        Represents the {@link JsonObject}.
     * @param crmResponse Represents the {@link CrmResponse} data.
     */
    private void getCompanyPreview(final Long orgId, final JsonObject data, final CrmResponse crmResponse) {
        final Long primaryId = data.getLong(CompanyFields.PRIMARY_COMPANY_ID);
        final Long secondaryId = data.getLong(CompanyFields.SECONDARY_COMPANY_ID);

        if (validatePrimaryAndSecondaryEntities(orgId, primaryId, secondaryId,
                ContactErrorCodes.SAME_PRIMARY_AND_SECONDARY_ID_RESTRICTED,
                ContactErrorCodes.COMPANY_NOT_FOUND, "i18n.company.not.found")) {
            final Company company = companyMergeManager.getCompanyPreviewDetails(orgId, primaryId, secondaryId);

            crmResponse.put(CompanyFields.PREVIEW_RESULT, companyConverter.convertPrimaryCompanyToJson(company));
        }
    }

    /**
     * <p>
     * Merges the primary and secondary company.
     * </p>
     *
     * @param orgId       Represents the organization id.
     * @param userId      Represents the user id.
     * @param data        Represents the {@link JsonObject}.
     * @param crmResponse Response of the Merged companies.
     */
    private void mergeCompanies(final long orgId, final long userId, final JsonObject data, final CrmResponse crmResponse) {
        final Long primaryCompanyId = data.getLong(CompanyFields.PRIMARY_COMPANY_ID);
        final Long secondaryCompanyId = data.getLong(CompanyFields.SECONDARY_COMPANY_ID);
        final Collection<Long> duplicateIds = List.of(secondaryCompanyId);
        final Collection<Long> companyIds = buildAllIds(primaryCompanyId, duplicateIds);

        validatePrimaryAndDuplicateEntities(orgId, primaryCompanyId, duplicateIds,
                ContactErrorCodes.COMPANY_NOT_FOUND, "i18n.company.not.found");
        final Map<String, Object> mergedData = companyMergeManager.mergeCompanies(
                orgId, userId, primaryCompanyId, duplicateIds, companyIds);
        final Company retainedCompany = mergedData.containsKey(CompanyFields.COMPANY) ?
                (Company) mergedData.get(CompanyFields.COMPANY) : companyMergeManager.get(orgId, primaryCompanyId);

        crmResponse.put(CompanyFields.MERGED_STATUS, Map.of(CompanyFields.MERGED_RECORDS, mergedData.get(CompanyFields.MERGED_RECORDS)));
        publishMergeIndexData(orgId, retainedCompany, duplicateIds);
    }
}