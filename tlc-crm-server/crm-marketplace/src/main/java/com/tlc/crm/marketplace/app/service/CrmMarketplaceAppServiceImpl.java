package com.tlc.crm.marketplace.app.service;

import com.tlc.crm.marketplace.app.api.model.CrmMarketplaceApp;
import com.tlc.crm.marketplace.app.internal.cache.resolver.CrmMarketplaceAppResolver;

import java.util.Collection;

/**
 * Provides services to handle the {@code CrmMarketplaceApp}.
 *
 * @author ThalaimalaiPandiyanT, Muthu kumar V
 * @version 1.0
 */
public class CrmMarketplaceAppServiceImpl implements CrmMarketplaceAppService {

    private final CrmMarketplaceAppResolver crmMarketplaceAppResolver;

    private CrmMarketplaceAppServiceImpl() {
        this.crmMarketplaceAppResolver = CrmMarketplaceAppResolver.getInstance();
    }

    private static class Instance {
        private static final CrmMarketplaceAppServiceImpl INSTANCE = new CrmMarketplaceAppServiceImpl();
    }

    /**
     * Gets the {@link CrmMarketplaceAppServiceImpl} instance.
     *
     * @return {@link CrmMarketplaceAppServiceImpl}.
     */
    public static CrmMarketplaceAppServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param id The unique ID of the MarketplaceApp to retrieve.
     * @return The {@code CrmMarketplaceApp} with the specified ID, or {@code null} if not found.
     */
    @Override
    public CrmMarketplaceApp get(final long id) {
        return crmMarketplaceAppResolver.get(id);
    }
}