package com.tlc.analytics.service.column;

import com.tlc.analytics.config.model.table.ColumnDetail;
import com.tlc.analytics.internal.ColumnDetailsServiceImpl;

import java.util.Collection;
import java.util.Set;

/**
 * Service interface for accessing {@link ColumnDetail} with caching support.
 *
 * @author Karthick V
 * @version 1.0
 */
public interface ColumnDetailsService {

    /**
     * <p>
     * Retrieves the singleton instance of {@link ColumnDetailsService}.
     * </p>
     *
     * @return The {@link ColumnDetailsService} instance
     */
    static ColumnDetailsService getInstance() {
        return ColumnDetailsServiceImpl.getInstance();
    }

    /**
     * <p>
     * Retrieves column details for the given table ID.
     * Resolves from cache first, falls back to DB on cache miss.
     * </p>
     *
     * @param tableId Represents the table identifier
     * @return A {@link Collection} of {@link ColumnDetail}
     */
    Collection<ColumnDetail> getByTableId(final Long tableId);


    /**
     * <p>
     * Asynchronously retrieves a single {@link ColumnDetail} by its column ID.
     * Resolves from cache first, falls back to DB on cache miss.
     * </p>
     *
     * @param columnId Represents the column identifier
     * @return The {@link ColumnDetail} or null if not found
     */
    ColumnDetail get(final Long columnId);

    /**
     * <p>
     * Asynchronously retrieves multiple {@link ColumnDetail} by their IDs.
     * </p>
     *
     * @param columnIds The set of column identifiers
     * @return A collection of {@link ColumnDetail}
     */
    Collection<ColumnDetail> getAll(final Set<Long> columnIds);
}
