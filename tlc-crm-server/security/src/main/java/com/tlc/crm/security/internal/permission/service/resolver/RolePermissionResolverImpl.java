package com.tlc.crm.security.internal.permission.service.resolver;

import com.tlc.commons.concurrent.LockStore;
import com.tlc.crm.security.internal.permission.service.cache.RolePermissionCache;
import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.model.RolePermissions;
import com.tlc.crm.security.permission.manager.RolePermissionManager;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * <p>
 * Provides the implementations for the services presented in the {@link RolePermissionResolver}
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class RolePermissionResolverImpl implements RolePermissionResolver {

    private static final byte STRIPES_COUNT = 10;

    private final RolePermissionCache rolePermissionCache;
    private final RolePermissionManager rolePermissionManager;
    private final LockStore<Long> lockStore;

    private static final class Instance {
        private static final RolePermissionResolverImpl INSTANCE = new RolePermissionResolverImpl();
    }

    private RolePermissionResolverImpl() {
        rolePermissionCache = RolePermissionCache.getInstance();
        rolePermissionManager = RolePermissionManager.getInstance();
        lockStore = new LockStore<>(STRIPES_COUNT);
    }

    /**
     * Gets the instance of the {@link RolePermissionResolver}
     *
     * @return {@link RolePermissionResolver}
     */
    public static RolePermissionResolverImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents the organization id.
     * @param roleId Represents the role id.
     * @return Collection of {@link RolePermission} that holds the permissions of the actions.
     */
    @Override
    public Collection<RolePermission> getRolePermissions(final long orgId, final long roleId) {
        final Optional<RolePermissions> rolePermissions = getRolePermissions(orgId, Set.of(roleId)).stream().findAny();

        return rolePermissions.map(RolePermissions::getPermissions).orElseGet(Collections::emptyList);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId   Represents the organization id.
     * @param roleIds Represents the role id.
     * @return Collection of {@link RolePermissions} that holds the permissions of the actions
     */
    @Override
    public Collection<RolePermissions> getRolePermissions(final long orgId, final Set<Long> roleIds) {
        final ReentrantReadWriteLock lock = lockStore.getLock(orgId);
        final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
        final Set<RolePermissions> existingPermissions;

        readLock.lock();

        try {
            existingPermissions = rolePermissionCache.getRolePermissions(orgId, roleIds);
        } finally {
            readLock.unlock();
        }

        if (existingPermissions.size() != roleIds.size()) {
            final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

            writeLock.lock();

            try {
                final Collection<RolePermissions> rolePermissionsList = rolePermissionManager.get(orgId, roleIds);

                rolePermissionsList.forEach(rolePermissionCache::addPermission);
                return rolePermissionsList;
            } finally {
                writeLock.unlock();
            }
        }
        return existingPermissions;
    }
}