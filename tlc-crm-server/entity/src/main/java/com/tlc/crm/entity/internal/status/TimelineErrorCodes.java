package com.tlc.crm.entity.internal.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * <p>
 * Provides the error codes of timeline.
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public enum TimelineErrorCodes implements ErrorCodeProvider {

    CONTACT_NOT_FOUND(0x13),
    DEAL_NOT_FOUND(0x14),
    UNSUPPORTED_TIMELINE_FILTER(0x15),
    COMPANY_NOT_FOUND(0x16),
    USER_RETRIEVAL_FAILED(0x17),
    UNSUPPORTED_SOURCE_CATEGORY(0x18),
    UNSUPPORTED_MERGE_CATEGORY(0x19),
    ATTACHMENTS_RETRIEVAL_FAILED(0x20),
    ;

    private final int code;

    TimelineErrorCodes(final int localCode) {
        this.code = TimelineErrorCodeGroup.GROUP.getConvertedCode(localCode);
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

    private static class TimelineErrorCodeGroup implements ErrorCodeGroup {
        private static final ErrorCodeGroup GROUP = new TimelineErrorCodeGroup();

        @Override
        public int getPrefix() {
            return 0x25_0_0001;
        }
    }
}
