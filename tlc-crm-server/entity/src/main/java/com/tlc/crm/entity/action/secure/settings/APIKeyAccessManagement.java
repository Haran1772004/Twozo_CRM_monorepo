package com.tlc.crm.entity.action.secure.settings;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.AuthorizationController;
import com.tlc.crm.common.iam.service.OrganizationService;
import com.tlc.crm.common.iam.service.UserService;

import com.tlc.crm.entity.fields.FieldKeys;

import com.tlc.iam.client.ApiKeyAccessService;
import com.tlc.iam.common.fields.IamFields;
import com.tlc.iam.common.fields.ProfilePictureFields;
import com.tlc.iam.common.status.IamErrorCodes;

import com.tlc.web.WebAction;
import com.tlc.web.client.event.EventCallback;

/**
 * <p>
 * Manages the web actions of API keys.
 * </p>
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@WebAction(path = "/settings/api-keys")
public class APIKeyAccessManagement extends AuthorizationController {

    private final ApiKeyAccessService apiKeyAccessService;
    private final UserService crmUserService;
    private final OrganizationService organizationService;

    public APIKeyAccessManagement() {
        this.apiKeyAccessService = ApiKeyAccessService.getInstance();
        this.crmUserService = UserService.getInstance();
        this.organizationService = OrganizationService.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request     The request to be handled.
     * @param crmResponse The response to be sent.
     */
    @Override
    protected void authorizedProcess(final CrmRequest request, final CrmResponse crmResponse) throws Exception {
        final long orgId = request.orgId();
        final long userId = request.userId();
        final JsonObject requestJson = request.getRequestJson();
        final String type = requestJson.getString(FieldKeys.TYPE.getName());
        final JsonObject data = requestJson.getJsonObject(FieldKeys.DATA.getName());

        switch (type) {
            case "generate" -> generate(orgId, userId, data, crmResponse);
            case "get" -> fetchAndDispatchApiKey(orgId, userId, crmResponse);
            case "delete" -> delete(orgId, userId, crmResponse);
        }
    }

    /**
     * Generates a new API key for the specified organization and user and updates
     * the given {@link CrmResponse} accordingly.
     *
     * @param orgId       The organization identifier for which the API key is generated.
     * @param userId      The user identifier associated with the generated API key.
     * @param data        The JSON payload to be enriched with API key details.
     * @param crmResponse The response object to be updated with processing outcomes.
     */
    private void generate(final long orgId, final long userId, final JsonObject data, final CrmResponse crmResponse) {
        final Long iamOrgId = organizationService.getIamOrgId(orgId);
        final Long iamUserId = crmUserService.getUser(orgId, userId).getIamId();

        crmResponse.convertIntoAsync();

        final EventCallback<JsonObject> apiKeyGenerateCallback = new EventCallback<>() {

            @Override
            public void onSuccess(final JsonObject jsonObject) {
                if (jsonObject.containsKey(IamFields.ERROR)) {
                    sendError(crmResponse, null, ErrorCode.get(IamErrorCodes.API_KEY_GENERATION_FAILED, "i18n.iam.api.key.generation.failed"));
                } else {
                    jsonObject.forEach(crmResponse::put);
                    crmResponse.send();
                }
            }

            @Override
            public void onFailure(final Throwable cause) {
                sendError(crmResponse, null, ErrorCode.get(IamErrorCodes.API_KEY_GENERATION_FAILED, "i18n.iam.api.key.generation.failed", cause));
            }
        };
        apiKeyAccessService.generateApikey(iamOrgId, iamUserId, data, apiKeyGenerateCallback);
    }

    /**
     * Fetches the API key for the specified organization and user and updates
     * the given {@link CrmResponse} accordingly.
     *
     * @param orgId       The organization identifier for which the API key is generated.
     * @param userId      The user identifier associated with the generated API key.
     * @param crmResponse The response object to be updated with processing outcomes.
     */
    private void fetchAndDispatchApiKey(final long orgId, final long userId, final CrmResponse crmResponse) {
        final Long iamOrgId = organizationService.getIamOrgId(orgId);
        final Long iamUserId = crmUserService.getUser(orgId, userId).getIamId();

        crmResponse.convertIntoAsync();

        final EventCallback<JsonObject> apiKeyGenerateCallback = new EventCallback<>() {

            @Override
            public void onSuccess(final JsonObject jsonObject) {
                jsonObject.forEach(crmResponse::put);
                crmResponse.send();
            }

            @Override
            public void onFailure(final Throwable cause) {
                sendError(crmResponse, null, ErrorCode.get(IamErrorCodes.FAILED_TO_FETCH_API_KEY, "i18n.iam.api.key.fetch.failed"));
            }
        };
        apiKeyAccessService.fetchApiKey(iamOrgId, iamUserId, apiKeyGenerateCallback);
    }

    /**
     * Deletes the API key for the specified organization and user and updates
     * the given {@link CrmResponse} accordingly.
     *
     * @param orgId       The organization identifier for which the API key is generated.
     * @param userId      The user identifier associated with the generated API key.
     * @param crmResponse The response object to be updated with processing outcomes.
     */
    private void delete(final long orgId, final long userId, final CrmResponse crmResponse) {
        final Long iamOrgId = organizationService.getIamOrgId(orgId);
        final Long iamUserId = crmUserService.getUser(orgId, userId).getIamId();

        crmResponse.convertIntoAsync();

        final EventCallback<JsonObject> apiKeyGenerateCallback = new EventCallback<>() {

            @Override
            public void onSuccess(final JsonObject jsonObject) {
                final String message = jsonObject.optString(ProfilePictureFields.MESSAGE, "");

                switch (message) {
                    case "success" -> crmResponse.put(IamFields.MESSAGE, crmResponse.resolveI18nString("i18n.iam.api.key.deletion.success"));
                    case "failed" -> {
                        crmResponse.setErrorCode(ErrorCode.get(IamErrorCodes.API_KEY_DELETION_FAILED));
                        crmResponse.put(IamFields.MESSAGE, crmResponse.resolveI18nString("i18n.iam.api.key.deletion.failed"));
                    }
                    case "notExists" -> {
                        crmResponse.setErrorCode(ErrorCode.get(IamErrorCodes.API_KEY_NOT_EXISTS));
                        crmResponse.put(IamFields.MESSAGE, crmResponse.resolveI18nString("i18n.iam.api.key.not.exists"));
                    }
                }
                crmResponse.send();
            }

            @Override
            public void onFailure(final Throwable cause) {
                sendError(crmResponse, null, ErrorCode.get(IamErrorCodes.API_KEY_DELETION_FAILED, "i18n.iam.api.key.deletion.failed"));
            }
        };
        apiKeyAccessService.deleteApiKey(iamOrgId, iamUserId, apiKeyGenerateCallback);
    }
}