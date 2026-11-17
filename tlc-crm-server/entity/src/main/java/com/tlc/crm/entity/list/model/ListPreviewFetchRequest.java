package com.tlc.crm.entity.list.model;

import java.util.Collection;

/**
 * <p>
 * Holds the {@link ListPreviewFetchRequest} details to fetch the preview details to be loaded into the list response.
 * </p>
 *
 * @param orgId     Represents the organization ID
 * @param userId    Represents the user ID
 * @param entityIds Represents the collection of entity IDs for which the partial data is to be fetched
 * @param provider  Represents the {@link Provider}
 */
public record ListPreviewFetchRequest(long orgId, long userId, Collection<Long> entityIds, Provider provider) {
}