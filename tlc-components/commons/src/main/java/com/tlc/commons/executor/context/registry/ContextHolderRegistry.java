package com.tlc.commons.executor.context.registry;

import com.tlc.commons.executor.context.CommonContextType;
import com.tlc.commons.executor.context.Context;
import com.tlc.commons.executor.context.ContextHolder;
import com.tlc.commons.executor.context.session.SessionContextHolder;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry responsible for storing and managing context holders along with their associated context types
 *
 * @author sundar
 * @version 1.0
 * @see Registry
 */
public final class ContextHolderRegistry extends Registry<ContextHolder<? extends Context>> {

    private final Map<Context.Type, ContextHolder<? extends Context>> contextHolders;

    private ContextHolderRegistry() {
        this.contextHolders = new ConcurrentHashMap<>();
    }

    private static class Instance {
        private static final ContextHolderRegistry CONTEXT_HOLDER_REGISTRY = new ContextHolderRegistry();

        static {
            CONTEXT_HOLDER_REGISTRY.register(CommonContextType.SESSION, SessionContextHolder.getInstance());
        }
    }

    /**
     * Gets the instance of the {@link ContextHolderRegistry}
     *
     * @return {@link ContextHolderRegistry}
     */
    public static ContextHolderRegistry getInstance() {
        return ContextHolderRegistry.Instance.CONTEXT_HOLDER_REGISTRY;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Map} containing the context holders associated with their respective context types
     */
    @Override
    Map<Context.Type, ContextHolder<? extends Context>> getServiceMap() {
        return contextHolders;
    }

    /**
     * Gets the registered context holders
     *
     * @return {@link Collection} of registered context holders
     */
    public Collection<ContextHolder<?>> getContextHolders() {
        return contextHolders.values();
    }
}
