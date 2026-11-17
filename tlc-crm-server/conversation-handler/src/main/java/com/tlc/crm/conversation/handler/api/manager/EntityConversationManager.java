package com.tlc.crm.conversation.handler.api.manager;

import com.tlc.crm.conversation.handler.api.manager.internal.EntityConversationManagerImpl;
import com.tlc.crm.core.conversation.api.model.ConversableModel;
import com.tlc.crm.core.conversation.api.model.EntityConversation;
import com.tlc.crm.entity.fields.source.Source;

import java.util.Collection;

/**
 * Manages the entity conversation DB related actions
 *
 * @author kishore J
 * @version 1.0
 */
public interface EntityConversationManager {

    /**
     * <p>
     * Gets the {@link EntityConversationManager}
     * </p>
     *
     * @return The {@link EntityConversationManager}
     */
    static EntityConversationManager getInstance() {
        return EntityConversationManagerImpl.getInstance();
    }

    /**
     * <p>
     * Fetches the count of conversations for a given entity based on the specified
     * source and conversation types.
     * </p>
     *
     * @param orgId    Represents an org id
     * @param userId   Represents an user id
     * @param entityId Represents an entity id
     * @param source   Represents the {@link Source}
     * @param types    Which contains collection of {@link EntityConversation.Type}
     * @return The total number of matching conversations
     */
    long getSourceConversationCount(final long orgId, final long userId, final long entityId,
                                    final Source source, final Collection<EntityConversation.Type> types);

    /**
     * <p>
     * Retrieves the mails associated with the given entity id
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param userId   Represents an user id
     * @param entityId Represents an entity id
     * @param types    Which contains a collection of {@link EntityConversation.Type}
     * @param source   Represents the {@link Source}
     * @param start    Start value represents the lower bound of conversations to be fetched
     * @param limit    Limit value represents the upper bound of conversations to be fetched
     * @return Collection of {@link EntityConversation}
     */
    Collection<EntityConversation<? extends ConversableModel>> getConversations(final long orgId, final Long userId, final Long entityId,
                                                                                final Collection<EntityConversation.Type> types,
                                                                                final Source source, final int start, final int limit);

    /**
     * <p>
     * Deletes the entity conversation
     * </p>
     *
     * @param orgId                Represents the org id
     * @param entityConversationId Represents the entity conversation id
     */
    void delete(final long orgId, final long entityConversationId);

    /**
     * <p>
     * Checks whether the entity conversation exists
     * </p>
     *
     * @param orgId                Represents the org id
     * @param entityConversationId Represents the entity conversation id
     * @return true, if the entity conversation exists, false otherwise
     */
    boolean exists(final long orgId, final long entityConversationId);
}
