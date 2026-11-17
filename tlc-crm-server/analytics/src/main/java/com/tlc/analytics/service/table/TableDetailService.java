package com.tlc.analytics.service.table;

import com.tlc.analytics.config.model.table.TableDetail;
import com.tlc.analytics.internal.TableDetailServiceImpl;

import java.util.Collection;
import java.util.Set;

/**
 * Service interface for accessing {@link TableDetail} with caching support.
 *
 * @author Karthick V
 * @version 1.0
 */
public interface TableDetailService {

    /**
     * <p>
     * Retrieves the singleton instance of {@link TableDetailService}.
     * </p>
     *
     * @return The {@link TableDetailService} instance
     */
    static TableDetailService getInstance() {
        return TableDetailServiceImpl.getInstance();
    }


    /**
     * <p>
     * Retrieves table details for the given source/module ID.
     * Resolves from cache first, falls back to DB on cache miss.
     * </p>
     *
     * @param moduleId Represents the module identifier
     * @return A {@link Collection} of {@link TableDetail}
     */
    Collection<TableDetail> getBySourceId(final Integer moduleId);

    /**
     * <p>
     * Asynchronously retrieves a single {@link TableDetail} by its table ID.
     * Resolves from cache first, falls back to DB on cache miss.
     * </p>
     *
     * @param tableId Represents the table identifier
     * @return The {@link TableDetail} or null if not found
     */
    TableDetail get(final Long tableId);

    /**
     * <p>
     * Asynchronously retrieves multiple {@link TableDetail} by their IDs.
     * </p>
     *
     * @param tableIds The set of table identifiers
     * @return A collection of {@link TableDetail}
     */
    Collection<TableDetail> getAll(final Set<Long> tableIds);
}
