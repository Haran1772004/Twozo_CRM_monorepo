package com.tlc.crm.security.cache.internal.impl;

import com.tlc.cache.Cache;
import com.tlc.cache.CacheManager;
import com.tlc.cache.remote.CacheConfig;
import com.tlc.cache.remote.CacheService;
import com.tlc.cache.service.provider.CacheServiceProvider;

import com.tlc.commons.util.StringUtil;

import com.tlc.crm.security.cache.internal.FieldPermissionCache;

import com.tlc.crm.security.permission.model.ApiDataExtractionRules;
import com.tlc.crm.security.permission.model.ApiDetails;
import com.tlc.crm.security.permission.model.FieldPermission;
import com.tlc.crm.security.permission.model.FieldPermissions;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * Stores field permissions of the role into the cache.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class FieldPermissionCacheImpl implements FieldPermissionCache {

    private final Cache<String, Collection<FieldPermission>> fieldPermissionsCache;
    private final Cache<ApiDetails, ApiDataExtractionRules> apiDataExtractionRulesCache;

    private static final class Instance {
        private static final FieldPermissionCacheImpl INSTANCE = new FieldPermissionCacheImpl();
    }

    private FieldPermissionCacheImpl() {
        final CacheService cacheService = CacheServiceProvider.get();
        final CacheConfig<String, Collection<FieldPermission>> fieldPermissionCacheConfig = new CacheConfig<>("fieldPermissions");
        final int expireTime = (int) TimeUnit.MINUTES.toMillis(5);

        fieldPermissionCacheConfig.setTtl(expireTime);

        fieldPermissionsCache = cacheService.createOrOpenCache(fieldPermissionCacheConfig);
        apiDataExtractionRulesCache = CacheManager.getInstance().createCache();
    }

    private void clear() {
        fieldPermissionsCache.clear();
        apiDataExtractionRulesCache.clear();
    }

    /**
     * Gets the instance of the {@link FieldPermissionCache}
     *
     * @return {@link FieldPermissionCache}
     */
    public static FieldPermissionCacheImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents the organization ID
     * @param roleId Represents the role id
     * @return {@link Collection} of {@link FieldPermission} that holds the permissions of the actions
     */
    @Override
    public Collection<FieldPermission> getPermissions(final long orgId, final long roleId) {
        final String orgRoleId = buildCacheKey(orgId, roleId);

        return fieldPermissionsCache.containsKey(orgRoleId)
                ? fieldPermissionsCache.get(orgRoleId) : Collections.emptySet();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId   Represents the organization id.
     * @param roleIds Represents the role id.
     * @return Collection of {@link FieldPermissions} that holds the field permissions of the roles
     */
    @Override
    public Set<FieldPermissions> getPermissions(final long orgId, final Collection<Long> roleIds) {
        final Set<FieldPermissions> fieldPermissionsList = new HashSet<>();

        for (final Long roleId : roleIds) {
            final Collection<FieldPermission> permissions = fieldPermissionsCache.get(buildCacheKey(orgId, roleId));

            if (Objects.nonNull(permissions)) {
                final FieldPermissions fieldPermissions = new FieldPermissions();

                fieldPermissions.setOrgId(orgId);
                fieldPermissions.setRoleId(roleId);
                fieldPermissions.setPermissions(permissions);
                fieldPermissionsList.add(fieldPermissions);
            }
        }
        return fieldPermissionsList;
    }

    /**
     * {@inheritDoc}
     *
     * @param fieldPermissions {@link FieldPermissions} holds the field permissions of the role.
     */
    @Override
    public void addPermission(final FieldPermissions fieldPermissions) {
        fieldPermissionsCache.put(buildCacheKey(fieldPermissions.orgId(), fieldPermissions.getRoleId()), fieldPermissions.getPermissions());
    }

    /**
     * Retrieves the data extraction rules for a specific API endpoint, if they exist.
     *
     * @param apiDetails the API endpoint metadata to evaluate; must not be null.
     * @return an {@link Optional} containing the {@link ApiDataExtractionRules} if found, or {@code Optional.empty()} if no rules are configured.
     */
    @Override
    public Optional<ApiDataExtractionRules> getApiDataExtractionDetails(final ApiDetails apiDetails) {
        return Optional.ofNullable(apiDataExtractionRulesCache.get(apiDetails));
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
            orgRoleIds.add(buildCacheKey(orgId, roleId));
        }
        fieldPermissionsCache.removeAll(orgRoleIds);
    }

    /**
     * Stores the data extraction configuration for a specific API endpoint.
     *
     * @param apiDetails the metadata of the API endpoint to configure; must not be null.
     * @param apiDataExtractionRules the extraction rules defining how to locate data within the API's JSON structure; must not be null.
     */
    @Override
    public void addApiDataExtractionRules(final ApiDetails apiDetails, final ApiDataExtractionRules apiDataExtractionRules) {
        apiDataExtractionRulesCache.put(apiDetails, apiDataExtractionRules);
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
    private String buildCacheKey(final long orgId, final Long roleId) {
        return StringUtil.join(StringUtil.Delimiter.COLON, String.valueOf(orgId), String.valueOf(roleId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void load() {
    }
}