package com.tlc.crm.security.internal.permission.service.cache;

import com.tlc.cache.Cache;
import com.tlc.cache.remote.CacheConfig;
import com.tlc.cache.remote.CacheService;
import com.tlc.cache.remote.listener.ExpiryListener;
import com.tlc.cache.service.provider.CacheServiceProvider;

import com.tlc.commons.util.StringUtil;

import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.model.RolePermissions;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * Stores permissions of the role into the cache.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class RolePermissionCacheImpl implements RolePermissionCache {

    private final Cache<String, Collection<RolePermission>> rolePermissionsCache;

    private static final class Instance {
        private static final RolePermissionCacheImpl INSTANCE = new RolePermissionCacheImpl();
    }

    private RolePermissionCacheImpl() {
        final CacheService cacheService = CacheServiceProvider.get();
        final CacheConfig<String, Collection<RolePermission>> rolePermissionCacheConfig = new CacheConfig<>("rolePermissions");
        final int expireTime = (int) TimeUnit.MINUTES.toMillis(5);

        rolePermissionCacheConfig.addListener((ExpiryListener<String, Collection<RolePermission>>) (orgId, rolePermissions) -> clear());
        rolePermissionCacheConfig.setTtl(expireTime);

        rolePermissionsCache = cacheService.createOrOpenCache(rolePermissionCacheConfig);
    }

    private void clear() {
        rolePermissionsCache.clear();
    }

    /**
     * Gets the instance of the {@link RolePermissionCache}
     *
     * @return {@link RolePermissionCache}
     */
    public static RolePermissionCacheImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents the organization ID
     * @param roleId Represents the role id
     * @return {@link Collection} of {@link RolePermission} that holds the permissions of the actions
     */
    @Override
    public Collection<RolePermission> getRolePermissions(final long orgId, final long roleId) {
        return buildCacheKey(orgId, roleId).map(rolePermissionsCache::get).orElseGet(Collections::emptySet);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId   Represents the organization id.
     * @param roleIds Represents the role id.
     * @return Collection of {@link RolePermissions} that holds the permissions of the roles
     */
    @Override
    public Set<RolePermissions> getRolePermissions(final long orgId, final Collection<Long> roleIds) {
        final Set<RolePermissions> rolePermissionsList = new HashSet<>();

        for (final Long roleId : roleIds) {
            buildCacheKey(orgId, roleId).map(rolePermissionsCache::get).ifPresent(rolePermissionList -> {
                final RolePermissions rolePermissions = new RolePermissions();

                rolePermissions.setOrgId(orgId);
                rolePermissions.setRoleId(roleId);
                rolePermissions.setPermissions(rolePermissionList);
                rolePermissionsList.add(rolePermissions);
            });
        }
        return rolePermissionsList;
    }

    /**
     * {@inheritDoc}
     *
     * @param rolePermissions {@link RolePermissions} holds the permissions of the role.
     */
    @Override
    public void addPermission(final RolePermissions rolePermissions) {
        final Long orgId = rolePermissions.orgId();
        final Long roleId = rolePermissions.getRoleId();

        buildCacheKey(orgId, roleId).ifPresent(key -> rolePermissionsCache.put(key, rolePermissions.getPermissions()));
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId   Represents the org id.
     * @param roleIds Represents the role id.
     */
    @Override
    public void invalidateCache(final long orgId, final Set<Long> roleIds) {
        final Set<String> orgRoleIds = new HashSet<>();

        for (final Long roleId : roleIds) {
            buildCacheKey(orgId, roleId).ifPresent(orgRoleIds::add);
        }
        rolePermissionsCache.removeAll(orgRoleIds);
    }

    /**
     * <p>
     * Builds the key for the permissions cache. It contains org id and role id.
     * </p>
     *
     * @param orgId  Represents the org id.
     * @param roleId Represents the role id.
     * @return Cache key.
     */
    private Optional<String> buildCacheKey(final long orgId, final Long roleId) {
        return StringUtil.joinNonNullValues(StringUtil.Delimiter.UNDER_SCORE, orgId, roleId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void load() {
    }
}