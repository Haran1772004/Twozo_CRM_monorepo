package com.tlc.pk.trash.db;

import com.tlc.commons.functions.SafeConsumer;
import com.tlc.commons.util.executor.NullSafeExecutor;

import com.tlc.entity.EntityManager;

import com.tlc.pk.trash.AbstractTrashManager;

import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import lombok.NonNull;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Abstract base class for managing trash items backed by a database.
 *
 * <p>This class extends {@link AbstractTrashManager} and provides functionality to
 * handle trash operations such as moving items to trash, restoring them, retrieving
 * trashed items, and permanently deleting them. It relies on a database for persistence
 * and uses an {@link EntityManager} to interact with the database.</p>
 *
 * <p>Subclasses must implement abstract methods to define service-specific behavior,
 * such as fetching the service ID, trash category, and custom trash operations.</p>
 *
 * @param <I> the type of trash items managed by this class, extending {@link TrashEntity}
 */
public abstract class DBBackedTrashManager<I extends TrashEntity> extends AbstractTrashManager<Long, I>
{
    /**
     * Moves a collection of items to the trash by marking them as trashed
     * and persisting their updated state in the database.
     *
     * @param items the collection of items to move to the trash
     * @throws Exception if an error occurs during the operation
     */
    @Override
    public void moveAllToTrash( final @NonNull Collection<I> items ) throws Exception
    {
        performTrashOperation( items ); // Mark items as trashed
        getEntityManager().persistAll( items ); // Persist the updated state in the database
    }

    /**
     * Restores items from the trash based on their unique identifiers (keys).
     *
     * @param keys the keys of items to restore from the trash
     * @throws Exception if an error occurs during the restoration process
     */
    @Override
    public void restoreAllFromTrash( final @NonNull Set<Long> keys ) throws Exception
    {
        deleteAllPermanently( keys, this::performRestoreOperation );
    }

    /**
     * Retrieves all items currently marked as trashed in the database.
     *
     * @return a collection of trashed items
     * @throws Exception if an error occurs during the retrieval
     */
    @Override
    public @NonNull Collection<I> getAllTrashedItems() throws Exception
    {
        return fetchTrashedItems( null ); // Fetch all trashed items without any custom filter
    }

    /**
     * Fetches trashed items from the database based on optional filtering criteria.
     *
     * @param customFilter optional criteria for filtering trashed items
     * @return the collection of trashed items that match the criteria
     * @throws Exception if an error occurs during the retrieval
     */
    private Collection<I> fetchTrashedItems( final Criteria customFilter ) throws Exception
    {
        return getEntityManager().findAll( () ->
        {
            final Table table = getTable();
            final Criteria serviceFilter = Criteria.eq( table.getColumn( "SERVICE_ID" ), getServiceId() );
            final Criteria categoryFilter = Criteria.eq( table.getColumn( "CATEGORY_ID" ), getTrashCategory() );
            final WhereClause whereClause = new WhereClause( serviceFilter ).and( categoryFilter );

            // Apply additional filtering if provided
            NullSafeExecutor.acceptIfPresent( customFilter, whereClause::and );

            return whereClause;
        } );
    }

    /**
     * Returns the database table associated with trash metadata.
     *
     * @return the database table used for storing trash items
     */
    private Table getTable()
    {
        return Table.get( "Trash" );
    }

    /**
     * Permanently deletes items from the trash based on their unique identifiers.
     *
     * @param keys the keys of items to delete permanently
     * @throws Exception if an error occurs during the operation
     */
    @Override
    public void deleteAllPermanently( final @NonNull Collection<Long> keys ) throws Exception
    {
        deleteAllPermanently( keys, this::performPermanentDeleteOperation );
    }

    /**
     * A utility method to delete items permanently using a custom operation.
     *
     * @param keys     the keys of items to delete permanently
     * @param consumer the custom operation to execute on the trashed items
     * @throws Exception if an error occurs during the operation
     */
    private void deleteAllPermanently( final @NonNull Collection<Long> keys,
                                       final @NonNull SafeConsumer<Collection<I>> consumer ) throws Exception
    {
        final Criteria itemCriteria = Criteria.in( getTable().getColumn( "ITEM_ID" ), keys );
        final Collection<I> trashedItems = fetchTrashedItems( itemCriteria );

        consumer.accept( trashedItems );

        final Set<Long> restoredIds = trashedItems.stream().map( TrashEntity::id ).collect( Collectors.toSet() );

        getEntityManager().deleteAllByIds( restoredIds ); // Remove trashed metadata from the database
    }

    /**
     * Returns the unique identifier for the service that owns the trash items.
     *
     * @return the service ID
     */
    @NonNull
    protected abstract Long getServiceId();

    /**
     * Returns the category identifier used to classify trash items.
     *
     * @return the trash category ID
     */
    @NonNull
    protected abstract Long getTrashCategory();

    /**
     * Provides the entity manager used to interact with the database for trash operations.
     *
     * @return the entity manager instance
     */
    @NonNull
    protected abstract EntityManager<Long, I> getEntityManager();

    /**
     * Performs the operation to mark items as trashed.
     *
     * @param items the items to mark as trashed
     * @throws Exception if an error occurs during the operation
     */
    protected abstract void performTrashOperation( final Collection<I> items ) throws Exception;

    /**
     * Performs the operation to restore items from the trash.
     *
     * @param items the items to restore
     * @throws Exception if an error occurs during the operation
     */
    protected abstract void performRestoreOperation( final Collection<I> items ) throws Exception;

    /**
     * Performs the operation to permanently delete items from the trash.
     *
     * @param items the items to delete permanently
     * @throws Exception if an error occurs during the operation
     */
    protected abstract void performPermanentDeleteOperation( final Collection<I> items ) throws Exception;
}

