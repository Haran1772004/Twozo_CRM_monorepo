package com.tlc.crm.notifications.internal.loader.notifications.impl.notifications;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.service.registry.auto.service.AutoServiceRegistrar;
import com.tlc.crm.common.export.model.AppExportAlertRequest;
import com.tlc.crm.notification.app.AppNotificationEventType;
import com.tlc.crm.notifications.fields.NotificationAttributes;
import com.tlc.crm.notifications.internal.loader.registry.NotificationLoaderRegistry;

import java.util.Collection;
import java.util.List;

/**
 * <p>
 * Loads export in-progress notification into DB.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
@AutoServiceRegistrar(registryClazz = NotificationLoaderRegistry.class)
public final class ExportingNotificationLoader extends AbstractNotificationLoader<AppExportAlertRequest> {

    private ExportingNotificationLoader() {
    }

    private static class Instance {
        private static final ExportingNotificationLoader INSTANCE = new ExportingNotificationLoader();
    }

    /**
     * <p>
     * Gets {@link ExportingNotificationLoader}.
     * </p>
     *
     * @return {@link ExportingNotificationLoader}.
     */
    public static ExportingNotificationLoader getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId         Represents an organization id.
     * @param exportAlerts  Collection of {@link AppExportAlertRequest}.
     */
    @Override
    public void load(final long orgId, final Collection<AppExportAlertRequest> exportAlerts) {
        for (final AppExportAlertRequest exportAlert : exportAlerts) {
            final JsonObject notificationJson = Json.object();

            notificationJson.put(NotificationAttributes.ID, exportAlert.id());
            notificationJson.put(NotificationAttributes.EXPORT_NAME, exportAlert.getExportName());
            saveNotification(orgId, getSender(orgId, exportAlert.getUserId()),
                    getReceivers(orgId, List.of(exportAlert.getUserId())), notificationJson);
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
        return AppNotificationEventType.EXPORTING;
    }
}
