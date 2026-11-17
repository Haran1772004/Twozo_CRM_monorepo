package com.tlc.crm.core.features.internal.displayPicture.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * <p>
 * Provides the error codes of profile picture module
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public enum DisplayPictureErrorCode implements ErrorCodeProvider {

    UNSUPPORTED_WEB_ACTION_TYPE(0x01),
    FILE_NOT_FOUND(0x02),
    PROFILE_PICTURE_UPLOAD_ACTION_FAILED(0x03),
    INVALID_SOURCE(0x04),
    PROFILE_PICTURE_DELETION_ACTION_FAILED(0x05),
    DP_NOT_FOUND(0x06),
    PROFILE_PICTURE_FETCH_ACTION_FAILED(0x07),
    VALIDATION_FAILED(0x08);

    private final int code;

    DisplayPictureErrorCode(final int localCode) {
        this.code = DisplayPictureCodeGroup.GROUP.getConvertedCode(localCode);
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

    private static class DisplayPictureCodeGroup implements ErrorCodeGroup {
        private static final DisplayPictureCodeGroup GROUP = new DisplayPictureCodeGroup();

        @Override
        public int getPrefix() {
            return 0x26_0_0001;
        }
    }
}
