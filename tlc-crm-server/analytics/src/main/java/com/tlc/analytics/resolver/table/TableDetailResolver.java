package com.tlc.analytics.resolver.table;

import com.tlc.analytics.config.model.table.TableDetail;
import com.tlc.analytics.resolver.internal.TableDetailResolverImpl;

import java.util.Collection;

/**
 * Resolver interface for {@link TableDetail} that handles cache-miss resolution.
 * Checks remote cache first; on miss, fetches from DB and populates cache.
 *
 * @author Karthick V
 * @version 1.0
 */
public interface TableDetailResolver {

    /**
     * <p>
     * Retrieves the singleton instance of {@link TableDetailResolver}.
     * </p>
     *
     * @return The {@link TableDetailResolver} instance
     */
    static TableDetailResolver getInstance() {
        return TableDetailResolverImpl.getInstance();
    }

    /**
     * <p>
     * Retrieves the {@link Collection} of {@link TableDetail} for the given module identifier.
     * Checks cache first; on miss, fetches from DB and populates cache.
     * </p>
     *
     * @param moduleId Represents the module identifier
     * @return The {@link Collection} of {@link TableDetail} for the given module identifier
     */
    Collection<TableDetail> getBySourceId(final Integer moduleId);

    /**
     * <p>
     * Retrieves a single {@link TableDetail} by its table identifier.
     * Checks cache first; on miss, fetches from DB and populates cache.
     * </p>
     *
     * @param tableId Represents the table identifier
     * @return The {@link TableDetail} associated with the given table identifier
     */
    TableDetail getById(final Long tableId);
}
