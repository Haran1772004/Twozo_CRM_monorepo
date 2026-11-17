package com.tlc.crm.notifications.internal.loader.notifications.impl.notifications;

import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.service.registry.auto.service.AutoServiceRegistrar;
import com.tlc.crm.common.iam.api.model.CrmRole;
import com.tlc.crm.notifications.internal.loader.registry.NotificationLoaderRegistry;
import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.crm.common.iam.api.manager.RoleManager;
import com.tlc.crm.notification.app.AppNotificationEventType;
import com.tlc.crm.notifications.fields.NotificationAttributes;

import java.util.Collection;
import java.util.List;
import java.util.HashSet;
import java.util.Objects;

/**
 * <p>
 * Provides the service for the role changed notification type
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
@AutoServiceRegistrar(registryClazz = NotificationLoaderRegistry.class)
public final class UserRoleNotificationLoader extends AbstractNotificationLoader<CrmRole> {

    private final RoleManager roleManager;

    private UserRoleNotificationLoader() {
        roleManager = RoleManager.getInstance();
    }

    private static class Instance {
        private static final UserRoleNotificationLoader INSTANCE = new UserRoleNotificationLoader();
    }

    /**
     * <p>
     * Gets {@link UserRoleNotificationLoader}.
     * </p>
     *
     * @return {@link UserRoleNotificationLoader}.
     */
    public static UserRoleNotificationLoader getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents an organization id.
     * @param crmRoles Collection {@link CrmRole}.
     */
    @Override
    public void load(final long orgId, final Collection<CrmRole> crmRoles) {
        final Long userId = LoggedInUserInfo.getUser().id();
        final JsonArray roles = appNotificationConverter.convertRolesToJson(crmRoles);
        final int size = roles.size();

        for (int index = 0; index < size; index++) {
            final JsonObject roleJson = roles.getJsonObject(index);
            final JsonObject json = buildRoleJson(orgId, roleJson);

            if (!json.getString(NotificationAttributes.OLD_VALUE).equals(json.getString(NotificationAttributes.NEW_VALUE))) {
                final JsonArray userIds = roleJson.getJsonArray(NotificationAttributes.USER_IDS);
                final Collection<Long> ids = new HashSet<>();

                for (int userIndex = 0; userIndex < userIds.size(); userIndex++) {
                    ids.add(userIds.getLong(userIndex));
                }
                json.remove(NotificationAttributes.USER_IDS);
                final Collection<Long> filteredUserIds = ids.stream().filter(id -> !Objects.equals(userId, id)).toList();

                for (final Long filteredUserId : filteredUserIds) {
                    saveNotification(orgId, getSender(orgId, userId), getReceivers(orgId, List.of(filteredUserId)), json);
                }
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
        return AppNotificationEventType.ROLE_CHANGED;
    }

    /**
     * <p>
     * Builds the role notification json with new value
     * </p>
     *
     * @param orgId      Represents an organization id
     * @param jsonObject Represents the {@link JsonObject}
     * @return the {@link JsonObject}
     */
    private JsonObject buildRoleJson(final long orgId, final JsonObject jsonObject) {
        jsonObject.put(NotificationAttributes.NEW_VALUE, roleManager.get(orgId, jsonObject.getLong(NotificationAttributes.ID)).getName());

        return jsonObject;
    }
}
