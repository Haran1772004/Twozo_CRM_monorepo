package com.tlc.crm.marketplace.app.service;

import com.tlc.crm.marketplace.app.api.model.CrmMarketplaceApp;

import java.util.Collection;

/**
 * Provides services to handle the {@code MarketplaceApp}.
 *
 * @author ThalaimalaiPandiyanT, Muthu kumar V
 * @version 1.0
 */
public interface CrmMarketplaceAppService {

    /**
     * Retrieves the instance of {@link CrmMarketplaceAppService}.
     *
     * @return The {@link CrmMarketplaceAppService} instance.
     */
    static CrmMarketplaceAppService getInstance() {
        return CrmMarketplaceAppServiceImpl.getInstance();
    }

    /**
     * Retrieves a {@link CrmMarketplaceApp} by its unique identifier.
     *
     * @param id The unique ID of the MarketplaceApp to retrieve.
     * @return The {@code MarketplaceApp} with the specified ID, or {@code null} if not found.
     */
    CrmMarketplaceApp get(final long id);
}