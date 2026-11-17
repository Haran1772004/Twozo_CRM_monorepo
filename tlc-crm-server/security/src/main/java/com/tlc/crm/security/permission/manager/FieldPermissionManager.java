package com.tlc.crm.security.permission.manager;

import com.tlc.crm.security.internal.permission.manager.FieldPermissionManagerImpl;
import com.tlc.crm.security.permission.model.ApiDataExtractionRules;
import com.tlc.crm.security.permission.model.ApiDetails;
import com.tlc.crm.security.permission.model.FieldPermission;
import com.tlc.crm.security.permission.model.FieldPermissions;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.validator.ProcessOutcome;

import java.util.Collection;
import java.util.Optional;

/**
 * Manages CRUD operations and lifecycle handling for {@link FieldPermission} and {@link FieldPermissions}.
 * <p>
 * This interface provides methods for creating, retrieving, and loading field-level permissions
 * associated with organizations. Implementations of this interface handle the business logic
 * for managing permission configurations at the field level.
 * </p>
 *
 * @author  Sathish
 * @version 1.0
 */
public interface FieldPermissionManager extends PermissionManager<FieldPermission, FieldPermissions> {

    /**
     * Returns the singleton instance of {@link FieldPermissionManager}.
     *
     * @return the {@link FieldPermissionManager} implementation instance
     */
    static FieldPermissionManager getInstance() {
        return FieldPermissionManagerImpl.getInstance();
    }

    /**
     * Provides the table of {@link FieldPermission}.
     *
     * @return {@link FieldPermission} table.
     */
    Table getTable();

    /**
     * Creates field permissions for the given organization.
     *
     * @param orgId        the organization ID
     * @param permissions  the collection of {@link FieldPermissions} to be created
     * @return a {@link ProcessOutcome} indicating the result of the create operation
     */
    ProcessOutcome create(final long orgId, final Collection<FieldPermissions> permissions);

    /**
     * Loads field permissions from the source container into the target container.
     * <p>
     * This method is invoked during permission create or update operations to ensure
     * field-level permission data is properly initialized and mapped between containers.
     * </p>
     *
     * @param isCreateEvent     flag indicating whether this is a create event
     * @param fieldPermissions  the {@link FieldPermissions} instance to be loaded
     * @param sourceContainer   the source {@link DataContainer} containing field data
     * @param targetContainer   the target {@link DataContainer} to populate with permissions
     */
    void loadFieldPermissionsIntoContainer(final boolean isCreateEvent, final FieldPermissions fieldPermissions,
                                           final DataContainer sourceContainer, final DataContainer targetContainer);

    /**
     * Retrieves the data extraction rules for a batch of API endpoints.
     * <p>
     * This method processes the provided collection of {@link ApiDetails} and returns a mapping
     * of each endpoint to its corresponding {@link ApiDataExtractionRules}. These rules define
     * how to locate the field using ID or Alias information within the request/response payloads for each API.
     * </p>
     *
     * @param apiDetails the collection of API endpoints to look up; must not be null.
     * @return a {@link Collection} of {@link ApiDataExtractionRules}.
     */
    Collection<ApiDataExtractionRules> getApiDataExtractionDetails(final Collection<ApiDetails> apiDetails);

    /**
     * Retrieves the data extraction rules for a specific API endpoint, if they exist.
     *
     * @param apiDetails the API endpoint metadata to evaluate; must not be null.
     * @return an {@link Optional} containing the {@link ApiDataExtractionRules} if found, or {@code Optional.empty()} if no rules are configured.
     */
    Optional<ApiDataExtractionRules> getApiDataExtractionDetails(final ApiDetails apiDetails);
}
