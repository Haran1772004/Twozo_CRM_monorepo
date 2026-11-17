package com.tlc.crm.core.action.rest;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.common.fields.ListActionFields;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.internal.IamErrorCodes;

import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.converter.CompanyConverter;

import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.common.list.model.ListType;
import com.tlc.crm.entity.list.model.Provider;

import com.tlc.iam.common.models.User;

import com.tlc.web.HttpMethod;
import com.tlc.web.client.event.EventCallback;
import com.tlc.web.undertow.rest.annotation.ApiRoute;
import com.tlc.web.undertow.rest.annotation.RestApiHandler;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * Manages {@code Company} list related REST web actions.
 * </p>
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
@RestApiHandler(path = "/v1/company/view")
public class CompanyRestListActionV1 extends AbstractSecureRestListActionV1 {

    private final CompanyManager companyManager;
    private final CompanyConverter companyConverter;

    public CompanyRestListActionV1() {
        this.companyManager = CompanyManager.getInstance();
        this.companyConverter = CompanyConverter.getInstance();
    }

    /**
     * Handles the HTTP GET request for fetching the list of {@code Company} entities based on the provided request parameters and context.
     * This method processes the request, retrieves the relevant data, and constructs the appropriate response to be sent back to the client.
     *
     * @param request  Refers the incoming {@code CrmRequest}.
     * @param response Refers the {@code CrmResponse} to be sent.
     */
    @ApiRoute(path = "/list", method = HttpMethod.GET)
    public void list(final CrmRequest request, final CrmResponse response) {
        processList(request, response);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Provider getProvider() {
        return Provider.COMPANY_LIST;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Source getSource() {
        return Source.COMPANY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ListType getListType() {
        return ListType.COMPANY_LIST;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId       The ID of the organization for which entities are being fetched; must not be null.
     * @param request     The CRM request containing context and parameters; must not be null.
     * @param entityIds   The collection of entity IDs to retrieve and process; must not be null or empty.
     * @param crmResponse The response object to which the processed entity data should be written; must not be null.
     */
    @Override
    protected void fetchAndDispatchEntities(final Long orgId, final CrmRequest request, final Collection<Long> entityIds, final CrmResponse crmResponse) {
        final Map<Long, Company> companies = companyManager.get(orgId, entityIds).stream().collect(Collectors.toMap(Company::id, Function.identity()));
        final Collection<Long> userIds = new HashSet<>();
        final JsonArray array = Json.array();

        for (final Company company : companies.values()) {
            userIds.addAll(company.userIds());
        }
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, false);

        if (userIds.isEmpty()) {

            for (final Long modelId : entityIds) {
                final Company company = companies.get(modelId);

                array.put(companyConverter.convertToRestJson(company, converterConfig));
            }
            crmResponse.put(ListActionFields.ROWS, array);
            crmResponse.send();
        } else {
            crmResponse.convertIntoAsync();

            final EventCallback<Collection<User>> usersFetchCallBack = new EventCallback<>() {

                @Override
                public void onSuccess(final Collection<User> users) {
                    try {
                        for (final Long modelId : entityIds) {
                            final Company company = companies.get(modelId);

                            setUser(company, users);
                            array.put(companyConverter.convertToRestJson(company, converterConfig));
                        }
                        crmResponse.put(ListActionFields.ROWS, array);
                    } finally {
                        crmResponse.send();
                    }
                }

                @Override
                public void onFailure(final Throwable throwable) {
                    crmResponse.setErrorCode(ErrorCode.get(IamErrorCodes.USER_RETRIEVAL_FAILED, "i18n.user.detail.retrieve.failed", throwable));
                    crmResponse.send();
                }
            };
            userService.fetchUsers(organizationService.getIamOrgId(orgId), crmUserService.getUsers(orgId, userIds).stream().map(CrmUser::getIamId).collect(Collectors.toSet()), usersFetchCallBack);
        }
    }

    /**
     * Sets the {@link CrmUser} details into {@link Company}.
     *
     * @param company Represents the {@link Company} details.
     * @param users   Represents the collection of {@link User}.
     */
    private void setUser(final Company company, final Collection<User> users) {
        final Map<Long, User> crmUsers = users.stream().collect(Collectors.toMap(User::id, Function.identity()));
        final Collection<CrmUser> valueHolders = company.getValueHolder().getUserValues();
        final CrmUser owner = company.getSalesOwner();
        final CrmUser createdBy = company.getCreatedBy();
        final CrmUser updatedBy = company.getUpdatedBy();

        if (Objects.nonNull(owner)) {
            owner.setUser(crmUsers.get(owner.getIamId()));
            company.setSalesOwner(owner);
        }

        if (Objects.nonNull(createdBy)) {
            createdBy.setUser(crmUsers.get(createdBy.getIamId()));
            company.setCreatedBy(createdBy);
        }

        if (Objects.nonNull(updatedBy)) {
            updatedBy.setUser(crmUsers.get(updatedBy.getIamId()));
            company.setUpdatedBy(updatedBy);
        }

        for (final CrmUser crmUser : valueHolders) {
            crmUser.setUser(crmUsers.get(crmUser.getIamId()));
        }
    }
}
