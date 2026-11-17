package com.tlc.crm.entity.resolver.list;

import com.tlc.commons.pair.Pair;
import com.tlc.crm.entity.list.model.Provider;
import com.tlc.crm.entity.resolver.internal.ProviderResolverImpl;

import java.util.Map;

/**
 * A Resolver that resolves the {@link Provider} related details
 *
 * @author Kishore J
 * @version 1.0
 */
public interface ProviderResolver {

    /**
     * <p>
     * Gets the instance of {@link ProviderResolver}.
     * </p>
     *
     * @return {@link ProviderResolver}.
     */
    static ProviderResolver getInstance() {
        return ProviderResolverImpl.getInstance();
    }

    /**
     * <p>
     * Fetches all providers grouped by their IDs. If not cached, fetches data from the database,
     * caches it, and returns the providers from the cache.
     * </p>
     *
     * @return The {@link Map} containing key as provider ID value as {@link Provider}
     */
    Map<Long, Provider> getAll();

    /**
     * <p>
     * Fetches provider for the given source ID. If not cached,
     * fetches data from the database, caches it, and returns the provider from the cache.
     * </p>
     *
     * @param sourceId Represents the source ID
     * @return The {@link Pair} contains the key as provider ID value as {@link Provider}
     */
    Pair<Long, Provider> getProvider(final Long sourceId);
}