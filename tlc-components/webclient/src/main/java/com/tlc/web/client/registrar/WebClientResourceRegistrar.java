package com.tlc.web.client.registrar;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.service.registrar.ResourceRegistrar;
import com.tlc.web.client.event.EventConsumer;
import com.tlc.web.client.event.EventConsumerRegistrar;
import com.tlc.web.client.event.EventListener;
import com.tlc.web.client.registrar.model.WebClientRegistrarData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * <p>
 * The registrar that registers the given web client related actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 * @see ResourceRegistrar
 */
public class WebClientResourceRegistrar implements ResourceRegistrar<WebClientRegistrarData> {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebClientResourceRegistrar.class);

    private final EventConsumerRegistrar eventConsumerRegistrar;
    private final List<WebClientRegistrarData> registeredData;

    private WebClientResourceRegistrar() {
        eventConsumerRegistrar = EventConsumerRegistrar.get();
        registeredData = new CopyOnWriteArrayList<>();
    }

    private static final class Instance {
        private static final WebClientResourceRegistrar WEB_CLIENT_REGISTRAR = new WebClientResourceRegistrar();
    }

    public static WebClientResourceRegistrar getInstance() {
        return Instance.WEB_CLIENT_REGISTRAR;
    }

    /**
     * {@inheritDoc}
     *
     * @param registrarData Holds the {@link WebClientRegistrarData} which is needed for registering the resources/actions.
     */
    @Override
    public void register(final WebClientRegistrarData registrarData) {
        registerConsumers(registrarData);
        registeredData.add(registrarData);
    }

    /**
     * Re-registers all retained event listeners onto the current Event Bus after an in-JVM Vert.x reboot.
     */
    public void reregisterAll() {
        final List<WebClientRegistrarData> snapshot = new ArrayList<>(registeredData);

        LOGGER.info("Re-registering event listeners for {} module group(s) after Vert.x reboot", snapshot.size());

        for (final WebClientRegistrarData registrarData : snapshot) {
            registerConsumers(registrarData);
        }
    }

    /**
     * Whether this JVM hosts clustered {@link EventConsumer} handlers (provider role).
     * Requester-only nodes must refresh routing when peers join after a ZooKeeper outage.
     */
    public boolean hostsEventConsumers() {
        return !registeredData.isEmpty();
    }

    private void registerConsumers(final WebClientRegistrarData registrarData) {
        final Collection<Class<?>> actionClasses = registrarData.getActionClasses();
        final Map<EventListener, EventConsumer> consumers = new HashMap<>();
        final String moduleName = registrarData.getModuleName();

        try {
            for (final Class<?> classObj : actionClasses) {
                final String className = classObj.getName().replace('.', '/') + ".class";

                if (className.contains("/action/event/listener")) {
                    final EventListener annotation = classObj.getAnnotation(EventListener.class);

                    if (annotation != null) {
                        final Constructor<EventConsumer<?>> constructor = (Constructor<EventConsumer<?>>) classObj.getConstructor();
                        final EventConsumer<?> consumer = constructor.newInstance();

                        consumers.put(annotation, consumer);
                        LOGGER.info("Registering event listener for event {}, Module : {}", annotation.name(), moduleName);
                    }
                }
            }
            eventConsumerRegistrar.register(moduleName, consumers);
        } catch (Exception exception) {
            throw ErrorCode.get(ErrorCodes.INVALID_DATA, exception);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param registrarData Holds the {@link WebClientRegistrarData} which is needed for removing the resources/actions.
     */
    @Override
    public void unregister(final WebClientRegistrarData registrarData) {
        eventConsumerRegistrar.unregister(registrarData.getModuleName());
        registeredData.remove(registrarData);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unregisterAll() {
        try {
            eventConsumerRegistrar.unregisterAll();
        } catch (Exception exception) {
            LOGGER.warn("Event consumer unregisterAll failed; discarding local consumer state. Error: {}",
                    exception.getMessage());
            EventConsumerRegistrar.get().discardAllConsumers();
        }
    }
}
