package com.tlc.analytics.service.table;

import com.tlc.analytics.config.model.table.TableRelation;
import com.tlc.analytics.internal.TableRelationServiceImpl;

import java.util.Collection;
import java.util.Set;

/**
 * Service interface for accessing {@link TableRelation} with caching support.
 *
 * @author Karthick V
 * @version 1.0
 */
public interface TableRelationService {

    /**
     * <p>
     * Retrieves the singleton instance of {@link TableRelationService}.
     * </p>
     *
     * @return The {@link TableRelationService} instance
     */
    static TableRelationService getInstance() {
        return TableRelationServiceImpl.getInstance();
    }


    /**
     * <p>
     * Retrieves a single {@link TableRelation} by its relation identifier.
     * Resolves from cache first, falls back to DB on cache miss.
     * </p>
     *
     * @param id Represents the relation identifier
     * @return A {@link TableRelation} or null if not found
     */
    TableRelation get(final Long id);

    /**
     * <p>
     * Retrieves multiple {@link TableRelation} by their IDs.
     * </p>
     *
     * @param ids The set of relation identifiers
     * @return A collection of {@link TableRelation}
     */
    Collection<TableRelation> getAll(final Set<Long> ids);
}
