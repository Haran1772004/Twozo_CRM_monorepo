package com.tlc.crm.core.features.internal.followers.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * <p>
 * Represents error codes related to user followers
 * </p>
 *
 * @author Ryan N
 * @version 1.0
 */
public enum UserFollowerErrorCode implements ErrorCodeProvider {

    USER_FOLLOWER_LIMIT_REACHED(0x01);

    private final int code;

    UserFollowerErrorCode(final int localCode) {
        this.code = UserFollowerErrorCode.UserFollowerErrorCodeGroup.GROUP.getConvertedCode(localCode);
    }

    @Override
    public int getCode() {
        return code;
    }

    private static class UserFollowerErrorCodeGroup implements ErrorCodeGroup {
        private static final ErrorCodeGroup GROUP = new UserFollowerErrorCode.UserFollowerErrorCodeGroup();

        @Override
        public int getPrefix() {
            return 0x25_0_0008;
        }
    }
}
