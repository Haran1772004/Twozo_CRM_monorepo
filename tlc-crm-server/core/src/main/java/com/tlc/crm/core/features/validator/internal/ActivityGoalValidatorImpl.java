package com.tlc.crm.core.features.validator.internal;

import com.tlc.crm.core.features.api.activity.model.goal.ActivityGoal;
import com.tlc.crm.core.features.api.activity.model.goal.GoalRange;
import com.tlc.crm.core.features.api.activity.model.goal.GoalType;
import com.tlc.crm.core.features.api.activity.model.goal.TeamGoal;
import com.tlc.crm.core.features.api.activity.model.goal.TeamUserGoal;
import com.tlc.crm.core.features.api.activity.model.goal.UserGoal;
import com.tlc.crm.core.features.validator.annotation.ActivityGoalValidator;
import com.tlc.crm.entity.features.field.activity.ActivityGoalFields;
import com.tlc.crm.entity.validator.ValidatorContext;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * It validates {@link ActivityGoal} entity and returns appropriate message
 * </p>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public class ActivityGoalValidatorImpl implements ConstraintValidator<ActivityGoalValidator, ActivityGoal> {

    /**
     * {@inheritDoc}
     *
     * @param activityGoal which the {@link ActivityGoal} values has to be validated
     * @param context      Validation context
     * @return If the given entity is valid, it will return true
     */
    @Override
    public boolean isValid(final ActivityGoal activityGoal, final ConstraintValidatorContext context) {
        final ValidatorContext validatorContext = new ValidatorContext(context);
        final Collection<GoalRange> ranges = activityGoal.getGoalPeriod().getRanges();

        if (!ranges.contains(activityGoal.getGoalRange())) {
            addConstraintViolationMessage(validatorContext, "i18n.activity.goal.goalRange.unsupported", ActivityGoalFields.GOAL_RANGE);
        }
        final long target = activityGoal.getAttainment().getTarget();

        if (0 >= target) {
            addConstraintViolationMessage(validatorContext, "i18n.activity.goal.target.value.invalid", ActivityGoalFields.TARGET);
        }
        final GoalType goalType = activityGoal.getGoalType();
        final TeamGoal teamGoal = activityGoal.getTeamGoal();

        if (Objects.isNull(teamGoal)) {

            if (GoalType.TEAM_GOAL.getId() == goalType.getId()) {
                addConstraintViolationMessage(validatorContext, "i18n.activity.goal.goalType.unsupported", ActivityGoalFields.USER_GOAL);
            }
            final Collection<UserGoal> userGoals = activityGoal.getUserGoals();

            if (Objects.isNull(userGoals) || userGoals.isEmpty()) {
                addConstraintViolationMessage(validatorContext, "i18n.activity.goal.userGoals.invalid", ActivityGoalFields.USER_GOAL);
            }
        } else {

            if (GoalType.USER_GOAL.getId() == goalType.getId()) {
                addConstraintViolationMessage(validatorContext, "i18n.activity.goal.goalType.unsupported", ActivityGoalFields.TEAM_GOAL);
            }
            final Collection<TeamUserGoal> teamUserGoals = teamGoal.getTeamUserGoals();

            if (Objects.isNull(teamUserGoals) || teamUserGoals.isEmpty()) {
                addConstraintViolationMessage(validatorContext, "i18n.activity.goal.teamGoal.teamUserGoal.required", ActivityGoalFields.TEAM_GOAL);
            }
            long totalTarget = 0;

            for (final TeamUserGoal teamUserGoal : teamUserGoals) {
                final long quota = teamUserGoal.getAttainment().getTarget();
                final boolean isQuotaEnabled = teamUserGoal.isQuotaEnabled();
                totalTarget += quota;

                if ((isQuotaEnabled && quota <= 0)) {
                    addConstraintViolationMessage(validatorContext, "i18n.activity.goal.target.value.invalid", ActivityGoalFields.QUOTA);
                }

                if (!isQuotaEnabled && 0 != quota) {
                    addConstraintViolationMessage(validatorContext, "i18n.activity.goal.quota.value.invalid", ActivityGoalFields.QUOTA);
                }
            }

            if (target != totalTarget) {
                addConstraintViolationMessage(validatorContext, "i18n.activity.goal.team.target.invalid", ActivityGoalFields.TARGET);
            }
        }
        disableDefaultConstraintViolation(validatorContext);

        return !validatorContext.isErrorsFound();
    }

    /**
     * Adds the constraint violation message to the validation context.
     *
     * @param validatorContext Refers the validator context.
     * @param message          Violation message.
     */
    private void addConstraintViolationMessage(final ValidatorContext validatorContext, final String message, final String fieldName) {
        validatorContext.setErrorsFound(true);
        validatorContext.getContext().buildConstraintViolationWithTemplate(message).addPropertyNode(fieldName).addConstraintViolation();
    }

    /**
     * <p>
     * Disables the default constraint violation of the {@link ValidatorContext}
     * </p>
     *
     * @param validatorContext Represents {@link ValidatorContext}
     */
    private void disableDefaultConstraintViolation(final ValidatorContext validatorContext) {
        if (validatorContext.isErrorsFound()) {
            validatorContext.getContext().disableDefaultConstraintViolation();
        }
    }
}