package com.tlc.entity;

/**
 * Represents an entity.
 *
 * @param <ID> The type of the entity's identifier.
 */
public interface Entity<ID>
{
    /**
     * Gets the identifier of the entity.
     *
     * @return The identifier of the entity.
     */
    ID id();
}