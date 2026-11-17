package com.tlc.components.starter.initializer.components;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.service.Service;
import com.tlc.commons.service.registry.ServiceRegistry;

import com.tlc.components.starter.initializer.AbstractModuleInitializer;
import com.tlc.components.starter.initializer.Modules;

import com.tlc.i18n.I18nService;
import com.tlc.i18n.internal.I18nServiceImpl;
import com.tlc.i18n.internal.registry.I18nServiceRegistry;
import com.tlc.i18n.internal.status.I18nErrorCodes;
import com.tlc.i18n.registrar.I18NResourceRegistrar;
import com.tlc.i18n.registrar.model.I18NRegistrarData;
import com.tlc.i18n.service.provider.I18nServiceProvider;

import org.apache.commons.io.FilenameUtils;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

import java.net.URL;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * Responsible for triggering the initialization process of the i18n services.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@Component
final class I18nInitializer extends AbstractModuleInitializer {

    private final ServiceRegistry<I18nService> serviceRegistry;

    public I18nInitializer() {
        this.serviceRegistry = I18nServiceRegistry.getInstance();
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
        return serviceRegistry;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize() {
        final I18nServiceImpl i18nService = new I18nServiceImpl();

        i18nService.start(null);
        serviceRegistry.register(i18nService);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postInitialize() {
        final I18nService i18nService = I18nServiceProvider.get();
        final I18NResourceRegistrar i18NRegistrar = new I18NResourceRegistrar(i18nService);

        for (final Map.Entry<Modules, Collection<Resource>> moduleResource : fetchI18nResources().entrySet()) {
            final Modules module = moduleResource.getKey();
            final String moduleName = module.getName();
            final Map<Locale, Map<String, String>> i18nKeys = fetchI18nKeys(moduleName, moduleResource.getValue());
            final I18NRegistrarData i18NRegistrarData = new I18NRegistrarData();

            i18NRegistrarData.setModuleName(moduleName);
            i18NRegistrarData.setModuleId(module.getId());
            i18NRegistrarData.setI18nKeys(i18nKeys);

            i18NRegistrar.register(i18NRegistrarData);
        }
    }

    /**
     * Retrieves the i18n keys mapped to their respective locales for the specified module.
     *
     * @param name      The name of the module.
     * @param resources The collection of {@link Resource} for the module.
     * @return A map of i18n keys grouped by locale.
     */
    private Map<Locale, Map<String, String>> fetchI18nKeys(final String name, final Collection<Resource> resources) {
        final Map<Locale, Map<String, String>> i18nKeys = new HashMap<>();

        try {
            for (final Resource resource : resources) {
                final URL metaFileUrl = resource.getURL();
                final String fileName = FilenameUtils.getBaseName(metaFileUrl.getFile());
                final String[] localeInfo = fileName.split("_");
                final Locale locale = new Locale(localeInfo[0], localeInfo[1]);

                LOGGER.info("Locale {} discovered in module: {}", locale, name);

                final Map<String, String> localProp = new HashMap<>();

                try (final InputStream inputStream = metaFileUrl.openStream()) {
                    final Properties properties = new Properties();

                    properties.load(inputStream);
                    properties.forEach((key, value) -> localProp.put(String.valueOf(key), String.valueOf(value)));
                }
                i18nKeys.put(locale, localProp);
            }
        } catch (Exception exp) {
            throw ErrorCode.get(I18nErrorCodes.I18N_PROPERTY_FILE_COPY_ACTION_FAILED, exp);
        }
        return i18nKeys;
    }

    /**
     * Retrieves the i18n {@link Resource} files and organizes them into a map ordered by module.
     *
     * @return {@link Map} of modules with its 18n resources.
     */
    private Map<Modules, Collection<Resource>> fetchI18nResources() {
        try {
            final Collection<Resource> resources = resourceScanner.scanResources("resources/i18n/", "*.properties");

            return segregateResourcesByModule(resources);
        } catch (IOException exception) {
            throw ErrorCode.get(ErrorCodes.INVALID_DATA);
        }
    }
}
