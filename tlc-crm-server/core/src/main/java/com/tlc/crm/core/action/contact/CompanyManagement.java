package com.tlc.crm.core.action.contact;

import com.tlc.attachment.model.profilePicture.DisplayPicture;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.fields.ActionFields;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.SummaryOverview;
import com.tlc.crm.security.permission.annotation.authorizer.FollowerAuthorizer;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.crm.security.permission.annotation.authorizer.SourceAuthorizer;
import com.tlc.crm.common.iam.api.manager.CrmUserManager;
import com.tlc.crm.common.iam.api.manager.TerritoryManager;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.Territory;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.contact.model.converter.CompanyConverter;
import com.tlc.crm.core.api.contact.model.converter.ContactConverter;
import com.tlc.crm.core.field.contact.CompanyFields;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.core.field.deal.ParticipantFields;
import com.tlc.crm.core.internal.contact.status.ContactErrorCodes;
import com.tlc.crm.entity.action.EntityConfigAction;
import com.tlc.crm.entity.builder.ConverterConfigBuilder;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.iam.common.models.User;
import com.tlc.validator.TlcModel;
import com.tlc.web.WebAction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * Manages {@link Company} related web actions
 * </p>
 *
 * @author Selvakumar
 * @version 1.0
 * @see EntityConfigAction
 */
@FollowerAuthorizer(subCategory = SubCategory.Type.COMPANY_FOLLOWERS_RECORDS)
@SourceAuthorizer(subCategory = SubCategory.Type.COMPANIES, validateFieldPermission = true)
@WebAction(path = "/contact/company/mgmt")
public class CompanyManagement extends EntityConfigAction<Company> {

    public static final String CONFIG_ACTION_NAME = "company";
    private static final int DEFAULT_START_VALUE = 1;
    private static final int DEFAULT_LIMIT = 50;

    protected final CompanyConverter companyConverter;

    private final ConverterConfigBuilder converterConfigBuilder;
    private final CompanyManager companyManager;
    private final FieldService fieldService;
    private final ContactManager contactManager;
    private final ContactConverter contactConverter;
    private final CrmUserManager crmUserManager;
    private final TerritoryManager territoryManager;

