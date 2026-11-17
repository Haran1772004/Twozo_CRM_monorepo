package com.tlc.entity.converter;

import com.tlc.entity.Entity;

import lombok.NonNull;

/**
 * Interface for removing reference fields from an entity and its related entities recursively.
 */
public interface ReferenceFieldRemover
{
    /**
     * Recursively removes all reference fields (such as relationships or linked entities)
     * from the given entity and its related entities.
     *
     * @param entity The entity from which reference fields should be removed recursively.
     * @throws Exception If an error occurs during the removal process.
     */
    void removeReferenceFieldsRecursively( @NonNull final Entity<?> entity ) throws Exception;
}
