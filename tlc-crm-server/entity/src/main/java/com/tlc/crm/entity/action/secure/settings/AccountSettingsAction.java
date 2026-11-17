package com.tlc.crm.entity.action.secure.settings;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.AuthorizationController;
import com.tlc.crm.common.iam.api.manager.CrmUserManager;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.internal.CrmUserEntity;
import com.tlc.crm.common.iam.service.OrganizationService;
import com.tlc.crm.entity.models.AccountDetails;

import com.tlc.crm.entity.fields.settings.AccountSettingsFields;
import com.tlc.iam.client.UserService;
import com.tlc.iam.common.models.Organization;
import com.tlc.iam.common.models.User;
import com.tlc.iam.common.status.IamErrorCodes;
import com.tlc.web.WebAction;
import com.tlc.web.client.event.EventCallback;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * Handles the account settings related web actions.
 *
 * @author Maharaja S
 * @version 1.0
 */
@WebAction(path = "/account/settings/mgmt")
public class AccountSettingsAction extends AuthorizationController {

    private static final String ACCOUNT_SETTINGS_UPDATE_ACTION_FAILED_I18N_KEY = "i18n.account.settings.account.update.failed";

    private final OrganizationService organizationService;
    private final UserService userService;
    private final CrmUserManager crmUserManager;
    private final CrmUserManager secureCrmUserManager;

    public AccountSettingsAction() {
        this.organizationService = OrganizationService.getInstance();
        this.userService = UserService.getInstance();
        this.crmUserManager = CrmUserManager.getInstance();
        this.secureCrmUserManager = CrmUserManager.getSecureInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param crmRequest  Represents the {@link CrmRequest}
     * @param crmResponse Represents the {@link CrmResponse}
     */
    @Override
    protected void authorizedProcess(final CrmRequest crmRequest, final CrmResponse crmResponse) {
        final JsonObject requestJson = crmRequest.getRequestJson();
        final String type = requestJson.getString(AccountSettingsFields.TYPE);
        final JsonObject data = requestJson.getJsonObject(AccountSettingsFields.DATA);
        final long orgId = crmRequest.orgId();

        switch (type) {
            case "update" -> update(orgId, convertJsonToModel(orgId, crmRequest.userId(), data), crmResponse);
            case "get" -> fetchAndDispatchAccountDetails(orgId, crmResponse);
        }
    }

    /**
     * <p>
     * Fetches and dispatches the account details of the organization.
     * </p>
     *
     * @param orgId       Represents the organization id.
     * @param crmResponse Represents the {@link CrmResponse}.
     */
    private void fetchAndDispatchAccountDetails(final long orgId, final CrmResponse crmResponse) {
        final Long iamOrgId = organizationService.getIamOrgId(orgId);
        final Long orgAccountCreatorId = crmUserManager.getOrgAccountCreatorId(orgId);

        if (Objects.nonNull(orgAccountCreatorId)) {
            crmResponse.convertIntoAsync();
            final EventCallback<User> userFetchCallback = new EventCallback<>() {

                @Override
                public void onSuccess(final User user) {
                    final AccountDetails accountDetails = new AccountDetails(orgId, user.id(), user.getFirstName(),
                            user.getOrganization().getName(), user.getMobile(), user.getEmail());

                    convert(accountDetails).forEach(crmResponse::put);
                    crmResponse.send();
                }

                @Override
                public void onFailure(final Throwable throwable) {
                    sendError(crmResponse, null, ErrorCode.get(IamErrorCodes.USER_RETRIEVAL_FAILED, "i18n.iam.user.retrieval.failed"));
                }
            };

            userService.fetchUser(iamOrgId, orgAccountCreatorId, userFetchCallback);
        } else {
            throw ErrorCode.get(IamErrorCodes.USER_NOT_FOUND, "i18n.account.settings.user.not.found");
        }
    }

    /**
     * <p>
     * Updates the account details of the organization.
     * </p>
     *
     * @param orgId          Represents the organization id.
     * @param crmResponse    Represents the {@link CrmResponse}.
     * @param accountDetails Represents the dto which contains the account details.
     */
    private void update(final long orgId, final AccountDetails accountDetails, final CrmResponse crmResponse) {
        final long iamOrgId = organizationService.getIamOrgId(orgId);
        final Long orgAccountCreatorId = crmUserManager.getOrgAccountCreatorId(orgId);

        if (Objects.nonNull(orgAccountCreatorId)) {
            crmResponse.convertIntoAsync();
            final EventCallback<User> userFetchCallback = new EventCallback<>() {
                @Override
                public void onSuccess(final User user) {
                    final User userToUpdate = new User();

                    setUserData(iamOrgId, userToUpdate, accountDetails, user);
                    final EventCallback<JsonObject> userUpdateCallback = new EventCallback<>() {

                        /**
                         * {@inheritDoc}
                         *
                         * @param jsonObject Represents the response data from the consumer.
                         */
                        @Override
                        public void onSuccess(final JsonObject jsonObject) {
                            final String errorKey = AccountSettingsFields.ERROR;
                            final String errorsKey = AccountSettingsFields.ERRORS;

                            if (jsonObject.containsKey(errorKey)) {
                                crmResponse.put(errorKey, crmResponse.resolveI18nString(jsonObject.getString(errorKey)));
                                sendError(crmResponse, null, ErrorCode.get(IamErrorCodes.UPDATE_ACTION_FAILED, ACCOUNT_SETTINGS_UPDATE_ACTION_FAILED_I18N_KEY));
                            } else if (jsonObject.containsKey(errorsKey)) {
                                final JsonArray errorJsonArray = jsonObject.getJsonArray(errorsKey);
                                final Collection<String> errors = new ArrayList<>();

                                for (int index = 0; index < errorJsonArray.size(); index++) {
                                    errors.add(crmResponse.resolveI18nString(errorJsonArray.getString(index)));
                                }
                                crmResponse.put(errorsKey, crmResponse.resolveI18nString(errors));
                                sendError(crmResponse, null, ErrorCode.get(IamErrorCodes.UPDATE_ACTION_FAILED, ACCOUNT_SETTINGS_UPDATE_ACTION_FAILED_I18N_KEY));
                            } else {
                                final CrmUser crmUser = new CrmUserEntity();

                                crmUser.setUser(userToUpdate);
                                secureCrmUserManager.upsert(false, crmUser);
                                crmResponse.put(AccountSettingsFields.MESSAGE, crmResponse.resolveI18nString("i18n.account.settings.account.update.successful"));
                            }
                            crmResponse.send();
                        }

                        /**
                         * {@inheritDoc}
                         *
                         * @param throwable Represents the cause of the failure.
                         */
                        @Override
                        public void onFailure(final Throwable throwable) {
                            sendError(crmResponse, null, ErrorCode.get(IamErrorCodes.USER_UPDATE_FAILED, ACCOUNT_SETTINGS_UPDATE_ACTION_FAILED_I18N_KEY));
                        }
                    };

                    userService.updateAccountDetails(iamOrgId, userToUpdate, userUpdateCallback);
                }

                /**
                 * {@inheritDoc}
                 *
                 * @param throwable Represents the cause of the failure.
                 */
                @Override
                public void onFailure(final Throwable throwable) {
                    sendError(crmResponse, null, ErrorCode.get(IamErrorCodes.USER_RETRIEVAL_FAILED, "i18n.iam.user.retrieval.failed"));
                }
            };

            userService.fetchUser(iamOrgId, orgAccountCreatorId, userFetchCallback);
        } else {
            throw ErrorCode.get(IamErrorCodes.USER_NOT_FOUND, "i18n.account.settings.user.not.found");
        }
    }

    /**
     * <p>
     * Converts the {@link AccountDetails} object into a json object.
     * </p>
     *
     * @param accountDetails Represents the {@link AccountDetails} to be converted.
     * @return the {@link JsonObject} containing the account details.
     */
    private JsonObject convert(final AccountDetails accountDetails) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(AccountSettingsFields.NAME, accountDetails.name());
        jsonObject.put(AccountSettingsFields.MOBILE, accountDetails.mobile());
        jsonObject.put(AccountSettingsFields.EMAIL, accountDetails.email());
        jsonObject.put(AccountSettingsFields.ORGANIZATION_NAME, accountDetails.organizationName());

        return jsonObject;
    }

