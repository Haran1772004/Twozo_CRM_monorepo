package com.tlc.crm.notifications.internal.loader.notifications.impl.notifications;

import com.tlc.commons.service.registry.auto.service.AutoServiceRegistrar;
import com.tlc.crm.email.notification.email.tracking.model.EmailTrackingDetail;
import com.tlc.crm.notification.app.AppNotificationEventType;
import com.tlc.crm.notifications.internal.loader.registry.NotificationLoaderRegistry;

import java.util.Objects;

/**
 * <p>
 * Handles the {@link EmailClickedNotificationLoader}
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
@AutoServiceRegistrar(registryClazz = NotificationLoaderRegistry.class)
public final class EmailClickedNotificationLoader extends AbstractNotificationTrackerLoader {

    private EmailClickedNotificationLoader() {
    }

    private static class Instance {
        private static final EmailClickedNotificationLoader INSTANCE = new EmailClickedNotificationLoader();
    }

    /**
     * <p>
     * Gets {@link EmailClickedNotificationLoader}.
     * </p>
     *
     * @return {@link EmailClickedNotificationLoader}.
     */
    public static EmailClickedNotificationLoader getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Whether {@link EmailTrackingDetail.Type} is of clicked.
     * </p>
     *
     * @param type {@link EmailTrackingDetail.Type}.
     * @return whether {@link EmailTrackingDetail.Type} is of clicked.
     */
    @Override
    protected boolean isOfSameType(final EmailTrackingDetail.Type type) {
        return Objects.equals(EmailTrackingDetail.Type.CLICKED, type);
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
        return AppNotificationEventType.EMAIL_CLICKED;
    }
}