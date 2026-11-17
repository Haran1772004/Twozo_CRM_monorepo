package com.tlc.crm.core.api.insights.service;

import com.tlc.crm.core.api.insights.models.EntityInsightsGroup;
import com.tlc.crm.entity.fields.source.Source;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * Recomputes one {@link EntityInsightsGroup} for a
 * batch of entities by issuing fresh aggregate queries against the source tables.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public interface EntityInsightsGroupAggregator {

    /**
     * <p>
     * Recomputes the group for the given entities.
     * </p>
     *
     * @param orgId             Represents the organization id.
     * @param source            Represents the entity type.
     * @param affectedEntityIds Represents the entity ids that need recompute.
     */
    void aggregate(long orgId, Source source, Collection<Long> affectedEntityIds);

    /**
     * <p>
     * Convenience overload that recomputes the group for every
     * {@code (source, entityIds)} entry in the supplied map, issuing one
     * aggregation call per source.
     * </p>
     *
     * @param orgId             Represents the organization id.
     * @param entityIdsBySource Represents the mapping of source to entity ids.
     */
    void aggregate(final long orgId, final Map<Source, Collection<Long>> entityIdsBySource);
}