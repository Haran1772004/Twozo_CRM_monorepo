package com.tlc.commons.executor.context.registry;

import com.tlc.commons.executor.context.CommonContextType;
import com.tlc.commons.executor.context.Context;
import com.tlc.commons.executor.context.ContextCodec;
import com.tlc.commons.executor.context.session.SessionCodec;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry responsible for storing and managing context codecs along with their associated context types
 *
 * @author sundar
 * @version 1.0
 * @see Registry
 */
public final class ContextCodecRegistry extends Registry<ContextCodec> {

    private final Map<Context.Type, ContextCodec> contextCodecs;

    private ContextCodecRegistry() {
        this.contextCodecs = new ConcurrentHashMap<>();
    }

    private static class Instance {
        private static final ContextCodecRegistry CONTEXT_CODEC_REGISTRY = new ContextCodecRegistry();

        static {
            CONTEXT_CODEC_REGISTRY.register(CommonContextType.SESSION, new SessionCodec());
        }
    }

    /**
     * Gets the instance of the {@link ContextCodecRegistry}
     *
     * @return {@link ContextCodecRegistry}
     */
    public static ContextCodecRegistry getInstance() {
        return Instance.CONTEXT_CODEC_REGISTRY;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Map} containing the context codecs associated with their respective context types
     */
    @Override
    Map<Context.Type, ContextCodec> getServiceMap() {
        return contextCodecs;
    }

}