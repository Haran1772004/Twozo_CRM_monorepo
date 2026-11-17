package com.tlc.crm.messenger.page;

import com.tlc.commons.page.Page;
import com.tlc.crm.messenger.api.chat.manager.ChatManager;
import com.tlc.crm.messenger.api.chat.model.CrmChat;
import com.tlc.crm.messenger.api.chat.model.CrmChatFilter;

/**
 * <p>
 * Page handler for the {@link CrmChat}.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class ChatPageHandler extends MessengerPageHandler<CrmChat> {

    private final ChatManager crmChatManager;
    private final CrmChatFilter filter;
    private final String searchCriteria;

    public ChatPageHandler(final int start, final int limit, final Long orgId, final CrmChatFilter filter, final String searchCriteria) {
        super(start, limit, orgId);
        crmChatManager = ChatManager.getInstance();
        this.filter = filter;
        this.searchCriteria = searchCriteria;
    }

    /**
     * {@inheritDoc}
     *
     * @return chat page.
     */
    @Override
    public Page<CrmChat> fetchPageData() {
        final Page<CrmChat> page = new Page<>();

        page.setData(crmChatManager.get(orgId, recordStartingIndex, recordsPerPage, filter, searchCriteria));

        return page;
    }

    /**
     * {@inheritDoc}
     *
     * @return chat count.
     */
    @Override
    public long totalRecordsCount() {
        return crmChatManager.getChatCount(orgId, filter, searchCriteria);
    }
}