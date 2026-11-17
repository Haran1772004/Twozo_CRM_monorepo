package com.tlc.crm.notifications.internal.handler.impl;

import com.tlc.commons.service.registry.auto.service.AutoServiceRegistrar;
import com.tlc.crm.core.features.api.activity.model.ActivityHolder;
import com.tlc.crm.notification.app.AppNotificationEventType;
import com.tlc.crm.notification.app.CrmModelType;
import com.tlc.crm.notification.app.Notifiable;
import com.tlc.crm.notification.app.model.AssignmentNotificationSettings;
import com.tlc.crm.notifications.internal.handler.registry.NotifiableModelHandlerRegistry;

import java.util.Arrays;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * Handles {@link ActivityHolder} for notification/alert purposes.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
@AutoServiceRegistrar(registryClazz = NotifiableModelHandlerRegistry.class)
public final class NotifiableActivityHandler extends AbstractNotifiableModelHandler<ActivityHolder> {

    private NotifiableActivityHandler() {
    }

    private static class Instance {
        private static final NotifiableActivityHandler INSTANCE = new NotifiableActivityHandler();
    }

    /**
     * <p>
     * Gets {@link NotifiableActivityHandler}.
     * </p>
     *
     * @return {@link NotifiableActivityHandler}.
     */
    public static NotifiableActivityHandler getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Handles {@link ActivityHolder}.
     * </p>
     *
     * @param orgId      Represents an organization id
     * @param activities Collection {@link ActivityHolder}
     */
    @Override
    public void handle(final Long orgId, final Collection<ActivityHolder> activities) {
        final Collection<AppNotificationEventType> notificationEventTypes = new ArrayList<>(Arrays.asList(
                ActivityHolder.class.getAnnotation(Notifiable.class).types()));
        final Optional<ActivityHolder> optionalActivityHolder = activities.stream().findFirst();

        if (optionalActivityHolder.isPresent()) {
            final ActivityHolder activityHolder = optionalActivityHolder.get();
            final Long assigneeId = activityHolder.getActivity().getAssigneeId();

            if (Objects.nonNull(assigneeId)) {
                final Optional<AssignmentNotificationSettings> settings =
                        notificationSettingsManager.retrieveAssignmentNotificationSettings(orgId, assigneeId);

                if (settings.isPresent() && !settings.get().isActivityAssigned()) {
                    notificationEventTypes.remove(AppNotificationEventType.NEW_ACTIVITY);
                }
            }
            load(orgId, activities, notificationEventTypes);
        }
    }

    /**
     * <p>
     * Gets {@link CrmModelType}.
     * </p>
     *
     * @return {@link CrmModelType}.
     */
    @Override
    public CrmModelType getType() {
        return CrmModelType.ACTIVITY;
    }
}