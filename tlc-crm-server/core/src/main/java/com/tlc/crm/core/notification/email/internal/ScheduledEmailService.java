package com.tlc.crm.core.notification.email.internal;

import com.tlc.crm.core.notification.email.internal.impl.ScheduledEmailServiceImpl;

import com.tlc.crm.email.notification.model.MessageDispatchType;

import com.tlc.email.common.model.Thread;
import com.tlc.email.common.model.type.Email;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * Provides the services for deletes, cancels and gets the scheduled mail
 * </p>
 *
 * @author kavinila E
 * @version 1.0
 */
public interface ScheduledEmailService {

    /**
     * <p>
     * Fetches the {@link ScheduledEmailService} instance
     * </p>
     *
     * @return the instance of {@link ScheduledEmailService}
     */
    static ScheduledEmailService getInstance() {
        return ScheduledEmailServiceImpl.getInstance();
    }

    /**
     * <p>
     * Deletes the scheduled emails which is associated with the provided {@link Collection} of conversation ids
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param userId          Represents the user id
     * @param threadIds       Represents the {@link Collection} of thread id
     * @param conversationIds Represents the {@link Collection} of conversationId
     */
    void deleteScheduledMail(final long orgId, final long userId, final Collection<Long> threadIds, final Collection<Long> conversationIds);

    /**
     * <p>
     * Retrieves the {@link Long} of conversation id which is mapped for given ids
     * </p>
     *
     * @param orgId               Represents an organization id
     * @param ids                 Represents the {@link Collection} of thread id or bulk mail ids differ based on {@link MessageDispatchType}
     * @param messageDispatchType Represents the {@link MessageDispatchType}
     * @return {@link Long} of conversation id
     */
    Map<Long, Long> getConversationIds(final long orgId, final Collection<Long> ids, final MessageDispatchType messageDispatchType);

    /**
     * <p>
     * Deletes the emails which are scheduled after 90 days
     * </p>
     */
    void deleteScheduledMails();

    /**
     * <p>
     * Verifies whether all the specified email IDs have been scheduled or not
     * </p>
     *
     * @param orgId               Represents an organization id
     * @param userId              Represents the user id
     * @param ids                 Represents the {@link Collection} of thread ids or {@link Collection} of bulk email ids differ based on {@link MessageDispatchType}
     * @param messageDispatchType Which represents the {@link MessageDispatchType}
     * @return true , if all the mails are scheduled
     */
    boolean verifyAllEmailsAreScheduled(final long orgId, final long userId, final Collection<Long> ids, final MessageDispatchType messageDispatchType);

    /**
     * <p>
     * Updates the mail state based on provided {@link MessageDispatchType}.
     * </p>
     *
     * @param orgId          Represents an organization id
     * @param userId         Represents the user id
     * @param mailId         Represents the ail id
     * @param conversationId Represents the conversation id
     * @param thread         Represents the {@link Thread}
     */
    void updateMailState(final long orgId, final long userId, final Long mailId, final Long conversationId, final Thread thread);

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
    void updateMailState(final long orgId, final long userId, final Long threadId, final Long mailId, final Long conversationId,
                         final Email email);
}