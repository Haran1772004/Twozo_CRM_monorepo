package com.tlc.crm.entity.fieldaudit.internal.status;

import com.tlc.commons.code.ErrorCodeProvider;

/**
 * Error codes for the Field Audit feature.
 *
 * @author Muthu kumar v
 * @version 1.0
 */
public enum FieldAuditErrorCodes implements ErrorCodeProvider {

    INVALID_SOURCE(60_001),
    INVALID_RECORD_ID(60_002),
    INVALID_FIELD(60_003),
    INVALID_TIME_RANGE(60_004),
    INVALID_CHANGE_SOURCE(60_005),
    INVALID_REQUEST(60_006),
    TRACKING_DISABLED(60_010),
    TRACKED_FIELDS_LIMIT_EXCEEDED(60_011),
    UNKNOWN_TRACKED_FIELD(60_012),
    HISTORY_FETCH_FAILED(60_020),
    HISTORY_PERSIST_FAILED(60_021),
    CONFIG_FETCH_FAILED(60_022),
    CONFIG_UPDATE_FAILED(60_023),
    UNSUPPORTED_ACTION(60_023);

    private final int code;

    FieldAuditErrorCodes(final int code) {
        this.code = code;
    }

    /**
     * {@inheritDoc}
     *
     * @return The error code
     */
    @Override
    public int getCode() {
        return code;
    }
}