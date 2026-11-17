package com.tlc.components.starter.initializer.components;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.font.store.FontRegistry;
import com.tlc.commons.data.annotation.RequestProcessor;
import com.tlc.commons.internal.template.service.VelocityRuntimeTemplateService;
import com.tlc.commons.service.Service;
import com.tlc.commons.service.registrar.DataProcessorRegistrar;
import com.tlc.commons.service.registrar.model.DataProcessorRegistrarData;
import com.tlc.commons.service.registry.ServiceRegistry;
import com.tlc.commons.internal.template.registrar.TemplateResourceRegistrar;
import com.tlc.commons.internal.template.service.VelocityTemplateService;
import com.tlc.commons.internal.template.service.VelocityTemplateServiceRegistry;
import com.tlc.commons.template.model.TemplateRegistrarData;
import com.tlc.commons.util.ConfHolder;

import com.tlc.components.starter.initializer.AbstractModuleInitializer;
import com.tlc.components.starter.initializer.Modules;

import com.tlc.notification.internal.status.NotificationErrorCodes;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

/**
 * Responsible for triggering the initialization process of the commons services
 *
 * @author Kishore J
 * @version 1.0
 */
@Component
public class CommonsInitializer extends AbstractModuleInitializer {

    private static final String RESOURCES_PATH = "/resources/";
    private static final String TEMPLATE_FILES_BASE_PATH = String.format("%s%s", RESOURCES_PATH, "templates/");
    private static final String VM_FILE_EXTENSION = "*.vm";
    private static final String SERVICES = "services";
    private static final String FONT = "font";

    private final ServiceRegistry<VelocityTemplateService> templateServiceRegistry;
    private final ConfHolder confHolder;
    private final FontRegistry fontRegistry;

    private CommonsInitializer() {
        templateServiceRegistry = VelocityTemplateServiceRegistry.getInstance();
        confHolder = ConfHolder.getInstance();
        fontRegistry = FontRegistry.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize() {
        final VelocityTemplateService velocityTemplateService = new VelocityTemplateService();

        VelocityRuntimeTemplateService.getInstance().start(null);
        velocityTemplateService.start(null);
        templateServiceRegistry.register(velocityTemplateService);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isServiceEnabled() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ServiceRegistry<? extends Service> getServiceRegistry() {
        return templateServiceRegistry;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        DataProcessorRegistrar.getInstance().unregisterAll();
        TemplateResourceRegistrar.getInstance().unregisterAll();
        VelocityRuntimeTemplateService.getInstance().stop();

        if (confHolder.getProperties(SERVICES).containsValue(FONT)) {
            fontRegistry.unregister();
        }
        super.shutdown();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postInitialize() {
        final TemplateResourceRegistrar templateResourceRegistrar = TemplateResourceRegistrar.getInstance();
        final DataProcessorRegistrar dataProcessorRegistrar = DataProcessorRegistrar.getInstance();
        final Map<Modules, Collection<Resource>> templateResources = fetchTemplateResources();
        final Map<Modules, Collection<Class<?>>> requestProcessorClasses = fetchRequestProcessorRegisteredClasses();

        for (final Map.Entry<Modules, Collection<Resource>> moduleResource : templateResources.entrySet()) {
            final Modules module = moduleResource.getKey();
            final TemplateRegistrarData templateRegistrarData = new TemplateRegistrarData();

            templateRegistrarData.setModuleId(module.getId());
            templateRegistrarData.setModuleName(module.getName());
            templateRegistrarData.setTemplateUrls(getTemplateUrls(moduleResource.getValue()));
            templateResourceRegistrar.register(templateRegistrarData);
        }

        for (final Map.Entry<Modules, Collection<Class<?>>> requestProcessorClassEntry : requestProcessorClasses.entrySet()) {
            final Modules module = requestProcessorClassEntry.getKey();
            final DataProcessorRegistrarData dataProcessorRegistrarData = new DataProcessorRegistrarData();

            dataProcessorRegistrarData.setModuleId(module.getId());
            dataProcessorRegistrarData.setModuleName(module.getName());
            dataProcessorRegistrarData.setClasses(requestProcessorClassEntry.getValue());
            dataProcessorRegistrar.register(dataProcessorRegistrarData);
        }

        if (confHolder.getProperties(SERVICES).containsValue(FONT)) {
            fontRegistry.register(confHolder.getProperties(FONT));
        }
    }

    /**
     * Fetches the {@link RequestProcessor} registered {@link Class} files and organizes them into a map ordered by module.
     *
     * @return {@link Map} of modules with its {@link RequestProcessor} registered classes.
     */
    private Map<Modules, Collection<Class<?>>> fetchRequestProcessorRegisteredClasses() {
        try {
            final Collection<Class<?>> classes = classScanner.findClassesWithAnnotation("com.tlc", RequestProcessor.class);

            return segregateClassesByModule(classes);
        } catch (Exception exception) {
            throw ErrorCode.get(ErrorCodes.INVALID_DATA);
        }
    }

    /**
     * <p>
     * Fetches template resources by scanning the base path for template file and segregating them by module
     * </p>
     *
     * @return A {@link Map} containing the key is the {@link Modules} and the value is a collection of {@link Resource}
     */
    private Map<Modules, Collection<Resource>> fetchTemplateResources() {

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
