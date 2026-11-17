package com.tlc.crm.email.notification.email.action;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.email.notification.email.converter.MailBoxConfigurationConverter;
import com.tlc.crm.email.notification.email.fields.EmailFields;
import com.tlc.email.client.internal.impl.MailBoxConfigServiceImpl;
import com.tlc.email.client.service.MailBoxConfigService;
import com.tlc.email.common.internal.status.EmailErrorCodes;
import com.tlc.email.common.model.address.SyncAddressInfo;
import com.tlc.email.server.internal.protocols.models.MailBoxConfiguration;
import com.tlc.email.server.service.EmailProviderService;
import com.tlc.notification.model.sync.ConnectionStatus;
import com.tlc.sync.service.oauth.model.provider.EmailProvider;
import com.tlc.web.WebAction;

/**
 * <p>
 * Handles email synchronization-related actions for mailbox configuration.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
@WebAction(path = "/email/sync")
public class EmailSyncAction extends AbstractEmailSyncAction {

    private final MailBoxConfigurationConverter mailBoxConfigurationConverter;
    private final MailBoxConfigService mailBoxConfigService;

    /**
     * <p>
     * Constructs an {@link EmailSyncAction} and initializes required services
     * and converters using their singleton instances.
     * </p>
     */
    public EmailSyncAction() {
        this.mailBoxConfigurationConverter = MailBoxConfigurationConverter.getInstance();
        this.mailBoxConfigService = MailBoxConfigServiceImpl.getInstance();
    }

    /**
     * <p>
     * Processes authorized email sync requests.
     * </p>
     *
     * @param request     The incoming CRM request.
     * @param crmResponse The response object used to send results back to the client.
     */
    @Override
    public void sProcess(final CrmRequest request, final CrmResponse crmResponse) {
        final JsonObject requestJson = request.getRequestJson();

        if (requestJson.getString(EmailFields.TYPE.getName()).equals("connect")) {
            handle(mailBoxConfigurationConverter.convert(requestJson.getJsonObject(EmailFields.DATA.getName())), request, crmResponse);
        }
    }

    /**
     * <p>
     * Handles the mailbox connection and synchronization process.
     * </p>
     *
     * @param mailBoxConfiguration The mailbox configuration to be processed.
     * @param crmRequest           The CRM request containing user and organization details.
     * @param crmResponse          The CRM response to return validation status.
     */
    private void handle(final MailBoxConfiguration mailBoxConfiguration, final CrmRequest crmRequest, final CrmResponse crmResponse) {
        final Long orgId = crmRequest.orgId();
        final Long userId = crmRequest.userId();
        final EmailProvider provider = EmailProviderService.getProvider(mailBoxConfiguration.getEmailAddress());

        if (provider.isOauthProvider()) {
            throw ErrorCode.get(EmailErrorCodes.OAUTH_PROVIDER_NOT_SUPPORTED, "i18n.email.notifications.mail.oauth.provider.not.supported");
        }
        mailBoxConfigService.loadServerData(mailBoxConfiguration);
        final boolean status = mailBoxConfigService.validate(mailBoxConfiguration);//TODO authentication and validation should be done as a background job

        if (status) {
            mailBoxConfigService.save(mailBoxConfiguration);
            initiateEmailSync(orgId, userId, (SyncAddressInfo) mailBoxConfiguration.getEmailAddress(), provider);
        }
        crmResponse.put(EmailFields.CONNECTION_STATUS.getName(), status ? ConnectionStatus.ACTIVE.name() : ConnectionStatus.INACTIVE.name());
    }
}