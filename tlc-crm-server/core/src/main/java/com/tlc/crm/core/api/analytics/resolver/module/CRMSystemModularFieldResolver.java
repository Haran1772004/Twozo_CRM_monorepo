package com.tlc.crm.core.api.analytics.resolver.module;

import com.tlc.crm.core.api.analytics.entity.config.modules.CRMSystemModularField;
import com.tlc.crm.core.api.analytics.resolver.internal.CRMSystemModularFieldResolverImpl;

import java.util.Collection;

/**
 * Resolver interface for {@link CRMSystemModularField} that handles cache-miss resolution.
 * Checks local cache first; on miss, fetches from DB and populates cache.
 *
 * @author Karthick V
 * @version 1.0
 */
public interface CRMSystemModularFieldResolver {

    /**
     * <p>
     * Retrieves the singleton instance of {@link CRMSystemModularFieldResolver}.
     * </p>
     *
     * @return The {@link CRMSystemModularFieldResolver} instance
     */
    static CRMSystemModularFieldResolver getInstance() {
        return CRMSystemModularFieldResolverImpl.getInstance();
    }

    /**
     * <p>
     * Retrieves the {@link Collection} of {@link CRMSystemModularField} for the given module identifier.
     * Checks cache first; on miss, fetches from DB and populates cache.
     * </p>
     *
     * @param moduleId Represents the module identifier
     * @return The collection of {@link CRMSystemModularField} instances
     */
    Collection<CRMSystemModularField> getBySourceId(final Integer moduleId);

    /**
     * <p>
     * Retrieves a single {@link CRMSystemModularField} by its system modular field identifier.
     * Checks cache first; on miss, fetches from DB and populates cache.
     * </p>
     *
     * @param fieldId Represents the system modular field identifier
     * @return The {@link CRMSystemModularField} instance
     */
    CRMSystemModularField getById(final Long fieldId);
}
