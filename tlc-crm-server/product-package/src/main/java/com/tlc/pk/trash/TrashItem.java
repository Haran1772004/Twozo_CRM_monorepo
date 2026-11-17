package com.tlc.pk.trash;

/**
 * A generic interface representing an item that can be managed in a trash system.
 *
 * <p>This interface defines a method to retrieve the unique key of the item,
 * which can be used to identify and manage the item in a trash management system.</p>
 *
 * @param <K> the type of the key used to uniquely identify the item (e.g., an ID)
 */
public interface TrashItem<K>
{
    /**
     * Retrieves the unique key of this item.
     *
     * @return the key of the item
     */
    K getKey();
}
