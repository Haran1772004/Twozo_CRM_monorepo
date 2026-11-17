package com.tlc.crm.core.notification.email.internal.service;

import com.tlc.crm.core.action.service.EmailSummaryActionService;
import com.tlc.crm.core.api.deal.model.EmailSummary;
import com.tlc.crm.core.conversation.api.manager.EmailEntityConversationManager;
import com.tlc.crm.core.notification.email.api.manager.EntityMailManager;

import com.tlc.crm.entity.fields.source.Source;

import com.tlc.l10n.temporal.model.Date;

/**
 * <p>
 * Implements the {@link EmailSummaryActionService} provides a service for retrieving email summary data
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public class EmailSummaryActionServiceImpl implements EmailSummaryActionService {

    private final EntityMailManager entityMailManager;
    private final EmailEntityConversationManager emailEntityConversationManager;

    private EmailSummaryActionServiceImpl() {
        entityMailManager = EntityMailManager.getInstance();
        emailEntityConversationManager = EmailEntityConversationManager.getInstance();
    }

    private static class InstanceHolder {
        private static final EmailSummaryActionServiceImpl INSTANCE =  new EmailSummaryActionServiceImpl();
    }

    /**
     * <p>
     * Gets the {@link EmailSummaryActionServiceImpl} instance
     * </p>
     *
     * @return The {@link EmailSummaryActionServiceImpl} instance
     */
    public static EmailSummaryActionServiceImpl getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents an organization id
     * @param entityId Represents the entity id
     * @param source   Represents the {@link Source}
     * @return The {@link EmailSummary}
     */
    @Override
    public EmailSummary get(final long orgId, final long entityId, final Source source) {
        final EmailSummary emailSummary = new EmailSummary();

        emailSummary.setEmailMessageCount(emailEntityConversationManager.getEmailMessageCount(orgId, entityId, source));
        emailSummary.setLastEmailReceived(new Date(entityMailManager.getRecentEmailReceivedTime(orgId, entityId, source)));
        emailSummary.setLastEmailSent(new Date(entityMailManager.getRecentEmailSentTime(orgId, entityId, source)));

        return emailSummary;
    }
}
