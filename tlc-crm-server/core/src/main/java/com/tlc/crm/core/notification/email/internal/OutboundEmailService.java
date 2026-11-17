package com.tlc.crm.core.notification.email.internal;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.core.notification.email.api.model.email.EntityEmail;
import com.tlc.crm.core.notification.email.api.model.EntityThread;
import com.tlc.crm.core.notification.email.internal.impl.OutboundEmailServiceImpl;

import com.tlc.crm.email.notification.email.api.model.MailResponse;
import com.tlc.crm.email.notification.email.tracking.model.EmailTrackingDetail;

import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.email.common.model.config.EmailConfiguration;
import com.tlc.email.common.model.type.OutboundEmail;

import java.util.Collection;

/**
 * <p>
 * Provides the outbound mail related services
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public interface OutboundEmailService {

    /**
     * <p>
     * Fetches the {@link OutboundEmailService} instance
     * </p>
     *
     * @return the instance of {@link OutboundEmailService}
     */
    static OutboundEmailService getInstance() {
        return OutboundEmailServiceImpl.getInstance();
    }

    /**
     * <p>
     * Creates and sends the email for the details given
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param thread      Represents the thread attribute
     * @param requestJson Represents the json which contains request data
     * @return {@link MailResponse}
     */
    MailResponse send(final Long orgId, final EntityThread thread, final JsonObject requestJson);

    /**
     * <p>
     * Sets the data that are required for draft mails
     * </p>
     *
     * @param orgId         Represents an organization id
     * @param thread        Represents the {@link EntityThread}
     * @param configuration Represents the {@link EmailConfiguration}
     * @param requestData   Represents the json which contains request data
     */
    MailResponse draft(final Long orgId, final EntityThread thread, final EmailConfiguration configuration, final JsonObject requestData);

    /**
     * <p>
     * Compose the mail with headers and recipients from the chosen reply mail
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param thread An entity that holds the thread attributes
     * @param data   Contains the Json object
     */
    void composeReply(final Long orgId, final EntityThread thread, final JsonObject data);

    /**
     * <p>
     * Compose the mail with headers and recipients from the chosen reply mail
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param thread An entity that holds the thread attributes
     * @param data   Contains the Json object
     */
    void composeReply(final Long orgId, final EntityThread thread, final JsonObject data, final boolean isDraftSendAction);

    /**
     * <p>
     * Compose the mail with headers from the chosen mail
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param thread   An entity that holds the thread attributes
     * @param jsonData Represents the json which contains request data
     */
    void composeForward(final Long orgId, final EntityThread thread, final JsonObject jsonData);

    /**
     * <p>
     * Checks whether the outbound id is present or not
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param mailIds  Represents the Collection of outbound ids
     * @param isActive Represents the flag for active status
     * @return true if data is present
     */
    boolean exists(final Long orgId, final Collection<Long> mailIds, final boolean isActive);

    /**
     * <p>
     * Unsubscribes the email and contact based on the given organization, user id and an respective email address.
     * </p>
     *
     * @param orgId        Represents an organization id
     * @param userId       Represents an user id
     * @param emailAddress Represents the email address
     */
    void unsubscribe(final long orgId, final long userId, final String emailAddress);

    /**
     * <p>
     * Checks whether the mail recipients are unsubscribed or not
     * </p>
     *
     * @param orgId Represents an organization id
     * @param mail  Represents the {@link EntityEmail}
     * @return true, if the mail is unsubscribed
     */
    boolean hasUnsubscribedRecipients(final Long orgId, final EntityEmail mail);

    /**
     * <p>
     * Checks whether the recipients are unsubscribed or not
     * </p>
     *
     * @param orgId          Represents an organization id
     * @param emailAddresses Represents the {@link Collection} of mail addresses
     * @return true, if the mail is unsubscribed
     */
    boolean hasUnsubscribedEmailAddress(final Long orgId, final Collection<String> emailAddresses);

    /**
     * Fetches a {@link Collection} of unsubscribed {@link EmailAddress} based on specified criteria
     * within a given organization and user context.
     *
     * @param orgId           Represents an organization id
     * @param userId          Represents a user id
     * @param contactEmailIds Represents the {@link Collection} of contact emails
     * @param mailAddresses   Represents the {@link Collection} of mail address
     * @return the {@link Collection} of unsubscribed {@link EmailAddress}
     */
    Collection<EmailAddress> fetchUnsubscribedMailAddresses(final long orgId, final long userId,
                                                            final Collection<Long> contactEmailIds, final Collection<String> mailAddresses);

    /**
     * <p>
     * Deletes the draft messages for the given message ids
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param userId          Represents the userId
     * @param conversationIds Represents the conversation ids
     * @param configuration   Refers to the {@link EmailConfiguration}
     */
    void deleteDraftMessages(final long orgId, final long userId, final Collection<Long> conversationIds, final EmailConfiguration configuration);

    /**
     * Sets the mail details into the thread.
     *
     * @param orgId         Refers the organization id.
     * @param userId        Refers the user id.
     * @param thread        Refers the {@link Thread} where the mail info is configured.
     * @param configuration Refers to the {@link EmailConfiguration}
     * @param jsonObject    Represents the json which contains request data
     */
    void setEmailThreadDetails(final Long orgId, final Long userId, final EntityThread thread, final EmailConfiguration configuration, final JsonObject jsonObject);

    /**
     * <p>
     * Updates the email tracking details for the specified tracking type and tracking id
     * </p>
     *
     * @param trackingType Represents the {@link EmailTrackingDetail.Type}
     * @param trackingId   Represents the tracking id
     */
    void updateTrackingDetails(final EmailTrackingDetail.Type trackingType, final String trackingId);

    /**
     * <p>
     * Discards the draft for the given thread ids
     * </p>
     *
     * @param orgId     Represents an organization id
     * @param userId    Represents the user id
     * @param threadIds Represents the thread ids
     * @return The collection of thread ids for which the draft was actually discarded
     */
    Collection<Long> discardDrafts(final long orgId, final Long userId, final Collection<Long> threadIds);

    /**
     * <p>
     * Updates the email unsubscribe details for the specified tracking id
     * </p>
     *
     * @param trackingId Represents the tracking id
     */
    void updateUnsubscribeDetails(final String trackingId);

    /**
     * <p>
     * Gets the {@link OutboundEmail.OutboundType} for the given conversation id
     * </p>
     *
     * @param orgId          Represents an organization id
     * @param conversationId Represents the conversation id
     * @return the {@link OutboundEmail.OutboundType}
     */
    OutboundEmail.OutboundType getOutboundType(final long orgId, final Long conversationId);

    /**
     * <p>
     * An enum represents the action like
     * </p>
     *
     * {@link #CREATE}
     * {@link #UPDATE}
     */
    enum Action {
        CREATE,
        UPDATE
    }
}
