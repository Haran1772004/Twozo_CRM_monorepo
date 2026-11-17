package com.tlc.analytics.config.entity.cache;

import com.tlc.analytics.config.internal.ConfigErrorCodes;
import com.tlc.analytics.config.model.BaseModel;
import com.tlc.analytics.config.manager.AnalyticsConfigManager;

import com.tlc.commons.code.ErrorCode;

import lombok.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * The AbstractConfigCacheManager class is an abstract implementation of the ConfigCacheManager interface
 * responsible for managing cached configurations
 * </p>
 *
 * @author Aswath
 * @version 1.0
 */
public abstract class AbstractConfigCacheManager<M extends BaseModel>
        implements ConfigCacheManager<M>
{
    private static final Logger LOGGER = LoggerFactory.getLogger( AbstractConfigCacheManager.class );

    /**
     * Retrieves all the models associated with the provided IDs from the cache
     *
     * @param orgId The ID of the organization to which the models belong
     * @param ids   The set of IDs of the models to be retrieved
     * @return A collection of models corresponding to the provided IDs
     */
    @Override
    public @NonNull Collection<M> getAll( final @NonNull Long orgId, final @NonNull Set<Long> ids )
    {
        final List<M> modelList = new ArrayList<>();
        final Set<Long> remainingId = new HashSet<>( ids );

        getCacheAdapter().getAll( ids )
                         .entrySet()
                         .stream()
                         .filter( entry -> !isModified( orgId, entry.getKey(), entry.getValue() ) )
                         .map( entry -> {
                             final CachedValue<M> cachedValue = entry.getValue();

                             cachedValue.setLastAccessTime( System.currentTimeMillis() );
                             remainingId.remove( entry.getKey() );
                             return cachedValue.getActualCache();
                         } )
                         .forEach( modelList::add );

        if( !remainingId.isEmpty() )
        {
            final long currentTime = System.currentTimeMillis();
            final Collection<M> modelsFromDB = getConfigManager().get( orgId, remainingId );
            final Map<Long, CachedValue<M>> cachedValueMap = new HashMap<>();

            modelsFromDB.forEach( m -> cachedValueMap.put( m.id(), CachedValue.create( m, currentTime ) ) );
            getCacheAdapter().putAll( cachedValueMap );
            modelList.addAll( modelsFromDB );
        }

        if( modelList.size() != ids.size() )
        {
            LOGGER.error( "Analytics: Invalid module ids :{}", ids );
            throw ErrorCode.get( ConfigErrorCodes.INVALID_MODEL_IDS,
                    "i18n.analytics.invalid.model.ids" );
        }
        return modelList;
    }

    /**
     * Removes the models corresponding to the provided IDs from the cache
     *
     * @param ids The set of IDs of the models to be removed from the cache
     */
    @Override
    public void removeAllFromCache( final @NonNull Set<Long> ids )
    {
        getCacheAdapter().removeAll( ids );
    }

    /**
     * Determines whether the cached value has been modified based on the organization ID
     *
     * @param orgId       The ID of the organization to which the model belongs.
     * @param id          The ID of the model being checked.
     * @param cachedValue The cached value of the model.
     * @return True if the cached value has been modified, false otherwise.
     */
    protected boolean isModified( final long orgId,
                                  final long id,
                                  final @NonNull CachedValue<M> cachedValue )
    {
        return false;
    }

    /**
     * Returns the cache adapter responsible for managing the caching mechanism
     *
     * @return The cache adapter for managing cache operations
     */
    @Nonnull
    abstract protected CacheAdapter<Long, CachedValue<M>> getCacheAdapter();

    /**
     * Returns the configuration manager responsible for managing the underlying data
     *
     * @return The configuration manager for managing models
     */
    @Nonnull
    abstract protected AnalyticsConfigManager<M> getConfigManager();
}
