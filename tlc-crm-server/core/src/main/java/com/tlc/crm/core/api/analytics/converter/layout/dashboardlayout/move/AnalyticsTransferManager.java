package com.tlc.crm.core.api.analytics.converter.layout.dashboardlayout.move;

import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMChildViewDetails;

import com.tlc.pk.transfer.model.EntityTransferConfig;

import lombok.NonNull;

/**
 * <p>
 * Manages the transfer of {@link CRMChildViewDetails} from the source to the target for move operations.
 * </p>
 *
 * @author pandiarajkumar G
 * @version 1.0
 */
public interface AnalyticsTransferManager
{

    static AnalyticsTransferManager getInstance()
    {
        return AnalyticsTransferManagerImpl.getInstance();
    }

    /**
     * <p>
     * Moves the given view from one dashboard to another
     * </p>
     *
     * @param config Represents the {@link EntityTransferConfig} details
     **/
    void move( final @NonNull EntityTransferConfig<Long, CRMChildViewDetails> config ) throws Exception;
}
