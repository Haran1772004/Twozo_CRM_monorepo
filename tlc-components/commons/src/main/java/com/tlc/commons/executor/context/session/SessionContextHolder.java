package com.tlc.commons.executor.context.session;

import com.tlc.commons.executor.context.CommonContextType;
import com.tlc.commons.executor.context.ContextHolder;
import com.tlc.commons.executor.context.registry.ContextHolderRegistry;

/**
 * Holder to hold the {@link SessionContext}
 *
 * @author sundar
 * @version 1.0
 */
public class SessionContextHolder extends ContextHolder<SessionContext> {

    private SessionContextHolder() {
    }

    private static class Instance {
        private static final SessionContextHolder SESSION_CONTEXT_HOLDER = new SessionContextHolder();

        static {
            ContextHolderRegistry.getInstance().register(CommonContextType.SESSION, SESSION_CONTEXT_HOLDER);
        }
    }

    /**
     * Gets the {@link SessionContextHolder} instance
     *
     * @return {@link SessionContextHolder}
     */
    public static SessionContextHolder getInstance() {
        return Instance.SESSION_CONTEXT_HOLDER;
    }
}