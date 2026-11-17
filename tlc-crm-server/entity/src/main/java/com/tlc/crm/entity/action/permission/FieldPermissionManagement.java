package com.tlc.crm.entity.action.permission;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.AuthorizationController;
import com.tlc.crm.common.iam.action.permissions.PermissionConfigAction;
import com.tlc.crm.common.iam.api.manager.RoleManager;
import com.tlc.crm.common.iam.field.permissions.PermissionAttributes;
import com.tlc.crm.common.iam.internal.IamErrorCodes;

import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.PresetConfig;
import com.tlc.crm.security.cache.service.FieldPermissionService;
import com.tlc.crm.entity.service.FieldService;

import com.tlc.crm.security.internal.permission.builder.FieldPermissionBuilder;
import com.tlc.crm.security.permission.manager.FieldPermissionManager;
import com.tlc.crm.security.permission.manager.PermissionManager;
import com.tlc.crm.security.permission.model.FieldPermission;
import com.tlc.crm.security.permission.model.FieldPermissions;
import com.tlc.crm.security.permission.model.Permissions;

import com.tlc.i18n.I18nResolutionAdapter;
import com.tlc.web.WebAction;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * Manages {@link FieldPermissions} related web actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 * @see AuthorizationController
 */
@WebAction(path = "/field/permission/mgmt")
public class FieldPermissionManagement extends PermissionConfigAction<FieldPermission, FieldPermissions> {

    private static final String FIELD_PERMISSIONS = "fieldPermissions";

    private final RoleManager roleManager;
    private final FieldPermissionManager permissionManager;
    private final FieldPermissionService permissionService;
    private final FieldService fieldService;
    private final FieldPermissionBuilder permissionBuilder;

