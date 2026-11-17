package com.tlc.crm.core.notification.email.bulk.service;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.notification.email.api.model.email.EntityEmail;
import com.tlc.crm.core.notification.email.api.model.email.EntityOutboundEmail;
import com.tlc.crm.core.notification.email.bulk.api.model.MailRecipientInfo;
import com.tlc.crm.core.notification.email.bulk.service.impl.BulkMailServiceImpl;
import com.tlc.crm.core.notification.email.bulk.api.model.BulkMail;
import com.tlc.crm.core.notification.email.bulk.api.model.MailInfo;

import com.tlc.crm.email.notification.email.api.model.MailResponse;

import com.tlc.email.common.model.config.EmailConfiguration;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Provides the functionalities related to sending, categorizing, and managing {@link BulkMail}.
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public interface BulkMailService {

    /**
     * Gets instance of {@link BulkMailService}.
     *
     * @return {@link BulkMailService} instance
     */
    static BulkMailService getInstance() {
        return BulkMailServiceImpl.getInstance();
    }

    /**
     * Groups the primary emails based on the provided entities within a specific organization and user.
     *
     * @param orgId          Represents an organization id
     * @param contactIds     Represents the {@link Collection} of contact id
     * @param entityEmailIds Represents the {@link Map} which grouped the primary emails of entities
     * @param bulkMailId     Represents the {@link Long} bulk mail ID
     * @return A {@link Map} of entity ids and their corresponding primary email id
     */
    Map<Long, Collection<Long>> groupPrimaryEmailsByContactIds(final Long orgId, final Collection<Long> contactIds,
                                                               final Collection<Long> entityEmailIds, final Long bulkMailId);

    /**
     * Groups the participants contacts based on the provided deals ids within a specific organization and user.
     *
     * @param orgId          Represents an organization id
     * @param dealIds        Represents the {@link Collection} of deal id
     * @param entityEmailIds Represents the {@link Map} which grouped the primary emails of entities
     * @param bulkMailId     Represents the {@link Long} bulk mail ID
     * @return A {@link Map} of deal ids and their corresponding participant contacts
     */
    Map<Long, Collection<Contact>> groupContactsByDealIds(final Long orgId, final Collection<Long> dealIds,
                                                          final Collection<Long> entityEmailIds, final Long bulkMailId);

    /**
     * Retrieves the {@link MailResponse} for draft emails associated with a specified {@link BulkMail},
     * covering the processes of creation, update, and transmission of bulk drafts associated with a particular organization.
     *
     * @param bulkMail      Represents the {@link BulkMail}
     * @return {@link MailResponse}
     */
    MailResponse bulkDraft(final BulkMail bulkMail, final JsonObject requestData);

    /**
     * Retrieves the {@link MailResponse} for a designated {@link BulkMail} linked to a specific organization
     * and manages the sending process accordingly.
     *
     * @param bulkMail    Represents the {@link BulkMail}
     * @param requestData Represents the request data.
     * @return {@link MailResponse}
     */
    MailResponse bulkSend(final BulkMail bulkMail, final JsonObject requestData);

    /**
     * Retrieves the total count of recipients for subscribed emails on the current day,
     * associated with the specified organization and user.
     *
     * @param orgId  Represents an organization id
     * @param userId Represents a user id
     * @return the count of total recipients
     */
    long getSubscribedRecipientsCountOnDay(final long orgId, final long userId);

    /**
     * Deletes the {@link Collection} of {@link BulkMail} records for the given
     * organization, user, and a collection of bulk mail ids.
     *
     * @param orgId       Represents an organization id
     * @param userId      Represents a user id
     * @param bulkMailIds Represents the {@link Collection} of bulk mail id
     */
    void deleteBulkMails(final long orgId, final long userId, final Collection<Long> bulkMailIds);

    /**
     * Retrieves a {@link BulkMail} record for the given organization, user, and bulk mail.
     *
     * @param orgId      Represents an organization id
     * @param userId     Represents a user id
     * @param bulkMailId Represents the bulk mail id
     * @return {@link BulkMail}
     */
    BulkMail getBulkMail(final long orgId, final long userId, final long bulkMailId);

    /**
     * Checks if a {@link BulkMail} exists for the given organization.
     *
     * @param orgId       Represents an organization id
     * @param bulkMailIds Represents the {@link Set} of bulk mail id
     * @return true, when the corresponding {@link BulkMail} is exists
     */
    boolean exists(final long orgId, final Set<Long> bulkMailIds);

    /**
     * Retrieves the count of recipients for a specific bulk mail associated with the provided organization id and bulk mail id.
     *
     * @param orgId      Represents an organization id
     * @param bulkMailId Represents the bulk mail id
     * @return the total recipients count
     */
    Long getBulkMailRecipientsCount(final long orgId, final long bulkMailId);

    /**
     * Retrieves a {@link Collection} of {@link MailRecipientInfo} for a specific {@link BulkMail} associated with the provided
     * organization id and bulk mail id, applying start and limit indices to fetch the records.
     *
     * @param orgId      Represents an organization id
     * @param bulkMailId Represents the bulk mail id
     * @param start      Represents the lower bound of recipients to be fetched
     * @param limit      Represents the upper bound of recipients to be fetched
     * @return {@link Collection} of {@link MailRecipientInfo}
     */
    Collection<MailRecipientInfo> getBulkMailRecipients(final long orgId, final long bulkMailId, final int start, final int limit);

    /**
     * Gets the email message and content from {@link BulkMail} based on the given conversation id
     *
     * @param orgId          Represents an organization id
     * @param conversationId Represents the conversation id
     * @return {@link BulkMail}
     */
    EntityEmail getEntityEmail(final long orgId, final long conversationId);

    /**
     * <p>
     * Processes the {@link EntityOutboundEmail} of both subject and content to get the placeholders
     * adequate data and fill in {@link MailInfo}
     * </p>
     *
     * @param orgId          Represents an organization id
     * @param outboundEmail  Represents the {@link MailInfo}
     * @param contactEmailId Represents the contact email id
     * @param contactId      Represents the deal participant contact ID
     */
    void fillPlaceHolderValues(final long orgId, final EntityOutboundEmail outboundEmail, final Long contactEmailId, final Long contactId);

    /**
     * <p>
     * Gets the drafted {@link Collection} of message ids for an respective bulkMail ids.
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param userId      Represents the user id
     * @param bulkMailIds Represents the {@link Set} of bulk mail ids
     * @return {@link Collection} of conversation ids
     */
    Collection<Long> getDraftedConversationIds(final long orgId, final long userId, final Set<Long> bulkMailIds);
}
