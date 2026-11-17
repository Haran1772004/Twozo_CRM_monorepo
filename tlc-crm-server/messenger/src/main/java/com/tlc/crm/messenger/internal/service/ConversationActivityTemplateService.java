package com.tlc.crm.messenger.internal.service;

import com.tlc.crm.messenger.api.conversation.model.ConversationActivityTemplate;
import com.tlc.crm.messenger.internal.service.impl.ConversationActivityTemplateServiceImpl;

/**
 * <p>
 * Offers the access to meta data of the conversation's activity template.
 * </p>
 *
 * @author Ajay.
 * @version 1.0
 */
public interface ConversationActivityTemplateService {

    static ConversationActivityTemplateService getInstance() {
        return ConversationActivityTemplateServiceImpl.getInstance();
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