package com.tlc.crm.core.features.internal.file.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * Contains the file related error codes.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public enum FileHolderErrorCodes implements ErrorCodeProvider {

    CONTACT_NOT_FOUND(0x11),
    DEAL_NOT_FOUND(0x12),
    ATTACHMENT_NOT_FOUND(0x13),
    COMPANY_NOT_FOUND(0x14),
    SOURCE_NOT_FOUND(0x15),
    USER_RETRIEVAL_FAILED(0x16),
    INVALID_FILE_CATEGORY(0x17),
    ;

    private final int code;

    FileHolderErrorCodes(int localCode) {
        this.code = FileHolderErrorCodeGroup.GROUP.getConvertedCode(localCode);
    }

    @Override
    public int getCode() {
        return code;
    }

    private static class FileHolderErrorCodeGroup implements ErrorCodeGroup {

        private static final ErrorCodeGroup GROUP = new FileHolderErrorCodeGroup();

        @Override
        public int getPrefix() {
            return 0x40_0_0001;
        }
    }

}