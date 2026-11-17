package com.tlc.crm.messenger.page;

import com.tlc.crm.messenger.api.channel.manager.FBPageManager;
import com.tlc.crm.messenger.api.channel.model.CrmFBPage;

/**
 * <p>
 * Pagination handler for the {@link CrmFBPage}.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class FBChannelPageHandler extends ChannelPageHandler<CrmFBPage> {

    public FBChannelPageHandler(final int start, final int limit, final Long orgId) {
        super(start, limit, orgId, FBPageManager.getInstance());
    }
}