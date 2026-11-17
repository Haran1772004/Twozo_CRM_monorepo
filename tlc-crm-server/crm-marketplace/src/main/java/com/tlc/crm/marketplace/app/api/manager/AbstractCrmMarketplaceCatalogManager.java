package com.tlc.crm.marketplace.app.api.manager;

import com.tlc.crm.marketplace.app.api.model.CrmMarketplaceApp;
import com.tlc.crm.security.sql.DataStoreProvider;

import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.sql.common.api.ds.ReadableDataStore;

import java.util.Collection;

/**
 * Abstract base class for managing database actions related to crm marketplace models.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public abstract class AbstractCrmMarketplaceCatalogManager {

    protected final ReadableDataStore readableDataStore;

    protected AbstractCrmMarketplaceCatalogManager() {
        this.readableDataStore = DataStoreProvider.getReadableDataStore();
    }

    /**
     * Gets the {@code OrgDataStore}.
     *
     * @param orgId Represents the organization id.
     * @return The {@link OrgDataStore}.
     */
    protected final OrgDataStore orgDataStore(final Long orgId) {
        return DataStoreProvider.getSecureOrgDataStore(orgId);
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
     * Fetches a specific marketplace entity by its unique ID.
     *
     * @param id The unique identifier of the marketplace entity.
     * @return The entity corresponding to the given ID, or {@code null} if not found.
     */
    protected abstract CrmMarketplaceApp get(final Long id);

    /**
     * Gets the {@code MarketplaceApp} name.
     *
     * @param appId Represents the name of the marketplace app id.
     * @return The marketplace app name.
     */
    protected abstract String getAppName(final Long appId);

    /**
     * Gets the {@code MarketplaceApp} by name.
     *
     * @param appName The name of the marketplace app.
     * @return The {@link CrmMarketplaceApp}.
     */
    protected abstract CrmMarketplaceApp getByName(final String appName);

    /**
     * Retrieves a collection of marketplace applications filtered by a search text.
     *
     * @param searchText The text to search for in-app names or descriptions.
     * @param start      The starting index for pagination.
     * @param limit      The maximum number of apps to return.
     * @return A collection of MarketplaceApp objects matching the search criteria.
     */
    protected abstract Collection<CrmMarketplaceApp> getApps(final String searchText, final int start, final int limit);
}