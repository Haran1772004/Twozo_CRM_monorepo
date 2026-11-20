package com.tlc.components.starter.initializer.components;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.service.Service;
import com.tlc.commons.service.registry.ServiceRegistry;

import com.tlc.components.starter.fields.ComponentsStarterFields;
import com.tlc.components.starter.initializer.AbstractModuleInitializer;
import com.tlc.components.starter.initializer.Modules;

import com.tlc.web.client.event.EventListener;
import com.tlc.web.client.internal.VertxClient;
import com.tlc.web.client.internal.registry.WebClientServiceRegistry;
import com.tlc.web.client.registrar.WebClientResourceRegistrar;
import com.tlc.web.client.registrar.model.WebClientRegistrarData;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;

/**
 * Responsible for initiating the initialization process of webclient
 * services and loading its event registered actions.
 *
 * <p>Vert.x cluster membership recovery (in-JVM reboot after ZooKeeper session loss)
 * is handled inside {@link VertxClient}. This initializer only performs the initial
 * {@code start} and {@code @EventListener} scan; retained registrar/codec state is
 * re-applied by {@link VertxClient} after a successful reboot.</p>
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@Component
final class WebClientInitializer extends AbstractModuleInitializer {

    private final ServiceRegistry<VertxClient> webClientServiceRegistry;

    private WebClientInitializer() {
        this.webClientServiceRegistry = WebClientServiceRegistry.getInstance();
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
        return webClientServiceRegistry;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        WebClientResourceRegistrar.getInstance().unregisterAll();
        super.shutdown();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize() {
        final VertxClient webClientService = new VertxClient();

        webClientService.start(getProperties(ComponentsStarterFields.WEB_CLIENT));
        webClientServiceRegistry.register(webClientService);
    }

    /**
     * {@inheritDoc}
     *
     * @throws Exception If an error occurs during the post-initialization process.
     */
    @Override
    public void postInitialize() {
        final WebClientResourceRegistrar webClientRegistrar = WebClientResourceRegistrar.getInstance();

        for (final Map.Entry<Modules, Collection<Class<?>>> registeredClassesByModule : fetchRegisteredClasses().entrySet()) {
            final WebClientRegistrarData webClientRegistrarData = new WebClientRegistrarData();
            final Modules modules = registeredClassesByModule.getKey();

            webClientRegistrarData.setModuleId(modules.getId());
            webClientRegistrarData.setModuleName(modules.getName());
            webClientRegistrarData.setActionClasses(registeredClassesByModule.getValue());
            webClientRegistrar.register(webClientRegistrarData);
        }
    }

    void startRecoveryMonitoring() {
        final VertxClient webClientService = webClientServiceRegistry.get();
        if (webClientService != null) {
            webClientService.startRecoveryMonitoring();
        }
    }

    /**
     * Fetches the {@link EventListener} registered {@link Class} files and organizes them into a map ordered by module.
     *
     * @return {@link Map} of modules with its {@link EventListener} registered classes.
     */
    private Map<Modules, Collection<Class<?>>> fetchRegisteredClasses() {
        try {
            final Collection<Class<?>> classes = classScanner.findClassesWithAnnotation("com.tlc", EventListener.class);

            return segregateClassesByModule(classes);
        } catch (Exception exception) {
            throw ErrorCode.get(ErrorCodes.INVALID_DATA);
        }
    }
}
