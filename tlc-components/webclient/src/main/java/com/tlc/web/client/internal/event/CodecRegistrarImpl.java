package com.tlc.web.client.internal.event;

import com.tlc.web.client.event.CodecRegistrar;
import com.tlc.web.client.service.provider.WebClientServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A registrar that registers the codec for a class
 *
 * @author Selvakumar
 * @version 1.0
 */
public class CodecRegistrarImpl implements CodecRegistrar {

    private static final Logger LOGGER = LoggerFactory.getLogger(CodecRegistrarImpl.class);

    private final Set<Class<?>> codecClasses;
    private final Set<String> registeredOnEventBus;

    private static class Instance {
        private static final CodecRegistrarImpl INSTANCE = new CodecRegistrarImpl();
    }

    public static CodecRegistrarImpl getInstance() {
        return CodecRegistrarImpl.Instance.INSTANCE;
    }

    private CodecRegistrarImpl() {
        codecClasses = ConcurrentHashMap.newKeySet();
        registeredOnEventBus = ConcurrentHashMap.newKeySet();
    }

    /**
     * Register the codec for the given class
     *
     * @param clazz Codec will be registered for the given class
     * @param <T>   Type of class to be registered
     */
    @Override
    public <T> void register(Class<T> clazz) {
        final String className = clazz.getName();

        codecClasses.add(clazz);

        if (registeredOnEventBus.add(className)) {
            WebClientServiceProvider.get().getVertx().eventBus().registerDefaultCodec(clazz, new ObjectCodec<T>(clazz));
        }
    }

    /**
     * Unregister the codec for the given class
     *
     * @param clazz Codec will be unregistered for the given class
     * @param <T>   Type of class to be unregistered
     */
    @Override
    public <T> void unregister(Class<T> clazz) {
        final String className = clazz.getName();

        codecClasses.remove(clazz);

        if (registeredOnEventBus.remove(className)) {
            WebClientServiceProvider.get().getVertx().eventBus().unregisterDefaultCodec(clazz);
        }
    }

    /**
     * Re-binds all previously registered codecs onto the current Event Bus.
     * Required after an in-JVM Vert.x reboot because codecs do not survive {@code vertx.close()}.
     */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void rebindAll() {
        registeredOnEventBus.clear();
        final List<Class<?>> snapshot = new ArrayList<>(codecClasses);

        LOGGER.info("Rebinding {} event-bus codec(s) after Vert.x reboot", snapshot.size());

        for (final Class<?> clazz : snapshot) {
            register((Class) clazz);
        }
    }
}
