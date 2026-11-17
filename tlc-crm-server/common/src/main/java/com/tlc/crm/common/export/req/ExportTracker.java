package com.tlc.crm.common.export.req;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.common.internal.export.code.DataExportErrorCodes;
import com.tlc.validator.TlcModel;

/**
 * <p>
 * Represents the status information of an export operation.
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public class ExportTracker implements TlcModel {

    private Long orgId;
    private Long userId;
    private Long id;
    private ExportStatus exportStatus;
    private Long exportFileId;

    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(final Long userId) {
        this.userId = userId;
    }

    public ExportStatus getExportStatus() {
        return exportStatus;
    }

    public void setExportStatus(final ExportStatus exportStatus) {
        this.exportStatus = exportStatus;
    }

    public Long getExportFileId() {
        return exportFileId;
    }

    public void setExportFileId(final Long exportFileId) {
        this.exportFileId = exportFileId;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    @Override
    public Long id() {
        return id;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    @Override
    public Object identity() {
        return null;
    }

    /**
     * Represents the export action status
     *
     */
    public enum ExportStatus {
        NEW((short) 1),
        EXPORTING((short) 2),
        EXPORTED((short) 3),
        EXPORT_FAILED((short) 4),
        ;

        private final short id;

        ExportStatus(final short id) {
            this.id = id;
        }

        public short getId() {
            return id;
        }

        /**
         * <p>
         * Gets the {@link ExportStatus} for the given id.
         * </p>
         *
         * @param id Represents the export status id
         * @return {@link ExportStatus}
         */
        public static ExportStatus get(final short id) {
            return switch (id) {
                case 1 -> NEW;
                case 2 -> EXPORTING;
                case 3 -> EXPORTED;
                case 4 -> EXPORT_FAILED;
                default ->
                        throw ErrorCode.get(DataExportErrorCodes.INVALID_EXPORT_STATUS, "i18n.data.export.invalid.status");
            };
        }
    }
}
