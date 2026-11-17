package com.tlc.pk.trash;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Abstract base class for implementing a generic Trash Manager.
 *
 * <p>This class provides a common structure and default implementations for managing
 * items in a trash system. It simplifies the implementation of trash-related operations
 * such as deleting all trashed items permanently and offers utility methods to be reused
 * in concrete implementations.</p>
 *
 * <p>Subclasses must provide specific implementations for the abstract methods in
 * {@link TrashManager}, such as moving items to trash, restoring items, and deleting
 * specific items permanently.</p>
 *
 * @param <K> the type of the unique identifier for the trash items (e.g., an ID)
 * @param <I> the type of the trash items managed by this class, extending {@link TrashItem}
 */
public abstract class AbstractTrashManager<K, I extends TrashItem<K>> implements TrashManager<K, I>
{
    /**
     * Empties the trash by permanently deleting all items currently in the trash.
     *
     * <p>This default implementation retrieves all trashed items, extracts their keys,
     * and deletes them permanently by calling {@link #deleteAllPermanently(Collection)}.</p>
     *
     * @throws Exception if an error occurs during the operation
     */
    @Override
    public void emptyTrash() throws Exception
    {
        final Collection<K> keys = getAllTrashedItems().stream()
                                                       .map( TrashItem::getKey )
                                                       .collect( Collectors.toList() );
        deleteAllPermanently( keys );
    }
}