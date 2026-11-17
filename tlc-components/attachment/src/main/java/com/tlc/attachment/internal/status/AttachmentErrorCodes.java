package com.tlc.attachment.internal.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * Provides the {@link com.tlc.commons.code.ErrorCode} to refer the attachment related errors
 *
 * @author Sundar
 * @version 1.0
 */
public enum AttachmentErrorCodes implements ErrorCodeProvider {

    UNSUPPORTED_FILE_FORMAT(0X10),
    FILE_READ_FAILED(0X11),
    MAX_READ_LIMIT_REACHED(0X12),
    UNSUPPORTED_IMAGE_EXTENSION(0x13),
    UNSUPPORTED_PROFILE_PICTURE_TYPE(0x14),
    UNSUPPORTED_CONTENT_TYPE(0x15),
    MAX_RECORD_LIMIT_REACHED(0x16),
    INVALID_CHUNK_SIZE(0x17),
    ;

    private final int code;

    AttachmentErrorCodes(int localCode) {
        this.code = Group.GROUP.getConvertedCode(localCode);
    }

    /**
     * {@inheritDoc}
     *
     * @return the error code value
     */
    @Override
    public int getCode() {
        return code;
    }

    private static class Group implements ErrorCodeGroup {
        private static final ErrorCodeGroup GROUP = new Group();

        /**
         * {@inheritDoc}
         *
         * @return the prefix value
         */
        @Override
        public int getPrefix() {
            return 0x10_0_0002;
        }
    }
}
