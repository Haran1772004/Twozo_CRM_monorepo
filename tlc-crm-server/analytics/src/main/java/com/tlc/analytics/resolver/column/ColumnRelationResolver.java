package com.tlc.analytics.resolver.column;

import com.tlc.analytics.config.model.table.ColumnRelation;
import com.tlc.analytics.resolver.internal.ColumnRelationResolverImpl;

import java.util.Collection;

/**
 * Resolver interface for {@link ColumnRelation} that handles cache-miss resolution.
 * Checks remote cache first; on miss, fetches from DB and populates cache.
 *
 * @author Karthick V
 * @version 1.0
 */
public interface ColumnRelationResolver {

    /**
     * <p>
     * Retrieves the singleton instance of {@link ColumnRelationResolver}.
     * </p>
     *
     * @return The {@link ColumnRelationResolver} instance
     */
    static ColumnRelationResolver getInstance() {
        return ColumnRelationResolverImpl.getInstance();
    }

    /**
     * <p>
     * Retrieves the {@link Collection} of {@link ColumnRelation} for the given relation identifier.
     * Checks cache first; on miss, fetches from DB and populates cache.
     * </p>
     *
     * @param relationId Represents the relation identifier
     * @return The {@link Collection} of {@link ColumnRelation} for the given relation identifier
     */
    Collection<ColumnRelation> getBySourceId(final Long relationId);

    /**
     * <p>
     * Retrieves a single {@link ColumnRelation} by its related column identifier.
     * Checks cache first; on miss, fetches from DB and populates cache.
     * </p>
     *
     * @param columnRelationId Represents the related column identifier
     * @return The {@link ColumnRelation} associated with the given identifier
     */
    ColumnRelation getById(final Long columnRelationId);
}
