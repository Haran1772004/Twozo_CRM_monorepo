package com.tlc.notification.internal.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * <p>
 * Provides the error codes of Notification module.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public enum NotificationErrorCodes implements ErrorCodeProvider {

    UNABLE_TO_MAKE_CALL(0x01),
    FOLDER_CLOSE_ERROR(0x02),
    EMAIL_SEND_FAILED(0x03),
    UNABLE_TO_LISTEN_BEFORE_SEND(0x05),
    UNABLE_TO_LISTEN_AFTER_SENT(0x06),
    UNABLE_TO_LISTEN_ON_ERROR(0x07),
    UNABLE_TO_SEND_WHATSAPP_MESSAGE(0x08),
    UNABLE_TO_SEND_SMS(0x09),
    COULD_NOT_FIND_CALL_RECORDINGS(0x10),
    COULD_NOT_CREATE_VOICE_CALL(0x11),
    COULD_NOT_FIND_CALL_DETAILS(0x12),
    COULD_NOT_VALIDATE_NUMBER(0x13),
    SAVE_FAILED(0x14),
    FOLDER_NOT_FOUND(0x15),
    FOLDER_READ_WRITE_FAILED(0x16),
    STORE_CONNECTION_FAILED(0x17),
    INVALID_FLAG(0x18),
    INVALID_EMAIL_ADDRESS(0x19),
    DATE_EXCEPTION(0x20),
    INVALID_SUBJECT(0x21),
    INVALID_CONTENT(0x22),
    INVALID_CONTENT_TYPE(0x23),
    DATA_HANDLER_EXCEPTION(0x24),
    MESSAGE_ID_FETCH_FAILED(0X25),
    FOLDER_CREATE_FAILED(0x26),
    FOLDER_ACCESS_RESTRICTED(0x27),
    HEADER_NOT_FOUND(0x28),
    WRITE_OPERATION_FAILED(0x29),
    BAD_REQUEST_ERROR(0x30),
    SEARCH_FAILED(0x31),
    APPEND_FAILED(0x32),
    INVALID_URL(0x33),
    NO_MESSAGE_FOUND(0x34),
    HEADER_EXCEPTION(0x35),
    INVALID_USER(0x36),
    INVALID_TOKEN(0x37),
    EMAIL_RETRIEVE_FAILED(0x38),
    SYNC_ADDRESS_NOT_SUPPORTED(0x39),
    MAIL_UPDATE_FAILED(0x40),
    STORE_CLOSE_FAILED(0x41),
    APP_CONFIGURATION_FILE_NOT_FOUND(0x42),
    MESSAGE_DELETION_FAILED(0X43),
    INVALID_DRAFT_ACTION(0x44),
    ARCHIVE_DELETION_FAILED(0x45),
    INVALID_REFRESH_TOKEN(0x46),
    INVALID_EMAIL_PROVIDER(0x47),
    INVALID_OUTBOUND_TYPE(0x48),
    INVALID_MESSAGE_STATE(0X49),
    INVALID_RECIPIENT_TYPE(0x50),
    JNDI_LOOKUP_FAILED(0x51),
    INVALID_MESSAGE_CATEGORY(0x52),
    INVALID_PROVIDER(0x53),
    INVALID_SETTINGS(0x54),
    ATTACHMENT_RETRIEVAL_FAILED(0x55),
    FILE_UPLOAD_FAILED(0x56),
    INVALID_MESSAGE_DISPATCH_TYPE(0x57),
    FAILED_TO_RETRIEVE_CALENDAR_EVENT(0x58),
    UNABLE_TO_ADD_EVENT(0x59),
    UNABLE_TO_UPDATE_EVENT(0x60),
    UNABLE_TO_DELETE_EVENT(0x61),
    UNABLE_TO_GET_CALENDAR(0x62),
    UNABLE_TO_GET_CONTACTS(0x63),
    UNABLE_TO_ADD_CONTACT(0x64),
    UNABLE_TO_UPDATE_CONTACT(0x65),
    UNABLE_TO_DELETE_CONTACT(0x66),
    ADDRESS_FETCH_FAILED(0x67),
    TOKEN_FETCH_FAILED(0X68),
    INVALID_SYNC_MODE(0x69),
    INVALID_CONNECTION_STATUS(0x70),
    EMAIL_NOT_FOUND(0x71),
    INVALID_GROUP(0x72),
    INVALID_SERVICE_CATEGORY(0x73),
    TEMPLATE_FILE_FETCH_ACTION_FAILED(0x74),
    RESOURCE_SCAN_FAILED(0x75),
    CONFIGURATION_REQUIRED(0x76),
    INVALID_SYNC_STATUS(0x77),
    INVALID_CALL_TYPE(0x78),
    INVALID_CALL_STATUS(0x79),
    INVALID_TELEPHONE_PROVIDER(0x80),
    UNSUPPORTED_OPERATION(0x81),
    ;

    private final int code;

    NotificationErrorCodes(final int localCode) {
        this.code = NotificationErrorCodes.NotificationErrorCodeGroup.GROUP.getConvertedCode(localCode);
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
     * Provides the error code group of notification module.
     * </p>
     */
    private static class NotificationErrorCodeGroup implements ErrorCodeGroup {
        private static final ErrorCodeGroup GROUP = new NotificationErrorCodes.NotificationErrorCodeGroup();

        /**
         * {@inheritDoc}
         */
        @Override
        public int getPrefix() {
            return 0x15_0_0001;
        }
    }
}
