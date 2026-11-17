package com.tlc.crm.email.notification.email.action;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;

import com.tlc.crm.common.util.oauth.RedirectHandler;

import com.tlc.crm.email.notification.email.fields.EmailFields;
import com.tlc.crm.email.notification.email.status.EmailErrorCodes;
import com.tlc.crm.email.notification.email.sync.api.EmailSyncManager;
import com.tlc.crm.email.notification.model.UserSyncAddressInfo;

import com.tlc.email.common.model.address.SyncAddressInfo;
import com.tlc.sync.service.oauth.internal.status.SyncErrorCodes;
import com.tlc.sync.service.oauth.model.SyncTokenRequestInstanceProvider;
import com.tlc.sync.service.oauth.model.SyncUrlConfig;
import com.tlc.sync.service.oauth.model.provider.EmailProvider;
import com.tlc.sync.service.oauth.model.request.TokenRequest;
import com.tlc.sync.service.oauth.model.request.email.EmailTokenWriteRequest;
import com.tlc.sync.service.oauth.service.sync.SyncService;

import com.tlc.web.HttpMethod;
import com.tlc.web.HttpUtil;
import com.tlc.web.WebAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * <p>
 * Configures the user consent using oauth authorization
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
@WebAction(path = "/oauth/user/consent/{provider}", method = HttpMethod.GET)
public class OauthEmailSyncAction extends AbstractEmailSyncAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(OauthEmailSyncAction.class);

    private final RedirectHandler redirectHandler;

    private static final String LOCAL_REDIRECT_URL_SYNC = "local.redirectUrl.sync";
    private static final String LOCAL_REDIRECT_URL_SYNC_ERROR_EXISTS = "local.redirectUrl.error.emailExists";
    private static final String LOCAL_REDIRECT_URL_SYNC_ERROR_PERMISSION = "local.redirectUrl.error.lacksPermission";
    private static final String LOCAL_REDIRECT_URL_SYNC_ERROR_FAILED = "local.redirectUrl.error.authorizationFailed";
    private static final String SERVER_REDIRECT_URL_SYNC = "server.redirectUrl.sync";
    private static final String SERVER_REDIRECT_URL_SYNC_ERROR_EXISTS = "server.redirectUrl.error.emailExists";
    private static final String SERVER_REDIRECT_URL_SYNC_ERROR_PERMISSION = "server.redirectUrl.error.lacksPermission";
    private static final String SERVER_REDIRECT_URL_SYNC_ERROR_FAILED = "server.redirectUrl.error.authorizationFailed";

    private final Configurations configurations;
    private final EmailSyncManager emailSyncManager;

    /**
     * <p>
     * Initializes the email sync service, configuration, manager, and redirect handler instances.
     * </p>
     */
    public OauthEmailSyncAction() {
        this.configurations = ConfHolder.getInstance().getProperties(EmailFields.SYNC_SERVICE.getName());
        this.emailSyncManager = EmailSyncManager.getInstance();
        this.redirectHandler = RedirectHandler.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request     the received request
     * @param crmResponse the response to be sent
     */
    @Override
    public void sProcess(final CrmRequest request, final CrmResponse crmResponse) {
        final EmailProvider emailProvider = EmailProvider.get(request.getParameter(EmailFields.PROVIDER.getName()));
        final SyncUrlConfig syncUrlConfig = SyncService.getSyncUrlDetails(emailProvider);
        final String code = request.getParameter(EmailFields.CODE.getName());

        if (Objects.isNull(code)) {
            this.redirectHandler.buildRedirectUrlWithState(request, syncUrlConfig.getConsentUrl(), crmResponse);
        } else {

            try {
                final EmailTokenWriteRequest emailTokenWriteRequest = SyncTokenRequestInstanceProvider.getEmailTokenWriteRequest(code, syncUrlConfig, TokenRequest.TokenType.REFRESH);

                emailProvider.getSyncService().fetchAndStoreRefreshToken(emailTokenWriteRequest);
                this.handle(new UserSyncAddressInfo(emailTokenWriteRequest.getEmailAddress().getEmailId()), emailProvider, request, crmResponse);
            } catch (final Exception exception) {

                if (exception.getMessage().contains(SyncErrorCodes.INSUFFICIENT_PERMISSIONS.name())) {
                    this.setRedirectResponse(request, crmResponse,
                            SERVER_REDIRECT_URL_SYNC_ERROR_PERMISSION, LOCAL_REDIRECT_URL_SYNC_ERROR_PERMISSION);
                    return;
                }
                this.setRedirectResponse(request, crmResponse,
                        SERVER_REDIRECT_URL_SYNC_ERROR_FAILED, LOCAL_REDIRECT_URL_SYNC_ERROR_FAILED);
            }
        }
    }

    /**
     * <p>
     * Handles the email synchronization process for the given email address.
     * </p>
     *
     * @param emailAddress  The email address for which synchronization is to be performed.
     * @param emailProvider Represents the email provider.
     * @param crmRequest    The CRM request object containing organization and user context.
     * @param crmResponse   The CRM response object used to send redirection or error information.
     */
    private void handle(final SyncAddressInfo emailAddress, final EmailProvider emailProvider, final CrmRequest crmRequest, final CrmResponse crmResponse) {
        final Long orgId = crmRequest.orgId();
        final Long userId = crmRequest.userId();

        try {

            if (this.emailSyncManager.exists(orgId, emailAddress)) {
                throw ErrorCode.get(EmailErrorCodes.EMAIL_ALREADY_EXISTS);
            }
            this.initiateEmailSync(orgId, userId, emailAddress, emailProvider);
            this.setRedirectResponse(crmRequest, crmResponse, SERVER_REDIRECT_URL_SYNC, LOCAL_REDIRECT_URL_SYNC);
        } catch (final Exception exception) {

            if (exception.getMessage().contains(EmailErrorCodes.EMAIL_ALREADY_EXISTS.name())) {
                this.setRedirectResponse(crmRequest, crmResponse,
                        SERVER_REDIRECT_URL_SYNC_ERROR_EXISTS, LOCAL_REDIRECT_URL_SYNC_ERROR_EXISTS);
                return;
            }
            LoggerUtil.logError(LOGGER, exception, "Oauth Email Sync: Authorization Failed, Reason: {}", exception.getMessage());
            throw ErrorCode.get(SyncErrorCodes.AUTHORIZATION_FAILED);
        }
    }

    /**
     * <p>
     * Sets the redirect response for the CRM request based on the provided redirect URLs.
     * </p>
     *
     * @param crmRequest        Represents the {@link CrmRequest}
     * @param crmResponse       Represents the {@link CrmResponse}
     * @param serverRedirectUrl The key for the server redirect URL in the configurations.
     * @param localRedirectUrl  The key for the local redirect URL in the configurations.
     */
    private void setRedirectResponse(final CrmRequest crmRequest, final CrmResponse crmResponse,
                                     final String serverRedirectUrl, final String localRedirectUrl) {
        this.redirectHandler.handleClientRedirect(crmRequest, crmResponse,
                HttpUtil.buildAbsoluteUrl(this.configurations.getConfig(serverRedirectUrl)),
                this.configurations.getConfig(localRedirectUrl)
        );
        crmResponse.send();
    }
}