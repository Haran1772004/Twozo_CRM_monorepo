package com.tlc.crm.security.cache.service;

import com.tlc.crm.security.cache.service.impl.FieldPermissionServiceImpl;

import com.tlc.crm.security.permission.model.ApiDataExtractionRules;
import com.tlc.crm.security.permission.model.ApiDetails;
import com.tlc.crm.security.permission.model.FieldPermission;
import com.tlc.crm.security.permission.model.FieldPermissions;

import java.util.Collection;
import java.util.Optional;

/**
 * <p>
 * The interface offers services to assess whether the specific role users have the necessary field permissions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface FieldPermissionService {

    /**
     * Gets the instance of the {@link FieldPermissionService}
     *
     * @return {@link FieldPermissionService}
     */
    static FieldPermissionService getInstance() {
        return FieldPermissionServiceImpl.getInstance();
    }

    /**
     * <p>
     * Fetches the {@link FieldPermission} associated with the provided organization, role.
     * </p>
     *
     * @param orgId  Represents the organization id.
     * @param roleId Represents the role id.
     * @return {@link FieldPermission} holds the field permission of the role.
     */
    FieldPermissions getPermissions(final long orgId, final long roleId);

    /**
     * <p>
     * Fetches the {@link FieldPermission} associated with the provided organization, role, and source.
     * </p>
     *
     * @param orgId    Represents the organization id.
     * @param roleId   Represents the role id.
     * @param sourceId Represents the source id of the field.
     * @return {@link FieldPermission} holds the field permission of the role.
     */
    Collection<FieldPermission> getPermissions(final long orgId, final long roleId, final long sourceId);

    /**
     * <p>
     * Fetches the {@link FieldPermission} associated with the provided organization, role, and source.
     * </p>
     *
     * @param orgId    Represents the organization id.
     * @param roleId   Represents the role id.
     * @param sourceId Represents the source id of the field.
     * @param fieldId  Represents the field id.
     * @return {@link FieldPermission} holds the field permission of the role.
     */
    Optional<FieldPermission> getPermission(final long orgId, final long roleId, final long sourceId, final long fieldId);

    /**
     * Retrieves the data extraction rules for a specific API endpoint, if they exist.
     *
     * @param apiDetails the API endpoint metadata to evaluate; must not be null.
     * @return an {@link Optional} containing the {@link ApiDataExtractionRules} if found, or {@code Optional.empty()} if no rules are configured.
     */
    Optional<ApiDataExtractionRules> getApiDataExtractionDetails(final ApiDetails apiDetails);
}