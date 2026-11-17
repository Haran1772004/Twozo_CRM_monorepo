package com.tlc.crm.entity.export;

import com.tlc.crm.entity.export.internal.model.FileExportRequest;

/**
 * An interface for converting the data by its field store type and exporting the data.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public interface DataExportHandler {

    /**
     * <p>
     * Exports data based on the provided {@link FileExportRequest}
     * </p>
     *
     * @param fileExportRequest Represents the {@link FileExportRequest}
     */
    void exportData(final FileExportRequest fileExportRequest);
}