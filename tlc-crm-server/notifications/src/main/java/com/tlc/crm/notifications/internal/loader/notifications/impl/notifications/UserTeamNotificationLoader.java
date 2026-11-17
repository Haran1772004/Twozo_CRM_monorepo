package com.tlc.crm.notifications.internal.loader.notifications.impl.notifications;

import com.tlc.commons.json.Json;
import com.tlc.commons.service.registry.auto.service.AutoServiceRegistrar;
import com.tlc.crm.common.iam.api.model.Team;
import com.tlc.crm.common.iam.api.model.TeamUser;
import com.tlc.crm.notifications.internal.loader.registry.NotificationLoaderRegistry;
import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.crm.notification.app.AppNotificationEventType;
import com.tlc.crm.notifications.fields.NotificationAttributes;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>
 * Provides the service for the user team notification type.
 * </p>
 *
 * @author NandiniRakAS.
 * @version 1.0.
 */
@AutoServiceRegistrar(registryClazz = NotificationLoaderRegistry.class)
public final class UserTeamNotificationLoader extends AbstractNotificationLoader<Team> {

    private UserTeamNotificationLoader() {
    }

    private static class Instance {
        private static final UserTeamNotificationLoader INSTANCE = new UserTeamNotificationLoader();
    }

    /**
     * <p>
     * Gets {@link UserTeamNotificationLoader}.
     * </p>
     *
     * @return {@link UserTeamNotificationLoader}.
     */
    public static UserTeamNotificationLoader getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents an organization id.
     * @param teams Collection {@link Team}.
     */
    @Override
    public void load(final long orgId, final Collection<Team> teams) {
        final Long userId = LoggedInUserInfo.getUser().id();

        for (final Team team : teams) {
            final Collection<Long> userIds = team.getUserIds();
            final Collection<TeamUser> teamUsers = team.getUsers();
            final Collection<Long> ids = new HashSet<>();

            if (Objects.nonNull(userIds)) {
                ids.addAll(userIds);
            } else if (Objects.nonNull(teamUsers)) {
                final Collection<Long> teamUserIds = teamUsers.stream().map(teamUser -> teamUser.getCrmUser().id()).collect(Collectors.toUnmodifiableSet());

                ids.addAll(teamUserIds);
            }
            final Collection<Long> filteredUserIds = ids.stream().filter(id -> !Objects.equals(userId, id)).toList();

            if (!filteredUserIds.isEmpty()) {
                saveNotification(orgId, getSender(orgId, userId), getReceivers(orgId, filteredUserIds),
                        Json.object().put(NotificationAttributes.TEAM, team.getName()));
            }
        }
    }

    /**
     * <p>
     * Gets {@link AppNotificationEventType}.
     * </p>
     *
     * @return {@link AppNotificationEventType}.
     */
    @Override
    public AppNotificationEventType getType() {
        return AppNotificationEventType.ADDED_TO_TEAM;
    }
}
