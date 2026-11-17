package com.tlc.crm.entity.internal.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * Provides the error codes of an activity
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see ErrorCodeProvider
 */
public enum ActivityErrorCodes implements ErrorCodeProvider {

    ACTIVITY_NOT_FOUND(0X13),
    ENTITY_NOT_FOUND(0x16),
    INVALID(0x17),
    UNSUPPORTED_STATUS(0x18),
    ACTIVITY_TYPE_NOT_FOUND(0x19),
    ADDRESS_NOT_FOUND(0x20),
    COMPANY_NOT_FOUND(0x21),
    ACTIVITY_ICON_NOT_FOUND(0x23),
    DUPLICATE_TYPE_ORDER(0x24),
    SOURCE_NOT_FOUND(0x25),
    CREATE_ACTION_FAILED(0x26),
    ACTIVITY_TYPE_DEACTIVATION_RESTRICTED(0x27),
    UNSUPPORTED_AVAILABILITY(0x28),
    UNSUPPORTED_REMINDER(0x29),
    UNSUPPORTED_PRIORITY(0x30),
    USER_NOT_FOUND(0x31),
    COLLABORATORS_NOT_FOUND(0x32),
    USER_RETRIEVAL_FAILED(0x33),
    UPDATE_ACTION_FAILED(0x34),
    DELETE_ACTION_FAILED(0x35),
    ACTIVITY_TYPE_ALREADY_EXISTS(0x36),
    ACTIVITY_RETRIEVAL_ACTION_FAILED(0x37),
    INVALID_SOURCE(0x38),
    ACTIVITY_TYPE_LIMIT_REACHED(0x39),
    ACTIVITY_FILTER_CRITERIA_NOT_FOUND(0x40),
    CHECK_IN_REQUIRED(0x41),
    UNSUPPORTED_ACTION(0x42),
    ;

    private final int code;

    ActivityErrorCodes(final int localCode) {
        this.code = ActivityErrorCodeGroup.GROUP.getConvertedCode(localCode);
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

    private static class ActivityErrorCodeGroup implements ErrorCodeGroup {
        private static final ErrorCodeGroup GROUP = new ActivityErrorCodeGroup();

        @Override
        public int getPrefix() {
            return 0x25_0_0001;
        }
    }
}
