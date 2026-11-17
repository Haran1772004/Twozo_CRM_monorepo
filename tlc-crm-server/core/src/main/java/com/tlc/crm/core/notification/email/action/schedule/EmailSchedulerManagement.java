package com.tlc.crm.core.notification.email.action.schedule;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.AuthorizationController;

import com.tlc.crm.core.notification.email.api.manager.CoreMailManager;
import com.tlc.crm.core.notification.email.api.manager.OutboundEmailManager;
import com.tlc.crm.core.notification.email.internal.status.EmailErrorCodes;
import com.tlc.crm.core.notification.email.bulk.service.BulkMailService;
import com.tlc.crm.core.notification.email.internal.ScheduledEmailService;
import com.tlc.crm.core.notification.email.fields.CoreEmailFields;

import com.tlc.crm.email.notification.email.EmailDispatcher;
import com.tlc.crm.email.notification.email.api.model.address.FromAddressInfo;
import com.tlc.crm.email.notification.model.MessageDispatchType;

import com.tlc.email.common.model.type.DraftEmail;
import com.tlc.web.WebAction;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.tlc.crm.email.notification.model.MessageDispatchType.BULK;

/**
 * <p>
 * Manages web actions related to scheduled mail entities
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
@WebAction(path = "/notifications/scheduled/email")
public class EmailSchedulerManagement extends AuthorizationController {

    private final ScheduledEmailService scheduledEmailService;
    private final BulkMailService bulkMailService;
    private final EmailDispatcher emailDispatcher;
    private final OutboundEmailManager outboundEmailManager;
    private final CoreMailManager coreMailManager;

    public EmailSchedulerManagement() {
        scheduledEmailService = ScheduledEmailService.getInstance();
        bulkMailService = BulkMailService.getInstance();
        emailDispatcher = EmailDispatcher.get();
        outboundEmailManager = OutboundEmailManager.getInstance();
        coreMailManager = CoreMailManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request     Handles the received request
     * @param crmResponse Generates the CRM response
     */
    @Override
    protected void authorizedProcess(final CrmRequest request, final CrmResponse crmResponse) {
        final JsonObject requestJson = request.getRequestJson();
        final String type = requestJson.getString(CoreEmailFields.TYPE.getName());
        final JsonObject data = requestJson.getJsonObject(CoreEmailFields.DATA.getName());

        switch (type) {
            case "updateScheduledMail" ->
                    crmResponse.put(CoreEmailFields.IDS.getName(), updateScheduledMailState(request.orgId(), data, request.userId()));
        }
    }

    /**
     * <p>
     * Updates the state of the scheduled mail.
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param data   Represents the json data
     * @param userId Represents the user id
     * @return {@link Collection} of id which are updated
     */
    private Collection<Long> updateScheduledMailState(final long orgId, final JsonObject data, final long userId) {
        final Collection<Long> mailIds = new HashSet<>();
        final JsonArray jsonArray = data.getJsonArray(CoreEmailFields.IDS.getName());
        final MessageDispatchType messageDispatchType = MessageDispatchType.get(data.getShort(CoreEmailFields.SCHEDULED_TYPE.getName()));

        for (int index = 0; index < jsonArray.size(); index++) {
            mailIds.add(jsonArray.getLong(index));
        }

        if (scheduledEmailService.verifyAllEmailsAreScheduled(orgId, userId, mailIds, messageDispatchType)) {
            final Map<Long, Long> conversationIds = scheduledEmailService.getConversationIds(orgId, mailIds, messageDispatchType);

            if (Objects.nonNull(conversationIds) && !conversationIds.isEmpty()) {
                final boolean isBulk = BULK == messageDispatchType;

                if (data.optBoolean(CoreEmailFields.IS_CANCELLED.getName())) {
                    final Map<Long, DraftEmail> email = outboundEmailManager.getDraftEmailForConversation(orgId, conversationIds.values());
                    final JsonObject jsonObject = Json.object();

                    jsonObject.put(CoreEmailFields.ORG_ID.getName(), orgId)
                            .put(CoreEmailFields.USER_ID.getName(), userId)
                            .put(CoreEmailFields.IS_CANCELLED.getName(), true)
                            .put(CoreEmailFields.SCHEDULED_TYPE.getName(), messageDispatchType.getId());

                    for (final Map.Entry<Long, Long> emailEntry : conversationIds.entrySet()) {
                        final Long conversationId = emailEntry.getValue();

                        jsonObject.put(CoreEmailFields.CONVERSATION_ID.getName(), conversationId);
                        jsonObject.put(CoreEmailFields.ID.getName(), emailEntry.getKey());
                        final DraftEmail draftEmail = email.get(conversationId);
                        final String fromEmailAddress = draftEmail.getSender().getEmailAddress();
                        final Optional<FromAddressInfo> optionalSyncedFromAddressInfo = coreMailManager.getSyncedFromAddressInfo(orgId, userId, fromEmailAddress, draftEmail.getConversationType());

                        if (optionalSyncedFromAddressInfo.isPresent() && optionalSyncedFromAddressInfo.get().getType().isSyncType()) {
                            draftEmail.setSender(optionalSyncedFromAddressInfo.get());
                            emailDispatcher.send(draftEmail, jsonObject);
                        } else {
                            scheduledEmailService.updateMailState(orgId, userId,
                                    coreMailManager.getEmailThreadId(orgId, conversationId), emailEntry.getKey(), conversationId, draftEmail);
                        }
                    }
                } else if (data.optBoolean(CoreEmailFields.IS_DELETED.getName())) {

                    if (isBulk) {
                        bulkMailService.deleteBulkMails(orgId, userId, mailIds);
                    } else {
                        scheduledEmailService.deleteScheduledMail(orgId, userId, mailIds, conversationIds.values());
                    }
                } else {
                    throw ErrorCode.get(EmailErrorCodes.UNSUPPORTED_UPDATE_ACTION);
                }
            }

            return mailIds;
        } else {
            throw ErrorCode.get(EmailErrorCodes.SCHEDULED_MAIL_NOT_FOUND);
        }
    }
}
