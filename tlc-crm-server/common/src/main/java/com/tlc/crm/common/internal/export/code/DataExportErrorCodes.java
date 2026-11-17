package com.tlc.crm.common.internal.export.code;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * <p>
 * Represents the export error codes.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see ErrorCodeProvider
 */
public enum DataExportErrorCodes implements ErrorCodeProvider {

    EXPORT_IN_PROGRESS(0x1),
    INVALID_DATA_EXPORTER_NAME(0x2),
    DUPLICATE_DATA_EXPORTERS_IDENTIFIED(0x3),
    FAILED_TO_REGISTER_DATA_EXPORTER_ACTIONS(0x4),
    NO_DATA_TO_EXPORT(0x5),
    EXPORT_FAILED(0x6),
    INVALID_EXPORT_ID(0x7),
    INVALID_EXPORT_STATUS(0x8),
    UNSUPPORTED_EXPORT_TYPE(0x9),
    DATA_EXPORT_CONSUMER_NOT_REGISTERED(0x10),
    EXPORT_FILE_NOT_FOUND(0x11),
    INVALID_FILE_OPERATION_TYPE(0x12),
    UNSUPPORTED_ACTION(0x13),
    ;

    private final int code;

    DataExportErrorCodes(final int localCode) {
        this.code = DataExportErrorCodeGroup.GROUP.getConvertedCode(localCode);
    }

    /**
     * {@inheritDoc}
     *
     * @return the code
     */
    @Override
    public int getCode() {
        return code;
    }

    /**
     * <p>
     * Represents the group of error codes.
     * </p>
     *
     * @author KowsalyaSP
     * @version 1.0
     * @see ErrorCodeGroup
     */
    private static class DataExportErrorCodeGroup implements ErrorCodeGroup {
        private static final ErrorCodeGroup GROUP = new DataExportErrorCodeGroup();

        @Override
        public int getPrefix() {
            return 0x27_0_0001;
        }
    }
}
