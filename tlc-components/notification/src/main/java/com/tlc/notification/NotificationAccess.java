package com.tlc.notification;

import com.tlc.commons.service.registry.ServiceRegistry;
import com.tlc.commons.util.ConfHolder;

import com.tlc.commons.util.Configurations;

import com.tlc.commons.internal.template.service.VelocityTemplateServiceRegistry;
import com.tlc.commons.internal.template.service.VelocityTemplateService;

import com.tlc.commons.listeners.TemplateListener;

import com.tlc.notification.service.NotificationService;
import com.tlc.notification.internal.impl.NotificationsServiceImpl;
import com.tlc.notification.internal.registry.NotificationsServiceRegistry;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * <p>
 * Activates the bundle and provides the notification access.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class NotificationAccess implements BundleActivator {

    private final ServiceRegistry<NotificationService> serviceRegistry = NotificationsServiceRegistry.getInstance();
    private final ServiceRegistry<VelocityTemplateService> templateServiceRegistry;

    public NotificationAccess() {
        templateServiceRegistry = VelocityTemplateServiceRegistry.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(final BundleContext context) {
        final NotificationsServiceImpl notificationsService = new NotificationsServiceImpl();
        final Configurations configurations = ConfHolder.getInstance().getProperties("notification");
        final VelocityTemplateService velocityTemplateService = new VelocityTemplateService();

        notificationsService.start(configurations);
        velocityTemplateService.start(null);

        serviceRegistry.register(notificationsService);
        templateServiceRegistry.register(velocityTemplateService);
        context.addBundleListener(new TemplateListener());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop(final BundleContext context) {
        final NotificationsServiceImpl service = (NotificationsServiceImpl) serviceRegistry.unregister();
        final VelocityTemplateService velocityTemplateService = templateServiceRegistry.unregister();

        service.stop();
        velocityTemplateService.stop();
        context.removeBundleListener(new TemplateListener());
    }
}