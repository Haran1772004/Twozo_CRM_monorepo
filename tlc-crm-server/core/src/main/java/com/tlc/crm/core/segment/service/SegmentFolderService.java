package com.tlc.crm.core.segment.service;

import com.tlc.crm.core.segment.models.SegmentFolder;
import com.tlc.crm.core.segment.service.impl.SegmentFolderServiceImpl;

/**
 * <p>
 * Provides an abstract way to deal with {@link SegmentFolder} definitions.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public interface SegmentFolderService {

    /**
     * <p>
     * Gets the {@link SegmentFolderService} instance.
     * </p>
     *
     * @return {@link SegmentFolderService}.
     */
    static SegmentFolderService getInstance() {
        return SegmentFolderServiceImpl.getInstance();
    }

    /**
     * <p>
     * Fetches the {@link SegmentFolder} for the given id.
     * </p>
     *
     * @param orgId Represents the organization id.
     * @param id    Represents the folder id.
     * @return {@link SegmentFolder}.
     */
    SegmentFolder get(final Long orgId, final Long id);

    /**
     * <p>
     * Fetches the partial data (NAME) of {@link SegmentFolder} for the given id.
     * </p>
     *
     * @param orgId Represents the organization id.
     * @param id    Represents the folder id.
     * @return {@link SegmentFolder}.
     */
    SegmentFolder partialGet(final Long orgId, final Long id);
}