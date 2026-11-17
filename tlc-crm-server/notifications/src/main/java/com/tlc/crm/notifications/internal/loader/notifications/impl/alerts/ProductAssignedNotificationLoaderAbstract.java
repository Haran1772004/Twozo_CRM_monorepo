package com.tlc.crm.notifications.internal.loader.notifications.impl.alerts;

import com.tlc.commons.json.JsonObject;
import com.tlc.commons.service.registry.auto.service.AutoServiceRegistrar;
import com.tlc.crm.core.api.product.model.Product;
import com.tlc.crm.notification.app.AppNotificationEventType;
import com.tlc.crm.notifications.api.model.alert.NotificationAlert;
import com.tlc.crm.notifications.internal.loader.registry.NotificationAlertLoaderRegistry;

import java.util.Collection;

/**
 * <p>
 * Loads product assigned notification into DB.
 * </p>
 *
 * @author NandiniRakAS.
 * @version 1.0.
 */
@AutoServiceRegistrar(registryClazz = NotificationAlertLoaderRegistry.class)
public final class ProductAssignedNotificationLoaderAbstract extends AbstractEntityAssignedNotificationLoader<Product> {

    private ProductAssignedNotificationLoaderAbstract() {
    }

    private static class Instance {
        private static final ProductAssignedNotificationLoaderAbstract INSTANCE = new ProductAssignedNotificationLoaderAbstract();
    }

    /**
     * <p>
     * Gets {@link ProductAssignedNotificationLoaderAbstract}.
     * </p>
     *
     * @return {@link ProductAssignedNotificationLoaderAbstract}.
     */
    public static ProductAssignedNotificationLoaderAbstract getInstance() {
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
        return AppNotificationEventType.PRODUCT_ASSIGNED;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents an organization id
     * @param products {@link Product} collection.
     */
    @Override
    protected JsonObject buildEntityAssignedJson(final Long orgId, final Collection<Product> products) {
        return appNotificationConverter.convertProductsToJson(products);
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
        return NotificationAlert.Category.PRODUCT_ASSIGNED;
    }
}