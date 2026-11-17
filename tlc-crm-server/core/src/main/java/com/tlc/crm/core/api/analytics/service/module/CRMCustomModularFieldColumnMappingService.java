package com.tlc.crm.core.api.analytics.service.module;

import com.tlc.crm.core.api.analytics.entity.integ.reports.CRMCustomModularFieldColumnMapping;
import com.tlc.crm.core.api.analytics.service.internal.CRMCustomModularFieldColumnMappingServiceImpl;

import java.util.Collection;

/**
 * Service interface for accessing {@link CRMCustomModularFieldColumnMapping} with caching support.
 *
 * @author Karthick V
 * @version 1.0
 */
public interface CRMCustomModularFieldColumnMappingService {

    /**
     * <p>
     * Retrieves the singleton instance of {@link CRMCustomModularFieldColumnMappingService}.
     * </p>
     *
     * @return The {@link CRMCustomModularFieldColumnMappingService} instance
     */
    static CRMCustomModularFieldColumnMappingService getInstance() {
        return CRMCustomModularFieldColumnMappingServiceImpl.getInstance();
    }

    /**
     * <p>
     * Retrieves custom modular field column mappings for the given module and field type.
     * Resolves from local cache first, falls back to DB on cache miss.
     * </p>
     *
     * @param moduleId    Represents the module identifier
     * @param fieldTypeId Represents the field type identifier
     * @return {@link Collection} of {@link CRMCustomModularFieldColumnMapping}
     */
    Collection<CRMCustomModularFieldColumnMapping> getCustomColumnMappings(final Integer moduleId, final Long fieldTypeId);
}
