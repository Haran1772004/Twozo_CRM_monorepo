package com.tlc.crm.core.features.api.activity.model.goal;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.entity.internal.status.ActivityGoalErrorCodes;

import java.util.Collection;
import java.util.List;

/**
 * <p>
 * An enum contains the various {@link GoalPeriod} related to {@link ActivityGoal}.
 * </p>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public enum GoalPeriod {

    WEEKLY((short) 1, "i18n.activity.goal.period.weekly", List.of(GoalRange.THIS_WEEK, GoalRange.NEXT_WEEK, GoalRange.ALL_WEEK_THIS_MONTH, GoalRange.ALL_WEEK_THIS_QUARTER, GoalRange.ALL_WEEK_THIS_YEAR, GoalRange.WEEK_WITH_CUSTOM_RANGE)),
    MONTHLY((short) 2, "i18n.activity.goal.period.monthly", List.of(GoalRange.THIS_MONTH, GoalRange.NEXT_MONTH, GoalRange.ALL_MONTH_THIS_QUARTER, GoalRange.ALL_MONTH_THIS_YEAR, GoalRange.MONTH_WITH_CUSTOM_RANGE)),
    QUARTERLY((short) 3, "i18n.activity.goal.period.quarterly", List.of(GoalRange.THIS_QUARTER, GoalRange.NEXT_QUARTER, GoalRange.ALL_QUARTER_THIS_YEAR, GoalRange.QUARTER_WITH_CUSTOM_RANGE)),
    YEARLY((short) 4, "i18n.activity.goal.period.yearly", List.of(GoalRange.THIS_YEAR, GoalRange.NEXT_YEAR, GoalRange.YEAR_WITH_CUSTOM_RANGE))
    ;

    private final short id;
    private final String name;
    private final Collection<GoalRange> ranges;

    GoalPeriod(final short id, final String name, final Collection<GoalRange> ranges) {
        this.id = id;
        this.name = name;
        this.ranges = ranges;
    }

    public short getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Collection<GoalRange> getRanges() {
        return ranges;
    }

    /**
     * <p>
     * Gets the {@link GoalPeriod} by ID
     * </p>
     *
     * @param id Represents the Id of the {@link GoalPeriod}
     * @return The {@link GoalPeriod}
     */
    public static GoalPeriod getGoalPeriodById(final short id) {
        return switch (id) {
            case 1 -> GoalPeriod.WEEKLY;
            case 2 -> GoalPeriod.MONTHLY;
            case 3 -> GoalPeriod.QUARTERLY;
            case 4 -> GoalPeriod.YEARLY;
            default -> throw ErrorCode.get(ActivityGoalErrorCodes.GOAL_PERIOD_NOT_FOUND, "i18n.activity.goal.goalPeriod.not.found");
        };
    }
}