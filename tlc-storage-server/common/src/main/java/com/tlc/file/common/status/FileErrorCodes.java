package com.tlc.file.common.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * Refers the file error codes.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public enum FileErrorCodes implements ErrorCodeProvider {

    AUTHORIZATION_FAILED(0x01),
    ATTACHMENT_DETAILS_NOT_FOUND(0x02),
    INVALID_ATTACHMENT_TYPE(0x03),
    FILE_UPLOAD_FAILED(0x04),
    UNSUPPORTED_FILE_EXTENSION(0x05),
    INVALID_STORAGE_TYPE(0x06),
    UPLOAD_ACTION_FAILED(0x07),
    DELETE_ACTION_FAILED(0x08),
    INVALID_DATA(0x09),
    ATTACHMENT_NOT_FOUND(0x10),
    USER_NOT_FOUND(0x11),
    ENTITY_TO_BYTE_CONVERSION_FAILED(0x12),
    ATTACHMENT_VALIDATION_FAILED(0x13),
    INVALID_ACTION_TYPE(0x14),
    ;

    private final int code;

    FileErrorCodes(int localCode) {
        this.code = FileErrorCodesGroup.GROUP.getConvertedCode(localCode);
    }

    @Override
    public int getCode() {
        return code;
    }

    private static class FileErrorCodesGroup implements ErrorCodeGroup {
        private static final ErrorCodeGroup GROUP = new FileErrorCodesGroup();

        /**
         * {@inheritDoc}
         */
        @Override
        public int getPrefix() {
            return 0x02_0_0001;
        }
    }

}
