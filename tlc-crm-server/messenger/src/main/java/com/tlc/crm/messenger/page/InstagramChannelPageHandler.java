package com.tlc.crm.messenger.page;

import com.tlc.crm.messenger.api.channel.manager.InstagramPageManager;
import com.tlc.crm.messenger.api.channel.model.CrmInstagramPage;

/**
 * <p>
 * Pagination handler for the {@link CrmInstagramPage}.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class InstagramChannelPageHandler extends ChannelPageHandler<CrmInstagramPage> {

    public InstagramChannelPageHandler(final int start, final int limit, final Long orgId) {
        super(start, limit, orgId, InstagramPageManager.getInstance());
    }
}
