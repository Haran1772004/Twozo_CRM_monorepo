package com.tlc.crm.core.conversation.api.manager;

import com.tlc.crm.core.conversation.api.manager.internal.EmailEntityConversationManagerImpl;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.dml.Table;

import java.util.Collection;
import java.util.Map;

/**
 * Manages email entity conversation related DB actions
 *
 * @author kishore J
 * @version 1.0
 */
public interface EmailEntityConversationManager {

    /**
     * <p>
     * Gets the {@link EmailEntityConversationManager}
     * </p>
     *
     * @return The {@link EmailEntityConversationManager}
     */
    static EmailEntityConversationManager getInstance() {
        return EmailEntityConversationManagerImpl.getInstance();
    }

    /**
     * <p>
     * Gets the email conversation table
     * </p>
     *
     * @return the email conversation table
     */
    Table getTable();

    /**
     * <p>
     * Loads the email entity conversations row into container
     * </p>
     *
     * @param orgId          Represents an organization id
     * @param userId         Represents the user id
     * @param dealId         Represents the deal id
     * @param conversationId Represents the conversation id
     * @param contactIds     Represents the {@link Collection} of contact ids
     * @param companyIds     Represents the {@link Collection} of company ids
     * @param dataContainer  Holds the updated information of field values provided by user
     */
    void loadEmailEntityConversationRowIntoContainer(final Long orgId, final Long userId, final Long dealId, final Long conversationId,
                                                     final Collection<Long> contactIds, final Collection<Long> companyIds, final DataContainer dataContainer);

    /**
     * <p>
     * Updates the timestamp of entity conversation row, when it was a drafted mail
     * </p>
     *
     * @param orgId          Represents an organization id
     * @param userId         Represents the user id
     * @param conversationId Represents the conversation id
     */
    void UpdateEmailConversationCreatedTime(final long orgId, final long userId, final Long conversationId);

    /**
     * <p>
     * Checks if a conversation exists for the given org id, user id and thread id and source
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param userId   Represents an user id
     * @param threadId Represents the thread id
     * @param source   Represents the {@link Source}
     * @return true if the conversation exists, otherwise false
     */
    boolean isConversationExistsForSource(final long orgId, final long userId, final long threadId, final Source source);

    /**
     * <p>
     * Fetches the count of email messages for the given org id, entity id and {@link Source}
     * </p>
     *
     * @param orgId    Represents an org id
     * @param entityId Represents  an entity id
     * @param source   Represents the {@link Source}
     * @return The count of email messages
     */
    Long getEmailMessageCount(final long orgId, final long entityId, final Source source);

    /**
     * <p>
     * Loads email entity conversations into the data container
     * </p>
     *
     * @param orgId          Represents an org id
     * @param userId         Represents an user id
     * @param threadId       Represents the thread id
     * @param conversationId Represents conversation id
     * @param contactIds     Which contains the collection of contact ids
     * @param companyIds     Which contains the collection of company ids
     * @param dealId         Represents the deal id
     * @param dataContainer  Holds the entity conversations entity related records
     */
    void loadEmailEntityConversationIntoContainer(final long orgId, final long userId, final long threadId, final long conversationId,
                                                  final Collection<Long> contactIds, final Collection<Long> companyIds, final Long dealId, final DataContainer dataContainer);

    /**
     * <p>
     * Fetches the email message counts for the given entity ids
     * </p>
     *
     * @param orgId     Represents an organization id
     * @param entityIds Which contains the collection of entity ids
     * @param source    Represents the  {@link Source}
     * @return A {@link Map} where key is the entity id value is the email message count
     */
    Map<Long, Long> getEmailMessageCounts(final long orgId, final Collection<Long> entityIds, final Source source);

    /**
     * <p>
     * Updates the entity conversation into the target {@link DataContainer}.
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param userId          Represents the user id
     * @param contactId       Represents the contact id
     * @param conversationId  Represents the conversation id
     * @param dataContainer   Holds the existing information in the existing {@link DataContainer}
     * @param targetContainer Holds the updated and new information about the conversations into the target {@link DataContainer}
     */
    void updateEmailEntityConversationIntoContainer(final long orgId, final long userId, final Long contactId, final Long conversationId,
                                                    final DataContainer dataContainer, final DataContainer targetContainer);

    /**
     * <p>
     * Deletes the unmapped entity conversation row
     * </p>
     *
     * @param conversationId   Represents the conversation ID
     * @param sourceContainer  Holds the existing information in the existing {@link DataContainer}
     * @param targetContainer  Holds the updated and new information about the conversations into the target {@link DataContainer}
     */
     void deleteUnmappedEntityConversationRow(final Long conversationId, final DataContainer sourceContainer, final DataContainer targetContainer);
}