    public FieldPermissionManagement() {
        roleManager = RoleManager.getInstance();
        permissionManager = FieldPermissionManager.getInstance();
        permissionService = FieldPermissionService.getInstance();
        fieldService = FieldService.getInstance();
        permissionBuilder = FieldPermissionBuilder.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request  Represents the {@link CrmRequest}.
     * @param response Represents the {@link CrmResponse}.
     */
    @Override
    protected final void handle(final CrmRequest request, final CrmResponse response) {
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId       Represents the organization id.
     * @param permissions Collection of {@link FieldPermissions}.
     * @return true if the given details are valid.
     */
    @Override
    protected final boolean validatePermissionDetails(final Long orgId, final Collection<FieldPermissions> permissions) {
        final Collection<Long> roleIds = permissions.stream().map(FieldPermissions::getRoleId).collect(Collectors.toSet());

        if (roleManager.exists(orgId, roleIds)) {
            return true;
        }
        throw ErrorCode.get(IamErrorCodes.ROLE_NOT_FOUND, "i18n.role.not.found");
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId      Represents the organisation id.
     * @param jsonObject Holds the role permission values as json.
     * @return {@link FieldPermissions} Holds the details of the permissions of the role.
     */
    @Override
    protected final FieldPermissions convert(final long orgId, final JsonObject jsonObject) {
        final FieldPermissions fieldPermissions = new FieldPermissions();
        final long roleId = jsonObject.getLong(PermissionAttributes.ROLE_ID.getName());
        final JsonArray permissionJson = jsonObject.getJsonArray(FIELD_PERMISSIONS);
        final FieldPermissions existingPermissions = permissionService.getPermissions(orgId, roleId);

        fieldPermissions.setOrgId(orgId);
        fieldPermissions.setRoleId(roleId);
        fieldPermissions.setPermissions(permissionBuilder.build(orgId, roleId, permissionJson, existingPermissions.getPermissions()));
        return fieldPermissions;
    }

    /**
     * {@inheritDoc}
     *
     * @param permissions           Which holds the values of role permission.
     * @param i18nResolutionAdapter {@link I18nResolutionAdapter} to resolve the text language.
     * @return {@link JsonObject} holds the role permission values as json.
     */
    @Override
    protected final JsonObject convert(final FieldPermissions permissions, final I18nResolutionAdapter i18nResolutionAdapter) {
        final JsonArray fieldPermissionsArray = Json.array();
        final Collection<FieldPermission> fieldPermissions = permissions.getPermissions();
        final Collection<String> fieldI18nNames = new HashSet<>();
        final Map<Long, FieldInfo> sourceFields = fieldService.getAllFields(permissions.orgId()).stream()
                .collect(Collectors.toMap(fieldInfo -> {
                    fieldI18nNames.add(fieldInfo.i18nName().getKey());

                    return fieldInfo.id();
                }, Function.identity()));

        final Map<String, String> fieldNames = i18nResolutionAdapter.get(fieldI18nNames);
        final Map<Long, List<FieldPermission>> sourceFieldPermissions = fieldPermissions.stream()
                .collect(Collectors.groupingBy(FieldPermission::getSourceId));

        for (final Map.Entry<Long, List<FieldPermission>> sourceFieldPermissionsEntry : sourceFieldPermissions.entrySet()) {
            final JsonObject sourceFieldPermissionJson = Json.object();
            final JsonArray fieldsPermissionArray = Json.array();
            final Collection<FieldPermission> fieldPermissionList = sourceFieldPermissionsEntry.getValue();

            for (final FieldPermission fieldPermission : fieldPermissionList) {
                final Long fieldId = fieldPermission.getFieldId();
                final FieldInfo field = sourceFields.get(fieldId);

                if (Objects.nonNull(field)) {
                    final PresetConfig presetConfig = field.presetConfig();
                    final boolean readOnly = presetConfig.readOnly();
                    final JsonObject fieldPermissionJson = Json.object();

                    fieldPermissionJson
                            .put(PermissionAttributes.ID.getName(), fieldId)
                            .put(PermissionAttributes.NAME.getName(), fieldNames.get(field.i18nName().getKey()))
                            .put(PermissionAttributes.ACCESS_LEVEL.getName(), fieldPermission.getAccessLevel().getId())
                            .put(PermissionAttributes.IS_REQUIRED.getName(), fieldPermission.isRequired())
                            .put(PermissionAttributes.IS_REQUIRED_ON_DATA_FIELDS.getName(), presetConfig.required())
                            .put(PermissionAttributes.IS_HIDDEN_ON_DATA_FIELDS.getName(), presetConfig.hidden())
                            .put(PermissionAttributes.CAN_MAKE_HIDDEN.getName(), !(readOnly || presetConfig.isDefaultSystemField()))
                            .put(PermissionAttributes.CAN_MAKE_READ_ONLY.getName(), readOnly || !presetConfig.immutable())
                            .put(PermissionAttributes.CAN_MAKE_EDITABLE.getName(), !readOnly)
                            .put(PermissionAttributes.CAN_EDIT.getName(), !(presetConfig.hidden() || presetConfig.required() || presetConfig.isGroupedField()));

                    fieldsPermissionArray.put(fieldPermissionJson);
                }
            }

            sourceFieldPermissionJson
                    .put(PermissionAttributes.SOURCE_ID.getName(), sourceFieldPermissionsEntry.getKey())
                    .put(PermissionAttributes.FIELDS.getName(), fieldsPermissionArray);
            fieldPermissionsArray.put(sourceFieldPermissionJson);
        }

        return Json.object().put(FIELD_PERMISSIONS, fieldPermissionsArray);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link PermissionManager} instance.
     */
    @Override
    protected final PermissionManager<FieldPermission, FieldPermissions> getPermissionManager() {
        return permissionManager;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents the organisation ID.
     * @param data  Represents the request data.
     * @return The {@link Permissions}.
     */
    @Override
    protected final FieldPermissions get(final Long orgId, final JsonObject data) {
        final long roleId = data.getLong(PermissionAttributes.ID.getName());

        if (roleManager.exists(orgId, Set.of(roleId))) {
            return permissionService.getPermissions(orgId, roleId);
        }
        throw ErrorCode.get(IamErrorCodes.ROLE_NOT_FOUND, "i18n.role.not.found");
    }

    /**
     * {@inheritDoc}
     *
     * @param permissionJson Holds the details of the permissions.
     * @param response       Represents the {@link CrmResponse}.
     */
    @Override
    protected final void loadPermissionJsonIntoResponse(final JsonObject permissionJson, final CrmResponse response) {
        for (final String key : permissionJson.keySet()) {
            response.put(key, permissionJson.get(key));
        }
    }
}
