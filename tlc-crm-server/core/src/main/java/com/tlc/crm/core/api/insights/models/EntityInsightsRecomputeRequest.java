package com.tlc.crm.core.api.insights.models;

import com.tlc.crm.entity.fields.source.Source;

/**
 * <p>
 * One drained row of the {@code OMCRMEntityInsightsRecomputeRequest} queue.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public record EntityInsightsRecomputeRequest(long id, long orgId, Source source, long entityId,
                                               EntityInsightsGroup group, EntityInsightsIdentifierType payload) {
}