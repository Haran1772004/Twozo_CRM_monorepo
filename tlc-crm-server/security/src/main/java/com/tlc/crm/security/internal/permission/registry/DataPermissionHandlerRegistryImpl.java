package com.tlc.crm.security.internal.permission.registry;

import com.tlc.crm.security.context.SecurityContext;
import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.crm.security.internal.permission.handler.DataPermissionHandler;
import com.tlc.crm.security.internal.permission.handler.DataPermissionHandlerImpl;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides implementation for the {@link DataPermissionHandlerRegistry} to manage the storage and retrieval of {@link DataPermissionHandler}
 *
 * @author sundar
 * @version 1.0
 */
public class DataPermissionHandlerRegistryImpl implements DataPermissionHandlerRegistry {

    private final Map<String, DataPermissionHandler> permissionHandlerCache;

    private DataPermissionHandlerRegistryImpl() {
        this.permissionHandlerCache = new ConcurrentHashMap<>();
    }

    private static class Instance {
        private static final DataPermissionHandlerRegistry DATA_PERMISSION_HANDLER_REGISTRY = new DataPermissionHandlerRegistryImpl();
    }

    /**
     * Gets the instance of the {@link DataPermissionHandlerRegistry}
     *
     * @return {@link DataPermissionHandlerRegistry}
     */
    public static DataPermissionHandlerRegistry getInstance() {
        return Instance.DATA_PERMISSION_HANDLER_REGISTRY;
    }

    /**
     * {@inheritDoc}
     *
     * @return true if the {@link DataPermissionHandler} is already present for the current session
     */
    @Override
    public boolean contains() {
        final SecurityContext securityContext;

        try {
            securityContext = LoggedInUserInfo.getSecurityContext();
        } catch (Exception e) {
            return false;
        }
        return Objects.nonNull(securityContext) && permissionHandlerCache.containsKey(securityContext.id());
    }

    /**
     * {@inheritDoc}
     *
     * @return the {@link DataPermissionHandler}
     */
    @Override
    public DataPermissionHandler get() {
        final SecurityContext securityContext = LoggedInUserInfo.getSecurityContext();

        return permissionHandlerCache.computeIfAbsent(securityContext.id(), contextId -> new DataPermissionHandlerImpl(securityContext));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        permissionHandlerCache.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove() {
        permissionHandlerCache.remove(LoggedInUserInfo.getSecurityContext().id());
    }
}