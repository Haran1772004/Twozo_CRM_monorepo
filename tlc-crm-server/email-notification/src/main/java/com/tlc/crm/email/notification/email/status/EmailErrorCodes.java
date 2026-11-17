package com.tlc.crm.email.notification.email.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * <p>
 * Represents error codes related to mail
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public enum EmailErrorCodes implements ErrorCodeProvider {

    EMAIL_ID_NOT_FOUND(0x1),
    EMAIL_NOT_SYNCED(0x2),
    EMAIL_NOT_FOUND(0x3),
    SYNC_EMAIL_NOT_FOUND(0x4),
    CREATE_ACTION_FAILED(0x5),
    INVALID_DATE(0x6),
    INVALID_PROVIDER_ACTION(0X7),
    INVALID_SYNC_TYPE(0X8),
    INVALID_CONFIG_STATE(0X9),
    INVALID_SYNC_CONFIG(0x10),
    INVALID_DATE_CONFIG(0x11),
    INVALID_FROM_TYPE(0x12),
    UPLOAD_ACTION_FAILED(0x13),
    USER_EMAIL_RETRIEVAL_FAILED(0x14),
    EMAIL_ALREADY_EXISTS(0x15),
    FAILED_TO_REGISTER_ACTIONS(0x16),
    NO_MAILS_FOUND(0x17),
    INVALID_CONFIGURATION_FORMAT(0x18),
    FAILED_TO_LOAD_EMAILS(0x19),
    DEFAULT_FROM_ADDRESS_DOMAIN_MISMATCH(0x20),
    DEFAULT_REPLY_TO_ADDRESS_DOMAIN_MISMATCH(0x21),
    INVALID_DISCONNECT_MODE(0x22),
    INVALID_DEFAULT_SYNC_ADDRESS(0x23),
    INVALID_SYNC_ADDRESS(0x24),
    DEFAULT_SYNC_ID_MANDATORY(0x25),
    MODIFICATION_ACTION_RESTRICTED(0x26),
    ;

    private final int code;

    EmailErrorCodes(final int localCode) {
        this.code = EmailErrorCodeGroup.GROUP.getConvertedCode(localCode);
    }

    @Override
    public int getCode() {
        return code;
    }

    private static class EmailErrorCodeGroup implements ErrorCodeGroup {
        private static final ErrorCodeGroup GROUP = new EmailErrorCodeGroup();

        @Override
        public int getPrefix() {
            return 0x7_0_0001;
        }
    }
}