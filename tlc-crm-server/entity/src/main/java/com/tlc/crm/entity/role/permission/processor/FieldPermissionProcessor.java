package com.tlc.crm.entity.role.permission.processor;

import com.tlc.crm.common.iam.api.manager.RoleManager;
import com.tlc.crm.common.iam.api.model.CrmRole;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.PresetConfig;

import com.tlc.crm.security.permission.manager.FieldPermissionManager;
import com.tlc.crm.security.permission.model.FieldAccessLevel;
import com.tlc.crm.security.permission.model.FieldPermission;
import com.tlc.crm.security.permission.model.FieldPermissions;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>A singleton processor responsible for managing Field Permissions across all CRM Roles.</p>
 *
 * <p>This processor iterates through all existing CrmRoles in batches and applies
 * {@link FieldPermission} changes (create, update, or delete) based on the provided {@link FieldInfo}
 * and configuration. It acts as the bridge between core entity configurations (PresetConfig)
 * and the security layer (FieldPermissionManager).</p>
 *
 * @author Sathish
 * @version 1.0
 */
public final class FieldPermissionProcessor {

    private static final int DEFAULT_START = 1;
    private static final int DEFAULT_LIMIT = 20;

    private final FieldPermissionManager permissionManager;
    private final RoleManager roleManager;

    private FieldPermissionProcessor() {
        permissionManager = FieldPermissionManager.getInstance();
        roleManager = RoleManager.getInstance();
    }

    private static final class InstanceHolder {
        private static final FieldPermissionProcessor FIELD_PERMISSION_LOADER = new FieldPermissionProcessor();
    }

    /**
     * Returns the singleton instance of the FieldPermissionProcessor.
     *
     * @return The single instance of FieldPermissionProcessor.
     */
    public static FieldPermissionProcessor getInstance() {
        return InstanceHolder.FIELD_PERMISSION_LOADER;
    }

    /**
     * Performs an upsert operation (Create or Update) for field permissions across all roles
     * based on the provided field configuration.
     *
     * @param isCreateEvent If true, the operation is treated as a creation event (insert/default).
     *                      If false, it is treated as an update event.
     * @param orgId         The organization ID.
     * @param fields        The collection of field information whose permissions need to be updated.
     */
    public void upsert(final boolean isCreateEvent, final long orgId, final Collection<FieldInfo> fields) {
        upsert(isCreateEvent, false, orgId, fields);
    }

    /**
     * Performs an upsert operation (Create or Update) for field permissions across all roles
     * based on the provided field configuration.
     *
     * @param isCreateEvent If true, the operation is treated as a creation event (insert/default).
     *                      If false, it is treated as an update event.
     * @param orgId         The organization ID.
     * @param fields        The collection of field information whose permissions need to be updated.
     */
    public void upsert(final boolean isCreateEvent, final boolean isFieldCreateEvent, final long orgId, final Collection<FieldInfo> fields) {
        upsert(isCreateEvent, isFieldCreateEvent, orgId, fields, FieldPermissions::addPermission);
    }

    /**
     * Performs a delete operation for field permissions across all roles.
     * This effectively removes the FieldPermission entry from the FieldPermissions model.
     *
     * @param orgId  The organization ID.
     * @param fields The collection of field information whose permissions need to be deleted.
     */
    public void delete(final long orgId, final Collection<FieldInfo> fields) {
        upsert(false, false, orgId, fields, FieldPermissions::removePermission);
    }

    /**
     * The core logic for iterating through all roles in batches and applying
     * the permission changes (Create, Update, or Delete).
     *
     * @param isCreateEvent      Flag indicating if the underlying persistence call should be created or updated.
     * @param orgId              The organization ID.
     * @param fields             The fields for which permissions are being modified.
     * @param permissionConsumer A BiConsumer defining the action (add or remove) to take on the FieldPermissions model.
     */
    private void upsert(final boolean isCreateEvent, final boolean isFieldCreateEvent, final long orgId,
                        final Collection<FieldInfo> fields, final BiConsumer<FieldPermissions, FieldPermission> permissionConsumer) {
        final long totalRolesCount = roleManager.getTotalCount(orgId);
        int start = DEFAULT_START;

        do {
            final Collection<CrmRole> crmRoles = roleManager.get(orgId, start, DEFAULT_LIMIT);
            final Collection<Long> roleIds = crmRoles.stream().map(CrmRole::id).collect(Collectors.toSet());
            final Map<Long, FieldPermissions> fieldPermissionsMap = isCreateEvent ? new HashMap<>()
                    : permissionManager.get(orgId, roleIds).stream().collect(Collectors.toMap(FieldPermissions::getRoleId, Function.identity()));

            for (final CrmRole crmRole : crmRoles) {
                final Long roleId = crmRole.id();
                final FieldPermissions fieldPermissions = fieldPermissionsMap.computeIfAbsent(roleId, id -> new FieldPermissions());
                final Collection<FieldPermission> permissions = fieldPermissions.getPermissions();
                final Map<Long, FieldPermission> fieldPermissionMap = Objects.isNull(permissions) ? Collections.emptyMap()
                        : permissions.stream().collect(Collectors.toMap(FieldPermission::getFieldId, Function.identity()));

                for (final FieldInfo field : fields) {
                    final PresetConfig fieldConfig = field.presetConfig();
                    final Long fieldId = field.id();
                    final FieldPermission fieldPermission = fieldPermissionMap.getOrDefault(fieldId, new FieldPermission());
                    final boolean isRequired = fieldConfig.required();

                    fieldPermission.setOrgId(orgId);
                    fieldPermission.setRoleId(roleId);
                    fieldPermission.setSourceId(field.sourceId());
                    fieldPermission.setFieldId(fieldId);
                    fieldPermission.setOrder(fieldConfig.order());

                    if (isCreateEvent || isFieldCreateEvent || Objects.isNull(fieldPermission.getAccessLevel())) {
                        fieldPermission.setAccessLevel(fieldConfig.readOnly() ? FieldAccessLevel.READ_ONLY : FieldAccessLevel.EDITABLE);
                    }

                    if (isCreateEvent || isRequired != fieldPermission.isRequiredOnDataFields()) {
                        fieldPermission.setRequired(isRequired);
                        fieldPermission.setRequiredOnDataFields(isRequired);

                        if (isRequired) {
                            fieldPermission.setAccessLevel(FieldAccessLevel.EDITABLE);
                        }
                    }
                    permissionConsumer.accept(fieldPermissions, fieldPermission);
                }
                fieldPermissions.setOrgId(orgId);
                fieldPermissions.setRoleId(roleId);
            }
            final Collection<FieldPermissions> fieldPermissions = fieldPermissionsMap.values();

            if (isCreateEvent) {
                permissionManager.create(orgId, fieldPermissions);
            } else {
                permissionManager.update(orgId, fieldPermissions);
            }
            start += DEFAULT_LIMIT;
        } while (start <= totalRolesCount);
    }
}
