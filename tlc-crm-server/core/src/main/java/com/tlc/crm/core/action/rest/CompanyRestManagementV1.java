package com.tlc.crm.core.action.rest;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.rest.CrmSecureRestAction;
import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.event.model.SecureEventData;
import com.tlc.crm.common.iam.api.manager.CrmUserManager;
import com.tlc.crm.common.iam.api.manager.TerritoryManager;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.Territory;
import com.tlc.crm.common.models.Action;

import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.converter.CompanyConverter;
import com.tlc.crm.core.formatter.CompanyRestReqFormatter;
import com.tlc.crm.core.internal.contact.status.ContactErrorCodes;

import com.tlc.crm.entity.builder.ConverterConfigBuilder;
import com.tlc.crm.entity.fields.FieldKeys;

import com.tlc.iam.common.models.User;

import com.tlc.validator.TlcModel;

import com.tlc.web.HttpMethod;
import com.tlc.web.undertow.rest.annotation.ApiRoute;
import com.tlc.web.undertow.rest.annotation.RestApiHandler;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * Manages {@link Company} related REST web actions.
 * </p>
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
@RestApiHandler(path = "/v1/company")
public class CompanyRestManagementV1 extends CrmSecureRestAction<Company> {

    private final CompanyRestReqFormatter companyRestReqFormatter;
    private final CompanyConverter companyConverter;
    private final CompanyManager companyManager;
    private final ConverterConfigBuilder converterConfigBuilder;

    private final CrmUserManager crmUserManager;
    private final TerritoryManager territoryManager;

