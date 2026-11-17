package com.tlc.search.internal.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * <p>
 * Provides the error codes of indexing the documents.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public enum IndexingErrorCodes implements ErrorCodeProvider {

    REST_CLIENT_CREATION_FAILED(0x01),
    INDEX_CREATION_FAILED(0x02),
    INDEX_DELETION_FAILED(0x03),
    UNABLE_TO_CHECK_EXISTS(0x05),
    INDEX_CLONING_FAILED(0x06),
    FAILED_TO_FETCH_INDICES(0x07),
    INDEX_SETTINGS_UPDATE_FAILED(0x08),
    FIELD_MAPPINGS_UPDATE_FAILED(0x09),
    FAILED_TO_FETCH_INDEX_SETTINGS(0x10),
    DOCUMENT_RETRIEVAL_FAILED(0x11),
    DOCUMENT_SEARCH_FAILED(0x12),
    UNABLE_TO_FETCH_COUNT(0x13),
    SCROLL_REQUEST_FAILED(0x14),
    SCROLL_CLEAR_FAILED(0x15),
    DOCUMENT_INDEXING_FAILED(0x16),
    DOCUMENT_COPYING_FAILED(0x17),
    DOCUMENT_DELETION_FAILED(0x18),
    DOCUMENT_UPDATE_FAILED(0x19),
    BULK_REQUEST_EXECUTION_FAILED(0x20),
    DOCUMENT_RESTORE_FAILED(0x21),
    SESSION_CREATION_FAILED(0x22),
    FAILED_TO_CLOSE_SESSION(0x23),
    SEARCH_TEXT_NOT_FOUND(0x24),
    INVALID_INDEX(0x25),
    EXIST_REQUEST_FAILED(0x26),
    INVALID_ANALYSIS_MODEL(0x27),
    SCRIPT_UPLOAD_FAILED(0x28),
    SCRIPT_DELETION_FAILED(0x29),
    SCRIPT_PARSING_FAILED(0x30),
    SEARCH_RESPONSE_CONVERSION_FAILED(0x31),
    SEARCH_RESULT_PROCESSING_FAILED(0x32),
    ;

    private final int code;

    IndexingErrorCodes(final int localCode) {
        this.code = IndexingErrorCodeGroup.GROUP.getConvertedCode(localCode);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getCode() {
        return code;
    }

    /**
     * <p>
     * Provides the error code group of indexing the documents.
     * </p>
     */
    private static class IndexingErrorCodeGroup implements ErrorCodeGroup {
        private static final ErrorCodeGroup GROUP = new IndexingErrorCodeGroup();

        /**
         * {@inheritDoc}
         */
        @Override
        public int getPrefix() {
            return 0x16_0_0001;
        }
    }
}
