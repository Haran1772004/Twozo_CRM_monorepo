package com.tlc.crm.marketplace.app.internal.cache.resolver;

import com.tlc.crm.marketplace.app.api.manager.CrmMarketplaceCatalogManager;
import com.tlc.crm.marketplace.app.api.model.CrmMarketplaceApp;
import com.tlc.crm.marketplace.app.internal.cache.CrmMarketplaceAppCache;

import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Implementation of the MarketplaceAppResolver interface responsible for resolving MarketplaceApp instances.
 *
 * @author ThalaimalaiPandiyanT, Muthu kumar V
 * @version 1.0
 */
final class CrmMarketplaceAppResolverImpl implements CrmMarketplaceAppResolver {

    private final CrmMarketplaceAppCache crmMarketplaceAppCache;
    private final ReentrantReadWriteLock lock;
    private final CrmMarketplaceCatalogManager crmMarketplaceCatalogManager;

    private CrmMarketplaceAppResolverImpl() {
        this.crmMarketplaceAppCache = CrmMarketplaceAppCache.getInstance();
        this.lock = new ReentrantReadWriteLock();
        this.crmMarketplaceCatalogManager = CrmMarketplaceCatalogManager.getInstance();
    }

    private static class Instance {
        private static final CrmMarketplaceAppResolverImpl INSTANCE = new CrmMarketplaceAppResolverImpl();
    }

    /**
     * Gets the {@link CrmMarketplaceAppResolverImpl} instance.
     *
     * @return {@link CrmMarketplaceAppResolverImpl}.
     */
    public static CrmMarketplaceAppResolverImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param id The unique ID of the MarketplaceApp to retrieve.
     * @return The {@code MarketplaceApp} with the specified ID, or {@code null} if not found.
     */
    @Override
    public CrmMarketplaceApp get(final long id) {
        final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
        final CrmMarketplaceApp crmMarketplaceApp;

        readLock.lock();

        try {
            crmMarketplaceApp = crmMarketplaceAppCache.get(id);
        } finally {
            readLock.unlock();
        }

        if (Objects.isNull(crmMarketplaceApp)) {
            final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

            writeLock.lock();

            try {
                final CrmMarketplaceApp app = crmMarketplaceCatalogManager.get(id);

                crmMarketplaceAppCache.setMarketplaceApp(app);
                return app;
            } finally {
                writeLock.unlock();
            }
        }
        return crmMarketplaceApp;
    }
}