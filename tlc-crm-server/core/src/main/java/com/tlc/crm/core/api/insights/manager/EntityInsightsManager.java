package com.tlc.crm.core.api.insights.manager;

import com.tlc.crm.core.api.insights.models.EntityInsightsData;
import com.tlc.crm.core.api.insights.models.ActivityInsights;
import com.tlc.crm.core.api.insights.models.DealInsights;
import com.tlc.crm.core.api.insights.models.EmailInsights;
import com.tlc.crm.entity.fields.source.Source;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * Represents the entity insights manager.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public interface EntityInsightsManager {

    /**
     * <p>
     * Reads an insights row, lazily inserting an empty row if none exists.
     * </p>
     *
     * @param orgId    Represents the organization id.
     * @param entityId Represents the entity id.
     * @param source   Represents the entity type.
     * @return The {@link EntityInsightsData} for the entity.
     */
    EntityInsightsData get(final long orgId, final long entityId, final Source source);

    /**
     * <p>
     * Deletes the insights row of a single entity.
     * </p>
     *
     * @param orgId    Represents the organization id.
     * @param entityId Represents the entity id.
     * @param source   Represents the entity type.
     */
    void delete(final long orgId, final long entityId, final Source source);

    /**
     * <p>
     * Inserts blank insights rows for the given newly-created entities.
     * </p>
     *
     * @param orgId     Represents the organization id.
     * @param source    Represents the entity type.
     * @param entityIds Represents the ids of the entities to bootstrap.
     */
    void create(final long orgId, final Source source, final Collection<Long> entityIds);

    /**
     * <p>
     * Deletes the insights rows of the given entities in bulk.
     * </p>
     *
     * @param orgId     Represents the organization id.
     * @param source    Represents the entity type.
     * @param entityIds Represents the ids of the entities to remove.
     */
    void delete(final long orgId, final Source source, final Collection<Long> entityIds);

    /**
     * <p>
     * Deletes insights rows for multiple sources in a single query.
     * </p>
     *
     * @param orgId       Represents the organization id.
     * @param idsBySource Represents the entity ids grouped by source.
     */
    void delete(final long orgId, final Map<Source, Collection<Long>> idsBySource);


    /**
     * <p>
     * Rewrites only the ACTIVITY-group columns for every source in a single
     * I/O pair: one combined {@code createIfNotExists} SELECT across all
     * sources, followed by one bulk UPDATE covering all entities.
     * </p>
     *
     * @param orgId            Represents the organization id.
     * @param insightsBySource Per-source map of entity-id → activity insights.
     */
    void updateActivityInsights(final long orgId, final Map<Source, Map<Long, ActivityInsights>> insightsBySource);

    /**
     * <p>
     * Rewrites only the EMAIL-group columns for every source in a single
     * I/O pair: one combined {@code createIfNotExists} SELECT across all
     * sources, followed by one bulk UPDATE covering all entities.
     * </p>
     *
     * @param orgId            Represents the organization id.
     * @param insightsBySource Per-source map of entity-id → email insights.
     */
    void updateEmailInsights(final long orgId, final Map<Source, Map<Long, EmailInsights>> insightsBySource);

    /**
     * <p>
     * Rewrites only the DEAL-group columns for every applicable source in a
     * single I/O pair: one combined {@code createIfNotExists} SELECT across all
     * sources, followed by one bulk UPDATE covering all entities.
     * </p>
     *
     * @param orgId            Represents the organization id.
     * @param insightsBySource Per-source map of entity-id → deal insights.
     */
    void updateDealInsights(long orgId, Map<Source, Map<Long, DealInsights>> insightsBySource);

    /**
     * <p>
     * Recomputes all the entity insights.
     * </p>
     */
    void recomputeAllEntityInsights();
}