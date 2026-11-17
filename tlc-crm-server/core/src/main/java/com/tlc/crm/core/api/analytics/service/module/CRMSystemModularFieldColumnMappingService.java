package com.tlc.crm.core.api.analytics.service.module;

import com.tlc.crm.core.api.analytics.entity.integ.reports.CRMSystemModularFieldColumnMapping;
import com.tlc.crm.core.api.analytics.service.internal.CRMSystemModularFieldColumnMappingServiceImpl;

import java.util.Collection;

/**
 * Service interface for accessing {@link CRMSystemModularFieldColumnMapping} with caching support.
 *
 * @author Karthick V
 * @version 1.0
 */
public interface CRMSystemModularFieldColumnMappingService {

    /**
     * <p>
     * Retrieves the singleton instance of {@link CRMSystemModularFieldColumnMappingService}.
     * </p>
     *
     * @return The {@link CRMSystemModularFieldColumnMappingService} instance
     */
    static CRMSystemModularFieldColumnMappingService getInstance() {
        return CRMSystemModularFieldColumnMappingServiceImpl.getInstance();
    }

    /**
     * <p>
     * Retrieves system modular field column mappings for the given modular field identifier.
     * Resolves from local cache first, falls back to DB on cache miss.
     * </p>
     *
     * @param modularFieldId Represents the modular field identifier
     * @return {@link Collection} of {@link CRMSystemModularFieldColumnMapping}
     */
    Collection<CRMSystemModularFieldColumnMapping> getSystemColumnMappings(final Long modularFieldId);
}