    public CompanyManagement() {
        converterConfigBuilder = ConverterConfigBuilder.getInstance();
        companyConverter = CompanyConverter.getInstance();
        fieldService = FieldService.getInstance();
        companyManager = CompanyManager.getInstance();
        contactManager = ContactManager.getInstance();
        contactConverter = ContactConverter.getInstance();
        crmUserManager = CrmUserManager.getInstance();
        territoryManager = TerritoryManager.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleRequest(final CrmRequest request, final CrmResponse response) {
        final JsonObject requestJson = request.getRequestJson();
        final String type = requestJson.getString(CompanyFields.TYPE);
        final JsonObject data = requestJson.getJsonObject(CompanyFields.DATA);
        final Long orgId = request.orgId();

        switch (type) {
            case "summary" -> summary(orgId, data, request, response);
            case "summaryV2" -> summaryV2(orgId, data, request, response);
            case "getRelatedContacts" -> response.put(CompanyFields.CONTACTS, getRelatedContacts(orgId, data, new ConverterConfig(false, false), response));
            case "linkContacts" -> linkContacts(orgId, data, response);
            case "profile" -> profile(orgId, data, request, response);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param request  Represents the {@link CrmRequest}
     * @param response Represents the {@link CrmResponse}
     * @throws Exception if an error occurs during the request processing
     */
    @Override
    protected void handleMobileRequest(final CrmRequest request, final CrmResponse response) throws Exception {
        final JsonObject requestJson = request.getRequestJson();
        final String type = requestJson.getString(CompanyFields.TYPE);
        final JsonObject data = requestJson.getJsonObject(CompanyFields.DATA);
        final Long orgId = request.orgId();

        switch (type) {
            case "getRelatedContacts" -> response.put(CompanyFields.CONTACTS, getRelatedContacts(orgId, data, new ConverterConfig(false, true), response));
            case "summary" -> summary(data, request, response);
            case "summaryV2" -> summaryV2(orgId, data, request, response);
            case "socialProfile" -> socialProfile(orgId, data, request, response);
            default -> authorizedProcess(request, response);
        }
    }

    /**
     * <p>
     * Links the given contacts with company
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param jsonObject  Represents the {@link JsonObject}
     * @param crmResponse Represents the {@link CrmResponse}
     */
    private void linkContacts(final long orgId, final JsonObject jsonObject, final CrmResponse crmResponse) {
        final Collection<Long> contactIds = new ArrayList<>();
        final JsonArray jsonArray = jsonObject.getJsonArray(CompanyFields.CONTACT_IDS);
        final Long companyId = jsonObject.getLong(CompanyFields.COMPANY_ID);

        if (companyManager.exists(orgId, companyId)) {

            for (int index = 0; index < jsonArray.size(); index++) {
                contactIds.add(jsonArray.getLong(index));
            }

            if (!contactIds.isEmpty() && !contactManager.exists(orgId, contactIds)) {
                throw ErrorCode.get(ContactErrorCodes.CONTACT_NOT_FOUND, "i18n.contact.not.found");
            } else {
                companyManager.linkContactsToCompany(orgId, companyId, contactIds);
                crmResponse.put(CompanyFields.MESSAGE, crmResponse.resolveI18nString("i18n.contact.linked.successfully"));
            }
        } else {
            throw ErrorCode.get(ContactErrorCodes.COMPANY_NOT_FOUND, "i18n.company.not.found");
        }
    }

    /**
     * <p>
     * Fetches the {@link Company} related summary fields
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param data     Represents the request {@link JsonObject}
     * @param request  Represents the {@link CrmRequest} data
     * @param response Represents the {@link CrmResponse} to be sent
     */
    private void summary(final long orgId, final JsonObject data, final CrmRequest request, final CrmResponse response) {
        final Company company = get(orgId, data.getLong(CompanyFields.ID));

        companyManager.loadSummaryDetails(orgId, company);
        fetchAndDispatchSummary(request, company, data, response);
    }

    /**
     * <p>
     * Fetches the {@link Company} related summary fields
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param data     Represents the request {@link JsonObject}
     * @param request  Represents the {@link CrmRequest} data
     * @param response Represents the {@link CrmResponse} to be sent
     */
    private void summaryV2(final long orgId, final JsonObject data, final CrmRequest request, final CrmResponse response) {
        final Company company = get(orgId, data.getLong(CompanyFields.ID));

        companyManager.loadSummaryInfo(orgId, company);
        fetchAndDispatchSummaryV2(request, company, data, response);
    }

    /**
     * <p>
     * Fetches the {@link Company} related summary fields
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param data     Represents the request {@link JsonObject}
     * @param request  Represents the {@link CrmRequest} data
     * @param response Represents the {@link CrmResponse} to be sent
     */
    private void profile(final long orgId, final JsonObject data, final CrmRequest request, final CrmResponse response) {
        final long companyId = data.getLong(CompanyFields.ID);

        if (companyManager.exists(orgId, companyId)) {
            final Company company = companyManager.getProfile(orgId, companyId);

            fetchAndDispatchProfile(request, company, data, response);
        } else {
            throw ErrorCode.get(ContactErrorCodes.COMPANY_NOT_FOUND, "i18n.company.not.found");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompanyManager getConfigManager() {
        return companyManager;
    }

    /**
     * {@inheritDoc}
     */
    public Company convert(final CrmRequest request, final JsonObject jsonObject) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, false, jsonObject);

        return companyConverter.convert(converterConfig, jsonObject);
    }

    /**
     * {@inheritDoc}
     */
    public JsonObject convert(final CrmRequest crmRequest, final Company company) {
        final ConverterConfig converterConfig = ConverterConfigBuilder.getInstance().build(crmRequest, false, null);

        return companyConverter.convertModelToJson(company, converterConfig);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Company convertPartialData(CrmRequest request, JsonObject jsonObject) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, true, jsonObject);

        return companyConverter.convert(converterConfig, jsonObject);
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

    /**
     * <p>
     * Gets the related {@link Contact} by using given {@link Company} id
     * </p>
     *
     * @param orgId           Represents the organization id
     * @param data            Which contains the request data
     * @param converterConfig Represents the {@link ConverterConfig}
     * @param response        Represents the {@link CrmResponse} data
     */
    private JsonArray getRelatedContacts(final long orgId, final JsonObject data, final ConverterConfig converterConfig, final CrmResponse response) {
        final JsonArray contactArray = Json.array();
        final long companyId = data.getLong(ContactFields.ID);
        final int start = Math.max(data.optInt(CompanyFields.START, DEFAULT_START_VALUE), DEFAULT_START_VALUE);
        final int limit = Math.min(data.optInt(CompanyFields.LIMIT, DEFAULT_LIMIT), DEFAULT_LIMIT);
        final String companyKey = ContactFields.COMPANY;
        final long companyFieldId = fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE, companyKey).get(companyKey).id();
        final long totalRecords = contactManager.getRelatedContactsCount(orgId, companyId, companyFieldId);

        if (totalRecords > 0 && start <= totalRecords) {
            final Collection<Contact> contacts = contactManager.getRelatedContactsByCompany(orgId, companyId, companyFieldId, start, limit);

            if (Objects.nonNull(contacts)) {

                for (final Contact contact : contacts) {
                    final JsonObject contactJson = contactConverter.convertRelatedContactToJson(contact, converterConfig);

                    if (converterConfig.isMobileClient()) {
                        final DisplayPicture displayPicture = contact.getThumbnail();

                        if (Objects.nonNull(displayPicture)) {
                            contactJson.put(ContactFields.THUMBNAIL, companyConverter.convertThumbnailToJson(contact.id(), Contact.ENTITY_TYPE, displayPicture));
                        }
                    }
                    contactArray.put(contactJson);
                }
            }
        }

        response.put(ParticipantFields.TOTAL_RECORDS.getName(), totalRecords);
        response.put(ParticipantFields.HAS_MORE.getName(), (start + limit) <= totalRecords);

        return contactArray;
    }

    /**
     * {@inheritDoc}
     *
     * @param crmRequest Holds the {@link CrmRequest} details
     * @param company    Represents the entity model
     * @param data       Which contains the request data
     * @param users      Contains the collection of users
     * @return The {@link JsonObject} details
     */
    protected JsonObject convertSummary(final CrmRequest crmRequest, final Company company, final JsonObject data, final Collection<User> users) {
        if (!users.isEmpty()) {
            setUser(company, users);
        }

        return companyConverter.summary(company, converterConfigBuilder.build(crmRequest, true, data, true));
    }

    /**
     * {@inheritDoc}
     *
     * @param crmRequest Holds the {@link CrmRequest} details
     * @param company    Represents the {@link Company}
     * @param data       Which contains the request data
     * @param users      Contains the collection of users
     * @return The {@link JsonObject} details
     */
    @Override
    protected JsonObject convertSummaryV2(final CrmRequest crmRequest, final Company company, final JsonObject data, final Collection<User> users) {
        return companyConverter.summaryV2(crmRequest.orgId(), crmRequest.userId(), company, converterConfigBuilder.build(crmRequest, true, data, true));
    }

    /**
     * {@inheritDoc}
     *
     * @param crmRequest Holds the {@link CrmRequest} details
     * @param company    Represents the {@link Company}
     * @param data       Which contains the request data
     * @param users      Contains the collection of users
     * @return The {@link JsonObject} details
     */
    @Override
    protected JsonObject convertProfile(final CrmRequest crmRequest, final Company company, final JsonObject data, final Collection<User> users) {
        return companyConverter.profile(company, converterConfigBuilder.build(crmRequest, true, data, true));
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
     *
     * @param request Represents the {@link CrmRequest}
     * @return The collection of models.
     */
    @Override
    public Collection<TlcModel> getCurrentModels(final CrmRequest request) {
        final JsonObject requestJson = request.getRequestJson();
        final String type = requestJson.getString(ActionFields.TYPE);
        final JsonObject data = requestJson.getJsonObject(ActionFields.DATA);
        final long orgId = request.orgId();

        return switch (type) {
            case "update", "pUpdate", "delete" -> {
                if (data.containsKey(ActionFields.ID)) {
                    final Company company = companyManager.get(orgId, data.getLong(ActionFields.ID));

                    companyManager.loadSummaryDetails(orgId, company);
                    yield List.of(company);
                }
                yield Collections.emptyList();
            }
            default ->  Collections.emptyList();
        };
    }

    /**
     * <p>
     * Fetches the {@link Company} related summary fields for mobile view
     * </p>
     *
     * @param jsonObject  Represents the request {@link JsonObject}
     * @param crmRequest  Represents the {@link CrmRequest} data
     * @param crmResponse Represents the {@link CrmResponse} to be sent
     */
    private void summary(final JsonObject jsonObject, final CrmRequest crmRequest, final CrmResponse crmResponse) {
        final long orgId = crmRequest.orgId();
        final Company company = get(orgId, jsonObject.getLong(CompanyFields.ID));

        companyManager.loadSummaryDetails(orgId, company);
        fetchAndDispatchSummary(crmRequest, jsonObject, company, crmResponse);
    }

    /**
     * {@inheritDoc}
     *
     * @param crmRequest Holds the {@link CrmRequest} details
     * @param jsonObject Which contains the request data
     * @param company    Represents the entity model
     * @param users      Contains the collection of users
     * @return The {@link JsonObject} details
     */
    @Override
    protected JsonObject convertSummary(final CrmRequest crmRequest, final JsonObject jsonObject, final Company company, final Collection<User> users) {
        if (!users.isEmpty()) {
            setUser(company, users);
        }

        return companyConverter.convertSummaryIntoJson(company, converterConfigBuilder.build(crmRequest, true, jsonObject, true), jsonObject.optBoolean(FieldKeys.IS_HIDDEN.getName(), false));
    }

    /**
     * {@inheritDoc}
     *
     * @return The config action name
     */
    @Override
    protected String getConfigActionName() {
        return CONFIG_ACTION_NAME;
    }

    /**
     * Sets the response with company overview details such as activity type and active users etc.
     *
     * @param orgId    Refers the organization id.
     * @param data     Refers the request json.
     * @param response Refers the {@link CrmResponse} to be sent.
     */
    @Override
    public void overview(final long orgId, final JsonObject data, final CrmResponse response) {
        final long companyId = data.getLong(CompanyFields.ID);

        if (companyManager.exists(orgId, companyId)) {
            final SummaryOverview summaryOverview = companyManager.getSummaryOverview(orgId, companyId, Source.COMPANY);

            fetchUsersForOverview(orgId, summaryOverview, response);
        } else {
            throw ErrorCode.get(ContactErrorCodes.COMPANY_NOT_FOUND, "i18n.company.not.found");
        }
    }

    /**
     * <p>
     * Fetches the company profile details
     * </p>
     *
     * @param orgId    Refers the organization id.
     * @param data     Refers the request json data.
     * @param request  Refers the crm request object.
     * @param response Refers the response to be sent.
     */
    private void socialProfile(final Long orgId, final JsonObject data, final CrmRequest request, final CrmResponse response) {
        final long id = data.getLong(CompanyFields.ID);

        if (companyManager.exists(orgId, id)) {
            companyConverter.convertSocialProfile(companyManager.getSocialProfile(orgId, id),
                    converterConfigBuilder.build(request, true, data)).forEach(response::put);
        } else {
            throw ErrorCode.get(ContactErrorCodes.COMPANY_NOT_FOUND, "i18n.company.not.found");
        }
    }
}