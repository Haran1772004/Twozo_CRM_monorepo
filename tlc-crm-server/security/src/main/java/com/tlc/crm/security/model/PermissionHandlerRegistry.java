package com.tlc.crm.security.model;

import com.tlc.crm.security.permission.handler.PermissionHandler;

import java.util.EnumMap;
import java.util.Map;

/**
 * <p>
 * Holds the registry for {@link PermissionHandlerType} and its related {@link PermissionHandler}
 * </p>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public final class PermissionHandlerRegistry {

   private final Map<PermissionHandlerType, PermissionHandler> permissionHandlerMap;

    private PermissionHandlerRegistry() {
        this.permissionHandlerMap = new EnumMap<>(PermissionHandlerType.class);
    }

    private static class Instance {
        private static final PermissionHandlerRegistry PERMISSION_HANDLER_HOLDER = new PermissionHandlerRegistry();
    }

    /**
     * <p>
     * Gets the instance {@link PermissionHandlerRegistry}
     * </p>
     *
     * @return The instance {@link PermissionHandlerRegistry}
     */
    public static PermissionHandlerRegistry getInstance() {
        return Instance.PERMISSION_HANDLER_HOLDER;
    }

    /**
     * <p>
     * Loads the {@link PermissionHandlerType} and its related {@link PermissionHandler}
     * </p>
     *
     * @param permissionHandlerType Represents the {@link PermissionHandlerType}
     * @param permissionHandler     Represents the {@link PermissionHandler}
     */
    public void loadPermissionHandlers(final PermissionHandlerType permissionHandlerType, final PermissionHandler permissionHandler) {
        permissionHandlerMap.put(permissionHandlerType, permissionHandler);
    }

    /**
     * <p>
     * Gets the {@link PermissionHandler} with the {@link PermissionHandlerType}
     * </p>
     *
     * @param permissionHandlerType Represents the {@link PermissionHandlerType}
     * @return The {@link PermissionHandlerType}
     */
    public PermissionHandler getPermissionHandler(final PermissionHandlerType permissionHandlerType) {
        return permissionHandlerMap.get(permissionHandlerType);
    }
}