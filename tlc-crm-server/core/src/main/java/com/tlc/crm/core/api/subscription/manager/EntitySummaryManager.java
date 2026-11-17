package com.tlc.crm.core.api.subscription.manager;

import com.tlc.crm.core.api.subscription.manager.impl.EntitySummaryManagerImpl;
import com.tlc.crm.core.api.subscription.model.DataLossSummary;

/**
 * Provides data-loss summary details for organizations.
 *
 * @author Sathish
 * @version 1.0
 */
public interface EntitySummaryManager {

    static EntitySummaryManager getInstance() {
        return EntitySummaryManagerImpl.getInstance();
    }

    /**
     * Fetches the data-loss summary for the given organization.
     *
     * @param orgId The unique identifier of the organization.
     * @return {@link DataLossSummary}.
     */
    DataLossSummary get(final long orgId);
}