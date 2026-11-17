package com.tlc.crm.core.features.action.merge.contact.duplicate;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.core.features.action.merge.EntityMergeManagement;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.security.permission.annotation.authorizer.SourceAuthorizer;
import com.tlc.crm.common.iam.service.OrganizationService;
import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.converter.CompanyConverter;
import com.tlc.crm.core.features.api.contact.manager.CompanyMergeManager;
import com.tlc.crm.core.field.EntityMergeFields;
import com.tlc.crm.core.field.contact.CompanyFields;
import com.tlc.crm.core.internal.contact.status.ContactErrorCodes;
import com.tlc.crm.core.internal.status.CoreErrorCodes;
import com.tlc.crm.entity.manager.EntityConfigManager;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.iam.client.UserService;
import com.tlc.iam.common.models.User;
import com.tlc.web.WebAction;
import com.tlc.web.client.event.EventCallback;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Manages the duplicate companies list and merge company related web actions
 *
 * @author EswariNivethaVU
 * @version 1.0
 */
@SourceAuthorizer(subCategory = SubCategory.Type.COMPANIES)
@WebAction(path = "/contact/company/duplicate/mgmt")
public class CompanyDuplicateManagement extends EntityMergeManagement<Company> {

    private static final int DEFAULT_START_VALUE = 1;
    private static final int DEFAULT_LIMIT = 50;

    private final CompanyMergeManager companyDuplicateManager;
    private final CompanyManager companyManager;
    private final CompanyConverter companyConverter;
    private final UserService iamUserService;
    private final OrganizationService organizationService;
    private final FieldService fieldService;

