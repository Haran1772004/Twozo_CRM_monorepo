package com.tlc.crm.security.cache.internal.resolver;

import com.tlc.crm.security.cache.internal.resolver.impl.FieldPermissionResolverImpl;

import com.tlc.crm.security.permission.model.ApiDataExtractionRules;
import com.tlc.crm.security.permission.model.ApiDetails;
import com.tlc.crm.security.permission.model.FieldPermission;
import com.tlc.crm.security.permission.model.FieldPermissions;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/**
 * Resolver for retrieving and handling essential data related to permissions.
 *
 * @author Sathish
 * @version 1.0
 */
public interface FieldPermissionResolver {

    /**
     * Gets the instance of {@link FieldPermissionResolver}
     *
     * @return {@link FieldPermissionResolver}
     */
    static FieldPermissionResolver getInstance() {
        return FieldPermissionResolverImpl.getInstance();
    }

    /**
     * <p>
     * Fetches the collection of actions based on the specified organization, and role.
     * If the actions are not already cached, they will be fetched from the database, loaded into the cache, and then returned.
     * </p>
     *
     * @param orgId  Represents the organization id.
     * @param roleId Represents the role id.
     * @return Collection of {@link FieldPermission} that holds the field permissions of the actions
     */
    Collection<FieldPermission> getPermissions(final long orgId, final long roleId);

    /**
     * <p>
     * Fetches the collection of permissions based on the specified organization, and roles.
     * If the permissions are not already cached, they will be fetched from the database, loaded into the cache, and then returned.
     * </p>
     *
     * @param orgId   Represents the organization id.
     * @param roleIds Represents the role id.
     * @return Collection of {@link FieldPermissions} that holds the field permissions of the actions
     */
    Collection<FieldPermissions> getPermissions(final long orgId, final Set<Long> roleIds);

    /**
     * Retrieves the data extraction rules for a specific API endpoint, if they exist.
     *
     * @param apiDetails the API endpoint metadata to evaluate; must not be null.
     * @return an {@link Optional} containing the {@link ApiDataExtractionRules} if found, or {@code Optional.empty()} if no rules are configured.
     */
    Optional<ApiDataExtractionRules> getApiDataExtractionDetails(final ApiDetails apiDetails);
}
