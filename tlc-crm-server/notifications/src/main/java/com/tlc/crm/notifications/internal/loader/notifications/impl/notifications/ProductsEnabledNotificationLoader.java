package com.tlc.crm.notifications.internal.loader.notifications.impl.notifications;

import com.tlc.commons.json.Json;
import com.tlc.commons.service.registry.auto.service.AutoServiceRegistrar;
import com.tlc.crm.fields.models.settings.SourceSettings;
import com.tlc.crm.notification.app.AppNotificationEventType;
import com.tlc.crm.notifications.internal.loader.registry.NotificationLoaderRegistry;

import java.util.Collection;
import java.util.List;

/**
 * <p>
 * Provides the service for the products module enabled notification type
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
@AutoServiceRegistrar(registryClazz = NotificationLoaderRegistry.class)
public final class ProductsEnabledNotificationLoader extends AbstractNotificationLoader<SourceSettings> {

    private ProductsEnabledNotificationLoader() {
    }

    private static class Instance {
        private static final ProductsEnabledNotificationLoader INSTANCE = new ProductsEnabledNotificationLoader();
    }

    /**
     * <p>
     * Gets {@link ProductsEnabledNotificationLoader}.
     * </p>
     *
     * @return {@link ProductsEnabledNotificationLoader}.
     */
    public static ProductsEnabledNotificationLoader getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId      Represents an organization id.
     * @param settings Collection {@link SourceSettings}.
     */
    @Override
    public void load(final long orgId, final Collection<SourceSettings> settings) {
        final long userId = settings.stream().findFirst().get().getUserId();

        saveNotification(orgId, getSender(orgId, userId), getReceivers(orgId, List.of(userId)), Json.object());
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
        return AppNotificationEventType.PRODUCTS_MODULE_ENABLED;
    }
}
