package com.tlc.crm.email.notification.email.tracking.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

import com.tlc.crm.email.notification.email.tracking.model.EmailTrackingDetail;

/**
 * <p>
 * Provides the error codes of {@link EmailTrackingDetail}
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public enum TrackingErrorCodes implements ErrorCodeProvider {

    INVALID_TRACKING_TYPE(0x1),
    TRACKING_FILE_NOT_FOUND(0x2),
    FAILED_TO_LOAD_THE_IMAGE_FILE(0x3),
    ;

    private final int code;

    TrackingErrorCodes(int localCode) {
        this.code = TrackerErrorCodesGroup.GROUP.getConvertedCode(localCode);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getCode() {
        return code;
    }

    private static class TrackerErrorCodesGroup implements ErrorCodeGroup {
        private static final ErrorCodeGroup GROUP = new TrackerErrorCodesGroup();

        @Override
        public int getPrefix() {
            return 0x19_0_0001;
        }
    }
}
