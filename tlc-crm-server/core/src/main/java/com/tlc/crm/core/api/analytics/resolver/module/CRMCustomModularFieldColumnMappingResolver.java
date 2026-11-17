package com.tlc.crm.core.api.analytics.resolver.module;

import com.tlc.crm.core.api.analytics.entity.integ.reports.CRMCustomModularFieldColumnMapping;
import com.tlc.crm.core.api.analytics.resolver.internal.CRMCustomModularFieldColumnMappingResolverImpl;

import java.util.Collection;

/**
 * Resolver interface for {@link CRMCustomModularFieldColumnMapping} that handles cache-miss resolution.
 *
 * @author Karthick V
 * @version 1.0
 */
public interface CRMCustomModularFieldColumnMappingResolver {

    /**
     * <p>
     * Retrieves the singleton instance of {@link CRMCustomModularFieldColumnMappingResolver}.
     * </p>
     *
     * @return The {@link CRMCustomModularFieldColumnMappingResolver} instance
     */
    static CRMCustomModularFieldColumnMappingResolver getInstance() {
        return CRMCustomModularFieldColumnMappingResolverImpl.getInstance();
    }

    /**
     * <p>
     * Retrieves the {@link Collection} of {@link CRMCustomModularFieldColumnMapping} for the given module identifier.
     * Checks cache first; on miss, fetches from DB and populates cache.
     * </p>
     *
     * @param moduleId Represents the module identifier
     * @return The {@link Collection} of {@link CRMCustomModularFieldColumnMapping} for the given module identifier
     */
    Collection<CRMCustomModularFieldColumnMapping> getByModuleId(final Integer moduleId);

    /**
     * <p>
     * Retrieves a single {@link CRMCustomModularFieldColumnMapping} by its custom column mapping identifier.
     * Checks cache first; on miss, fetches from DB and populates cache.
     * </p>
     *
     * @param columnMappingId Represents the custom column mapping identifier
     * @return The {@link CRMCustomModularFieldColumnMapping} for the given custom column mapping identifier
     */
    CRMCustomModularFieldColumnMapping getByColumnMappingId(final Long columnMappingId);
}
