package com.tlc.crm.common.action.secure;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.action.AbstractCrmAction;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.iam.api.model.CrmOrg;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.field.CrmOrgFields;
import com.tlc.crm.common.iam.handler.OrgInitializationStatusHandler;
import com.tlc.crm.common.iam.handler.OrgInitializationStatusHandler.InternalOrgInitializationStatus;
import com.tlc.crm.common.iam.service.OrganizationService;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.common.internal.service.RequestService;
import com.tlc.crm.common.models.RequestType;

import com.tlc.crm.common.service.DateTimeFieldsResolver;
import com.tlc.crm.common.service.UserPreferenceHolderService;
import com.tlc.crm.security.context.SecurityContext;
import com.tlc.i18n.I18nResolver;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.validator.TlcModel;
import com.tlc.web.Action;
import com.tlc.web.HttpStatus;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Abishek
 * @version 1.0
 */
public abstract class CrmSecureAction extends AbstractCrmAction {

    private static final String TYPE = "type";

    protected final DateTimeFieldsResolver dateTimeFieldsResolver;
    private final UserPreferenceHolderService userPreferenceHolderService;
    private final OrganizationService organizationService;

    public CrmSecureAction() {
        userPreferenceHolderService = UserPreferenceHolderService.get();
        dateTimeFieldsResolver = DateTimeFieldsResolver.get();
        this.organizationService = OrganizationService.getInstance();
    }

    @Override
    public final void process(final CrmRequest request, final CrmResponse crmResponse) throws Exception {
        final List<SecureRequestAuthenticator> requestAuthenticators = getRequestAuthenticators();

        if (requestAuthenticators.isEmpty()) {
            sendError(crmResponse, HttpStatus.UNAUTHORIZED, ErrorCode.get(ErrorCodes.AUTHORIZATION_FAILED, "i18n.common.authorization.failed"));
            return;
        }

        final Optional<SecureRequestAuthenticator> requestAuthenticator = requestAuthenticators.stream()
                .filter(secureRequestAuthenticator -> secureRequestAuthenticator.canHandle(request)).findFirst();

        requestAuthenticator.ifPresentOrElse(authenticator ->
                authenticator.authenticate(request, crmResponse, (isAuthenticate) -> {

                    if (isAuthenticate) {
                        final Long orgId = request.orgId();
                        final CrmUser crmUser = request.getUser();
                        final CrmOrg crmOrg = OrganizationService.getInstance().getCrmOrg(orgId);

                        if (CrmOrg.InitializationStatus.COMPLETED != crmOrg.getInitializationStatus()) {
                            final JsonObject responseData = Json.object();
                            final Optional<InternalOrgInitializationStatus> initializationStatusOptional = OrgInitializationStatusHandler.getInstance().getStatus(organizationService.getIamOrgId(orgId));
                            final String signupCompletedKey = CrmOrgFields.IS_SIGNUP_COMPLETED.getName();

                            initializationStatusOptional.ifPresentOrElse(status -> {
                                switch (status) {
                                    case SIGNUP_PROCESS_DONE, FINISHED -> responseData.put(signupCompletedKey, true);
                                    default -> responseData.put(signupCompletedKey, false);
                                }
                            }, () -> responseData.put(signupCompletedKey, false));

                            responseData.put(CrmOrgFields.IS_INITIALIZED.getName(), false);

                            crmResponse.put(CrmOrgFields.DATA.getName(), responseData);
                            crmResponse.put(CrmOrgFields.TYPE.getName(), CrmOrgFields.ORG_INITIALIZATION.getName());
                            crmResponse.setStatus(HttpStatus.ACCEPTED);
                            crmResponse.send();
                            return;
                        }

                        try {
                            validateHttpRequest(request);
                        } catch (Throwable throwable) {
                            sendError(crmResponse, null, throwable);
                        }

                        crmUser.setPreferenceHolder(userPreferenceHolderService.get(orgId, crmUser.id()));
                        final I18nResolver i18nResolver = request.i18nResolver();

                        request.initializeI18nResolutionAdapter(orgId, i18nResolver);
                        crmResponse.initializeI18nResolutionAdapter(orgId, i18nResolver);
                        final String apiType = request.getRequestJson().optString(TYPE, fetchApiType().orElseGet(() -> null));
                        final RequestType requestType = Objects.isNull(apiType) ? RequestType.SINGLE : fetchRequestType(apiType);

                        SecurityContext.load(crmUser);
                        RequestService.get(requestType).getRequestHandler().handle(() -> {

                            try {
                                sProcess(request, crmResponse);
                            } catch (Throwable throwable) {
                                sendError(crmResponse, null, throwable);
                            }

                            if (!crmResponse.isAsync()) {
                                crmResponse.send();
                            }
                        });
                    } else {
                        sendError(crmResponse, HttpStatus.UNAUTHORIZED, ErrorCode.get(ErrorCodes.AUTHORIZATION_FAILED, "i18n.common.authorization.failed"));
                    }
                }), () -> sendError(crmResponse, HttpStatus.UNAUTHORIZED, ErrorCode.get(CommonErrorCodes.NO_AUTH_HANDLER_DEFINED, "i18n.common.request.authenticator.not.defined")));
    }

    public abstract void sProcess(CrmRequest request, CrmResponse crmResponse) throws Exception;

    /**
     * Returns the list of request authenticators to be used for this action.
     * Override this method to provide custom or additional authenticators
     * such as JWT, Basic Auth, or others as needed.
     *
     * @return List of SecureRequestAuthenticator implementations.
     */
    protected List<SecureRequestAuthenticator> getRequestAuthenticators() {
        return List.of(JWTAuthenticator.get());
    }

    /**
     * <p>
     * Fetches the {@link RequestType} of the current API.
     * </p>
     *
     * @param apiType Represents the current API.
     * @return The {@link RequestType}.
     */
    protected RequestType fetchRequestType(final String apiType) {
        return RequestType.SINGLE;
    }

    /**
     * <p>
     * Verify the validity of an HTTP request by performing various
     * checks on the incoming request object.
     * </p>
     *
     * @param request Refers the {@link CrmRequest}.
     */
    protected void validateHttpRequest(final CrmRequest request) {
    }

    /**
     * <p>
     * Fetches the API type if it is not present in the current request.
     * </p>
     *
     * @return API type of the current {@link Action}.
     */
    protected Optional<String> fetchApiType() {//TODO FYI - If a request data does not contain a key named 'type', then you should override this method.
        return Optional.empty();
    }

    /**
     * <p>
     * Resolves {@link DateTime} fields in a model.
     * </p>
     *
     * @param model Represents {@link TlcModel}.
     */
    protected void resolveDateTimeFields(final TlcModel model) {
        dateTimeFieldsResolver.resolve(model);
    }

    /**
     * <p>
     * Resolves {@link DateTime} fields in a model.
     * </p>
     *
     * @param model      Represents {@link TlcModel}.
     * @param fieldNames Represents the field's name in an Array.
     */
    protected void resolveDateTimeFields(final TlcModel model, final String... fieldNames) {
        dateTimeFieldsResolver.resolve(model, fieldNames);
    }
}