package com.tlc.notification.internal.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * <p>
 * Provides the error codes of Email in the notification module.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public enum EmailErrorCodes implements ErrorCodeProvider {

    INVALID_ADDRESS_TYPE(0x01),
    INVALID_EMAIL_PROVIDER(0x02),
    JNDI_LOOKUP_FAILED(0x03),
    INVALID_OUTBOUND_TYPE(0x04),
    FOLDER_NOT_FOUND(0x05),
    INVALID_DRAFT_ACTION(0x06),
    UNSUPPORTED_SETTINGS(0x07),
    NO_MESSAGE_FOUND(0x08),
    FOLDER_READ_WRITE_FAILED(0x09),
    FOLDER_CREATE_FAILED(0x10),
    SEARCH_FAILED(0x11),
    FOLDER_CLOSE_ERROR(0x12),
    INVALID_EMAIL_ADDRESS(0x13),
    EMAIL_SEND_FAILED(0x14),
    INVALID_SUBJECT(0x15),
    MESSAGE_ID_FETCH_FAILED(0x16),
    HEADER_EXCEPTION(0x17),
    DATE_EXCEPTION(0x18),
    APPEND_FAILED(0x19),
    SAVE_FAILED(0x20),
    FILE_UPLOAD_FAILED(0x21),
    WRITE_OPERATION_FAILED(0x22),
    EMAIL_RETRIEVE_FAILED(0x23),
    EMAIL_NOT_FOUND(0x24),
    INVALID_MESSAGE(0x25),
    ATTACHMENT_RETRIEVAL_FAILED(0x26),
    INVALID_CONTENT_TYPE(0x27),
    INVALID_CONTENT(0x28),
    HEADER_NOT_FOUND(0x29),
    INVALID_FLAG(0x30),
    MESSAGE_DELETION_FAILED(0x31),
    INVALID_MESSAGE_CATEGORY(0x32),
    MAIL_UPDATE_FAILED(0x33),
    ARCHIVE_DELETION_FAILED(0x34),
    INVALID_SERVICE_CATEGORY(0x35),
    INVALID_PROVIDER(0x36),
    INVALID_SYNC_JOB_TYPE(0x37),
    ;

    private final int code;

    EmailErrorCodes(final int localCode) {
        this.code = EmailErrorCodes.EmailErrorCodeGroup.GROUP.getConvertedCode(localCode);
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
     * Provides the error code group of email in the notification module.
     * </p>
     */
    private static class EmailErrorCodeGroup implements ErrorCodeGroup {
        private static final ErrorCodeGroup GROUP = new EmailErrorCodes.EmailErrorCodeGroup();

        /**
         * {@inheritDoc}
         */
        @Override
        public int getPrefix() {
            return 0x65_0_0001;
        }
    }
}