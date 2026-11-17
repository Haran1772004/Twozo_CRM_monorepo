package com.tlc.crm.common.export.manager;

import com.tlc.crm.common.export.req.ExportTracker;
import com.tlc.crm.common.internal.export.manager.DataExportManagerImpl;

import java.util.Optional;

/**
 * <p>
 * Manages the export data operations and includes the export status information in the database.
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public interface DataExportManager {

    /**
     * <p>
     * Gets the instance of {@link DataExportManager}.
     * </p>
     *
     * @return {@link DataExportManager}
     */
    static DataExportManager getInstance() {
        return DataExportManagerImpl.getInstance();
    }

    /**
     * <p>
     * Creates the export status for the given {@link ExportTracker}
     * </p>
     *
     * @param exportTracker Represents the {@link ExportTracker}
     */
    void create(final ExportTracker exportTracker);

    /**
     * <p>
     * Gets the {@link ExportTracker} for a given org and export id
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param exportId Represents the export id
     * @return An {@code Optional} containing the {@link ExportTracker} if found, or an empty {@code Optional} if no information is available
     */
    Optional<ExportTracker> getDataExport(final long orgId, final Long exportId);

    /**
     * <p>
     * Updates the {@link ExportTracker.ExportStatus} for the given export id
     * </p>
     *
     * @param orgId        Represents on organization id
     * @param exportId     Represents the export id
     * @param exportStatus Represents the {@link ExportTracker.ExportStatus}
     */
    void updateStatus(final long orgId, final long exportId, final ExportTracker.ExportStatus exportStatus);

    /**
     * <p>
     * Updates the file ID associated with a specific export for an organization.
     * </p>
     *
     * @param orgId    Refers the ID of the organization for which the export is associated.
     * @param exportId Refers the ID of the export whose file ID needs to be updated.
     * @param fileId   Refers the new file ID to be associated with the export.
     */
    void updateExportFileId(final long orgId, final long exportId, final long fileId);
}
