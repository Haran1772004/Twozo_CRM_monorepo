package com.tlc.crm.core.notification.email.internal.impl;

import com.tlc.crm.common.iam.SecureUserTaskProcessor;

import com.tlc.crm.core.notification.email.api.manager.schedule.EmailSchedulerManager;
import com.tlc.crm.core.notification.email.api.model.email.EntityScheduledEmail;
import com.tlc.crm.core.notification.email.internal.ScheduledEmailService;
import com.tlc.crm.email.notification.model.MessageDispatchType;
import com.tlc.email.common.model.Thread;
import com.tlc.email.common.model.type.Email;

import java.time.Instant;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Provides the implementation for scheduled email related services
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public class ScheduledEmailServiceImpl implements ScheduledEmailService, SecureUserTaskProcessor {

    private final EmailSchedulerManager emailSchedulerManager;

    private ScheduledEmailServiceImpl() {
        emailSchedulerManager = EmailSchedulerManager.getInstance();
    }

    private static class Instance {
        private static final ScheduledEmailServiceImpl INSTANCE = new ScheduledEmailServiceImpl();
    }

    /**
     * <p>
     * Gets the {@link ScheduledEmailServiceImpl} Instance
     * </p>
     *
     * @return {@link ScheduledEmailServiceImpl}
     */
    public static ScheduledEmailServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId           Represents an organization id
     * @param userId          Represents the user id
     * @param threadIds       Represents the {@link Collection} of thread id
     * @param conversationIds Represents the {@link Collection} of ConversationId
     */
    @Override
    public void deleteScheduledMail(final long orgId, final long userId, final Collection<Long> threadIds, final Collection<Long> conversationIds) {
        emailSchedulerManager.deleteScheduledMail(orgId, userId, threadIds, conversationIds);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId               Represents an organization id
     * @param ids                 Represents the {@link Collection} of thread ids or bulk mail ids differ based on {@link MessageDispatchType}
     * @param messageDispatchType Represents the {@link MessageDispatchType}
     * @return {@link Long} of conversation id
     */
    @Override
    public Map<Long, Long> getConversationIds(final long orgId, final Collection<Long> ids, final MessageDispatchType messageDispatchType) {
        return emailSchedulerManager.getConversationIds(orgId, ids, messageDispatchType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteScheduledMails() {
        final long utcTime = Instant.now().toEpochMilli();

        if (emailSchedulerManager.hasScheduledTrashMails(utcTime)) {
            final Collection<EntityScheduledEmail> scheduledConversations = emailSchedulerManager.getScheduledTrashMails(utcTime);

            if (Objects.nonNull(scheduledConversations)) {

                for (final EntityScheduledEmail scheduledConversation : scheduledConversations) {
                    final Long orgId = scheduledConversation.orgId();
                    final long userId = scheduledConversation.getUserId();

                    secureProcess(orgId, userId, () -> emailSchedulerManager.deleteScheduledMail(orgId,
                            userId, scheduledConversation));
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId               Represents an organization id
     * @param userId              Represents the user id
     * @param ids                 Represents the {@link Collection} of thread ids or bulk mail ids differ based on {@link MessageDispatchType}
     * @param messageDispatchType Represents the {@link MessageDispatchType}
     * @return true , if all the mail are scheduled
     */
    @Override
    public boolean verifyAllEmailsAreScheduled(final long orgId, final long userId, final Collection<Long> ids, final MessageDispatchType messageDispatchType) {
        return emailSchedulerManager.verifyAllEmailsAreScheduled(orgId, userId, ids, messageDispatchType);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId          Represents an organization id
     * @param userId         Represents the user id
     * @param mailId         Represents the ail id
     * @param conversationId Represents the conversation id
     * @param thread         Represents the {@link Thread}
     */
    @Override
    public void updateMailState(final long orgId, final long userId, final Long mailId, final Long conversationId, final Thread thread) {
        emailSchedulerManager.updateMailState(orgId, userId, mailId, conversationId, thread);
    }

    /**
     * <p>
     * Updates the mail state based on {@link MessageDispatchType}.
     * </p>
     *
     * @param orgId          Represents an organization id
     * @param userId         Represents the user id
     * @param threadId       Represents the thread id
     * @param mailId         Represents the mail id
     * @param conversationId Represents the conversation id
     * @param email          Represents the {@link Thread}
     */
    public void updateMailState(final long orgId, final long userId, final Long threadId, final Long mailId, final Long conversationId,
                                final Email email) {
        emailSchedulerManager.updateMailState(orgId, userId, threadId, mailId, conversationId, email);
    }
}
