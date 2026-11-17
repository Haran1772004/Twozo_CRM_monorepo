package com.tlc.crm.entity.internal.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * Provides the error codes of an activity goal
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public enum ActivityGoalErrorCodes implements ErrorCodeProvider {

    GOAL_PERIOD_NOT_FOUND(0x1),
    GOAL_RANGE_NOT_FOUND(0x2),
    GOAL_TYPE_NOT_FOUND(0x3),
    USER_NOT_FOUND(0x4),
    USER_IS_NOT_PART_OF_TEAM(0x5),
    TEAM_NOT_FOUND(0x6),
    ACTIVITY_TYPE_NOT_FOUND(0x7),
    ACTIVITY_GOAL_NOT_FOUND(0x8),
    INVALID_TARGET_VALUE(0x9),
    ;

    private final int code;

    ActivityGoalErrorCodes(final int localCode) {
        this.code = ActivityGoalErrorCodeGroup.GROUP.getConvertedCode(localCode);
    }

    @Override
    public int getCode() {
        return code;
    }

    private static class ActivityGoalErrorCodeGroup implements ErrorCodeGroup {
        private static final ErrorCodeGroup GROUP = new ActivityGoalErrorCodeGroup();

        @Override
        public int getPrefix() {
            return 0x25_0_0001;
        }
    }
}