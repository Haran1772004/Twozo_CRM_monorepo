package com.tlc.crm.notifications.internal.loader.notifications.impl.alerts;

import com.tlc.commons.json.JsonObject;
import com.tlc.commons.service.registry.auto.service.AutoServiceRegistrar;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.notification.app.AppNotificationEventType;
import com.tlc.crm.notifications.api.model.alert.NotificationAlert;
import com.tlc.crm.notifications.internal.loader.registry.NotificationAlertLoaderRegistry;

import java.util.Collection;

/**
 * <p>
 * Loads company assigned notification into DB.
 * </p>
 *
 * @author NandiniRakAS.
 * @version 1.0.
 */
@AutoServiceRegistrar(registryClazz = NotificationAlertLoaderRegistry.class)
public final class CompanyAssignedNotificationLoaderAbstract extends AbstractEntityAssignedNotificationLoader<Company> {

    private CompanyAssignedNotificationLoaderAbstract() {
    }

    private static class Instance {
        private static final CompanyAssignedNotificationLoaderAbstract INSTANCE = new CompanyAssignedNotificationLoaderAbstract();
    }

    /**
     * <p>
     * Gets {@link CompanyAssignedNotificationLoaderAbstract}.
     * </p>
     *
     * @return {@link CompanyAssignedNotificationLoaderAbstract}.
     */
    public static CompanyAssignedNotificationLoaderAbstract getInstance() {
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
        return AppNotificationEventType.COMPANY_ASSIGNED;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId      Represents an organization id
     * @param companies {@link Company} collection.
     */
    @Override
    protected JsonObject buildEntityAssignedJson(final Long orgId, final Collection<Company> companies) {
        return appNotificationConverter.convertCompaniesToJson(companies);
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
        return NotificationAlert.Category.COMPANY_ASSIGNED;
    }
}