package com.tlc.pk.transfer.model;

import com.tlc.commons.functions.SafeFunction;

import com.tlc.entity.Entity;
import com.tlc.entity.EntityManager;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * <p>
 * The {@link  EntityTransferConfig} class Represents the configuration required for moving an entity
 * from source to target
 * </p>
 *
 * @param <ID> The type of the entity's identifier
 * @param <E>  The type of the entity
 * @author pandiarajkumarG
 * @version 1.0
 */
@Getter
@Builder
@RequiredArgsConstructor( access = AccessLevel.PROTECTED )
public class EntityTransferConfig<ID, E extends Entity<ID>>
{
    /**
     * <p>
     *  Stores the entity manager responsible for the entity to be moved
     * </p>
     */
    @NonNull
    private EntityManager<ID, E> entityManager;

    /**
     * <p>
     * The ID of the source entity to be transferred
     * </p>
     */
    @NonNull
    private ID sourceId;

    /**
     * <p>
     * Customizes the updated field before persisting
     * </p>
     */
    @NonNull
    private SafeFunction<E, E> entityCustomizer;
}
