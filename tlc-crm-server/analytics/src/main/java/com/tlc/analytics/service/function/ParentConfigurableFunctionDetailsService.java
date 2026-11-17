package com.tlc.analytics.service.function;

import com.tlc.analytics.config.function.ParentConfigurableFunctionDetail;
import com.tlc.analytics.internal.ParentConfigurableFunctionDetailsServiceImpl;

import java.util.Collection;

/**
 * Service interface for accessing {@link ParentConfigurableFunctionDetail} with caching support.
 *
 * @author Karthick V
 * @version 1.0
 */
public interface ParentConfigurableFunctionDetailsService {

    /**
     * <p>
     * Retrieves the singleton instance of {@link ParentConfigurableFunctionDetailsService}.
     * </p>
     *
     * @return The {@link ParentConfigurableFunctionDetailsService} instance
     */
    static ParentConfigurableFunctionDetailsService getInstance() {
        return ParentConfigurableFunctionDetailsServiceImpl.getInstance();
    }

    /**
     * <p>
     * Retrieves parent configurable function details for the given column function identifier.
     * </p>
     *
     * @param columnFunctionId Represents the column function identifier
     * @return The collection of {@link ParentConfigurableFunctionDetail} instances
     */
    Collection<ParentConfigurableFunctionDetail> getParentConfigurableFunctionDetailsList(final Long columnFunctionId);

    /**
     * <p>
     * Retrieves a single {@link ParentConfigurableFunctionDetail} by its function identifier.
     * </p>
     *
     * @param functionId Represents the function identifier
     * @return The {@link ParentConfigurableFunctionDetail} instance
     */
    ParentConfigurableFunctionDetail getParentConfigurableFunctionDetail(final Long functionId);
}
