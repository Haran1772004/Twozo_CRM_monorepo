package com.tlc.crm.core.api.analytics.resolver.module;

import com.tlc.crm.core.api.analytics.entity.integ.reports.CRMSystemModularFieldColumnMapping;
import com.tlc.crm.core.api.analytics.resolver.internal.CRMSystemModularFieldColumnMappingResolverImpl;

import java.util.Collection;

/**
 * Resolver interface for {@link CRMSystemModularFieldColumnMapping} that handles cache-miss resolution.
 *
 * @author Karthick V
 * @version 1.0
 */
public interface CRMSystemModularFieldColumnMappingResolver {

    /**
     * <p>
     * Retrieves the singleton instance of {@link CRMSystemModularFieldColumnMappingResolver}.
     * </p>
     *
     * @return The {@link CRMSystemModularFieldColumnMappingResolver} instance
     */
    static CRMSystemModularFieldColumnMappingResolver getInstance() {
        return CRMSystemModularFieldColumnMappingResolverImpl.getInstance();
    }

    /**
     * <p>
     * Retrieves the {@link Collection} of {@link CRMSystemModularFieldColumnMapping} for the given modular field identifier.
     * Checks cache first; on miss, fetches from DB and populates cache.
     * </p>
     *
     * @param modularFieldId Represents the modular field identifier
     * @return The {@link Collection} of {@link CRMSystemModularFieldColumnMapping}
     */
    Collection<CRMSystemModularFieldColumnMapping> getBySourceId(final Long modularFieldId);

    /**
     * <p>
     * Retrieves a single {@link CRMSystemModularFieldColumnMapping} by its system column mapping identifier.
     * Checks cache first; on miss, fetches from DB and populates cache.
     * </p>
     *
     * @param mappingId Represents the system column mapping identifier
     * @return The {@link CRMSystemModularFieldColumnMapping} instance
     */
    CRMSystemModularFieldColumnMapping getById(final Long mappingId);
}
