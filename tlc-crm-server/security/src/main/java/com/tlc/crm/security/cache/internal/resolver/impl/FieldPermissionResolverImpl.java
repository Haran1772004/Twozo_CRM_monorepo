package com.tlc.crm.security.cache.internal.resolver.impl;

import com.tlc.crm.security.cache.internal.FieldPermissionCache;
import com.tlc.crm.security.cache.internal.resolver.FieldPermissionResolver;

import com.tlc.crm.security.permission.manager.FieldPermissionManager;
import com.tlc.crm.security.permission.model.ApiDataExtractionRules;
import com.tlc.crm.security.permission.model.ApiDetails;
import com.tlc.crm.security.permission.model.FieldPermission;
import com.tlc.crm.security.permission.model.FieldPermissions;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

/**
 * <p>
 * Provides the implementations for the services presented in the {@link FieldPermissionResolver}
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class FieldPermissionResolverImpl implements FieldPermissionResolver {

    private final FieldPermissionCache fieldPermissionCache;
    private final FieldPermissionManager fieldPermissionManager;

    private static final class Instance {
        private static final FieldPermissionResolverImpl INSTANCE = new FieldPermissionResolverImpl();
    }

    private FieldPermissionResolverImpl() {
        fieldPermissionCache = FieldPermissionCache.getInstance();
        fieldPermissionManager = FieldPermissionManager.getInstance();
    }

    /**
     * Gets the instance of the {@link FieldPermissionResolver}
     *
     * @return {@link FieldPermissionResolver}
     */
    public static FieldPermissionResolverImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents the organization id.
     * @param roleId Represents the role id.
     * @return Collection of {@link FieldPermission} that holds the field permissions of the actions.
     */
    @Override
    public Collection<FieldPermission> getPermissions(final long orgId, final long roleId) {
        final Optional<FieldPermissions> fieldPermissions = getPermissions(orgId, Set.of(roleId)).stream().findAny();

        return fieldPermissions.map(FieldPermissions::getPermissions).orElseGet(Collections::emptyList);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId   Represents the organization id.
     * @param roleIds Represents the role id.
     * @return Collection of {@link FieldPermissions} that holds the field permissions of the actions
     */
    @Override
    public Collection<FieldPermissions> getPermissions(final long orgId, final Set<Long> roleIds) {
        final Set<FieldPermissions> existingPermissions = fieldPermissionCache.getPermissions(orgId, roleIds);

        if (existingPermissions.size() != roleIds.size()) {
            final Collection<FieldPermissions> fieldPermissionsList = fieldPermissionManager.get(orgId, roleIds);

            fieldPermissionsList.forEach(fieldPermissionCache::addPermission);
            return fieldPermissionsList;
        }
        return existingPermissions;
    }

    /**
     * Retrieves the data extraction rules for a specific API endpoint, if they exist.
     *
     * @param apiDetails the API endpoint metadata to evaluate; must not be null.
     * @return an {@link Optional} containing the {@link ApiDataExtractionRules} if found, or {@code Optional.empty()} if no rules are configured.
     */
    @Override
    public Optional<ApiDataExtractionRules> getApiDataExtractionDetails(final ApiDetails apiDetails) {

        final Optional<ApiDataExtractionRules> optionalCachedApiDataExtractionRules = fieldPermissionCache.getApiDataExtractionDetails(apiDetails);

        if (optionalCachedApiDataExtractionRules.isEmpty()) {
            final Optional<ApiDataExtractionRules> optionalApiDataExtractionRules = fieldPermissionManager.getApiDataExtractionDetails(apiDetails);

            optionalApiDataExtractionRules.ifPresent(apiDataExtractionRules ->
                    fieldPermissionCache.addApiDataExtractionRules(apiDetails, apiDataExtractionRules));
            return optionalApiDataExtractionRules;
        }
        return optionalCachedApiDataExtractionRules;
    }
}