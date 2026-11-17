package com.tlc.crm.core.notification.email.org.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * <p>
 * Represents error codes related to orgEmail
 * </p>
 *
 * @author AswiniN
 * @version 1.0
 */
public enum OrgEmailErrorCodes implements ErrorCodeProvider {
    ORG_EMAIL_NOT_FOUND(0x20),
    ORG_EMAIL_ADDRESS_ALREADY_EXISTS(0x21),
    ORG_EMAIL_DISPLAY_NAME_ALREADY_EXISTS(0x22),
    ROLE_NOT_FOUND(0x23),
    DELETE_ACTION_FAILED(0x24),
    UPDATE_ACTION_FAILED(0x25),
    CREATE_ACTION_FAILED(0x26),
    INVALID_OTP(0x27),
    ;

    private final int code;

    OrgEmailErrorCodes(final int localCode) {
        this.code = OrgEmailErrorCodeGroup.GROUP.getConvertedCode(localCode);
    }

    @Override
    public int getCode() {
        return code;
    }

    private static class OrgEmailErrorCodeGroup implements ErrorCodeGroup {
        private static final ErrorCodeGroup GROUP = new OrgEmailErrorCodeGroup();

        @Override
        public int getPrefix() {
            return 0x20_0_0001;
        }
    }
}
