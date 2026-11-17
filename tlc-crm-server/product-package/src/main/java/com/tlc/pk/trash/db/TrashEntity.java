package com.tlc.pk.trash.db;

import com.tlc.entity.Entity;
import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.Table;

import com.tlc.pk.trash.TrashItem;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

/**
 * Represents a database entity for items managed in the trash system.
 *
 * <p>This class maps to the "TRASH" database table and includes metadata about
 * trashed items, such as the user who trashed them, the time of trashing, and
 * an optional expiry time for automatic deletion.</p>
 *
 * <p>Implements the {@link TrashItem} interface to provide the unique key for
 * each trashed item and the {@link Entity} interface for database operations.</p>
 */
@Getter
@SuperBuilder
@Table( name = "Trash" )
public class TrashEntity implements Entity<Long>, TrashItem<Long>
{
    /**
     * The primary key for the trash entity.
     */
    @Column( name = "ID", isPrimaryKey = true )
    @Accessors( fluent = true )
    private Long id;

    /**
     * The identifier of the service associated with this trash item.
     */
    @Column( name = "SERVICE_ID" )
    private Long serviceId;

    /**
     * The category of the trashed item, used to differentiate between item types.
     */
    @Column( name = "ITEM_CATEGORY" )
    private Long itemCategory;

    /**
     * The unique identifier of the actual item being trashed.
     */
    @Getter( AccessLevel.NONE ) // Exclude from default getter generation
    @Column( name = "ITEM_ID" )
    private Long itemId;

    /**
     * The identifier of the user who trashed the item.
     */
    @Column( name = "TRASHED_BY" )
    private Long trashedBy;

    /**
     * The timestamp indicating when the item was trashed.
     */
    @Column( name = "TRASHED_AT" )
    private Long trashedAt;

    /**
     * The timestamp indicating when the item should expire from the trash.
     */
    @Column( name = "EXPIRY_TIME" )
    private Long expiryTime;

    /**
     * Retrieves the unique key for this trashed item.
     *
     * <p>This overrides the {@link TrashItem#getKey()} method and returns
     * the `itemId`, which uniquely identifies the trashed item.</p>
     *
     * @return the unique key (itemId) of the trashed item
     */
    @Override
    public Long getKey()
    {
        return itemId;
    }
}