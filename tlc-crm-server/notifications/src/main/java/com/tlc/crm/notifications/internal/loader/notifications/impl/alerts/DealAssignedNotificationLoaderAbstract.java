package com.tlc.crm.notifications.internal.loader.notifications.impl.alerts;

import com.tlc.commons.json.JsonObject;
import com.tlc.commons.service.registry.auto.service.AutoServiceRegistrar;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.notification.app.AppNotificationEventType;
import com.tlc.crm.notifications.api.model.alert.NotificationAlert;
import com.tlc.crm.notifications.internal.loader.registry.NotificationAlertLoaderRegistry;

import java.util.Collection;

/**
 * <p>
 * Loads deal assigned notification into DB.
 * </p>
 *
 * @author NandiniRakAS.
 * @version 1.0.
 */
@AutoServiceRegistrar(registryClazz = NotificationAlertLoaderRegistry.class)
public final class DealAssignedNotificationLoaderAbstract extends AbstractEntityAssignedNotificationLoader<Deal> {

    private DealAssignedNotificationLoaderAbstract() {
    }

    private static class Instance {
        private static final DealAssignedNotificationLoaderAbstract INSTANCE = new DealAssignedNotificationLoaderAbstract();
    }

    /**
     * <p>
     * Gets {@link DealAssignedNotificationLoaderAbstract}.
     * </p>
     *
     * @return {@link DealAssignedNotificationLoaderAbstract}.
     */
    public static DealAssignedNotificationLoaderAbstract getInstance() {
        return Instance.INSTANCE;
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
        return AppNotificationEventType.DEAL_ASSIGNED;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents an organization id
     * @param deals {@link Deal} collection.
     */
    @Override
    protected JsonObject buildEntityAssignedJson(final Long orgId, final Collection<Deal> deals) {
        return appNotificationConverter.convertDealsToJson(deals);
    }

    /**
     * <p>
     * Gets {@link NotificationAlert.Category}.
     * </p>
     *
     * @return {@link NotificationAlert.Category}.
     */
    @Override
    protected NotificationAlert.Category getCategory() {
        return NotificationAlert.Category.DEAL_ASSIGNED;
    }
}
