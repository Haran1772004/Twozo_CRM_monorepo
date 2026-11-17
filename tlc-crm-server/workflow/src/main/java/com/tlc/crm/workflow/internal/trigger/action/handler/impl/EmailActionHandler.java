package com.tlc.crm.workflow.internal.trigger.action.handler.impl;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.pair.Pair;

import com.tlc.crm.core.notification.email.api.model.EntityThread;
import com.tlc.crm.core.notification.email.api.model.converter.EmailConverter;
import com.tlc.crm.core.notification.email.api.model.email.EntityOutboundEmail;
import com.tlc.crm.core.notification.email.settings.service.EmailSettingsService;
import com.tlc.crm.email.notification.email.api.model.settings.EmailLinkerSettings;
import com.tlc.crm.email.notification.email.api.model.settings.EmailSettings;
import com.tlc.crm.email.notification.email.api.model.settings.EmailTrackerSettings;
import com.tlc.crm.email.notification.model.UserSyncAddressInfo;

import com.tlc.email.common.model.config.EmailReceiverConfiguration;
import com.tlc.email.common.model.type.OutboundEmail.OutboundType;

import com.tlc.crm.common.converter.ConverterConfig;

import com.tlc.crm.core.notification.email.internal.OutboundEmailService;
import com.tlc.crm.email.notification.email.sync.api.EmailSyncManager;

import com.tlc.crm.workflow.api.model.WorkflowAction;
import com.tlc.crm.workflow.api.model.history.WorkflowActionExecutionFailureCause;
import com.tlc.crm.workflow.internal.trigger.action.ActionExecutionContext;
import com.tlc.crm.workflow.internal.trigger.action.formatter.EmailJsonFormatter;
import com.tlc.crm.workflow.internal.trigger.action.field.ActionAttributes;
import com.tlc.crm.workflow.internal.trigger.action.field.ActionDataParserFields;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.function.Function;

