package com.tlc.crm.security.cache.service.impl;

import com.tlc.crm.security.cache.internal.resolver.FieldPermissionResolver;
import com.tlc.crm.security.cache.service.FieldPermissionService;

import com.tlc.crm.security.permission.model.ApiDataExtractionRules;
import com.tlc.crm.security.permission.model.ApiDetails;
import com.tlc.crm.security.permission.model.FieldPermission;
import com.tlc.crm.security.permission.model.FieldPermissions;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <p>
 * Provides the implementations for the services presented in the {@link FieldPermissionService}.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class FieldPermissionServiceImpl implements FieldPermissionService {

    private final FieldPermissionResolver fieldPermissionResolver;

    private static final class Instance {
        private static final FieldPermissionServiceImpl INSTANCE = new FieldPermissionServiceImpl();
    }

    private FieldPermissionServiceImpl() {
        fieldPermissionResolver = FieldPermissionResolver.getInstance();
    }

    /**
     * Gets the instance of the {@link FieldPermissionService}
     *
     * @return {@link FieldPermissionService}
     */
    public static FieldPermissionServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Fetches the {@link FieldPermission} associated with the provided organization, role.
     * </p>
     *
     * @param orgId  Represents the organization id.
     * @param roleId Represents the role id.
     * @return {@link FieldPermission} holds the permission of the role.
     */
    @Override
    public FieldPermissions getPermissions(final long orgId, final long roleId) {
        final Collection<FieldPermission> fieldPermissions = fieldPermissionResolver.getPermissions(orgId, roleId);
        final FieldPermissions permissions = new FieldPermissions();

        permissions.setOrgId(orgId);
        permissions.setRoleId(roleId);
        permissions.setPermissions(fieldPermissions);

        return permissions;
    }

    /**
     * <p>
     * Fetches the {@link FieldPermission} associated with the provided organization, role, and source.
     * </p>
     *
     * @param orgId    Represents the organization id.
     * @param roleId   Represents the role id.
     * @param sourceId Represents the source of the field.
     * @return {@link FieldPermission} holds the field permission of the role.
     */
    @Override
    public Collection<FieldPermission> getPermissions(final long orgId, final long roleId, final long sourceId) {
        final Collection<FieldPermission> fieldPermissions = fieldPermissionResolver.getPermissions(orgId, roleId);

        return fieldPermissions.stream()
                .filter(fieldPermission -> Objects.equals(sourceId, fieldPermission.getSourceId()))
                .collect(Collectors.toSet());
    }

    /**
     * <p>
     * Fetches the {@link FieldPermission} associated with the provided organization, role, and source.
     * </p>
     *
     * @param orgId    Represents the organization id.
     * @param roleId   Represents the role id.
     * @param sourceId Represents the source of the field.
     * @param fieldId  Represents the field id.
     * @return {@link FieldPermission} holds the field permission of the role.
     */
    @Override
    public Optional<FieldPermission> getPermission(final long orgId, final long roleId, final long sourceId, final long fieldId) {
        final Collection<FieldPermission> fieldPermissions = fieldPermissionResolver.getPermissions(orgId, roleId);

        return fieldPermissions.stream()
                .filter(fieldPermission -> Objects.equals(sourceId, fieldPermission.getSourceId())
                        && Objects.equals(fieldId, fieldPermission.getFieldId()))
                .findFirst();
    }

    /**
     * Retrieves the data extraction rules for a specific API endpoint, if they exist.
     *
     * @param apiDetails the API endpoint metadata to evaluate; must not be null.
     * @return an {@link Optional} containing the {@link ApiDataExtractionRules} if found, or {@code Optional.empty()} if no rules are configured.
     */
    @Override
    public Optional<ApiDataExtractionRules> getApiDataExtractionDetails(ApiDetails apiDetails) {
        return fieldPermissionResolver.getApiDataExtractionDetails(apiDetails);
    }
}