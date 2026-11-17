package com.tlc.crm.core.api.analytics.service.module;

import com.tlc.commons.listeners.Callback;
import com.tlc.crm.core.api.analytics.entity.config.modules.ReportModularField;
import com.tlc.crm.core.api.analytics.service.internal.ReportModularFieldServiceImpl;

import java.util.Collection;
import java.util.Map;

/**
 * Service interface for accessing {@link ReportModularField} with caching support.
 *
 * @author Karthick V
 * @version 1.0
 */
public interface ReportModularFieldService {

    /**
     * <p>
     * Retrieves the singleton instance of {@link ReportModularFieldService}.
     * </p>
     *
     * @return The {@link ReportModularFieldService} instance
     */
    static ReportModularFieldService getInstance() {
        return ReportModularFieldServiceImpl.getInstance();
    }

    /**
     * <p>
     * Retrieves a {@link ReportModularField} by its identifier using callback pattern.
     * The callback runs on the async cache-worker thread, not the caller thread.
     * </p>
     *
     * @param orgId    Represents the organization identifier
     * @param id       Represents the field identifier
     * @param callback The callback to handle the result or failure
     */
    void get(final Long orgId, final Long id, final Callback<ReportModularField> callback);

    /**
     * <p>
     * Retrieves all {@link ReportModularField} entries for the given organization ID using callback pattern.
     * The callback runs on the async cache-worker thread, not the caller thread.
     * </p>
     *
     * @param orgId    Represents the organization identifier
     * @param callback The callback to handle the result or failure
     */
    void getByOrgId(final Long orgId, final Callback<Map<Long, ReportModularField>> callback);

    /**
     * <p>
     * Retrieves multiple {@link ReportModularField} entries by their identifiers using callback pattern.
     * </p>
     *
     * @param orgId    Represents the organization identifier
     * @param ids      Collection of field identifiers to retrieve
     * @param callback The callback to handle the result or failure
     */
    void getAll(final Long orgId, final Collection<Long> ids, final Callback<Map<Long, ReportModularField>> callback);
}
