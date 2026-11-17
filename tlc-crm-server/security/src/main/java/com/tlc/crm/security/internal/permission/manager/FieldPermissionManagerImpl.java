package com.tlc.crm.security.internal.permission.manager;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.security.internal.status.PermissionErrorCodes;
import com.tlc.crm.security.permission.manager.FieldPermissionManager;
import com.tlc.crm.security.permission.model.ApiDataExtractionRules;
import com.tlc.crm.security.permission.model.ApiDetails;
import com.tlc.crm.security.permission.model.FieldPermission;
import com.tlc.crm.security.permission.model.FieldPermissions;
import com.tlc.crm.security.permission.model.Permissions;

import com.tlc.crm.security.permission.model.AccessKeyBasis;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.crm.security.sql.resource.MIAMPAPIDATAEXTRACTIONRULES;
import com.tlc.crm.security.sql.resource.OIAMPFIELDPERMISSIONS;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.AdminDataStore;
import com.tlc.sql.common.api.ds.OrgDataStore;

import com.tlc.validator.ProcessOutcome;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manages CRUD operations and lifecycle handling for {@link FieldPermission} and {@link FieldPermissions}.
 * <p>
 * This interface provides methods for creating, retrieving, and loading field-level permissions
 * associated with organizations. Implementations of this interface handle the business logic
 * for managing permission configurations at the field level.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class FieldPermissionManagerImpl implements FieldPermissionManager {

    private final AdminDataStore adminDataStore;

    private FieldPermissionManagerImpl() {
        adminDataStore = DataStoreProvider.getAdminDataStore();
    }

    private static final class InstanceHolder {
        private static final FieldPermissionManagerImpl FIELD_PERMISSION_MANAGER = new FieldPermissionManagerImpl();
    }

    public static FieldPermissionManager getInstance() {
        return InstanceHolder.FIELD_PERMISSION_MANAGER;
    }

    private OrgDataStore orgDataStore(final long orgId) {
        return DataStoreProvider.getSecureOrgDataStore(orgId);
    }

    /**
     * Provides the table of {@link FieldPermission}.
     *
     * @return {@link FieldPermission} table.
     */
    @Override
    public Table getTable() {
        return Table.get(OIAMPFIELDPERMISSIONS.TABLE);
    }

    /**
     * Provides the table of {@link AccessKeyBasis}.
     *
     * @return {@link AccessKeyBasis} table.
     */
    public Table getApiDataExtractionConfigTable() {
        return Table.get(MIAMPAPIDATAEXTRACTIONRULES.TABLE);
    }

    /**
     * <p>
     * Fetches the {@link Permissions} of the given entity.
     * </p>
     *
     * @param orgId  Represents the organisation id.
     * @param roleId Refers the id of the role.
     * @return If no rows are having the given id, it will return null, otherwise return {@link FieldPermissions}
     */
    @Override
    public FieldPermissions get(final Long orgId, final Long roleId) {
        return get(orgId, Set.of(roleId)).stream().findAny()
                .orElseThrow(() -> ErrorCode.get(PermissionErrorCodes.FIELD_PERMISSION_NOT_FOUND));
    }

    /**
     * <p>
     * Fetches the permissions of the given entities.
     * </p>
     *
     * @param orgId   Represents the organisation id.
     * @param roleIds Which contains the collection of role ids.
     * @return If no rows are having the given ids, it will return empty list otherwise it will return the rows
     * that match the one of the given ids.
     */
    @Override
    public Collection<FieldPermissions> get(final Long orgId, final Collection<Long> roleIds) {
        final Table table = getTable();
        final DataContainer dataContainer = orgDataStore(orgId).get(table,
                new WhereClause(Criteria.in(table.getColumn(OIAMPFIELDPERMISSIONS.ROLE_ID), roleIds)));

        return dataContainer.getRows(table)
                .map(this::convertRowIntoFieldPermissions)
                .collect(Collectors.toSet());
    }

    /**
     * Creates field permissions for the given organization.
     *
     * @param orgId       the organization ID
     * @param permissions the collection of {@link FieldPermissions} to be created
     * @return a {@link ProcessOutcome} indicating the result of the create operation
     */
    @Override
    public ProcessOutcome create(final long orgId, final Collection<FieldPermissions> permissions) {
        final DataContainer targetContainer = DataContainer.create();

        for (final FieldPermissions fieldPermissions : permissions) {
            loadFieldPermissionsIntoContainer(true, fieldPermissions, null, targetContainer);
        }

        orgDataStore(orgId).commitChanges(targetContainer);
        return new ProcessOutcome();
    }

    /**
     * <p>
     * Updates the rows that are matches the given records.
     * Failure of one record doesn't impact other records from update operation.
     * </p>
     *
     * @param orgId       Represents the organisation id.
     * @param permissions Holds the collection of permissions.
     * @return The {@link ProcessOutcome} .
     */
    @Override
    public ProcessOutcome update(final Long orgId, final Collection<FieldPermissions> permissions) {
        final Table table = getTable();
        final DataContainer targetContainer = DataContainer.create();
        final Collection<Long> roleIds = permissions.stream().map(FieldPermissions::getRoleId).collect(Collectors.toSet());
        final DataContainer sourceContainer = orgDataStore(orgId).get(table,
                new WhereClause(Criteria.in(table.getColumn(OIAMPFIELDPERMISSIONS.ROLE_ID), roleIds)));

        for (final FieldPermissions fieldPermissions : permissions) {
            loadFieldPermissionsIntoContainer(false, fieldPermissions, sourceContainer, targetContainer);
        }

        orgDataStore(orgId).commitChanges(targetContainer);
        return new ProcessOutcome();
    }

    /**
     * Loads field permissions from the source container into the target container.
     * <p>
     * This method is invoked during permission create or update operations to ensure
     * field-level permission data is properly initialized and mapped between containers.
     * </p>
     *
     * @param isCreateEvent    flag indicating whether this is a create event
     * @param fieldPermissions the {@link FieldPermissions} instance to be loaded
     * @param sourceContainer  the source {@link DataContainer} containing field data
     * @param targetContainer  the target {@link DataContainer} to populate with permissions
     */
    @Override
    public void loadFieldPermissionsIntoContainer(final boolean isCreateEvent, final FieldPermissions fieldPermissions,
                                                  final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Table table = getTable();
        final Long orgId = fieldPermissions.orgId();
        final Long roleId = fieldPermissions.getRoleId();
        final Row fieldPermissionRow = isCreateEvent ? new Row(table)
                : sourceContainer.getRow(table, new WhereClause(Criteria.eq(table.getColumn(OIAMPFIELDPERMISSIONS.ROLE_ID), roleId)));

        if (Objects.isNull(fieldPermissionRow)) {
            throw ErrorCode.get(PermissionErrorCodes.FIELD_PERMISSION_NOT_FOUND,
                    String.format("No field permissions found for the Org ID: %s, Role ID: %s", orgId, roleId));
        }

        if (isCreateEvent) {
            fieldPermissionRow.setOrgId(orgId);
            fieldPermissionRow.set(OIAMPFIELDPERMISSIONS.ROLE_ID, roleId);
        }
        fieldPermissionRow.set(OIAMPFIELDPERMISSIONS.FIELD_PERMISSION_CONFIG, fieldPermissions.toJson());
        targetContainer.updateRow(fieldPermissionRow);
    }

    /**
     * Retrieves the data extraction rules for a batch of API endpoints.
     * <p>
     * This method processes the provided collection of {@link ApiDetails} and returns a mapping
     * of each endpoint to its corresponding {@link ApiDataExtractionRules}. These rules define
     * how to locate the field using ID or Alias information within the request/response payloads for each API.
     * </p>
     *
     * @param apiDetailsList the collection of API endpoints to look up; must not be null.
     * @return a {@link Collection} of {@link ApiDataExtractionRules}.
     */
    @Override
    public Collection<ApiDataExtractionRules> getApiDataExtractionDetails(final Collection<ApiDetails> apiDetailsList) {
        final Collection<String> apiPaths = new HashSet<>();
        final Collection<String> apiTypes = new HashSet<>();

        for (final ApiDetails apiDetails : apiDetailsList) {
            apiPaths.add(apiDetails.registeredPath());
            apiTypes.add(apiDetails.type());
        }
        final Table table = getApiDataExtractionConfigTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.setWhereClause(new WhereClause(Criteria.in(table.getColumn(MIAMPAPIDATAEXTRACTIONRULES.API_PATH), apiPaths))
                .and(Criteria.in(table.getColumn(MIAMPAPIDATAEXTRACTIONRULES.API_TYPE), apiTypes)));
        final Stream<Row> rows = adminDataStore.get(selectQuery).getRows(table);

        return rows.map(row -> {
            final ApiDetails apiDetails = new ApiDetails(row.get(MIAMPAPIDATAEXTRACTIONRULES.API_TYPE), row.get(MIAMPAPIDATAEXTRACTIONRULES.API_PATH));
            final JsonObject dataExtractionRules = Json.object((String) row.get(MIAMPAPIDATAEXTRACTIONRULES.RULES));

            return new ApiDataExtractionRules(apiDetails, dataExtractionRules);
        }).collect(Collectors.toSet());
    }

    /**
     * Retrieves the data extraction rules for a specific API endpoint, if they exist.
     *
     * @param apiDetails the API endpoint metadata to evaluate; must not be null.
     * @return an {@link Optional} containing the {@link ApiDataExtractionRules} if found, or {@code Optional.empty()} if no rules are configured.
     */
    @Override
    public Optional<ApiDataExtractionRules> getApiDataExtractionDetails(final ApiDetails apiDetails) {
        return getApiDataExtractionDetails(Set.of(apiDetails)).stream().findAny();
    }

    /**
     * Converts a DB {@link Row} into a {@link FieldPermissions} object.
     * <p>
     * This method extracts the organization ID, role ID, and field permission
     * configuration from the given row and maps them into a corresponding
     * {@link FieldPermissions} model instance.
     * </p>
     *
     * @param row the DB row containing field permission data
     * @return a {@link FieldPermissions} object populated with values from the row
     */
    private FieldPermissions convertRowIntoFieldPermissions(final Row row) {
        final FieldPermissions fieldPermissions = new FieldPermissions();

        fieldPermissions.setOrgId(row.getOrgId());
        fieldPermissions.setRoleId(row.get(OIAMPFIELDPERMISSIONS.ROLE_ID));
        fieldPermissions.setPermissionsAsJson(Json.array((String) row.get(OIAMPFIELDPERMISSIONS.FIELD_PERMISSION_CONFIG)));

        return fieldPermissions;
    }
}
