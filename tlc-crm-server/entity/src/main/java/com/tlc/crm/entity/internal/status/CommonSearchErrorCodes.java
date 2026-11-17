package com.tlc.crm.entity.internal.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * <p>
 * Provides the error codes of common search.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public enum CommonSearchErrorCodes implements ErrorCodeProvider {

    NO_INDEX_FIELDS_FOUND(0X01),
    NO_INDEXES_FOUND(0x02),
    SEARCH_TEXT_NOT_FOUND(0x03),
    INVALID_INDEX(0x04),
    FROM_VALUE_CANNOT_BE_ZERO_OR_NEGATIVE(0x05),
    INDEX_VALUE_CANNOT_BE_EMPTY(0x06),
    MODEL_CANNOT_BE_INVOKED(0X07),
    TOO_SHORT_SEARCH_TEXT(0x08),
    INDEX_KEY_NOT_FOUND(0x09),
    SEARCH_TEXT_CANNOT_BE_EMPTY(0x10),
    COUNT_VALUE_CANNOT_BE_ZERO_OR_NEGATIVE(0x11),
    INVALID_INDEX_DATA(0x12),
    SEARCH_TEXT_IS_EMPTY(0x13),
    ;

    private final int code;

    CommonSearchErrorCodes(final int localCode) {
        this.code = CommonSearchErrorCodeGroup.GROUP.getConvertedCode(localCode);
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

    private static class CommonSearchErrorCodeGroup implements ErrorCodeGroup {
        private static final ErrorCodeGroup GROUP = new CommonSearchErrorCodeGroup();

        @Override
        public int getPrefix() {
            return 0x25_0_0001;
        }
    }
}
