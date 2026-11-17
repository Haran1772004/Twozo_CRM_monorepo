package com.tlc.crm.messenger.api.chat.model;

import com.tlc.crm.core.conversation.api.model.EntityConversation;

/**
 * Model which represents the {@link ChatEntityConversation}
 *
 * @author Maharaja S
 * @version 1.0
 */
public class ChatEntityConversation  extends EntityConversation<CrmChat> {

    private final CrmChat chat;

    public ChatEntityConversation(final CrmChat chat){
        this.chat = chat;
    }

    /**
     * {@inheritDoc}
     *
     * @return the {@link CrmChat}
     */
    @Override
    public CrmChat getConversableModel() {
        return chat;
    }
}