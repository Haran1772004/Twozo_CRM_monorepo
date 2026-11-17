package com.tlc.crm.common.iam.action;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.CrmSecureAction;
import com.tlc.crm.common.iam.api.manager.RoleManager;
import com.tlc.crm.common.iam.api.model.converter.CrmUserConverter;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.preferences.manager.UserPreferenceManager;
import com.tlc.crm.common.iam.field.CrmUserFields;
import com.tlc.crm.common.iam.field.preferences.PreferenceFields;
import com.tlc.crm.common.iam.internal.IamErrorCodes;
import com.tlc.crm.common.iam.service.OrganizationService;

import com.tlc.iam.client.UserService;
import com.tlc.iam.common.models.User;

import com.tlc.web.HttpMethod;
import com.tlc.web.WebAction;
import com.tlc.web.client.event.EventCallback;

/**
 * <p>
 * Handles the web action related to {@link CrmUser} details
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
@WebAction(path = "/accounts/currentUser", method = HttpMethod.GET)
public class LoggedInUserInfoAction extends CrmSecureAction {

    private final UserService userService;
    private final OrganizationService organizationService;
    private final com.tlc.crm.common.iam.service.UserService crmUserService;
    private final CrmUserConverter crmUserConverter;
    private final UserPreferenceManager preferenceManager;
    private final RoleManager roleManager;

    public LoggedInUserInfoAction() {
        userService = UserService.getInstance();
        organizationService = OrganizationService.getInstance();
        crmUserService = com.tlc.crm.common.iam.service.UserService.getInstance();
        crmUserConverter = CrmUserConverter.getInstance();
        preferenceManager = UserPreferenceManager.getInstance();
        roleManager = RoleManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request     Holds the {@link CrmRequest} details
     * @param crmResponse Represents the {@link CrmResponse} details
     */
    @Override
    public void sProcess(final CrmRequest request, final CrmResponse crmResponse) {
        final Long orgId = request.orgId();
        final Long userId = request.userId();

        crmResponse.convertIntoAsync();
        final EventCallback<User> usersFetchCallBack = new EventCallback<>() {

            @Override
            public void onSuccess(final User user) {
                final JsonObject userJson = crmUserConverter.convert(orgId, userId, user);

                userJson.put(PreferenceFields.PREFERENCES.getName(), preferenceManager.get(orgId, userId).toJson(true));
                userJson.put(CrmUserFields.IS_ADMIN.getName(), roleManager.isAdminRole(orgId, request.getUser().getRoleId()));
                crmResponse.put(CrmUserFields.ORGANIZATION.getName(), userJson.getJsonObject(CrmUserFields.ORGANIZATION.getName()));
                crmResponse.put(CrmUserFields.USER.getName(), userJson.remove(CrmUserFields.ORGANIZATION.getName()));
                crmResponse.send();
            }

            @Override
            public void onFailure(final Throwable throwable) {
                sendError(crmResponse, null, ErrorCode.get(IamErrorCodes.USER_RETRIEVAL_FAILED, "i18n.common.user.retrieval.failed", throwable));
            }
        };
        userService.fetchUser(organizationService.getIamOrgId(orgId), crmUserService.getUser(orgId, userId).getIamId(), usersFetchCallBack);
    }
}
