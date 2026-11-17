package com.tlc.analytics.resolver.column;

import com.tlc.analytics.config.model.table.ColumnDetail;
import com.tlc.analytics.resolver.internal.ColumnDetailsResolverImpl;

import java.util.Collection;

/**
 * Resolver interface for {@link ColumnDetail} that handles cache-miss resolution.
 * Checks local cache first; on miss, fetches from DB and populates cache.
 *
 * @author Karthick V
 * @version 1.0
 */
public interface ColumnDetailsResolver {

    /**
     * <p>
     * Retrieves the singleton instance of {@link ColumnDetailsResolver}.
     * </p>
     *
     * @return The {@link ColumnDetailsResolver} instance
     */
    static ColumnDetailsResolver getInstance() {
        return ColumnDetailsResolverImpl.getInstance();
    }

    /**
     * <p>
     * Retrieves the {@link Collection} of {@link ColumnDetail} for the given table identifier.
     * Checks cache first; on miss, fetches from DB and populates cache.
     * </p>
     *
     * @param tableId Represents the table identifier
     * @return The collection of {@link ColumnDetail}
     */
    Collection<ColumnDetail> getBySourceId(final Long tableId);

    /**
     * <p>
     * Retrieves a single {@link ColumnDetail} by its column identifier.
     * Checks cache first; on miss, fetches from DB and populates cache.
     * </p>
     *
     * @param columnId Represents the column identifier
     * @return The {@link ColumnDetail}, or {@code null} if not found
     */
    ColumnDetail getById(final Long columnId);
}