    /**
     * <p>
     * Converts the json object into {@link AccountDetails}
     * </p>
     *
     * @param orgId     Represents the organization id.
     * @param crmUserId Represents the crm user id.
     * @param data      Represents the json data.
     * @return the {@link AccountDetails}.
     */
    private AccountDetails convertJsonToModel(final long orgId, final long crmUserId, final JsonObject data) {
        final String name = data.optString(AccountSettingsFields.NAME, null);
        final String mobile = data.optString(AccountSettingsFields.MOBILE, null);
        final String organizationName = data.optString(AccountSettingsFields.ORGANIZATION_NAME, null);

        return new AccountDetails(orgId, crmUserId, name, organizationName, mobile);
    }

    /**
     * <p>
     * Sets the {@link AccountDetails} details into {@link User}.
     * </p>
     *
     * @param iamOrgId       Represent the IAM organization id.
     * @param userToUpdate   Represents the user to be updated.
     * @param accountDetails Represents the dto that contains the account details.
     * @param user           Represents the existing user.
     */
    private void setUserData(final long iamOrgId, final User userToUpdate, final AccountDetails accountDetails, final User user) {
        final String name = accountDetails.name();
        final String mobile = accountDetails.mobile();
        final String organizationName = accountDetails.organizationName();

        userToUpdate.setFirstName(Objects.nonNull(name) ? name : user.getFirstName());
        userToUpdate.setMobile(Objects.nonNull(mobile) ? mobile : user.getMobile());
        userToUpdate.setId(user.id());
        userToUpdate.setOrgId(iamOrgId);

        if (Objects.nonNull(organizationName)) {
            final Organization organization = new Organization();

            organization.setId(iamOrgId);
            organization.setName(organizationName);
            userToUpdate.setOrganization(organization);
        }
    }
}