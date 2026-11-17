package com.tlc.analytics.resolver.function;

import com.tlc.analytics.config.function.ConfigurableFunctionDetail;
import com.tlc.analytics.resolver.internal.ConfigurableFunctionDetailResolverImpl;

import java.util.Collection;

/**
 * Resolver interface for {@link ConfigurableFunctionDetail} that handles cache-miss resolution.
 * Checks remote cache first; on miss, fetches from DB and populates cache.
 *
 * @author Karthick V
 * @version 1.0
 */
public interface ConfigurableFunctionDetailResolver {

    /**
     * <p>
     * Retrieves the singleton instance of {@link ConfigurableFunctionDetailResolver}.
     * </p>
     *
     * @return The {@link ConfigurableFunctionDetailResolver} instance
     */
    static ConfigurableFunctionDetailResolver getInstance() {
        return ConfigurableFunctionDetailResolverImpl.getInstance();
    }

    /**
     * <p>
     * Retrieves the {@link Collection} of {@link ConfigurableFunctionDetail} for the given column identifier.
     * Checks cache first; on miss, fetches from DB and populates cache.
     * </p>
     *
     * @param columnId Represents the column identifier
     * @return The {@link Collection} of {@link ConfigurableFunctionDetail}
     */
    Collection<ConfigurableFunctionDetail> getBySourceId(final Long columnId);

    /**
     * <p>
     * Retrieves a single {@link ConfigurableFunctionDetail} by its function identifier.
     * Checks cache first; on miss, fetches from DB and populates cache.
     * </p>
     *
     * @param functionId Represents the function identifier
     * @return The {@link ConfigurableFunctionDetail} instance
     */
    ConfigurableFunctionDetail getById(final Long functionId);
}