    public CompanyRestManagementV1() {
        this.companyRestReqFormatter = CompanyRestReqFormatter.getInstance();
        this.companyConverter = CompanyConverter.getInstance();
        this.companyManager = CompanyManager.getInstance();
        this.converterConfigBuilder = ConverterConfigBuilder.getInstance();
        this.crmUserManager = CrmUserManager.getInstance();
        this.territoryManager = TerritoryManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request  The CRM request containing path parameters, query parameters,
     *                 headers, and contextual metadata.
     * @param response The CRM response used to write the result or error payload.
     */
    @ApiRoute(path = "/{id}", method = HttpMethod.GET)
    @Override
    public void get(final CrmRequest request, final CrmResponse response) {
        final long orgId = request.orgId();
        final long id = Long.parseLong(request.getParameter("id"));

        if (companyManager.exists(orgId, id)) {
            fetchAndDispatchUser(request, companyManager.get(request.orgId(), id), response);
        } else {
            throw ErrorCode.get(ContactErrorCodes.COMPANY_NOT_FOUND, "i18n.company.not.found");
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param request  The CRM request containing the payload and request metadata.
     * @param response The CRM response used to send the updated resource or status result.
     */
    @ApiRoute(path = "/{id}", method = HttpMethod.PUT)
    @Override
    public void put(final CrmRequest request, final CrmResponse response) {
        final String idKey = FieldKeys.ID.getName();
        final JsonObject requestJson = request.getRequestJson();
        final EventData eventData = new SecureEventData(request.orgId(), request.userId());

        eventData.setLocale(request.locale());
        final Collection<TlcModel> currentModels = getCurrentModels(request);

        if (!currentModels.isEmpty()) {
            eventData.setPreviousValues(currentModels);
        }

        requestJson.put(idKey, Long.parseLong(request.getParameter(idKey)));
        final Company company = convert(request, request.getRequestJson());

        update(response, request.orgId(), request.userId(), eventData, List.of(company));
    }

    @Override
    public void getAll(final CrmRequest request, final CrmResponse response) {
    }

    /**
     * {@inheritDoc}
     *
     * @param request  The CRM request containing the payload of the resource to be created.
     * @param response The CRM response used to send the created resource or creation status.
     */
    @ApiRoute(method = HttpMethod.POST)
    @Override
    public void post(final CrmRequest request, final CrmResponse response) {
        final long orgId = request.orgId();
        final long userId = request.userId();
        final Company model = convert(request, request.getRequestJson());
        final EventData eventData = new SecureEventData(orgId, userId);

        eventData.setLocale(request.locale());
        create(response, orgId, userId, eventData, List.of(model));
    }

    /**
     * {@inheritDoc}
     *
     * @param request  The CRM request containing the partial update payload.
     * @param response The CRM response used to return the modified resource or status details.
     */
    @ApiRoute(path = "/{id}", method = HttpMethod.PATCH)
    @Override
    public void patch(final CrmRequest request, final CrmResponse response) {
        final String idKey = FieldKeys.ID.getName();
        final JsonObject requestJson = request.getRequestJson();
        final EventData eventData = new SecureEventData(request.orgId(), request.userId());

        eventData.setLocale(request.locale());
        final Collection<TlcModel> currentModels = getCurrentModels(request);

        if (!currentModels.isEmpty()) {
            eventData.setPreviousValues(currentModels);
        }

        requestJson.put(idKey, Long.parseLong(request.getParameter(idKey)));
        update(response, request.orgId(), request.userId(), eventData, List.of(convertPartialData(request, requestJson)));
    }

    /**
     * {@inheritDoc}
     *
     * @param request  The CRM request containing identifiers or parameters required to locate the resource.
     * @param response The CRM response used to send deletion confirmation or error details.
     */
    @ApiRoute(path = "/{id}", method = HttpMethod.DELETE)
    @Override
    public void delete(final CrmRequest request, final CrmResponse response) {
        final EventData eventData = new SecureEventData(request.orgId(), request.userId());

        eventData.setLocale(request.locale());
        final Collection<TlcModel> currentModels = getCurrentModels(request);

        if (!currentModels.isEmpty()) {
            eventData.setPreviousValues(currentModels);
        }
        delete(response, request.orgId(), request.userId(), eventData, List.of(companyManager.get(request.orgId(), Long.parseLong(request.getParameter("id")))));
    }

    /**
     * {@inheritDoc}
     *
     * @param request  The CRM request containing identifiers or parameters required to locate the resource.
     * @param response The CRM response used to send deletion confirmation or error details.
     */
    @Override
    public void options(final CrmRequest request, final CrmResponse response) {
    }

    /**
     * {@inheritDoc}
     *
     * @param request    The CRM request containing contextual information.
     * @param jsonObject The JSON payload to be converted.
     * @return
     */
    @Override
    protected Company convert(final CrmRequest request, final JsonObject jsonObject) {
        final JsonObject companyJson = companyRestReqFormatter.format(request.orgId(), jsonObject, request.i18nResolver());
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, false, companyJson);

        return companyConverter.convert(converterConfig, companyJson);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ConfigManager<Company> getConfigManager() {
        return companyManager;
    }

    /**
     * {@inheritDoc}
     *
     * @param request    The request data.
     * @param jsonObject The request json.
     * @return The company.
     */
    @Override
    protected Company convertPartialData(final CrmRequest request, final JsonObject jsonObject) {
        final JsonObject companyJson = companyRestReqFormatter.format(request.orgId(), jsonObject, request.i18nResolver());
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, true, companyJson);

        return companyConverter.convert(converterConfig, companyJson);
    }

    /**
     * <p>
     * Sets the {@link CrmUser} details into {@link Company}
     * </p>
     *
     * @param company Represents the company details
     * @param users   Contains the collection of {@link User}
     */
    @Override
    protected void setUser(final Company company, final Collection<User> users) {
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

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObject convert(final CrmRequest crmRequest, final Company company) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(crmRequest, false, null);

        return companyConverter.convertToRestJson(company, converterConfig);
    }

    /**
     * {@inheritDoc}
     *
     * @param request Represents the {@link CrmRequest}
     * @return The collection of models.
     */
    @Override
    public Collection<TlcModel> getCurrentModels(final CrmRequest request) {
        final long id = Long.parseLong(request.getParameter("id"));
        final long orgId = request.orgId();
        final Company company = companyManager.get(orgId, id);

        companyManager.loadSummaryDetails(orgId, company);
        return List.of(company);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId     Represents the organization id
     * @param userId    Represents the user id.
     * @param action    which contains data
     * @param companies Collection of {@link Company}
     * @return boolean Return true means is valid details
     */
    @Override
    protected final boolean isValidDetails(final Long orgId, final Long userId, final Action action, final Collection<Company> companies) {
        if (Objects.nonNull(companies) && !companies.isEmpty()) {
            final Collection<Long> parentCompanyIds = companies.stream().map(Company::getParentCompany)
                    .filter(parentCompany -> Objects.nonNull(parentCompany) && Objects.nonNull(parentCompany.id()))
                    .map(Company::id).collect(Collectors.toUnmodifiableSet());

            if (!parentCompanyIds.isEmpty() && !companyManager.exists(orgId, parentCompanyIds)) {
                throw ErrorCode.get(ContactErrorCodes.COMPANY_NOT_FOUND, "i18n.company.invalid.parentCompanyId");
            }

            switch (action) {
                case CREATE, UPDATE -> {

                    if (Objects.requireNonNull(action) == com.tlc.crm.common.models.Action.UPDATE) {
                        final Collection<Long> companyIds = companies.stream().map(Company::id).collect(Collectors.toSet());
                        final Collection<Long> salesOwnerIds = new HashSet<>();
                        final Collection<Long> territoryIds = new HashSet<>();

                        for (final Company company : companies) {
                            final Territory territory = company.getTerritory();

                            if (Objects.nonNull(territory)) {
                                territoryIds.add(territory.id());
                            }
                            salesOwnerIds.add(company.getSalesOwner().id());
                        }

                        if (!companyManager.exists(orgId, companyIds)) {
                            throw ErrorCode.get(ContactErrorCodes.COMPANY_NOT_FOUND, "i18n.company.not.found");
                        }

                        if (!crmUserManager.exists(orgId, salesOwnerIds)) {
                            throw ErrorCode.get(ContactErrorCodes.SALES_OWNER_NOT_FOUND, "i18n.company.salesOwner.not.found");
                        }

                        if (!territoryIds.isEmpty() && !territoryManager.exists(orgId, territoryIds)) {
                            throw ErrorCode.get(ContactErrorCodes.TERRITORY_NOT_FOUND, "i18n.company.territory.not.found");
                        }
                    } else {
                        final Collection<Long> salesOwnerIds = new HashSet<>();
                        final Collection<Long> territoryIds = new HashSet<>();

                        for (final Company company : companies) {
                            final Territory territory = company.getTerritory();

                            if (Objects.nonNull(territory)) {
                                territoryIds.add(territory.id());
                            }
                            salesOwnerIds.add(company.getSalesOwner().id());
                        }

                        if (!crmUserManager.exists(orgId, salesOwnerIds)) {
                            throw ErrorCode.get(ContactErrorCodes.SALES_OWNER_NOT_FOUND, "i18n.company.salesOwner.not.found");
                        }

                        if (!territoryIds.isEmpty() && !territoryManager.exists(orgId, territoryIds)) {
                            throw ErrorCode.get(ContactErrorCodes.TERRITORY_NOT_FOUND, "i18n.company.territory.not.found");
                        }
                    }
                }
            }
        }

        return true;
    }
}