    public CompanyDuplicateManagement() {
        companyDuplicateManager = CompanyMergeManager.getInstance();
        companyManager = CompanyManager.getInstance();
        companyConverter = CompanyConverter.getInstance();
        iamUserService = UserService.getInstance();
        organizationService = OrganizationService.getInstance();
        fieldService = FieldService.getSecureInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request  the received request
     * @param response the response to be sent
     */
    @Override
    protected void handle(final CrmRequest request, final CrmResponse response) {
        final JsonObject requestJson = request.getRequestJson();
        final JsonObject data = requestJson.getJsonObject(CompanyFields.DATA);
        final Long orgId = request.orgId();
        final Long userId = request.userId();
        final String type = requestJson.getString(CompanyFields.TYPE);

        switch (type) {
            case EntityMergeFields.REVIEW -> reviewCompanies(orgId, data, response);
            case EntityMergeFields.MERGE -> mergeCompanies(data, orgId, userId, response);
            case EntityMergeFields.LIST -> processCompanyList(data, orgId, response);
            case EntityMergeFields.DISMISS -> dismissCompany(orgId, data, response);
        }
    }

    @Override
    protected EntityConfigManager<Company> getEntityManager() {
        return companyManager;
    }

    /**
     * <p>
     * Fetches the company details for the given company id
     * </p>
     *
     * @param orgId Represents the organization id
     * @param data  Contains the JSON object of company fields
     * @return {@link Company} which contains primary company details
     */
    private Company getPrimaryCompany(final Long orgId, final JsonObject data) {
        final Long companyId = data.getLong(CompanyFields.COMPANY_ID);

        if (!companyManager.exists(orgId, companyId)) {
            throw ErrorCode.get(ContactErrorCodes.COMPANY_NOT_FOUND, "i18n.company.not.found");
        }

        return companyDuplicateManager.getPrimaryCompany(orgId, companyId);
    }

    /**
     * <p>
     * Review companies which contains the both primary and duplicate company records
     * </p>
     *
     * @param orgId    Represents the organization id
     * @param data     Contains the JSON object
     * @param response Response to be sent
     */
    private void reviewCompanies(final Long orgId, final JsonObject data, final CrmResponse response) {
        final Company primaryCompany = getPrimaryCompany(orgId, data);
        final int start = Math.max(data.optInt(CompanyFields.START, DEFAULT_START_VALUE), DEFAULT_START_VALUE);
        final int limit = Math.min(data.optInt(CompanyFields.LIMIT, DEFAULT_LIMIT), DEFAULT_LIMIT);
        final Long totalRecords = companyDuplicateManager.getDuplicateCompanyCount(orgId, primaryCompany);
        final boolean hasDuplicates = totalRecords > 1;

        if (hasDuplicates) {
            final Collection<Company> companies = companyDuplicateManager.getDuplicateCompanies(orgId, primaryCompany, start, limit);

            fetchAndDispatchDuplicateCompanies(orgId, (start + limit) <= totalRecords, totalRecords, response, companies, primaryCompany);
        } else {
            response.put(CompanyFields.TOTAL_RECORDS, totalRecords - 1);
            response.put(CompanyFields.HAS_MORE, false);
            response.put(CompanyFields.DUPLICATES, Json.array());
        }
    }

    /**
     * <p>
     * Fetches and dispatches the duplicate companies
     * </p>
     *
     * @param orgId          Represents an organization id
     * @param hasMore        Represents the remaining records to be fetched
     * @param totalRecords   Represents the number of records to be fetched
     * @param crmResponse    Represents the {@link CrmResponse} details
     * @param companies      Contains the collection of {@link Company} details
     * @param primaryCompany Represents the primary company details
     */
    private void fetchAndDispatchDuplicateCompanies(final Long orgId, final boolean hasMore, final Long totalRecords, final CrmResponse crmResponse,
                                                    final Collection<Company> companies, final Company primaryCompany) {
        final Collection<Long> iamUserIds = companies.stream().map(Company::getSalesOwner).map(CrmUser::getIamId).collect(Collectors.toSet());

        iamUserIds.add(primaryCompany.getSalesOwner().getIamId());
        crmResponse.convertIntoAsync();

        final EventCallback<Collection<User>> usersFetchCallBack = new EventCallback<>() {

            @Override
            public void onSuccess(final Collection<User> users) {
                final JsonArray duplicateCompanyArray = Json.array();
                final Map<Long, User> crmUsermap = users.stream().collect(Collectors.toMap(User::id, Function.identity()));
                final CrmUser primaryCompanySalesOwner = primaryCompany.getSalesOwner();

                if (Objects.nonNull(primaryCompanySalesOwner)) {
                    primaryCompanySalesOwner.setUser(crmUsermap.get(primaryCompanySalesOwner.getIamId()));
                }

                for (final Company company : companies) {
                    final CrmUser salesOwner = company.getSalesOwner();

                    if (Objects.nonNull(salesOwner)) {
                        salesOwner.setUser(crmUsermap.get(salesOwner.getIamId()));
                        company.setSalesOwner(salesOwner);
                    }
                    duplicateCompanyArray.put(companyConverter.convertDuplicateCompanyToJson(company));
                }
                crmResponse.put(CompanyFields.TOTAL_RECORDS, totalRecords);
                crmResponse.put(CompanyFields.HAS_MORE, hasMore);
                crmResponse.put(CompanyFields.DUPLICATES, duplicateCompanyArray);
                crmResponse.send();
            }

            @Override
            public void onFailure(final Throwable throwable) {
                sendError(crmResponse, null, ErrorCode.get(CoreErrorCodes.USER_RETRIEVAL_FAILED, "i18n.iam.user.retrieval.failed", throwable));
            }
        };
        iamUserService.fetchUsers(organizationService.getIamOrgId(orgId), iamUserIds, usersFetchCallBack);
    }

    /**
     * <p>
     * Merges the given duplicate companies with primary company
     * </p>
     *
     * @param data        Contains the JSON object
     * @param orgId       Represents the organization id
     * @param userId      Represents the user id
     * @param crmResponse Response to be sent
     */
    private void mergeCompanies(final JsonObject data, final Long orgId, final long userId,  final CrmResponse crmResponse) {
        final Long primaryCompanyId = data.getLong(CompanyFields.PRIMARY_COMPANY_ID);
        final JsonArray duplicateCompanyIdsArray = data.getJsonArray(CompanyFields.DUPLICATE_COMPANY_IDS);
        final Collection<Long> duplicateCompanyIds = buildDuplicateEntityIds(duplicateCompanyIdsArray);
        final Collection<Long> companyIds = buildAllIds(primaryCompanyId, duplicateCompanyIds);

        validateExists(companyManager.exists(orgId, companyIds), ContactErrorCodes.COMPANY_NOT_FOUND, "i18n.company.not.found");
        final Map<String, Object> mergedData = companyDuplicateManager.mergeCompanies(
                orgId, userId, primaryCompanyId, duplicateCompanyIds, companyIds);
        final Company retainedCompany = mergedData.containsKey(CompanyFields.COMPANY)
                ? (Company) mergedData.get(CompanyFields.COMPANY) : companyDuplicateManager.get(orgId, primaryCompanyId);

        crmResponse.put(CompanyFields.MERGED_STATUS, Map.of(CompanyFields.MERGED_RECORDS, mergedData.get(CompanyFields.MERGED_RECORDS)));
        publishMergeIndexData(orgId, retainedCompany, duplicateCompanyIds);
    }

    /**
     * <p>
     * Processes the primary and secondary duplicate company list by using orgId
     * </p>
     *
     * @param data  Contains the JSON object
     * @param orgId Represents the organization id
     */
    private void processCompanyList(final JsonObject data, final long orgId, final CrmResponse response) {
        final JsonArray companiesArray = Json.array();
        final int begin = data.optInt(ContactFields.START, DEFAULT_START_VALUE);
        final int count = data.optInt(ContactFields.LIMIT, DEFAULT_LIMIT);
        final int start = begin > 0 ? begin : DEFAULT_START_VALUE;
        final int limit = count > 0 ? count : DEFAULT_LIMIT;
        final long totalRecordsCount = companyDuplicateManager.getDuplicateCompaniesCount(orgId);
        final Collection<List<Company>> companyNameList = companyDuplicateManager.getCompaniesByDuplicateName(orgId, limit, start);

        for (final List<Company> companies : companyNameList) {
            final JsonObject jsonObject = Json.object();
            final Company primaryCompany = companies.get(0);
            final Company secondaryCompany = companies.get(1);

            jsonObject.put(CompanyFields.PRIMARY, companyConverter.convertCompanyWithThumbnail(primaryCompany));
            jsonObject.put(CompanyFields.SECONDARY, companyConverter.convertCompanyWithThumbnail(secondaryCompany));
            companiesArray.put(jsonObject);
        }

        response.put(CompanyFields.TOTAL_RECORDS, totalRecordsCount);
        response.put(CompanyFields.HAS_MORE, (start + limit) <= totalRecordsCount);
        response.put(CompanyFields.COMPANIES, companiesArray);
    }

    /**
     * <p>
     * Dismiss company based on the given company id
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param data     Represents the {@link JsonObject}
     * @param response Represents the {@link CrmResponse}
     */
    private void dismissCompany(final long orgId, final JsonObject data, final CrmResponse response) {
        try {
            final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(Company.ENTITY_TYPE);
            final Company primaryCompany = companyDuplicateManager.getPrimaryCompany(orgId, data.getLong(CompanyFields.COMPANY_ID));
            final FieldInfo fieldInfo = fields.get(CompanyFields.NAME);

            companyDuplicateManager.updateDismissedValues(orgId, fieldInfo.id(), fieldInfo.sourceId(), List.of(primaryCompany.getName()));
            response.put(CompanyFields.MESSAGE, response.resolveI18nString("i18n.company.duplicate.dismiss.success"));
        } catch (Exception e) {
            throw ErrorCode.get(ContactErrorCodes.COMPANY_DISMISSAL_FAILED, "i18n.company.duplicate.dismiss.failed", e);
        }
    }
}