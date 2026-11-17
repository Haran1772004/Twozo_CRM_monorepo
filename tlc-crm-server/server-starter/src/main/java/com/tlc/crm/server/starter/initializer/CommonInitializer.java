package com.tlc.crm.server.starter.initializer;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.util.Configurations;

import com.tlc.commons.service.Service;
import com.tlc.commons.service.registry.ServiceRegistry;

import com.tlc.components.starter.initializer.AbstractModuleInitializer;
import com.tlc.components.starter.initializer.Modules;

import com.tlc.crm.common.export.action.DataExporter;
import com.tlc.crm.common.iam.OrgInitiator;
import com.tlc.crm.common.iam.UserDataMigrator;
import com.tlc.crm.common.iam.api.preferences.model.builder.annotation.PreferenceModelBuilder;
import com.tlc.crm.common.initializer.CommonServiceInitializer;
import com.tlc.crm.common.registrar.CommonResourceRegistrar;
import com.tlc.crm.common.registrar.model.CrmServerRegistrarData;
import com.tlc.crm.common.list.transformer.annotation.Transformer;

import com.tlc.crm.server.starter.fields.ServerStarterFields;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Responsible for initiating the initialization process of common services.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@Component
final class CommonInitializer extends AbstractModuleInitializer {

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isServiceEnabled() {
        return isServiceEnabled(ServerStarterFields.CRM_COMMON);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        if (isServiceEnabled()) {
            CommonResourceRegistrar.getInstance().unregisterAll();
            CommonServiceInitializer.getInstance().stop();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize() {
        if (isServiceEnabled()) {
            final Configurations configurations = getProperties(ServerStarterFields.CRM_COMMON);

            CommonServiceInitializer.getInstance().preInitialize(configurations);
        }
    }

    /**
     * {@inheritDoc}
     *
     */
    @Override
    public void postInitialize() {
        final CommonResourceRegistrar commonRegistrar = CommonResourceRegistrar.getInstance();
        final Consumer<Map<Modules, Collection<Class<?>>>> annotationClassRegistrar = (registeredClasses) -> {

            for (final Map.Entry<Modules, Collection<Class<?>>> registeredClassesByModule : registeredClasses.entrySet()) {
                final CrmServerRegistrarData crmServerRegistrarData = new CrmServerRegistrarData();
                final Modules modules = registeredClassesByModule.getKey();

                crmServerRegistrarData.setModuleId(modules.getId());
                crmServerRegistrarData.setModuleName(modules.getName());
                crmServerRegistrarData.setClasses(registeredClassesByModule.getValue());

                commonRegistrar.register(crmServerRegistrarData);
            }
        };

        annotationClassRegistrar.accept(fetchOrgInitiatorRegisteredClasses());
        annotationClassRegistrar.accept(fetchDataExporterRegisteredClasses());
        annotationClassRegistrar.accept(fetchUserMigratorRegisteredClasses());
        annotationClassRegistrar.accept(fetchTransformerRegisteredClasses());
        annotationClassRegistrar.accept(fetchPreferenceBuilderRegisteredClasses());

        CommonServiceInitializer.getInstance().postInitialize(null);
        LOGGER.info("Application Initializer: Common module is initialized successfully");
    }

    /**
     * Fetches the {@link DataExporter} registered {@link Class} files and organizes them into a map ordered by module.
     *
     * @return {@link Map} of modules with its {@link DataExporter} registered classes.
     */
    private Map<Modules, Collection<Class<?>>> fetchDataExporterRegisteredClasses() {
        try {
            final Collection<Class<?>> classes = classScanner.findClassesWithAnnotation("com.tlc", DataExporter.class);

            return segregateClassesByModule(classes);
        } catch (Exception exception) {
            throw ErrorCode.get(ErrorCodes.INVALID_DATA);
        }
    }

    /**
     * Fetches the {@link OrgInitiator} registered {@link Class} files and organizes them into a map ordered by module.
     *
     * @return {@link Map} of modules with its {@link OrgInitiator} registered classes.
     */
    private Map<Modules, Collection<Class<?>>> fetchOrgInitiatorRegisteredClasses() {
        try {
            final Map<Modules, Collection<Class<?>>> sortedResourcesByModule = new LinkedHashMap<>();
            final Collection<Class<?>> classes = classScanner.findClassesWithAnnotation("com.tlc", OrgInitiator.class);

            segregateClassesByModule(classes).entrySet().stream()
                    .sorted(Comparator.comparing(entry -> entry.getKey().getId()))
                    .forEachOrdered(element -> sortedResourcesByModule.put(element.getKey(), element.getValue()));

            return sortedResourcesByModule;
        } catch (Exception exception) {
            throw ErrorCode.get(ErrorCodes.INVALID_DATA);
        }
    }

    /**
     * Fetches the {@link UserDataMigrator} registered {@link Class} files and organizes them into a map ordered by module.
     *
     * @return {@link Map} of modules with its {@link UserDataMigrator} registered classes.
     */
    private Map<Modules, Collection<Class<?>>> fetchUserMigratorRegisteredClasses() {
        try {
            final Collection<Class<?>> classes = classScanner.findClassesWithAnnotation("com.tlc", UserDataMigrator.class);

            return segregateClassesByModule(classes);
        } catch (Exception exception) {
            throw ErrorCode.get(ErrorCodes.INVALID_DATA);
        }
    }

    /**
     * Fetches the {@link Transformer} registered {@link Class} files and organizes them into a map ordered by module.
     *
     * @return {@link Map} of modules with its {@link Transformer} registered classes.
     */
    private Map<Modules, Collection<Class<?>>> fetchTransformerRegisteredClasses() {
        try {
            final Collection<Class<?>> classes = classScanner.findClassesWithAnnotation("com.tlc", Transformer.class);

            return segregateClassesByModule(classes);
        } catch (Exception exception) {
            throw ErrorCode.get(ErrorCodes.INVALID_DATA);
        }
    }

    /**
     * Fetches the {@link PreferenceModelBuilder} registered {@link Class} files and organizes them into a map ordered by module.
     *
     * @return {@link Map} of modules with its {@link PreferenceModelBuilder} registered classes.
     */
    private Map<Modules, Collection<Class<?>>> fetchPreferenceBuilderRegisteredClasses() {
        try {
            final Collection<Class<?>> classes = classScanner.findClassesWithAnnotation("com.tlc", PreferenceModelBuilder.class);

            return segregateClassesByModule(classes);
        } catch (Exception exception) {
            throw ErrorCode.get(ErrorCodes.INVALID_DATA);
        }
    }
}
