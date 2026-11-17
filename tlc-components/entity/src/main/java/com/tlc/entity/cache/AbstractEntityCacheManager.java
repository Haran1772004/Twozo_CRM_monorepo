package com.tlc.entity.cache;

import com.tlc.commons.util.executor.CodeBlockExecutor;
import com.tlc.entity.Entity;
import com.tlc.entity.EntityManager;

import lombok.NonNull;

import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractEntityCacheManager<ID, E extends Entity<ID>>
        implements EntityCacheManager<ID, E>
{
    @Override
    public @NonNull Collection<E> getAll( final @NonNull Set<ID> ids )
    {
        final List<E> entityList = new ArrayList<>();
        final Set<ID> remainingId = new HashSet<>( ids );
        getCacheAdapter().getAll( ids )
                         .entrySet()
                         .stream()
                         .filter( entry -> !isModified( entry.getKey(), entry.getValue() ) )
                         .map( entry ->
                         {
                             final CachedValue<E> cachedValue = entry.getValue();
                             cachedValue.setLastAccessTime( System.currentTimeMillis() );
                             remainingId.remove( entry.getKey() );
                             return cachedValue.getActualCache();
                         } )
                         .forEach( entityList::add );

        if( !remainingId.isEmpty() )
        {
            final long currentTime = System.currentTimeMillis();
            final Collection<E> entitiesFromDB = CodeBlockExecutor.getWithUnchecked( () -> getEntityManager().findAllByIds( remainingId ) );
            final Map<ID, CachedValue<E>> cachedValueMap = new HashMap<>();
            entitiesFromDB.forEach( m -> cachedValueMap.put( m.id(), CachedValue.create( m, currentTime ) ) );
            getCacheAdapter().putAll( cachedValueMap );
            entityList.addAll( entitiesFromDB );
        }

        if( entityList.size() != ids.size() )
        {
            throw new RuntimeException( "" );
        }
        return entityList;
    }

    @Override
    public void removeAllFromCache( final @NonNull Set<ID> ids )
    {
        getCacheAdapter().removeAll( ids );
    }

    protected boolean isModified( final ID id, final CachedValue<E> cachedValue )
    {
        return false;
    }

    @Nonnull
    abstract protected CacheAdapter<ID, CachedValue<E>> getCacheAdapter();

    @Nonnull
    abstract protected EntityManager<ID, E> getEntityManager();
}
