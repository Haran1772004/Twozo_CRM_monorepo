package com.tlc.components.starter.initializer.components;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.service.Service;
import com.tlc.commons.service.registry.ServiceRegistry;
import com.tlc.commons.util.Configurations;
import com.tlc.components.starter.fields.ComponentsStarterFields;
import com.tlc.components.starter.initializer.AbstractModuleInitializer;
import com.tlc.components.starter.initializer.Modules;

import com.tlc.notification.internal.status.NotificationErrorCodes;
import com.tlc.notification.internal.template.mail.model.EmailTemplateRegistrarData;
import com.tlc.notification.internal.template.mail.registry.EmailTemplateServiceRegistry;
import com.tlc.notification.internal.template.mail.registrar.EmailTemplateResourceRegistrar;
import com.tlc.notification.internal.template.mail.service.VelocityEmailTemplateService;
import com.tlc.notification.service.NotificationService;
import com.tlc.notification.internal.impl.NotificationsServiceImpl;
import com.tlc.notification.internal.registry.NotificationsServiceRegistry;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

/**
 * Responsible for triggering the initialization process of the notification services.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@Component
final class NotificationInitializer extends AbstractModuleInitializer {

    private static final String RESOURCES_PATH = "/resources/";
    private static final String TEMPLATE_FILES_BASE_PATH = String.format("%s%s", RESOURCES_PATH, "templates/mail/");
    private static final String VM_FILE_EXTENSION = "*.vm";

    private final ServiceRegistry<NotificationService> serviceRegistry;
    private final ServiceRegistry<VelocityEmailTemplateService> templateServiceRegistry;

    private NotificationInitializer() {
        this.serviceRegistry = NotificationsServiceRegistry.getInstance();
        this.templateServiceRegistry = EmailTemplateServiceRegistry.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isServiceEnabled() {
        return isServiceEnabled(ComponentsStarterFields.NOTIFICATION) && isServiceEnabled(ComponentsStarterFields.EMAIL_APP);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ServiceRegistry<? extends Service> getServiceRegistry() {
        return serviceRegistry;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize() {
        if (isServiceEnabled()) {
            final NotificationsServiceImpl notificationsService = new NotificationsServiceImpl();
            final Configurations input = getProperties(ComponentsStarterFields.NOTIFICATION);
            final VelocityEmailTemplateService velocityTemplateService = new VelocityEmailTemplateService();

            notificationsService.start(input);
            velocityTemplateService.start(null);

            serviceRegistry.register(notificationsService);
            templateServiceRegistry.register(velocityTemplateService);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postInitialize() {
        if (isServiceEnabled()) {
            final EmailTemplateResourceRegistrar emailTemplateResourceRegistrar = EmailTemplateResourceRegistrar.getInstance();
            final Map<Modules, Collection<Resource>> templateResources = fetchMailTemplateResources();

            for (final Map.Entry<Modules, Collection<Resource>> moduleResource : templateResources.entrySet()) {
                final Modules module = moduleResource.getKey();
                final EmailTemplateRegistrarData emailTemplateRegistrarData = new EmailTemplateRegistrarData();

                emailTemplateRegistrarData.setModuleId(module.getId());
                emailTemplateRegistrarData.setModuleName(module.getName());
                emailTemplateRegistrarData.setTemplateUrls(getTemplateUrls(moduleResource.getValue()));
                emailTemplateResourceRegistrar.register(emailTemplateRegistrarData);
            }
        }
    }

    /**
     * <p>
     * Fetches template resources by scanning the base path for template file and segregating them by module
     * </p>
     *
     * @return A {@link Map} containing the key is the {@link Modules} and the value is a collection of {@link Resource}
     */
    private Map<Modules, Collection<Resource>> fetchMailTemplateResources() {

        try {
            final Collection<Resource> resources = resourceScanner.scanResources(TEMPLATE_FILES_BASE_PATH, VM_FILE_EXTENSION);

            return segregateResourcesByModule(resources);
        } catch (final IOException exception) {
            throw ErrorCode.get(NotificationErrorCodes.RESOURCE_SCAN_FAILED);
        }
    }

    /**
     * <p>
     * Fetches a collection of templates urls from the collection of {@link Resource}
     * </p>
     *
     * @param resources Which contains the {@link Resource}
     * @return A {@link Collection} of template URL paths, derived from the resources
     */
    private Collection<String> getTemplateUrls(final Collection<Resource> resources) {
        final Collection<String> urls = new HashSet<>();

        for (final Resource resource : resources) {
            final String urlPath;

            try {
                urlPath = resource.getURL().getPath();
            } catch (final IOException ioException) {
                throw ErrorCode.get(NotificationErrorCodes.TEMPLATE_FILE_FETCH_ACTION_FAILED);
            }
            final int index = urlPath.indexOf(RESOURCES_PATH);

            if (index != -1) {
                urls.add(urlPath.substring(index + 1));
            }
        }
        return urls;
    }
}
