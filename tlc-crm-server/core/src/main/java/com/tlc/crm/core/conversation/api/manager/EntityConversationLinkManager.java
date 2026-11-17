package com.tlc.crm.core.conversation.api.manager;

import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.model.Deal;

import com.tlc.crm.core.conversation.api.manager.internal.EntityConversationLinkManagerImpl;
import com.tlc.crm.entity.fields.source.Source;

import java.util.Collection;

/**
 * <p>
 * Performs the functionality for {@link EntityConversationLinkManager} DB related actions
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public interface EntityConversationLinkManager {

    /**
     * <p>
     * Gets the  {@link EntityConversationLinkManager} instance
     * </p>
     *
     * @return The {@link EntityConversationLinkManager}
     */
    static EntityConversationLinkManager getInstance() {
        return EntityConversationLinkManagerImpl.getInstance();
    }

    /**
     * <p>
     * Links a thread to a deal for the given org id, user id, deal id, thread id
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param userId   Represents a user id
     * @param dealId   Represents the deal id
     * @param threadId Represents the thread id
     */
    void linkThreadToDeal(final long orgId, final long userId, final long dealId, final long threadId);

    /**
     * <p>
     * Adds an email to an existing contact for the given org id, user id, thread id, contact id and email
     * </p>
     *
     * @param orgId      Represents an organization id
     * @param userId     Represents a user id
     * @param threadId   Represents the thread id
     * @param contactId  Represents the contact id
     * @param emailValue Represents the email value
     */
    void addEmailToExistingContact(final long orgId, final long userId, final long threadId,
                                   final long contactId, final String emailValue);

    /**
     * <p>
     * Links the deal with the thread by creating a deal
     * </p>
     *
     * @param orgId    Represents an organization object
     * @param userId   Represents a user id
     * @param deal     Represents the deal entity
     * @param threadId Represents the thread id
     */
    void createNewDealAndLinkConversation(final long orgId, final long userId, final Deal deal, final long threadId);

    /**
     * <p>
     * Creates a new contact and links it to a conversation for the given org id, user id, thread id and contact
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param userId   Represents a user id
     * @param threadId Represents the thread id
     * @param contact  Represents the {@link Contact}
     */
    void createNewContactAndLinkConversation(final long orgId, final long userId, final long threadId, final Contact contact);

    /**
     * <p>
     * Unlinks entity conversations for the given organization id
     * </p>
     *
     * @param orgId                             Represents an organization id
     * @param userId                            Represents a user id
     * @param threadId                          Represents the thread id
     * @param source                            Represents the  {@link Source}
     * @param entityId                          Represents entity id
     * @param entityConversationIds             Which contains collection of entity conversation id
     * @param isRemoveRelatedEntityConversation A flag indicating whether to remove entity conversations based on the provided source and entity conversation ids
     */
    void unlinkEntityConversation(final long orgId, final long userId, final long threadId, final Source source, final long entityId,
                                  final Collection<Long> entityConversationIds, final boolean isRemoveRelatedEntityConversation);

    /**
     * <p>
     * Checks if there are existing entity conversations for the given organization id, conversation ids and {@link Source}
     * </p>
     *
     * @param orgId                 Represents an organization ids
     * @param entityConversationIds Which contains collection of entity conversation ids
     * @param source                Represents the {@link Source}
     * @return true if there are existing entity conversations, otherwise false
     */
    boolean isEntityConversationsExists(final long orgId, final Collection<Long> entityConversationIds, final Source source);

    /**
     * <p>
     * Fetches a collections of entity conversation ids for the given org id, user id and thread id
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param userId   Represents a user id
     * @param threadId Represents the thread id
     * @return The {@link Collection} of entity conversations ids
     */
    Collection<Long> getEntityConversationIds(final long orgId, final long userId, final long threadId);
}
