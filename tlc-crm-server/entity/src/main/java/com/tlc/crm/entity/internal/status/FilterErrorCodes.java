package com.tlc.crm.entity.internal.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * <p>
 * Provides the error code for filter.
 * </p>
 *
 * @author Sathish
 */
public enum FilterErrorCodes implements ErrorCodeProvider {

    FILTER_FIELD_NOT_FOUND(0x01),
    CONNECTOR_NOT_FOUND(0x02),
    COMPARATOR_NOT_FOUND(0x03),
    VALUE_NOT_FOUND(0x04),
    INVALID_ADDITIONAL_OPTION(0x05),
    FILTER_NOT_FOUND(0x6),
    FILTER_FIELD_ID_NOT_FOUND(0x7),
    FIELD_SOURCE_ID_NOT_FOUND(0x8),
    FILTER_ACTION_ALREADY_REGISTERED(0x9),
    FILTER_ACTION_NOT_FOUND(0x10),
    NO_FILTER_ACTIONS_REGISTERED(0x11),
    INVALID_QUARTER(0x12),
    INVALID_MONTH(0x13),
    CRITERIA_NOT_FOUND(0x14),
    FILTER_FIELDS_NOT_FOUND(0x15),
    INVALID_SOURCE(0x16),
    UNABLE_TO_PARSE_DATE(0x17),
    UNABLE_TO_PARSE_TIME(0x18),
    UNABLE_TO_PARSE_DATE_TIME(0x19),
    ACCESS_DENIED(0x20),
    FILTER_CATEGORY_NOT_FOUND(0x21),
    ;

    private final int code;

    FilterErrorCodes(final int localCode) {
        this.code = FilterErrorCodesGroup.GROUP.getConvertedCode(localCode);
    }

    @Override
    public int getCode() {
        return code;
    }

    private static class FilterErrorCodesGroup implements ErrorCodeGroup {

        private static final ErrorCodeGroup GROUP = new FilterErrorCodesGroup();

        @Override
        public int getPrefix() {
            return 0x10_0_0005;
        }
    }
}
