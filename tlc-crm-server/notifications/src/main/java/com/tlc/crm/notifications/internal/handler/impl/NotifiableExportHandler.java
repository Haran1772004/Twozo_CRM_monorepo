package com.tlc.crm.notifications.internal.handler.impl;

import com.tlc.commons.service.registry.auto.service.AutoServiceRegistrar;
import com.tlc.crm.common.export.model.AppExportAlertRequest;
import com.tlc.crm.notification.app.AppNotificationEventType;
import com.tlc.crm.notification.app.CrmModelType;
import com.tlc.crm.notifications.internal.handler.registry.NotifiableModelHandlerRegistry;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * Handles {@link AppExportAlertRequest} for notification/alert purposes.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
@AutoServiceRegistrar(registryClazz = NotifiableModelHandlerRegistry.class)
public final class NotifiableExportHandler extends AbstractNotifiableModelHandler<AppExportAlertRequest> {

    private NotifiableExportHandler() {
    }

    private static class Instance {
        private static final NotifiableExportHandler INSTANCE = new NotifiableExportHandler();
    }

    /**
     * <p>
     * Gets {@link NotifiableExportHandler}.
     * </p>
     *
     * @return {@link NotifiableExportHandler}.
     */
    public static NotifiableExportHandler getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Handles {@link AppExportAlertRequest}.
     * </p>
     *
     * @param orgId         Represents an organization id.
     * @param exportAlerts  Collection of {@link AppExportAlertRequest}.
     */
    @Override
    public void handle(final Long orgId, final Collection<AppExportAlertRequest> exportAlerts) {
        final Optional<AppExportAlertRequest> optionalExportAlert = exportAlerts.stream().findFirst();

        optionalExportAlert.ifPresent(exportAlert -> {
            final AppNotificationEventType eventType = exportAlert.getEventType();

            if (Objects.nonNull(eventType)) {
                load(orgId, exportAlerts, List.of(eventType));
            }
        });
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
        return CrmModelType.EXPORT;
    }
}
