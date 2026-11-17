package com.tlc.crm.core.internal.store;

import com.tlc.crm.core.internal.listener.RolePermissionCategoryListener;
import com.tlc.crm.security.permission.model.Category;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * <p>
 * Stores and manages mappings between {@link Category.Type} values and their corresponding {@link RolePermissionCategoryListener} implementations
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public final class RolePermissionCategoryListenerStore {

    private final Map<Category.Type, RolePermissionCategoryListener> rolePermissionCategoryListenerMap;

    private RolePermissionCategoryListenerStore() {
        rolePermissionCategoryListenerMap = new EnumMap<>(Category.Type.class);
    }

    private static class InstanceHolder {
        private static final RolePermissionCategoryListenerStore INSTANCE = new RolePermissionCategoryListenerStore();
    }

    /**
     * <p>
     * Gets the {@link RolePermissionCategoryListenerStore}
     * </p>
     *
     * @return The {@link RolePermissionCategoryListenerStore}
     */
    public static RolePermissionCategoryListenerStore getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * <p>
     * Adds a {@link RolePermissionCategoryListener} for the specified {@link Category.Type}
     * </p>
     *
     * @param categoryType                   Represents the {@link Category.Type}
     * @param rolePermissionCategoryListener Represents the {@link RolePermissionCategoryListener}
     */
    public void add(final Category.Type categoryType, final RolePermissionCategoryListener rolePermissionCategoryListener) {
        rolePermissionCategoryListenerMap.put(categoryType, rolePermissionCategoryListener);
    }

    /**
     * <p>
     * Gets the {@link RolePermissionCategoryListener} associated with the given {@link Category.Type}, if one exists
     * </p>
     *
     * @param categoryType Represents the {@link Category.Type}
     * @return An {@link Optional} containing the listener if present, or an empty {@link Optional} if not found
     */
    public Optional<RolePermissionCategoryListener> get(final Category.Type categoryType) {
        return Optional.ofNullable(rolePermissionCategoryListenerMap.get(categoryType));
    }

    /**
     * <p>
     * Removes all registered category type-to-listener mappings from the store
     * </p>
     */
    public void clear() {
        rolePermissionCategoryListenerMap.clear();
    }
}
