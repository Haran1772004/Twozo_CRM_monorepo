package com.tlc.crm.entity.list.model;

import java.util.Optional;

/**
 * <p>
 * Provides the service for retrieving a {@link ListPreviewResponse} containing the preview details to
 * be loaded into list response
 * </p>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public interface ListPreviewRetriever {

    /**
     * <p>
     * Fetches the {@link ListPreviewResponse}
     * </p>
     *
     * @param listPreviewFetchRequest Represents the {@link ListPreviewFetchRequest}
     * @return The optional {@link ListPreviewResponse}
     */
    Optional<ListPreviewResponse> fetchPreviewResponse(final ListPreviewFetchRequest listPreviewFetchRequest);
}