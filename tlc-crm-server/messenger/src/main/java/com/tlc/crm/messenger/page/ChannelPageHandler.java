package com.tlc.crm.messenger.page;

import com.tlc.commons.page.Page;
import com.tlc.crm.messenger.api.channel.manager.AbstractPageManager;
import com.tlc.crm.messenger.api.channel.model.CrmFBPage;
import com.tlc.crm.messenger.api.channel.model.CrmSocialBusinessChannel;

/**
 * <p>
 * Handles channels page's pagination.
 * </p>
 *
 * @param <T> {@link CrmFBPage}.
 * @author Ajay.
 * @version 1.0.
 */
public abstract class ChannelPageHandler<T extends CrmSocialBusinessChannel> extends MessengerPageHandler<T> {

    protected AbstractPageManager<T> channelManager;

    protected ChannelPageHandler(final int start, final int limit, final Long orgId,
                                 final AbstractPageManager<T> channelManager) {
        super(start, limit, orgId);
        this.channelManager = channelManager;
    }

    protected ChannelPageHandler(final int start, final int limit, final Long orgId) {
        super(start, limit, orgId);
    }

    /**
     * {@inheritDoc}
     *
     * @return the {@link Page< CrmFBPage >}
     */
    @Override
    public Page<T> fetchPageData() {
        final Page<T> page = new Page<>();

        page.setData(channelManager.getPages(orgId, recordStartingIndex, recordsPerPage));

        return page;
    }

    /**
     * {@inheritDoc}
     *
     * @return the total count
     */
    @Override
    public long totalRecordsCount() {
        return channelManager.getPageCount(orgId);
    }
}