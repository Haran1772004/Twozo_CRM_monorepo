package com.tlc.crm.messenger.internal.resolver;

import com.tlc.crm.messenger.api.conversation.model.ConversationActivityTemplate;
import com.tlc.crm.messenger.internal.resolver.impl.ConversationActivityTemplateResolverImpl;

/**
 * <p>
 * Resolves the meta data of the conversation's activity template.
 * </p>
 *
 * @author Ajay.
 * @version 1.0
 */
public interface ConversationActivityTemplateResolver {

    /**
     * <p>
     * Gets {@link ConversationActivityTemplateResolver}.
     * </p>
     *
     * @return {@link ConversationActivityTemplateResolver}.
     */
    static ConversationActivityTemplateResolver getInstance() {
        return ConversationActivityTemplateResolverImpl.getInstance();
    }

    /**
     * <p>
     * Resolves {@link ConversationActivityTemplate}.
     * </p>
     *
     * @param typeId Template type's id.
     * @return {@link ConversationActivityTemplate}.
     */
    ConversationActivityTemplate resolve(final Long typeId);
}