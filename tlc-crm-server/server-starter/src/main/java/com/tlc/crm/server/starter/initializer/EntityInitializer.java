package com.tlc.crm.server.starter.initializer;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;

import com.tlc.commons.util.ConfHolder;
import com.tlc.components.starter.initializer.AbstractModuleInitializer;
import com.tlc.components.starter.initializer.Modules;

import com.tlc.crm.common.fields.ApplicationFields;
import com.tlc.crm.common.registrar.model.CrmServerRegistrarData;
import com.tlc.crm.entity.dropdown.annotation.AsyncDropdown;
import com.tlc.crm.entity.dropdown.annotation.Dropdown;
import com.tlc.crm.entity.dropdown.annotation.StaticDropdown;
import com.tlc.crm.entity.features.api.activity.model.Activity;
import com.tlc.crm.entity.features.field.activity.ActivityFields;
import com.tlc.crm.entity.initializer.EntityServiceInitializer;
import com.tlc.crm.entity.list.filter.Filter;
import com.tlc.crm.entity.registrar.EntityResourceRegistrar;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;

import com.tlc.commons.service.Service;
import com.tlc.commons.service.registry.ServiceRegistry;

/**
 * Responsible for initiating the initialization process of entity services.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@Component
final class EntityInitializer extends AbstractModuleInitializer {

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
    public void shutdown() {
        EntityResourceRegistrar.getInstance().unregisterAll();
        EntityServiceInitializer.getInstance().stop();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize() {
        EntityServiceInitializer.getInstance().preInitialize(null);

        if (isServiceEnabled(ApplicationFields.ENTITY_URL)) {
            final ConfHolder confHolder = ConfHolder.getInstance();

            Activity.setURL(
                    confHolder.getProperties(ApplicationFields.WEB)
                            .getConfig(ApplicationFields.DEFAULT_DOMAIN),
                    confHolder.getProperties(ApplicationFields.ENTITY_URL)
                            .getNestedConfigs(ActivityFields.ACTIVITY.getName())
            );
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws Exception If an error occurs during the post-initialization process.
     */
    @Override
    public void postInitialize() {
        final EntityResourceRegistrar entityRegistrar = EntityResourceRegistrar.getInstance();
        final Consumer<Map<Modules, Collection<Class<?>>>> annotationClassRegistrar = (registeredClasses) -> {

            for (final Map.Entry<Modules, Collection<Class<?>>> registeredClassesByModule : registeredClasses.entrySet()) {
                final CrmServerRegistrarData crmServerRegistrarData = new CrmServerRegistrarData();
                final Modules modules = registeredClassesByModule.getKey();

                crmServerRegistrarData.setModuleId(modules.getId());
                crmServerRegistrarData.setModuleName(modules.getName());
                crmServerRegistrarData.setClasses(registeredClassesByModule.getValue());

                entityRegistrar.register(crmServerRegistrarData);
            }
        };

        annotationClassRegistrar.accept(fetchDropdownRegisteredClasses());
        annotationClassRegistrar.accept(fetchFilterRegisteredClasses());

        EntityServiceInitializer.getInstance().postInitialize(null);
        LOGGER.info("Application Initializer: Entity module is initialized successfully");
    }

    /**
     * Fetches the {@link Dropdown} registered {@link Class} files and organizes them into a map ordered by module.
     *
     * @return {@link Map} of modules with its {@link Dropdown} registered classes.
     */
    private Map<Modules, Collection<Class<?>>> fetchDropdownRegisteredClasses() {
        final Collection<Class<?>> dropdownRegisteredClasses = new ArrayList<>();

        try {
            final Collection<Class<?>> dropdownClasses = classScanner.findClassesWithAnnotation("com.tlc", Dropdown.class);
            final Collection<Class<?>> staticDropdownClasses = classScanner.findClassesWithAnnotation("com.tlc", StaticDropdown.class);
            final Collection<Class<?>> asyncDropdownClasses = classScanner.findClassesWithAnnotation("com.tlc", AsyncDropdown.class);

            dropdownRegisteredClasses.addAll(dropdownClasses);
            dropdownRegisteredClasses.addAll(staticDropdownClasses);
            dropdownRegisteredClasses.addAll(asyncDropdownClasses);

            return segregateClassesByModule(dropdownRegisteredClasses);
        } catch (Exception exception) {
            throw ErrorCode.get(ErrorCodes.INVALID_DATA);
        }
    }

    /**
     * Fetches the {@link Filter} registered {@link Class} files and organizes them into a map ordered by module.
     *
     * @return {@link Map} of modules with its {@link Filter} registered classes.
     */
    private Map<Modules, Collection<Class<?>>> fetchFilterRegisteredClasses() {
        try {
            final Collection<Class<?>> classes = classScanner.findClassesWithAnnotation("com.tlc", Filter.class);

            return segregateClassesByModule(classes);
        } catch (Exception exception) {
            throw ErrorCode.get(ErrorCodes.INVALID_DATA);
        }
    }

}
