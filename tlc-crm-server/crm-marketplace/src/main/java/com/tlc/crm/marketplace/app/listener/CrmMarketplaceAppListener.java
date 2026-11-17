package com.tlc.crm.marketplace.app.listener;

import com.tlc.crm.marketplace.app.internal.cache.CrmMarketplaceAppCache;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.listeners.RowUpdateListener;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * A Listener that listens create/update/ actions of CrmMarketplaceApp
 *
 * @author Muthu kumar v
 * @version 1.0
 */
public final class CrmMarketplaceAppListener implements RowUpdateListener {

    private final CrmMarketplaceAppCache crmMarketplaceAppCache;

    private final static class InstanceHolder {
        private static final CrmMarketplaceAppListener MARKETPLACE_APP_LISTENER = new CrmMarketplaceAppListener();
    }

    private CrmMarketplaceAppListener() {
        crmMarketplaceAppCache = CrmMarketplaceAppCache.getInstance();
    }

    /**
     * <p>
     * Gets the instance of {@link CrmMarketplaceAppListener}
     * </p>
     *
     * @return The instance of {@link CrmMarketplaceAppListener}
     */
    public static CrmMarketplaceAppListener getInstance() {
        return InstanceHolder.MARKETPLACE_APP_LISTENER;
    }

    /**
     * {@inheritDoc}
     *
     * @param rows Refers the collection of marketplace app rows.
     */
    @Override
    public void updateRows(final Collection<Row> rows) {
        crmMarketplaceAppCache.remove(rows.stream().map(Row::getPKValue).collect(Collectors.toSet()));
    }
}