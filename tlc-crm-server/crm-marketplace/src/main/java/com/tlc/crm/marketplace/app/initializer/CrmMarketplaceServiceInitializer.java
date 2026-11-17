package com.tlc.crm.marketplace.app.initializer;

import com.tlc.commons.service.initializer.ServiceInitializer;
import com.tlc.commons.util.Configurations;
import com.tlc.crm.marketplace.app.listener.CrmMarketplaceAppListener;
import com.tlc.crm.marketplace.app.sql.resource.MKPCRMAPPS;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.ds.WritableDataStore;

/**
 * <p>
 * An initializer that initializes the marketplace app service.
 * </p>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public class CrmMarketplaceServiceInitializer implements ServiceInitializer {

    private final WritableDataStore writableDataStore;
    private final CrmMarketplaceAppListener crmMarketplaceAppListener;

    private CrmMarketplaceServiceInitializer() {
        writableDataStore = DataStoreProvider.getWritableDataStore();
        crmMarketplaceAppListener = CrmMarketplaceAppListener.getInstance();
    }

    private static final class Instance {
        private static final CrmMarketplaceServiceInitializer MARKETPLACE_SERVICE_INITIALIZER = new CrmMarketplaceServiceInitializer();
    }

    /**
     * <p>
     * Gets the instance of {@link CrmMarketplaceServiceInitializer}
     * </p>
     *
     * @return The instance of {@link CrmMarketplaceServiceInitializer}
     */
    public static CrmMarketplaceServiceInitializer getInstance() {
        return Instance.MARKETPLACE_SERVICE_INITIALIZER;
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Holds the configuration properties.
     */
    @Override
    public void preInitialize(final Configurations configurations) {
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Holds the configuration properties.
     */
    @Override
    public void postInitialize(final Configurations configurations) {
        writableDataStore.registerUpdatedEntryListener(Table.get(MKPCRMAPPS.TABLE), crmMarketplaceAppListener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
    }
}