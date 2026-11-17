package com.tlc.crm.security.cache.internal;

import com.tlc.crm.security.cache.internal.impl.FieldPermissionCacheImpl;

import com.tlc.crm.security.internal.permission.service.cache.RemoteCache;

import com.tlc.crm.security.permission.model.ApiDataExtractionRules;
import com.tlc.crm.security.permission.model.ApiDetails;
import com.tlc.crm.security.permission.model.FieldPermission;
import com.tlc.crm.security.permission.model.FieldPermissions;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/**
 * <p>
 * Offers services for retrieving the {@link FieldPermissions}(s) associated with roles.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface FieldPermissionCache extends RemoteCache {

    /**
     * Gets the instance of the {@link FieldPermissionCache}
     *
     * @return {@link FieldPermissionCache}
     */
    static FieldPermissionCache getInstance() {
        return FieldPermissionCacheImpl.getInstance();
    }

    /**
     * <p>
     * Fetches all the {@link FieldPermission} based on the specified organization, role.
     * </p>
     *
     * @param orgId  Represents the organization id.
     * @param roleId Represents the role id.
     * @return Collection of {@link FieldPermission} that holds the field permissions of the actions
     */
    Collection<FieldPermission> getPermissions(final long orgId, final long roleId);

    /**
     * <p>
     * Fetches the collection of permissions based on the specified organization, and role.
     * If the permissions are not already cached, they will be fetched from the database, loaded into the cache, and then returned.
     * </p>
     *
     * @param orgId   Represents the organization id.
     * @param roleIds Represents the role id.
     * @return Collection of {@link FieldPermissions} that holds the field permissions of the roles
     */
    Set<FieldPermissions> getPermissions(final long orgId, final Collection<Long> roleIds);

    /**
     * <p>
     * Adds the {@link FieldPermissions} into cache.
     * </p>
     *
     * @param fieldPermissions {@link FieldPermissions} holds the field permissions of the role.
     */
    void addPermission(final FieldPermissions fieldPermissions);

    /**
     * Retrieves the data extraction rules for a specific API endpoint, if they exist.
     *
     * @param apiDetails the API endpoint metadata to evaluate; must not be null.
     * @return an {@link Optional} containing the {@link ApiDataExtractionRules} if found, or {@code Optional.empty()} if no rules are configured.
     */
    Optional<ApiDataExtractionRules> getApiDataExtractionDetails(final ApiDetails apiDetails);

    /**
     * <p>
     * Invalidates the role field permission cache based on action configs.
     * </p>
     *
     * @param orgId   Represents the org id.
     * @param roleIds Represents the role id.
     */
    void invalidateCache(final long orgId, final Set<Long> roleIds);

    /**
     * Stores the data extraction configuration for a specific API endpoint.
     *
     * @param apiDetails the metadata of the API endpoint to configure; must not be null.
     * @param apiDataExtractionRules the extraction rules defining how to locate data within the API's JSON structure; must not be null.
     */
    void addApiDataExtractionRules(final ApiDetails apiDetails, final ApiDataExtractionRules apiDataExtractionRules);
}
