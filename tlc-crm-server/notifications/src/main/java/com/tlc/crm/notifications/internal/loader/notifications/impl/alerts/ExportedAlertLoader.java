package com.tlc.crm.notifications.internal.loader.notifications.impl.alerts;

import com.tlc.commons.service.registry.auto.service.AutoServiceRegistrar;
import com.tlc.crm.common.export.model.AppExportAlertRequest;
import com.tlc.crm.notification.app.AppNotificationEventType;
import com.tlc.crm.notifications.api.model.alert.NotificationAlert;
import com.tlc.crm.notifications.internal.loader.registry.NotificationAlertLoaderRegistry;

import java.util.Collection;

/**
 * <p>
 * Loads export completed alert into DB.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
@AutoServiceRegistrar(registryClazz = NotificationAlertLoaderRegistry.class)
public final class ExportedAlertLoader extends AbstractAppNotificationAlertLoader<AppExportAlertRequest> {

    private ExportedAlertLoader() {
    }

    private static class Instance {
        private static final ExportedAlertLoader INSTANCE = new ExportedAlertLoader();
    }

    /**
     * <p>
     * Gets {@link ExportedAlertLoader}.
     * </p>
     *
     * @return {@link ExportedAlertLoader}.
     */
    public static ExportedAlertLoader getInstance() {
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
            notificationAlertManager.storeExportAlert(notificationAlertConverter.convertExportAlert(
                    orgId, exportAlert, NotificationAlert.Category.EXPORTED));
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
        return AppNotificationEventType.EXPORTED;
    }
}
