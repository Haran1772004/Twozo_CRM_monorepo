package com.tlc.crm.messenger.page;

import com.tlc.commons.page.Page;
import com.tlc.crm.messenger.api.conversation.manager.ConversationManager;
import com.tlc.crm.messenger.api.conversation.model.Conversation;

import java.util.List;

/**
 * <p>
 * Page handler for the {@link Conversation}.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class ConversationPageHandler extends MessengerPageHandler<Conversation> {

    private final ConversationManager conversationManager;
    private final Long chatId;

    public ConversationPageHandler(final int start, final int limit, final Long orgId, final Long chatId) {
        super(start, limit, orgId);
        this.conversationManager = ConversationManager.getInstance();
        this.chatId = chatId;
    }


    /**
     * {@inheritDoc}
     *
     * @return conversation page.
     */
    @Override
    public Page<Conversation> fetchPageData() {
        final Page<Conversation> page = new Page<>();

        page.setData(List.of(conversationManager.getConversation(orgId, chatId,  recordStartingIndex, recordsPerPage)));

        return page;
    }

    /**
     * {@inheritDoc}
     *
     * @return conversation count.
     */
    @Override
    public long totalRecordsCount() {
        return conversationManager.getConversationCount(orgId, chatId);
    }
}
