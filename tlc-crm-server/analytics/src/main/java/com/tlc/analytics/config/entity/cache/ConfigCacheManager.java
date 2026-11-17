package com.tlc.analytics.config.entity.cache;

import com.tlc.analytics.config.model.BaseModel;

import lombok.NonNull;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;

/**
 * <p>
 * Provides abstract way cache of configuration models.
 * </p>
 *
 * @author Aswath
 * @version 1.0
 */
public interface ConfigCacheManager<M extends BaseModel>
{
    /**
     * Retrieves a collection of configuration models from the cache based on the organization ID and a set of IDs.
     *
     * @param orgId the organization ID.
     * @param ids a set of IDs to retrieve.
     * @return a collection of models corresponding to the provided IDs.
     */
    @NonNull
    Collection<M> getAll( final @NonNull Long orgId,
                          final @NonNull Set<Long> ids );

    /**
     * Removes multiple configuration models from the cache based on a set of IDs.
     *
     * @param id a set of IDs whose corresponding models should be removed from the cache.
     */
    void removeAllFromCache( final @NonNull Set<Long> id );

    default void removeFromCache( final @NonNull Long id )
    {
        removeAllFromCache( Set.of( id ) );
    }

    /**
     * Removes a single configuration model from the cache based on the ID.
     * This method delegates to {@link #removeAllFromCache(Set)} by converting the single ID into a set.
     *
     * @param id the ID of the model to remove from the cache.
     */
    @Nullable
    default M get( final @NonNull Long orgId,
                   final @NonNull Long id )
    {
        return getAll( orgId, Set.of( id ) ).stream()
                                            .findFirst()
                                            .orElseGet( () -> null );
    }

    /**
     * Retrieves a single configuration model from the cache based on the organization ID and a single ID.
     * Returns {@code null} if no model is found.
     *
     * @param orgId Represents the organization ID.
     * @param id    Represents the ID of the model to retrieve.
     * @return the model corresponding to the provided ID, or {@code null} if not found.
     */
    @NonNull
    default M getOrThrow( final @NonNull Long orgId,
                          final @NonNull Long id )
    {
        final M model = get( orgId, id );
        return Objects.requireNonNull( model );
    }
}
