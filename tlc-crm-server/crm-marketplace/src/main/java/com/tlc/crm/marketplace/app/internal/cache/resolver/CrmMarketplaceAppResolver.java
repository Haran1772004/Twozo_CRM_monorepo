package com.tlc.crm.marketplace.app.internal.cache.resolver;

import com.tlc.crm.marketplace.app.api.model.CrmMarketplaceApp;

/**
 * Interface responsible for resolving MarketplaceApp instances.
 *
 * @author ThalaimalaiPandiyanT, Muthu kumar V
 * @version 1.0
 */
public interface CrmMarketplaceAppResolver {

    /**
     * Retrieves the instance of {@link CrmMarketplaceAppResolver}.
     *
     * @return The {@link CrmMarketplaceAppResolver} instance.
     */
    static CrmMarketplaceAppResolver getInstance() {
        return CrmMarketplaceAppResolverImpl.getInstance();
    }

    /**
     * Retrieves a {@link CrmMarketplaceApp} by its unique identifier.
     *
     * @param id The unique ID of the MarketplaceApp to retrieve.
     * @return The {@code MarketplaceApp} with the specified ID, or {@code null} if not found.
     */
    CrmMarketplaceApp get(final long id);
}