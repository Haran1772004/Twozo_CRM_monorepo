package com.tlc.marketplace.api.manager;

import com.tlc.marketplace.model.MarketplaceApp;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.ds.AdminDataStore;

import com.tlc.sql.common.provider.SqlServiceProvider;

import java.util.Collection;

/**
 * Abstract base class for managing database actions related to marketplace models.
 *
 * @author ThalaimalaiPandiyanT, Muthu kumar V
 * @version 1.0
 */
abstract class AbstractMarketplaceManager {

    protected final AdminDataStore adminDataStore;

    protected AbstractMarketplaceManager() {
        adminDataStore = SqlServiceProvider.get().getAdminDataStore();
    }

    /**
     * Gets the {@link Table}.
     *
     * @return The marketplace {@link Table}.
     */
    protected abstract Table getTable();

    /**
     * Checks if a marketplace record with the given ID exists in the database.
     *
     * @param id The unique identifier of the marketplace entity.
     * @return {@code true} if the entity exists, {@code false} otherwise.
     */
    protected abstract boolean exists(final Long id);

    /**
     * Retrieves all marketplace entities from the database.
     *
     * @return A collection of all available marketplace entities.
     */
    protected abstract Collection<MarketplaceApp> getAll(final int start, final int limit);

    /**
     * Fetches a specific marketplace entity by its unique ID.
     *
     * @param id The unique identifier of the marketplace entity.
     * @return The entity corresponding to the given ID, or {@code null} if not found.
     */
    protected abstract MarketplaceApp get(final Long id);
}