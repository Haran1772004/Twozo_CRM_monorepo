package com.tlc.crm.core.api.insights.models;

import com.tlc.crm.core.api.insights.manager.EntityInsightsRecomputeRequestManager;
import com.tlc.crm.entity.fields.source.Source;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * <p>
 * Represents the entity insights batch context
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public final class EntityInsightsRecomputeBatchRequest {

    private final Map<Long, Map<EntityInsightsGroup, Map<EntityInsightsIdentifierType,
            Map<Source, Collection<Long>>>>> batchEvents = new HashMap<>();
    private final Map<Long, Collection<Long>> emailConversationBatchEvents = new HashMap<>();

    /**
     * <p>
     * Adds a single entity ID under the given key, creating intermediate
     * maps lazily as needed.
     * </p>
     *
     * @param orgId                        Represents the organization id.
     * @param source                       Represents the entity source.
     * @param entityId                     Represents the entity id to accumulate.
     * @param entityInsightsGroup          Represents the insights group.
     * @param entityInsightsIdentifierType Represents the identifier type.
     */
    public void addEvent(final long orgId, final Source source, final long entityId,
                         final EntityInsightsGroup entityInsightsGroup, final EntityInsightsIdentifierType entityInsightsIdentifierType) {
        batchEvents
                .computeIfAbsent(orgId, key -> new HashMap<>())
                .computeIfAbsent(entityInsightsGroup, key -> new HashMap<>())
                .computeIfAbsent(entityInsightsIdentifierType, key -> new HashMap<>())
                .computeIfAbsent(source, key -> new HashSet<>())
                .add(entityId);
    }

    /**
     * <p>
     * Adds a conversation ID whose active / deleted status changed.
     * </p>
     *
     * @param orgId          Represents the organization id.
     * @param conversationId Represents the conversation id.
     */
    public void addEmailConversationEvent(final long orgId, final long conversationId) {
        emailConversationBatchEvents.computeIfAbsent(orgId, key -> new HashSet<>()).add(conversationId);
    }

    /**
     * <p>
     * Flushes the batched entities
     * </p>
     *
     * @param recomputeRequestManager Represents the recompute queue manager.
     */
    public void processEvent(final EntityInsightsRecomputeRequestManager recomputeRequestManager) {
        final Collection<Long> insertOrgIds = new HashSet<>();

        insertOrgIds.addAll(batchEvents.keySet());
        insertOrgIds.addAll(emailConversationBatchEvents.keySet());

        insertOrgIds.forEach(orgId ->
                recomputeRequestManager.processBatch(
                        orgId,
                        batchEvents.getOrDefault(orgId, Collections.emptyMap()),
                        emailConversationBatchEvents.getOrDefault(orgId, Collections.emptySet())));
    }
}