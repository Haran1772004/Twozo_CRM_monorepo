package com.tlc.analytics.service.column;

import com.tlc.analytics.config.model.table.ColumnRelation;
import com.tlc.analytics.internal.ColumnRelationServiceImpl;

import java.util.Collection;

/**
 * Service interface for accessing {@link ColumnRelation} with caching support.
 *
 * @author Karthick V
 * @version 1.0
 */
public interface ColumnRelationService {

    /**
     * <p>
     * Retrieves the singleton instance of {@link ColumnRelationService}.
     * </p>
     *
     * @return The {@link ColumnRelationService} instance
     */
    static ColumnRelationService getInstance() {
        return ColumnRelationServiceImpl.getInstance();
    }

    /**
     * <p>
     * Retrieves related columns for the given relation identifier.
     * </p>
     *
     * @param relationId Represents the relation identifier
     * @return The collection of related columns
     */
    Collection<ColumnRelation> getColumnRelationsList(final Long relationId);

    /**
     * <p>
     * Retrieves a single {@link ColumnRelation} by its identifier.
     * </p>
     *
     * @param id Represents the related column identifier
     * @return The related column
     */
    ColumnRelation getColumnRelation(final Long id);
}
