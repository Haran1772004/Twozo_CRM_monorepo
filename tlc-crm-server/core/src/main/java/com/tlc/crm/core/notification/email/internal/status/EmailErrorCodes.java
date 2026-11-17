package com.tlc.crm.core.notification.email.internal.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * <p>
 * Represents error codes related to mail
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public enum EmailErrorCodes implements ErrorCodeProvider {

    ENTITY_NOT_FOUND(0x20),
    MESSAGE_CATEGORY_NOT_SUPPORTED(0X21),
    EMAIL_ID_NOT_FOUND(0x22),
    EMAIL_NOT_SYNCED(0x23),
    INVALID_DRAFTED_CONVERSATION_ID(0x24),
    CONTACT_EMAIL_NOT_FOUND(0x25),
    INVALID_PROVIDER(0x26),
    LIMIT_EXCEEDED(0x27),
    UPDATE_ACTION_FAILED(0x28),
    INBOUND_EMAIL_NOT_FOUND(0x29),
    SOURCE_NOT_SUPPORTED(0X30),
    EMAIL_NOT_FOUND(0x31),
    SYNC_EMAIL_NOT_FOUND(0x32),
    ACCOUNTS_NOT_FOUND(0x33),
    NO_MAILS_FOUND(0x34),
    CREATE_ACTION_FAILED(0x35),
    DEAL_NOT_FOUND(0x36),
    RECIPIENT_NOT_FOUND(0x37),
    COMPOSE_RESTRICTED(0X38),
    COMPANY_NOT_FOUND(0x39),
    INVALID_INPUT(0x40),
    THREAD_NOT_FOUND(0x41),
    CONTACT_NOT_FOUND(0x42),
    REPLY_ALL_DISABLED(0x43),
    EMAIL_ALREADY_EXISTS(0x44),
    INVALID_OUTBOUND_TYPE(0x45),
    INVALID_COLUMN(0x46),
    RETRIEVE_FAILED(0x47),
    LIFE_CYCLE_STAGE_NOT_FOUND(0x48),
    CONVERSATION_NOT_FOUND(0x49),
    UNSUPPORTED_UPDATE_ACTION(0x50),
    SCHEDULED_MAIL_NOT_FOUND(0X51),
    SCHEDULED_MAIL_EXISTS(0X52),
    UPLOAD_ACTION_FAILED(0x53),
    INVALID_LINKED_CATEGORY(0x54),
    INVALID_FILTER_INTERVAL(0x55),
    INVALID_EMAIL_FILTER_ATTRIBUTE(0x56),
    INVALID_EMAIL_FOLDER(0x57),
    INVALID_DATE(0x58),
    INVALID_FOLDER_TYPE(0x59),
    INVALID_PROVIDER_ACTION(0X60),
    INVALID_SYNC_TYPE(0X61),
    INVALID_CONFIG_STATE(0X62),
    INVALID_SYNC_CONFIG(0x63),
    INVALID_DATE_CONFIG(0x64),
    INVALID_SETTING_CONFIGURATION(0x65),
    SOURCE_NOT_FOUND(0x66),
    INAPPROPRIATE_FILTER(0x67),
    EMAIL_SYNC_ACTION_FAILED(0x68),
    INVALID_SYNC_STATUS(0x69),
    USER_EMAIL_RETRIEVAL_FAILED(0x70),
    INVALID_FROM_TYPE(0x71),
    INVALID_FROM_ADDRESS(0x72),
    CONVERSATION_ID_NOT_FOUND(0x69),
    DRAFT_MAIL_EXISTS(0x70),
    UNSUBSCRIBED_RECIPIENTS(0x71),
    CONF_FILE_NOT_FOUND(0x72),
    TRACKING_CONFIG_FILE_NOT_FOUND(0x73),
    FILE_UPLOAD_FAILED(0x74),
    ATTACHMENT_NOT_FOUND(0x75),
    USER_RETRIEVAL_FAILED(0x76),
    PLACEHOLDER_NOT_SUPPORTED_FOR_MULTIPLE_RECIPIENTS(0x77),
    INVALID_SOURCE(0x78),
    EMAIL_DISPATCH_FAILED(0x79),
    USER_NOT_FOUND(0x80),
    INVALID_DRAFT_ACTION(0x81),
    INVALID_SCHEDULED_TIME(0x82),
    ATTACHMENT_PARSING_FAILED(0x83),
    RECIPIENT_LIMIT_EXCEEDED(0x84),
    INVALID_RECIPIENT(0x85),
    ATTACHMENT_DETAILS_NOT_FOUND(0x86),
    SEND_ACTION_FAILED(0x87),
    INVALID_SCHEDULED_STATE(0x88),
    DB_LOAD_ACTION_FAILED(0x89),
    INVALID_CATEGORY(0X90),
    UNSUBSCRIBED_EMAIL_ADDRESS_FOUND(0x91),
    UNSUPPORTED_EMAIL_VARIANT(0x92),
    FOLDER_ID_REQUIRED(0x93),
    UNSUPPORTED_VARIANT_TYPE(0x94),
    ;

    private final int code;

    EmailErrorCodes(final int localCode) {
        this.code = EmailErrorCodeGroup.GROUP.getConvertedCode(localCode);
    }

    @Override
    public int getCode() {
        return code;
    }

    /**
     * <p>
     * Represents the {@link EmailErrorCodeGroup}.
     * </P>
     *
     * @author KowsalyaSP
     * @version 1.0
     * @see ErrorCodeGroup
     */
    private static class EmailErrorCodeGroup implements ErrorCodeGroup {
        private static final ErrorCodeGroup GROUP = new EmailErrorCodeGroup();

        /**
         * {@inheritDoc}
         *
         * @return prefix
         */
        @Override
        public int getPrefix() {
            return 0x15_0_0001;
        }
    }
}