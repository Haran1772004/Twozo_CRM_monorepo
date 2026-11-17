package com.tlc.crm.core.api.insights.service;

import com.tlc.crm.core.api.insights.models.EntityInsightsGroup;
import com.tlc.crm.entity.fields.source.Source;

import java.util.Collection;
import java.util.function.BiConsumer;

/**
 * <p>
 * Service that provides insights-related operations, including resolving
 * source-reference identifiers into the corresponding affected entity.
 * </p>
 *
 * @author Mohamed Yasar K
 */
public interface EntityInsightsService {

    static EntityInsightsService getInstance() {
        return EntityInsightsServiceImpl.getInstance();
    }

    /**
     * <p>
     * Streams the entity identifiers associated with the supplied source-reference
     * identifiers for the specified insights group, invoking {@code batchConsumer}
     * once per {@code (targetSource, page)} pair as each DB page is read.
     * </p>
     *
     * @param orgId      organization id
     * @param group      insights group
     * @param refSource  source type of the reference identifiers
     * @param sourceKeys source reference identifiers
     * @param consumer   called once per {@code (targetSource, batch)} page; never
     *                   {@code null} and the batch is always non-empty
     */
    void processRelatedEntity(long orgId, EntityInsightsGroup group, Source refSource,
                              Collection<Long> sourceKeys, BiConsumer<Source, Collection<Long>> consumer);

    /**
     * <p>
     * Identifies entities affected by the deletion of the specified activity
     * identifiers and enqueues the required insights recomputation workflow
     * for the impacted entities.
     * </p>
     *
     * @param orgId       organization id
     * @param activityIds activity identifiers that are being deleted
     */
    void enqueueMappedEntitiesForDeletedActivities(final long orgId, final Collection<Long> activityIds);

    /**
     * <p>
     * Identifies entities affected by the deletion of the specified entity-conversation
     * identifiers and enqueues the required insights recomputation workflow
     * for the impacted entities.
     * </p>
     *
     * @param orgId                 organization id
     * @param entityConversationIds entity-conversation identifiers that are being deleted
     */
    void enqueueMappedEntitiesForDeletedEntityConversations(final long orgId, final Collection<Long> entityConversationIds);

    /**
     * <p>
     * Identifies company entities affected by the deletion of the specified deal
     * identifiers and enqueues the required insights recomputation.
     * </p>
     *
     * @param orgId   organization id
     * @param dealIds deal identifiers that are being deleted
     */
    void enqueueMappedEntitiesForDeletedDeals(final long orgId, final Collection<Long> dealIds);
}