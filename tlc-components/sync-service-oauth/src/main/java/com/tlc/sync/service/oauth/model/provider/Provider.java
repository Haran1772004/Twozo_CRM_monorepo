package com.tlc.sync.service.oauth.model.provider;

import com.tlc.sync.service.oauth.model.SyncServiceCategory;
import com.tlc.sync.service.oauth.service.sync.SyncService;

/**
 * <p>
 * A interface that represents the provider.
 * </p>
 *
 * @author Ajith venkadesh
 * @version 1.0
 */
public interface Provider {

    /**
     * <p>
     * Retrieves the sync service associated with the provider.
     * </p>
     *
     * @param <S> The type of the service, which extends {@link SyncService}.
     * @return The sync service associated with the provider.
     */
    <S extends SyncService> S getSyncService();

    /**
     * <p>
     * Gets the name of the provider.
     * </p>
     *
     * @return name of the provider.
     */
    String getName();

    /**
     * <p>
     * Gets the id of the provider.
     * </p>
     *
     * @return id of the provider.
     */
    int getId();

    /**
     * <p>
     * Gets the {@link SyncServiceCategory}
     * </p>
     *
     * @return the {@link SyncServiceCategory}
     */
    SyncServiceCategory serviceCategory();
}