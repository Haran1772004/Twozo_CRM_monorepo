package com.tlc.file.client.internal.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * @author ThalaimalaiPandiyanT
 */
public enum AttachmentErrorCodes implements ErrorCodeProvider {

    ATTACHMENT_DETAILS_NOT_FOUND(0x11),
    INVALID_ATTACHMENT_TYPE(0x12),
    UNSUPPORTED_FILE_EXTENSION(0x13),
    INVALID_STORAGE_TYPE(0x14),
    UPLOAD_ACTION_FAILED(0x15),
    UPDATE_ACTION_FAILED(0x16),
    DELETE_ACTION_FAILED(0x17),
    ENTITY_NOT_FOUND(0x18),
    INVALID_DATA(0x19),
    ATTACHMENT_NOT_FOUND(0x20),
    ATTACHMENT_VALIDATION_FAILED(0x21),
    ;

    private final int code;

    AttachmentErrorCodes(int localCode) {
        this.code = AttachmentErrorCodes.AttachmentErrorCodesGroup.GROUP.getConvertedCode(localCode);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getCode()
    {
        return code;
    }

    private static class AttachmentErrorCodesGroup implements ErrorCodeGroup {

        private static final ErrorCodeGroup GROUP = new AttachmentErrorCodes.AttachmentErrorCodesGroup();

        /**
         * {@inheritDoc}
         */
        @Override
        public int getPrefix()
        {
            return 0x15_0_0001;
        }
    }
}

