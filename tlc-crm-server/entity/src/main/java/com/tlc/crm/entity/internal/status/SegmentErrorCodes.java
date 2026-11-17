package com.tlc.crm.entity.internal.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * <p>
 * Provides the error codes for segment and segment folder actions.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public enum SegmentErrorCodes implements ErrorCodeProvider {

    SEGMENT_FOLDER_NAME_ALREADY_EXISTS(0x01),
    SEGMENT_FOLDER_NOT_FOUND(0x02),
    UNSUPPORTED_ACTION(0x03),
    SEGMENT_NOT_FOUND(0x04),
    UNSUPPORTED_SEGMENT_TYPE(0x05),
    UNSUPPORTED_FILTER_TYPE(0x06),
    UNSUPPORTED_SEGMENT_STATUS(0x07),
    SEGMENT_NAME_ALREADY_EXISTS(0x08),
    UNSUPPORTED_SEGMENT_COMPUTATION_STATE(0x09),
    INVALID_SEGMENT_ID(0x10),
    SEGMENT_LIMIT_REACHED(0x09),
    SEGMENT_FOLDER_LIMIT_REACHED(0x0A),
    ;

    private final int code;

    SegmentErrorCodes(final int localCode) {
        this.code = SegmentErrorCodesGroup.GROUP.getConvertedCode(localCode);
    }

    @Override
    public int getCode() {
        return code;
    }

    /**
     * <p>
     * Represent the segment error code groups.
     * </p>
     *
     * @author Mohamed Yasar K
     * @version 1.0
     */
    private static class SegmentErrorCodesGroup implements ErrorCodeGroup {

        private static final ErrorCodeGroup GROUP = new SegmentErrorCodesGroup();

        @Override
        public int getPrefix() {
            return 0x10_0_0015;
        }
    }
}