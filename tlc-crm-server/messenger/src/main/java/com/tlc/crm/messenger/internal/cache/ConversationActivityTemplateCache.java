package com.tlc.crm.messenger.internal.cache;

import com.tlc.crm.common.cache.LocalCache;
import com.tlc.crm.messenger.api.conversation.model.ConversationActivityTemplate;
import com.tlc.crm.messenger.internal.cache.internal.ConversationActivityTemplateCacheImpl;

/**
 * <p>
 * Stores the meta data of the conversation's activity template.
 * </p>
 *
 * @author Ajay.
 * @version 1.0
 */
public interface ConversationActivityTemplateCache extends LocalCache {

    static ConversationActivityTemplateCache getInstance() {
        return ConversationActivityTemplateCacheImpl.getInstance();
    }

    /**
     * <p>
     * Gets {@link ConversationActivityTemplate}.
     * </p>
     *
     * @param typeId Template type's id.
     * @return {@link ConversationActivityTemplate}.
     */
    ConversationActivityTemplate get(final Long typeId);
}
