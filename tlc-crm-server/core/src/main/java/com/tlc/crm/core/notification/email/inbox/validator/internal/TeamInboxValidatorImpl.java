package com.tlc.crm.core.notification.email.inbox.validator.internal;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.Team;
import com.tlc.crm.common.iam.api.model.Territory;

import com.tlc.crm.core.notification.email.inbox.api.model.TeamInbox;
import com.tlc.crm.core.notification.email.inbox.validator.annotation.TeamInboxValidator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Collection;
import java.util.Objects;

/**
 * Validates the {@link TeamInbox} and returns appropriate message
 *
 * @author Ajith venkadesh
 * @version 1.0
 */
public class TeamInboxValidatorImpl implements ConstraintValidator<TeamInboxValidator, TeamInbox> {

    /**
     * Checks whether the {@link TeamInbox} have the valid values or not
     *
     * @param teamInbox Represents the entity of {@link TeamInbox}
     * @param context   Validation context
     * @return If the given entity is valid, it will return true
     */
    @Override
    public boolean isValid(final TeamInbox teamInbox, final ConstraintValidatorContext context) {
        final Collection<Team> teams = teamInbox.getTeams();
        final Collection<Territory> territories = teamInbox.getTerritories();
        final Collection<CrmUser> users = teamInbox.getUsers();

        if (TeamInbox.VisibilityStatus.DESIGNATED == teamInbox.getVisibilityStatus()) {
            return (Objects.nonNull(teams) && !teams.isEmpty()) || (Objects.nonNull(territories) && !territories.isEmpty())
                    || (Objects.nonNull(users) && !users.isEmpty());
        }

        return true;
    }
}
