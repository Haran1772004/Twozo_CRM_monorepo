package com.tlc.attachment.storage.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * Provides the storage related error codes.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public enum StorageErrorCodes implements ErrorCodeProvider {

    STORAGE_SERVICE_INITIALIZATION_FAILED(0x21),
    OBJECT_UPLOAD_FAILED(0x22),
    OBJECT_DOWNLOAD_FAILED(0x23),
    BUCKET_CREATION_FAILED(0x24),
    BUCKET_VERSIONING_FAILED(0x25),
    BUCKET_NOT_FOUND(0x26),
    BUCKET_EXISTS_CHECK_ACTION_FAILED(0x27),
    OBJECT_NOT_FOUND(0x28),
    PRE_SIGNED_URL_GENERATION_FAILED(0x29),
    OBJECT_VERSION_DELETE_FAILED(0x30),
    OBJECT_LIST_ACTION_FAILED(0x31),
    OBJECT_COUNT_ACTION_FAILED(0x32),
    OBJECT_METADATA_ACTION_FAILED(0x33),

    FILE_CREATION_FAILED(0x35),
    UNSUPPORTED_FILE_EXTENSION(0x36),
    FILE_WRITE_ACTION_FAILED(0x37),
    FILE_NOT_FOUND(0x38),
    FAILED_TO_GET_INPUT_STREAM(0x39),
    FILE_CONVERSION_FAILED(0x40),
    INVALID_CSV_FILE(0x41),
    EXPORT_ACTION_FAILED(0x42),
    IMAGE_COMPRESSION_FAILED(0x43),
    PLAYWRIGHT_CLIENT_CREATION_FAILED(0x44),
    PDF_GENERATION_FAILED(0x45),
    ;

    private final int code;

    StorageErrorCodes(int localCode) {
        this.code = StorageErrorCodeGroup.GROUP.getConvertedCode(localCode);
    }

    @Override
    public int getCode() {
        return code;
    }

    private static class StorageErrorCodeGroup implements ErrorCodeGroup {
        private static final ErrorCodeGroup GROUP = new StorageErrorCodeGroup();

        @Override
        public int getPrefix() {
            return 0x35_0_0001;
        }
    }

}
