package com.tlc.crm.core.action.export.internal;

import com.tlc.crm.core.segment.fields.SegmentFields;
import com.tlc.crm.entity.export.AbstractDataExportHandler;
import com.tlc.crm.entity.export.internal.model.FileExportRequest;

/**
 * <p>
 * A handler that transforms the segment list data and writes the result to a file.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public final class SegmentDataExportHandler extends AbstractDataExportHandler {

    /**
     * <p>
     * Creates the segment data export handler. Private to enforce the singleton pattern.
     * </p>
     */
    private SegmentDataExportHandler() {
        super();
    }

    /**
     * <p>
     * Holds the singleton instance of the {@link SegmentDataExportHandler}, ensuring
     * it is lazily initialized when first accessed.
     * </p>
     */
    private static final class InstanceHolder {

        private static final SegmentDataExportHandler INSTANCE = new SegmentDataExportHandler();
    }

    /**
     * <p>
     * Gets the singleton instance of the {@link SegmentDataExportHandler}.
     * </p>
     *
     * @return The {@link SegmentDataExportHandler} instance.
     */
    public static SegmentDataExportHandler getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * <p>
     * Exports the segment list data by transforming the entity rows in the given request
     * and writing the result to a file.
     * </p>
     *
     * @param fileExportRequest Represents the {@link FileExportRequest}.
     */
    @Override
    public void exportData(final FileExportRequest fileExportRequest) {
        export(SegmentFields.SEGMENT.getName(), fileExportRequest);
    }
}