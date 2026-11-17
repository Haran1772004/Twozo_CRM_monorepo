package com.tlc.crm.core.features.internal.followers.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * <p>
 * Represents error codes related to followers
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public enum SourceFollowersErrorCode implements ErrorCodeProvider {

    ENTITY_NOT_FOUND(0x21),
    INVALID_SOURCE(0x22),
    CRM_USERS_NOT_FOUND(0x23),
    FOLLOWERS_RETRIEVAL_ACTION_FAILED(0x24),
    UNSUPPORTED_OPERATION(0x25),
    FIELD_IS_INVALID(0x26),
    ENTITIES_NOT_FOUND(0x27),
    DUPLICATE_FOLLOWER_FOUND(0x28)
    ;

    private final int code;

    SourceFollowersErrorCode(final int localCode) {
        this.code = SourceFollowerErrorCodeGroup.GROUP.getConvertedCode(localCode);
    }

    @Override
    public int getCode() {
        return code;
    }

    private static class SourceFollowerErrorCodeGroup implements ErrorCodeGroup {
        private static final ErrorCodeGroup GROUP = new SourceFollowerErrorCodeGroup();

        @Override
        public int getPrefix() {
            return 0x25_0_0008;
        }
    }
}
