package com.tlc.analytics.resolver.function;

import com.tlc.analytics.config.function.ParentConfigurableFunctionDetail;
import com.tlc.analytics.resolver.internal.ParentConfigurableFunctionDetailResolverImpl;

import java.util.Collection;

/**
 * Resolver interface for {@link ParentConfigurableFunctionDetail} that handles cache-miss resolution.
 * Checks remote cache first; on miss, fetches from DB and populates cache.
 *
 * @author Karthick V
 * @version 1.0
 */
public interface ParentConfigurableFunctionDetailResolver {

    /**
     * <p>
     * Retrieves the singleton instance of {@link ParentConfigurableFunctionDetailResolver}.
     * </p>
     *
     * @return The {@link ParentConfigurableFunctionDetailResolver} instance
     */
    static ParentConfigurableFunctionDetailResolver getInstance() {
        return ParentConfigurableFunctionDetailResolverImpl.getInstance();
    }

    /**
     * <p>
     * Retrieves the {@link Collection} of {@link ParentConfigurableFunctionDetail} for the given column function identifier.
     * Checks cache first; on miss, fetches from DB and populates cache.
     * </p>
     *
     * @param columnFunctionId Represents the column function identifier
     * @return The {@link Collection} of {@link ParentConfigurableFunctionDetail}
     */
    Collection<ParentConfigurableFunctionDetail> getBySourceId(final Long columnFunctionId);

    /**
     * <p>
     * Retrieves a single {@link ParentConfigurableFunctionDetail} by its parent function identifier.
     * Checks cache first; on miss, fetches from DB and populates cache.
     * </p>
     *
     * @param functionId Represents the parent function identifier
     * @return The {@link ParentConfigurableFunctionDetail} instance
     */
    ParentConfigurableFunctionDetail getById(final Long functionId);
}
