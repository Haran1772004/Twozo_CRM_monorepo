package com.tlc.crm.core.api.analytics.resolver.module;

import com.tlc.crm.core.api.analytics.entity.config.modules.ReportModularField;
import com.tlc.commons.listeners.Callback;
import com.tlc.crm.core.api.analytics.resolver.internal.ReportModularFieldResolverImpl;

import java.util.Collection;

/**
 * Resolver interface for {@link ReportModularField} that handles cache-miss resolution.
 * Checks remote cache first; on miss, fetches from DB and populates cache.
 *
 * @author Karthick V
 * @version 1.0
 */
public interface ReportModularFieldResolver {

    /**
     * <p>
     * Retrieves the singleton instance of {@link ReportModularFieldResolver}.
     * </p>
     *
     * @return The {@link ReportModularFieldResolver} instance
     */
    static ReportModularFieldResolver getInstance() {
        return ReportModularFieldResolverImpl.getInstance();
    }

    /**
     * <p>
     * Retrieves the {@link Collection} of {@link ReportModularField} for the given org identifier.
     * Checks cache first; on miss, fetches from DB and populates cache.
     * </p>
     *
     * @param orgId    Represents the org identifier 
     * @param callback The callback to handle the result or failure
     */
    void getBySourceId(final Long orgId, final Callback<Collection<ReportModularField>> callback);

    /**
     * <p>
     * Retrieves a single {@link ReportModularField} by its report modular field identifier.
     * Checks cache first; on miss, fetches from DB and populates cache.
     * </p>
     *
     * @param orgId   Represents the org identifier
     * @param fieldId Represents the report modular field identifier
     * @param callback The callback to handle the result or failure
     */
    void getById(final Long orgId, final Long fieldId, final Callback<ReportModularField> callback);
}
