package com.tlc.crm.core.page;

import com.tlc.commons.page.Page;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.model.Deal;

import java.util.ArrayList;
import java.util.Collection;

/**
 * <p>
 * Represents the page handler for the {@link Deal}
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public class DealMobilePageHandler extends SourceMobilePageHandler<Deal> {

    private final DealManager dealManager;
    private final Collection<Long> stateIds;

    public DealMobilePageHandler(final int start, final int limit) {
        recordStartingIndex = start;
        recordsPerPage = limit;
        dealManager = DealManager.getInstance();
        stateIds = new ArrayList<>();
    }

    /**
     * {@inheritDoc}
     *
     * @return the {@link Page<Deal>}
     */
    @Override
    public Page<Deal> fetchPageData() {
        final Page<Deal> page = new Page<>();

        page.setData(dealManager.getDealsBySource(getOrgId(), getEntityId(), getRecordStartingIndex(), getRecordsPerPage(), stateIds, getSource()));

        return page;
    }

    /**
     * {@inheritDoc}
     *
     * @return the total count
     */
    @Override
    public long totalRecordsCount() {
        return dealManager.getDealsCountBySource(getOrgId(), getEntityId(), stateIds, getSource());
    }

    /**
     * <p>
     * Adds the deal state id
     * </p>
     *
     * @param stateId Represents the deal state id
     */
    public void addState(final Long stateId) {
        stateIds.add(stateId);
    }
}

