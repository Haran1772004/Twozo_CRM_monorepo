package com.tlc.crm.core.features.api.activity.model.goal;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.entity.internal.status.ActivityGoalErrorCodes;

/**
 * <p>
 * An enum contains the various {@link GoalType} related to {@link ActivityGoal}.
 * </p>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public enum GoalType {

    USER_GOAL((short) 1, "i18n.activity.goal.user.goal"),
    TEAM_GOAL((short) 2, "i18n.activity.goal.team.goal"),
    PART_OF_TEAM_GOAL((short) 3, "i18n.activity.goal.part.of.team.goal"),
    ;

    private final short id;
    private final String name;

    GoalType(final short id, final String name) {
        this.id = id;
        this.name = name;
    }

    public short getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    /**
     * <p>
     * Gets the {@link GoalType} by ID
     * </p>
     *
     * @param id Represents the id of the {@link GoalType}
     * @return The {@link GoalType}
     */
    public static GoalType getGoalTypeById(final short id) {
        return switch (id) {
            case 1 -> GoalType.USER_GOAL;
            case 2 -> GoalType.TEAM_GOAL;
            default -> throw ErrorCode.get(ActivityGoalErrorCodes.GOAL_TYPE_NOT_FOUND, "i18n.activity.goal.goalType.not.found");
        };
    }
}