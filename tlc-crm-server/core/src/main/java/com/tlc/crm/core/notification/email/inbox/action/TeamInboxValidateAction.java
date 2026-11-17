package com.tlc.crm.core.notification.email.inbox.action;

import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.pub.CrmPublicAction;
import com.tlc.crm.common.fields.QueryParamAttributes;
import com.tlc.crm.common.iam.SecureUserTaskProcessor;
import com.tlc.crm.core.notification.email.api.manager.addon.team.TeamInboxManager;
import com.tlc.crm.core.notification.email.inbox.api.model.EmailVerificationLink;
import com.tlc.crm.core.notification.email.inbox.fields.TeamInboxFields;
import com.tlc.crm.core.notification.email.inbox.manager.EmailVerificationLinkManager;
import com.tlc.web.HttpMethod;
import com.tlc.web.HttpUtil;
import com.tlc.web.WebAction;

import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

/**
 * <p>
 * Handles the validation of team inbox email verification links.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
@WebAction(path = "/email/teamInbox/validate", method = HttpMethod.GET)
public class TeamInboxValidateAction extends CrmPublicAction implements SecureUserTaskProcessor {

    private final TeamInboxManager teamInboxManager;
    private final EmailVerificationLinkManager emailVerificationLinkManager;
    private final Configurations configurations;

    /**
     * <p>
     * Constructs a new instance of {@link TeamInboxValidateAction}, initializing the
     * required managers and configurations for handling validation requests.
     * </p>
     */
    public TeamInboxValidateAction() {
        this.teamInboxManager = TeamInboxManager.getInstance();
        this.emailVerificationLinkManager = EmailVerificationLinkManager.get();
        this.configurations = ConfHolder.getInstance()
                .getProperties(TeamInboxFields.EMAIL.getName())
                .getNestedConfigs(TeamInboxFields.ADD_ON_SERVICES.getName())
                .getNestedConfigs(TeamInboxFields.TEAM_INBOX.getName());
    }

    /**
     * <p>
     * Processes the email validation request. Decodes the tracking ID from the
     * request, retrieves the corresponding {@link EmailVerificationLink}, and
     * determines whether the link is expired or still valid.
     * </p>
     *
     * @param request     The CRM request containing the validation token.
     * @param crmResponse The CRM response used to set the redirection URL.
     */
    @Override
    public void pProcess(final CrmRequest request, final CrmResponse crmResponse) {
        final String trackingId = new String(Base64.getDecoder().decode(request.getParameter(QueryParamAttributes.TOKEN)));
        final Optional<EmailVerificationLink> optionalEmailVerificationLink = emailVerificationLinkManager.get(trackingId);

        optionalEmailVerificationLink.ifPresentOrElse(emailVerificationLink -> {
            final long orgId = emailVerificationLink.getOrgId();

            secureProcess(orgId, emailVerificationLink.getUserId(), () -> {
                if (Instant.now().toEpochMilli() > emailVerificationLink.getExpireTime()) {
                    crmResponse.setTemporaryRedirectUrl(getExpiryUrl(request));
                } else {
                    teamInboxManager.update(orgId, emailVerificationLink.getEmailAddress().getEmailAddress());
                    crmResponse.setTemporaryRedirectUrl(getSuccessUrl(request));
                }
            });
        }, () -> crmResponse.setTemporaryRedirectUrl(getExpiryUrl(request)));
    }

    /**
     * <p>
     * Builds and returns the verification success redirection URL.
     * </p>
     *
     * @param crmRequest The CRM request used to check for local redirection.
     * @return The absolute URL for success redirection.
     */
    private String getSuccessUrl(final CrmRequest crmRequest) {
        final boolean paramExists = Boolean.parseBoolean(crmRequest.getParameter(QueryParamAttributes.LOCAL_REDIRECTION_REQUIRED));

        return HttpUtil.buildAbsoluteUrl(
                configurations.getConfig(paramExists ? TeamInboxFields.VERIFICATION_LOCAL_SUCCESS_URL.getName()
                        : TeamInboxFields.VERIFICATION_SERVER_SUCCESS_URL.getName())
        );
    }

    /**
     * <p>
     * Builds and returns the verification failure (expiry) redirection URL.
     * </p>
     *
     * @param crmRequest The CRM request used to check for local redirection.
     * @return The absolute URL for failure redirection.
     */
    private String getExpiryUrl(final CrmRequest crmRequest) {
        final boolean paramExists = Boolean.parseBoolean(crmRequest.getParameter(QueryParamAttributes.LOCAL_REDIRECTION_REQUIRED));

        return HttpUtil.buildAbsoluteUrl(
                configurations.getConfig(paramExists ? TeamInboxFields.VERIFICATION_LOCAL_FAILURE_URL.getName()
                        : TeamInboxFields.VERIFICATION_SERVER_FAILURE_URL.getName())
        );
    }
}