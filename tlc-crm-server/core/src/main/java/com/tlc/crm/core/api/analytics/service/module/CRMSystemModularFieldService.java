package com.tlc.crm.core.api.analytics.service.module;

import com.tlc.crm.core.api.analytics.entity.config.modules.CRMSystemModularField;
import com.tlc.crm.core.api.analytics.service.internal.CRMSystemModularFieldServiceImpl;

import java.util.Collection;
import java.util.Map;

/**
 * Service interface for accessing {@link CRMSystemModularField} with caching support.
 *
 * @author Karthick V
 * @version 1.0
 */
public interface CRMSystemModularFieldService {

    /**
     * <p>
     * Retrieves the singleton instance of {@link CRMSystemModularFieldService}.
     * </p>
     *
     * @return The {@link CRMSystemModularFieldService} instance
     */
    static CRMSystemModularFieldService getInstance() {
        return CRMSystemModularFieldServiceImpl.getInstance();
    }

    /**
     * <p>
     * Retrieves a {@link CRMSystemModularField} by its identifier.
     * Resolves from local cache first, falls back to DB on cache miss.
     * </p>
     *
     * @param id Represents the field identifier
     * @return {@link CRMSystemModularField}
     */
    CRMSystemModularField get(final Long id);

    /**
     * <p>
     * Retrieves multiple {@link CRMSystemModularField} entries by their identifiers in a single batch.
     * </p>
     *
     * @param ids Collection of field identifiers to retrieve
     * @return {@link Map} from field ID to {@link CRMSystemModularField}
     */
    Map<Long, CRMSystemModularField> getAll(final Collection<Long> ids);
}
