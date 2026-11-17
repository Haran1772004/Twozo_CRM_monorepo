package com.tlc.analytics.service.function;

import com.tlc.analytics.config.function.ConfigurableFunctionDetail;
import com.tlc.analytics.internal.ConfigurableFunctionDetailServiceImpl;

import java.util.Collection;
import java.util.Set;

/**
 * Service interface for accessing {@link ConfigurableFunctionDetail} with caching support.
 *
 * @author Karthick V
 * @version 1.0
 */
public interface ConfigurableFunctionDetailService {

    /**
     * <p>
     * Retrieves the singleton instance of {@link ConfigurableFunctionDetailService}.
     * </p>
     *
     * @return The {@link ConfigurableFunctionDetailService} instance
     */
    static ConfigurableFunctionDetailService getInstance() {
        return ConfigurableFunctionDetailServiceImpl.getInstance();
    }

    /**
     * <p>
     * Retrieves a single {@link ConfigurableFunctionDetail} by its function identifier.
     * Resolves from cache first, falls back to DB on cache miss.
     * </p>
     *
     * @param functionId Represents the function identifier
     * @return A {@link ConfigurableFunctionDetail} or null if not found
     */
    ConfigurableFunctionDetail get(final Long functionId);

    /**
     * <p>
     * Retrieves multiple {@link ConfigurableFunctionDetail} by their IDs.
     * </p>
     *
     * @param functionIds The set of function identifiers
     * @return A collection of {@link ConfigurableFunctionDetail}
     */
    Collection<ConfigurableFunctionDetail> getAll(final Set<Long> functionIds);
}
