package com.tlc.analytics.resolver.table;

import com.tlc.analytics.config.model.table.TableRelation;
import com.tlc.analytics.resolver.internal.TableRelationResolverImpl;

import java.util.Collection;

/**
 * Resolver interface for {@link TableRelation} that handles cache-miss resolution.
 * Checks remote cache first; on miss, fetches from DB and populates cache.
 *
 * @author Karthick V
 * @version 1.0
 */
public interface TableRelationResolver {

    /**
     * <p>
     * Retrieves the singleton instance of {@link TableRelationResolver}.
     * </p>
     *
     * @return The {@link TableRelationResolver} instance
     */
    static TableRelationResolver getInstance() {
        return TableRelationResolverImpl.getInstance();
    }

    /**
     * <p>
     * Retrieves the {@link Collection} of {@link TableRelation} for the given parent table identifier.
     * Checks cache first; on miss, fetches from DB and populates cache.
     * </p>
     *
     * @param parentTableId Represents the parent table identifier
     * @return The {@link Collection} of {@link TableRelation} for the given parentTableId
     */
    Collection<TableRelation> getBySourceId(final Long parentTableId);

    /**
     * <p>
     * Retrieves a single {@link TableRelation} by its relation identifier.
     * Checks cache first; on miss, fetches from DB and populates cache.
     * </p>
     *
     * @param relationId Represents the relation identifier
     * @return The {@link TableRelation} associated with the given relationId
     */
    TableRelation getById(final Long relationId);
}
