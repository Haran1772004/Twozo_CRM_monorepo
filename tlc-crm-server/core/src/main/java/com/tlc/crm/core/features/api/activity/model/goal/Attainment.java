package com.tlc.crm.core.features.api.activity.model.goal;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.entity.internal.status.ActivityGoalErrorCodes;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * A Model which represents the Attainment details
 * </p>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
@Getter
@Setter
public final class Attainment {

    private long target;
    private long achieved;
    private int achievementRate;

    /**
     * <p>
     * Calculates the achievement rate
     * </p>
     *
     * @return The calculated achievement rate
     */
    public int calculateAchievementRate() {
        if (0 >= target) {
            throw ErrorCode.get(ActivityGoalErrorCodes.INVALID_TARGET_VALUE, "i18n.activity.goal.target.value.invalid");
        }

        return (int) ((achieved * 100) / target);
    }
}