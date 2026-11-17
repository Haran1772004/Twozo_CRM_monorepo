package com.tlc.pk.trash;

import lombok.NonNull;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * An interface for managing items in a trash system.
 *
 * <p>This interface provides methods for moving items to the trash, restoring them from the trash,
 * permanently deleting them, and retrieving all trashed items. It supports operations on both
 * individual items and collections of items.</p>
 *
 * <p>Implementations of this interface should define the specific behavior of how items are
 * managed in the trash, including any underlying storage or persistence mechanisms.</p>
 *
 * @param <K> the type of the key used to identify items (e.g., an ID)
 * @param <I> the type of the items being managed in the trash
 */
public interface TrashManager<K, I>
{
    /**
     * Moves a single item to the trash.
     *
     * @param item the item to move to the trash
     * @throws Exception if an error occurs during the operation
     */
    default void moveToTrash( final @NonNull I item ) throws Exception
    {
        final List<I> items = List.of( item );
        moveAllToTrash( items );
    }

    /**
     * Moves a collection of items to the trash.
     *
     * @param items the collection of items to move to the trash
     * @throws Exception if an error occurs during the operation
     */
    void moveAllToTrash( final @NonNull Collection<I> items ) throws Exception;

    /**
     * Restores a single item from the trash using its key.
     *
     * @param key the unique key of the item to restore
     * @throws Exception if an error occurs during the operation
     */
    default void restoreFromTrash( final @NonNull K key ) throws Exception
    {
        final Set<K> keys = Set.of( key );
        restoreAllFromTrash( keys );
    }

    /**
     * Restores multiple items from the trash using their keys.
     *
     * @param keys the set of keys for the items to restore
     * @throws Exception if an error occurs during the operation
     */
    void restoreAllFromTrash( final @NonNull Set<K> keys ) throws Exception;

    /**
     * Permanently deletes a single item using its key.
     *
     * @param key the unique key of the item to delete permanently
     * @throws Exception if an error occurs during the operation
     */
    default void deletePermanently( final @NonNull K key ) throws Exception
    {
        final Set<K> keys = Set.of( key );
        deleteAllPermanently( keys );
    }

    /**
     * Permanently deletes multiple items using their keys.
     *
     * @param keys the collection of keys for the items to delete permanently
     * @throws Exception if an error occurs during the operation
     */
    void deleteAllPermanently( final @NonNull Collection<K> keys ) throws Exception;

    /**
     * Empties the trash, permanently deleting all items in it.
     *
     * @throws Exception if an error occurs during the operation
     */
    void emptyTrash() throws Exception;

    /**
     * Retrieves all items currently in the trash.
     *
     * @return a collection of trashed items
     * @throws Exception if an error occurs while retrieving the trashed items
     */
    @NonNull Collection<I> getAllTrashedItems() throws Exception;
}

