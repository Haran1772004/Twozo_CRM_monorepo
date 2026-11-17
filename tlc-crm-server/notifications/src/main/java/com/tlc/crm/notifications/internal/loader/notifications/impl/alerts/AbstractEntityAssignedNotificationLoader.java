package com.tlc.crm.notifications.internal.loader.notifications.impl.alerts;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.entity.models.Entity;
import com.tlc.crm.notification.app.model.AssignmentNotificationSettings;
import com.tlc.crm.notifications.api.model.alert.NotificationAlert;
import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.validator.TlcModel;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * Loads entity assigned notification alerts into DB.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public abstract class AbstractEntityAssignedNotificationLoader<T extends Entity> extends AbstractAppNotificationAlertLoader<T> {

    protected AbstractEntityAssignedNotificationLoader() {
    }

    /**
     * <p>
     * Builds entity assigned json.
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param entities {@link Entity} collection.
     */
    protected abstract JsonObject buildEntityAssignedJson(final Long orgId, final Collection<T> entities);

    /**
     * <p>
     * Gets {@link NotificationAlert.Category}.
     * </p>
     *
     * @return {@link NotificationAlert.Category}.
     */
    protected abstract NotificationAlert.Category getCategory();

    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents an organization id
     * @param entities {@link Entity} collection.
     */
    @Override
    public void load(final long orgId, final Collection<T> entities) {
        final Long userId = LoggedInUserInfo.getUser().id();
        final Collection<T> notifiableEntities = entities.stream().filter(Entity::notifiable).toList();

        if (!notifiableEntities.isEmpty()) {
            final Optional<Long> salesOwnerId = notifiableEntities.stream().findFirst().map(Entity::getSalesOwner).map(TlcModel::id);

            if (salesOwnerId.isPresent() && !Objects.equals(userId, salesOwnerId.get())) {
                final Long ownerId = salesOwnerId.get();
                final Optional<AssignmentNotificationSettings> settings =
                        notificationSettingsManager.retrieveAssignmentNotificationSettings(orgId, ownerId);

                if (settings.isPresent() && settings.get().isSourceAssigned() && !Objects.equals(userId, ownerId)) {
                    notificationAlertManager.storeEntityAssignmentAlert(notificationAlertConverter.convertEntityAssignmentAlert(
                            orgId, ownerId, buildEntityAssignedJson(orgId, notifiableEntities), getCategory()));
                }
            }
        }
    }
}