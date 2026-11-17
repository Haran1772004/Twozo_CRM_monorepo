package com.tlc.crm.marketplace.app.internal.cache;

import com.tlc.crm.common.cache.RemoteCache;
import com.tlc.crm.marketplace.app.api.model.CrmMarketplaceApp;

import java.util.Collection;
import java.util.Set;

/**
 * Responsible for caching MarketplaceApp instances.
 *
 * @author ThalaimalaiPandiyanT, Muthu kumar V
 * @version 1.0
 */
public interface CrmMarketplaceAppCache extends RemoteCache {

    /**
     * Retrieves the instance of {@link CrmMarketplaceAppCache}.
     *
     * @return The {@link CrmMarketplaceAppCache} instance.
     */
    static CrmMarketplaceAppCache getInstance() {
        return CrmMarketplaceAppCacheImpl.getInstance();
    }

    /**
     * Retrieves a {@link CrmMarketplaceApp} by its unique identifier.
     *
     * @param id The unique ID of the MarketplaceApp to retrieve.
     * @return The {@code CrmMarketplaceApp} with the specified ID, or {@code null} if not found.
     */
    CrmMarketplaceApp get(final long id);

    /**
     * Sets the marketplace application in the cache.
     *
     * @param crmMarketplaceApp The {@code CrmMarketplaceApp}.
     */
    void setMarketplaceApp(final CrmMarketplaceApp crmMarketplaceApp);

    /**
     * Sets the collection of marketplace applications to be resolved.
     *
     * @param crmMarketplaceApps The collection of MarketplaceApp instances.
     */
    void setMarketplaceApps(final Collection<CrmMarketplaceApp> crmMarketplaceApps);

    /**
     * <p>
     * Removes the set of app ids
     * </p>
     *
     * @param appIds The app ids to remove from the cache
     */
    void remove(final Set<Long> appIds);
}