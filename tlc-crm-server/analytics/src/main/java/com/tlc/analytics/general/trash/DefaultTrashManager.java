package com.tlc.analytics.general.trash;

import com.tlc.crm.security.sql.DataStoreProvider;

import com.tlc.entity.EntityManager;
import com.tlc.entity.EntityManagerFactory;

import com.tlc.pk.trash.db.TrashEntity;

import com.tlc.sql.common.api.ds.OrgDataStore;

import lombok.NonNull;

import java.util.function.Supplier;

/**
 * Default implementation of a trash manager for database-backed trash operations.
 * This class extends BaseTrashManager to manage `TrashEntity` objects with specific
 * database interactions and organizational context handling.
 */
public abstract class DefaultTrashManager extends BaseTrashManager<TrashEntity>
{
    private final Supplier<OrgDataStore> supplier = () -> DataStoreProvider.getSecureOrgDataStore( getOrgId() );
    private final EntityManager<Long, TrashEntity> entityManager = EntityManagerFactory.create( TrashEntity.class, supplier );

    /**
     * Provides the entity manager used to interact with the database for trash operations.
     *
     * @return the EntityManager instance for performing database operations on TrashEntity
     */
    @Override
    protected final @NonNull EntityManager<Long, TrashEntity> getEntityManager()
    {
        return entityManager;
    }

    /**
     * Abstract method to obtain the organization ID for context-based database operations.
     * Subclasses must provide a specific organization context.
     *
     * @return the unique identifier representing the organization
     */
    @NonNull
    protected abstract Long getOrgId();
}