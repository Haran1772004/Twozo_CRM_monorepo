package com.tlc.crm.messenger.page;

import com.tlc.commons.page.Page;
import com.tlc.crm.messenger.api.channel.manager.WABusinessAccountManager;
import com.tlc.crm.messenger.api.channel.model.CrmWAPhoneNumber;

import java.util.Collection;

/**
 * <p>
 * Pagination handler for the {@link CrmWAPhoneNumber}.
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
public class WAChannelPageHandler extends ChannelPageHandler<CrmWAPhoneNumber> {

    private final WABusinessAccountManager waBusinessAccountManager;
    private final Collection<Short> stateIds;

    public WAChannelPageHandler(final int start, final int limit, final Long orgId, final Collection<Short> stateIds) {
        super(start, limit, orgId);
        waBusinessAccountManager = WABusinessAccountManager.getInstance();
        this.stateIds = stateIds;
    }

    /**
     * {@inheritDoc}
     *
     * @return the {@link Page}
     */
    @Override
    public Page<CrmWAPhoneNumber> fetchPageData() {
        final Page<CrmWAPhoneNumber> page = new Page<>();

        page.setData(waBusinessAccountManager.getPhoneNumbers(orgId, recordStartingIndex, recordsPerPage, stateIds));

        return page;
    }

    /**
     * {@inheritDoc}
     *
     * @return the total count
     */
    @Override
    public long totalRecordsCount() {
        return waBusinessAccountManager.getPhoneNumberCount(orgId, stateIds);
    }
}