/**
 * The handler which handles the email related actions of a {@link com.tlc.crm.workflow.api.model.Workflow}.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class EmailActionHandler {

    private static final String FROM_ADDRESS_KEY = "fromAddress";
    private static final String CONTACT_LINKING_ENABLED = "isContactLinkingEnabled";
    private static final String DEAL_LINKING_ENABLED = "isDealLinkingEnabled";
    private static final String LINK_TRACKER_ENABLED = "isLinkTrackerEnabled";
    private static final Logger LOGGER = LoggerFactory.getLogger(EmailActionHandler.class);

    private final OutboundEmailService outboundEmailService;
    private final EmailSyncManager emailSyncManager;
    private final EmailSettingsService emailSettingsService;
    private final EmailJsonFormatter emailJsonFormatter;
    private final EmailConverter emailConverter;
    private final WorkflowEmailPlaceHolderResolver workflowEmailPlaceHolderResolver;

    private EmailActionHandler() {
        this.outboundEmailService = OutboundEmailService.getInstance();
        this.emailSyncManager = EmailSyncManager.getInstance();
        this.emailSettingsService = EmailSettingsService.getInstance();
        this.emailJsonFormatter = EmailJsonFormatter.getInstance();
        this.emailConverter = EmailConverter.getInstance();
        this.workflowEmailPlaceHolderResolver = WorkflowEmailPlaceHolderResolver.getInstance();
    }

    private static class Instance {
        private static final EmailActionHandler INSTANCE = new EmailActionHandler();
    }

    public static EmailActionHandler getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * @param orgId                  Refers the organization id.
     * @param userId                 Refers the user id.
     * @param workflowAction         Refers the {@link WorkflowAction} that encapsulates specific details about the action
     *                               utilized in processing the associated entity.
     * @param actionExecutionContext Refers the {@link ActionExecutionContext}.
     * @return Sent email {@link EntityThread}.
     */
    public EntityThread sendMail(final Long orgId, final Long userId, final WorkflowAction workflowAction, final ActionExecutionContext actionExecutionContext) {
        final Long workflowActionId = workflowAction.getId();
        final JsonObject emailJson = emailJsonFormatter.format(orgId, workflowActionId, workflowAction.getFieldMappingAsJson(), actionExecutionContext, workflowAction.getTriggerActionSource());
        final Function<WorkflowActionExecutionFailureCause, EntityThread> reasonTracker = (reason) -> {
            actionExecutionContext.tracker().track(workflowActionId, reason);

            return null;
        };

        final EmailSettings emailSettings = emailSettingsService.getEmailSettings(orgId, userId);
        final EmailLinkerSettings emailLinkerSettings = emailSettings.getEmailLinkerSettings();
        final EmailTrackerSettings emailTrackerSettings = emailSettings.getEmailTrackerSettings();

        if (Objects.nonNull(emailLinkerSettings)) {
            emailJson.put(CONTACT_LINKING_ENABLED, emailLinkerSettings.isContactLinkingEnabled());
            emailJson.put(DEAL_LINKING_ENABLED, emailLinkerSettings.isDealLinkingEnabled());
        }

        if (Objects.nonNull(emailTrackerSettings)) {
            emailJson.put(LINK_TRACKER_ENABLED, emailTrackerSettings.isLinkTrackerEnabled());
        }
        final Pair<Long, UserSyncAddressInfo> senderInfo;

        try {
            senderInfo = emailSyncManager.getSenderInfo(orgId, userId);

            emailJson.put(FROM_ADDRESS_KEY, senderInfo.getKey());
        } catch (ErrorCode errorCode) {
            return reasonTracker.apply(WorkflowActionExecutionFailureCause.USER_EMAIL_NOT_SYNCED);
        }

        if (EmailValidator.isValidJson(emailJson)) {
            final EntityThread thread;

            try {
                thread = emailConverter.convert(new ConverterConfig(orgId, userId, null, false, emailJson), emailJson);
                final EmailReceiverConfiguration configuration = new EmailReceiverConfiguration();

                outboundEmailService.setEmailThreadDetails(orgId, userId, thread, configuration, emailJson);
            } catch (final Exception exception) {
                return reasonTracker.apply(WorkflowActionExecutionFailureCause.INVALID_DATA);
            }
            final EntityOutboundEmail mail = thread.getComposedEmail();

            if (Objects.nonNull(mail)) {
                mail.setOutboundType(OutboundType.SEND);

                if (outboundEmailService.hasUnsubscribedRecipients(orgId, mail)) {
                    return reasonTracker.apply(WorkflowActionExecutionFailureCause.EMAIL_RECIPIENTS_UNSUBSCRIBED);
                } else {
                    try {
                        workflowEmailPlaceHolderResolver.resolve(thread, actionExecutionContext.triggerModel());
                        emailJson.put(ActionAttributes.SUBJECT, mail.getSubject());
                        emailJson.put(ActionAttributes.MESSAGE, mail.getContent());
                    } catch (Exception exception) {
                        LOGGER.error("Workflow Execution: Error while resolving the email placeholder. Org ID: {}, User ID: {}, Reason: {}", orgId, userId, exception.getMessage(), exception);
                        return reasonTracker.apply(WorkflowActionExecutionFailureCause.FAILED_TO_RESOLVE_EMAIL_PLACEHOLDER);
                    }

                    try {
                        final JsonObject stateData = Json.object();

                        stateData.put(ActionDataParserFields.ORG_ID, orgId);
                        stateData.put(ActionDataParserFields.USER_ID, userId);
                        stateData.put(ActionDataParserFields.DATA, emailJson);

                        outboundEmailService.send(orgId, thread, stateData);

                        return thread;
                    } catch (Throwable cause) {
                        return reasonTracker.apply(WorkflowActionExecutionFailureCause.UNIDENTIFIED_ERROR);
                    }
                }
            }
        } else {
            return reasonTracker.apply(WorkflowActionExecutionFailureCause.INVALID_DATA);
        }

        return null;
    }

    /**
     * Validates the {@link Thread} entity of an email.
     *
     * @author ThalaimalaiPandiyanT
     * @version 1.0
     */
    private static class EmailValidator {

        /**
         * Validates the email json which holds all the necessary data.
         *
         * @param jsonObject Refers the {@link JsonObject} which holds the email attributes.
         * @return true if the json is valid, otherwise false.
         */
        private static boolean isValidJson(final JsonObject jsonObject) {
            return jsonObject.containsKey(ActionAttributes.TO_ADDRESS)
                    || jsonObject.containsKey(ActionAttributes.CC_ADDRESS)
                    || jsonObject.containsKey(ActionAttributes.BCC_ADDRESS);
        }
    }
}