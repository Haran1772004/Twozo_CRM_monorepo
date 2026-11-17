package com.tlc.entity.cache;

import com.tlc.entity.Entity;

import lombok.NonNull;

import javax.annotation.Nullable;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

public interface EntityCacheManager<ID, E extends Entity<ID>>
{
    @NonNull
    Collection<E> getAll( final @NonNull Set<ID> ids );

    void removeAllFromCache( final @NonNull Set<ID> idSet );

    default void removeFromCache( final @NonNull ID id )
    {
        removeAllFromCache( Set.of( id ) );
    }

    @Nullable
    default E get( final @NonNull ID id )
    {
        return getAll( Set.of( id ) ).stream()
                                     .findFirst()
                                     .orElse( null );
    }

    @NonNull
    default E getOrThrow( final @NonNull ID id )
    {
        final E model = get( id );
        return Objects.requireNonNull( model );
    }
}
