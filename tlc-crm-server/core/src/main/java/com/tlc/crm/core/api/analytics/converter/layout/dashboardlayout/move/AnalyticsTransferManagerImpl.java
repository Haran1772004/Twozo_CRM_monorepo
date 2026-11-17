package com.tlc.crm.core.api.analytics.converter.layout.dashboardlayout.move;

import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMChildViewDetails;

import com.tlc.pk.transfer.TransferManager;
import com.tlc.pk.transfer.TransferManagerFactory;
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
class AnalyticsTransferManagerImpl implements AnalyticsTransferManager
{
    private final TransferManager<EntityTransferConfig<Long, CRMChildViewDetails>> entityTransferManager = TransferManagerFactory.getEntityTransferManager();

    private static class Instance
    {
        private static final AnalyticsTransferManagerImpl ANALYTICS_TRANSFER_MANAGER = new AnalyticsTransferManagerImpl();
    }

    public static AnalyticsTransferManagerImpl getInstance()
    {
        return Instance.ANALYTICS_TRANSFER_MANAGER;
    }

    /**
     * {@inheritDoc}
     *
     * @param config Represents the {@link EntityTransferConfig} details
     */
    @Override
    public void move( final @NonNull EntityTransferConfig<Long, CRMChildViewDetails> config ) throws Exception
    {
        entityTransferManager.move( config );
    }
}
