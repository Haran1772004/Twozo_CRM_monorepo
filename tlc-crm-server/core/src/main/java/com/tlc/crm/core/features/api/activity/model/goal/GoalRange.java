package com.tlc.crm.core.features.api.activity.model.goal;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.entity.internal.status.ActivityGoalErrorCodes;

/**
 * <p>
 * An enum contains the various {@link GoalRange} related to {@link ActivityGoal}.
 * </p>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public enum GoalRange {

    THIS_WEEK((short) 1),
    NEXT_WEEK((short) 2),
    ALL_WEEK_THIS_MONTH((short) 3),
    ALL_WEEK_THIS_QUARTER((short) 4),
    ALL_WEEK_THIS_YEAR((short) 5),
    WEEK_WITH_CUSTOM_RANGE((short) 6),
    THIS_MONTH((short) 7),
    NEXT_MONTH((short) 8),
    ALL_MONTH_THIS_QUARTER((short) 9),
    ALL_MONTH_THIS_YEAR((short) 10),
    MONTH_WITH_CUSTOM_RANGE((short) 11),
    THIS_QUARTER((short) 12),
    NEXT_QUARTER((short) 13),
    ALL_QUARTER_THIS_YEAR((short) 14),
    QUARTER_WITH_CUSTOM_RANGE((short) 15),
    THIS_YEAR((short) 16),
    NEXT_YEAR((short) 17),
    YEAR_WITH_CUSTOM_RANGE((short) 18),
    ;

    private final short id;

    GoalRange(final short id) {
        this.id = id;
    }

    public short getId() {
        return id;
    }

    /**
     * <p>
     * Gets the {@link GoalRange} by ID
     * </p>
     *
     * @param id Represents the id of the {@link GoalRange}
     * @return The {@link GoalRange}
     */
    public static GoalRange getGoalRangeById(final short id) {
        return switch (id) {
            case 1 -> GoalRange.THIS_WEEK;
            case 2 -> GoalRange.NEXT_WEEK;
            case 3 -> GoalRange.ALL_WEEK_THIS_MONTH;
            case 4 -> GoalRange.ALL_WEEK_THIS_QUARTER;
            case 5 -> GoalRange.ALL_WEEK_THIS_YEAR;
            case 6 -> GoalRange.WEEK_WITH_CUSTOM_RANGE;
            case 7 -> GoalRange.THIS_MONTH;
            case 8 -> GoalRange.NEXT_MONTH;
            case 9 -> GoalRange.ALL_MONTH_THIS_QUARTER;
            case 10 -> GoalRange.ALL_MONTH_THIS_YEAR;
            case 11 -> GoalRange.MONTH_WITH_CUSTOM_RANGE;
            case 12 -> GoalRange.THIS_QUARTER;
            case 13 -> GoalRange.NEXT_QUARTER;
            case 14 -> GoalRange.ALL_QUARTER_THIS_YEAR;
            case 15 -> GoalRange.QUARTER_WITH_CUSTOM_RANGE;
            case 16 -> GoalRange.THIS_YEAR;
            case 17 -> GoalRange.NEXT_YEAR;
            case 18 -> GoalRange.YEAR_WITH_CUSTOM_RANGE;
            default -> throw ErrorCode.get(ActivityGoalErrorCodes.GOAL_RANGE_NOT_FOUND, "i18n.activity.goal.goalRange.not.found");
        };
    }
}