package com.tlc.crm.security.internal.context;

import com.tlc.commons.executor.context.ContextHolder;
import com.tlc.crm.security.context.SecurityContext;
import com.tlc.crm.security.internal.permission.registry.DataPermissionHandlerRegistry;

/**
 * A holder that holds the {@link SecurityContext} value
 *
 * @author sundar
 * @version 1.0
 */
public class SecurityContextHolder extends ContextHolder<SecurityContext> {

    private SecurityContextHolder() {
    }

    private static class Instance {
        private static final SecurityContextHolder SECURITY_CONTEXT_HOLDER = new SecurityContextHolder();
    }

    /**
     * Gets the {@link SecurityContextHolder} instance
     *
     * @return {@link SecurityContextHolder}
     */
    public static SecurityContextHolder getInstance() {
        return Instance.SECURITY_CONTEXT_HOLDER;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void cleanUp() {
        final DataPermissionHandlerRegistry dataPermissionHandlerRegistry = DataPermissionHandlerRegistry.getInstance();

        if (dataPermissionHandlerRegistry.contains()) {
            dataPermissionHandlerRegistry.remove();
        }
    }
}