package com.tlc.crm.core.notification.email.inbox.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * <p>
 * Represents error codes related to team inbox
 * </p>
 *
 * @author AswiniN
 * @version 1.0
 */
public enum TeamInboxErrorCodes implements ErrorCodeProvider {

    TEAM_INBOX_NOT_FOUND(0x01),
    UPDATE_ACTION_FAILED(0x02),
    EMAIL_ALREADY_IN_USE(0x03),
    USER_NOT_FOUND(0x04),
    TEAM_NOT_FOUND(0x05),
    TERRITORY_NOT_FOUND(0x06),
    CREATE_ACTION_FAILED(0x07),
    DELETE_ACTION_FAILED(0x08),
    AUTO_FORWARD_ADDRESS_CREATION_FAILED(0X09),
    TEAM_INBOX_LIMIT_EXCEEDED(0x10),
    INVALID_VERIFICATION_STATE(0x11),
    INVALID_VISIBILITY_STATUS(0x12),
    USERS_NOT_FOUND(0x13),
    INVALID_USER_TYPE(0x14),
    VISIBILITY_STATUS_REQUIRED(0x15),
    TEAM_INBOX_MUST_HAVE_ATLEAST_ONE_USER_OR_TEAM_OR_TERRITORY(0x16),
    ACCESS_DETAILS_REQUIRED(0x17),
    ;

    private final int code;

    TeamInboxErrorCodes(final int localCode) {
        this.code = TeamInboxErrorCodeGroup.GROUP.getConvertedCode(localCode);
    }

    @Override
    public int getCode() {
        return code;
    }

    private static class TeamInboxErrorCodeGroup implements ErrorCodeGroup {
        private static final ErrorCodeGroup GROUP = new TeamInboxErrorCodeGroup();

        @Override
        public int getPrefix() {
            return 0x20_0_0001;
        }
    }
}