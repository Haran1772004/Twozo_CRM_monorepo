package com.tlc.crm.core.segment.service.impl;

import com.tlc.crm.core.segment.manager.SegmentFolderManager;
import com.tlc.crm.core.segment.models.SegmentFolder;
import com.tlc.crm.core.segment.service.SegmentFolderService;

/**
 * <p>
 * Provides the default implementation of {@link SegmentFolderService}.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 * @see SegmentFolderService
 */
public class SegmentFolderServiceImpl implements SegmentFolderService {

    private final SegmentFolderManager folderManager;

    private static class Instance {
        private static final SegmentFolderServiceImpl INSTANCE = new SegmentFolderServiceImpl();
    }

    private SegmentFolderServiceImpl() {
        folderManager = SegmentFolderManager.getInstance();
    }

    public static SegmentFolderServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SegmentFolder get(final Long orgId, final Long id) {
        return folderManager.get(orgId, id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SegmentFolder partialGet(final Long orgId, final Long id) {
        return folderManager.partialGet(orgId, id);
    }
}