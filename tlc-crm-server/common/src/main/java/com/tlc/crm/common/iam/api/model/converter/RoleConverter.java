package com.tlc.crm.common.iam.api.model.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.iam.api.manager.RoleManager;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.permissions.model.converter.PermissionConverter;
import com.tlc.crm.common.iam.api.permissions.model.converter.impl.RolePermissionConverter;

import com.tlc.crm.common.iam.field.RoleFields;
import com.tlc.crm.common.iam.api.model.CrmRole;
import com.tlc.crm.common.iam.field.permissions.PermissionAttributes;
import com.tlc.crm.common.iam.internal.CrmUserEntity;
import com.tlc.crm.security.cache.service.FieldPermissionService;
import com.tlc.crm.security.internal.permission.builder.FieldPermissionBuilder;
import com.tlc.crm.security.permission.model.FieldPermissions;
import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.model.RolePermissions;
import com.tlc.crm.security.permission.model.Scope;
import com.tlc.l10n.temporal.resolver.DateTimeUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * Provides various methods to convert the data from / into {@link CrmRole}
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public class RoleConverter {

    private final RoleManager roleManager;
    private final CrmUserConverter crmUserConverter;
    private final PermissionConverter<RolePermission, RolePermissions> permissionConverter;
    private final DateTimeUtil dateTimeUtil;
    private final FieldPermissionService fieldPermissionService;
    private final FieldPermissionBuilder permissionBuilder;

    private RoleConverter() {
        roleManager = RoleManager.getInstance();
        crmUserConverter = CrmUserConverter.getInstance();
        permissionConverter = RolePermissionConverter.getInstance();
        dateTimeUtil = DateTimeUtil.getInstance();
        fieldPermissionService = FieldPermissionService.getInstance();
        permissionBuilder = FieldPermissionBuilder.getInstance();
    }

    private static class Instance {
        private static final RoleConverter INSTANCE = new RoleConverter();
    }

    /**
     * <p>
     * Gets the {@link RoleConverter} Instance
     * </p>
     *
     * @return The instance of {@link RoleConverter}
     */
    public static RoleConverter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Converts the {@link JsonObject} into {@link CrmRole}
     * </p>
     *
     * @param isPartialData A flag that indicates whether the data is partial or not
     * @param orgId         Represents an organization id
     * @param userId        Represents the user id
     * @param jsonObject    Holds the role {@link JsonObject} details
     * @return The {@link CrmRole} details
     */
    public CrmRole convert(final boolean isPartialData, final Long orgId, final Long userId, final JsonObject jsonObject) {
        return isPartialData ? buildRoleByPartialData(orgId, userId, jsonObject) : buildRole(orgId, userId, jsonObject);
    }

    /**
     * <p>
     * Builds the {@link JsonObject} into {@link CrmRole}
     * </p>
     *
     * @param orgId      Represents an organization id
     * @param userId     Represents ths user id
     * @param jsonObject Holds the role {@link JsonObject} details
     * @return The {@link CrmRole} details
     */
    private CrmRole buildRole(final Long orgId, final Long userId, final JsonObject jsonObject) {
        final CrmRole crmUserRole = new CrmRole();
        final CrmUser crmUser = new CrmUserEntity();

        crmUser.setId(userId);
        crmUserRole.setCreatedBy(crmUser);
        crmUserRole.setUpdatedBy(crmUser);
        setJsonDataToModel(orgId, crmUserRole, jsonObject);

        return crmUserRole;
    }

    /**
     * <p>
     * Sets the json data into the {@link CrmRole}
     * </p>
     *
     * @param orgId      Represents the organization id
     * @param role       Represents the {@link CrmRole} details
     * @param jsonObject Holds the role {@link JsonObject} details
     */
    private void setJsonDataToModel(final Long orgId, final CrmRole role, final JsonObject jsonObject) {
        final String nameKey = RoleFields.NAME.getName();
        final String idKey = RoleFields.ID.getName();
        final String fieldPermissionsKey = PermissionAttributes.FIELD_PERMISSIONS.getName();
        final CrmRole.Config config = new CrmRole.Config();
        final String defaultScopeKey = RoleFields.DEFAULT_SCOPE.getName();
        final String usersKey = RoleFields.USERS.getName();
        final Collection<CrmUser> crmUsers = new ArrayList<>();

        if (jsonObject.containsKey(idKey)) {
            final Long id = jsonObject.getLong(idKey);

            jsonObject.put(RoleFields.ROLE_ID.getName(), id);
            role.setId(id);
        }
        role.setName(jsonObject.containsKey(nameKey) ? jsonObject.getString(nameKey) : role.getName());
        role.setOrgId(orgId);
        role.setDefaultScope(jsonObject.containsKey(defaultScopeKey) ? Scope.get(jsonObject.getInt(defaultScopeKey))
                : Scope.getDefaultScope());

        if (jsonObject.containsKey(PermissionAttributes.PERMISSIONS.getName())) {
            role.setRolePermissions(permissionConverter.convert(orgId, jsonObject));
        }

        if (jsonObject.containsKey(fieldPermissionsKey)) {
            final FieldPermissions fieldPermissions = new FieldPermissions();
            final JsonArray permissionJson = jsonObject.getJsonArray(fieldPermissionsKey);
            final Long roleId = role.id();
            final FieldPermissions existingPermissions = fieldPermissionService.getPermissions(orgId,
                    Objects.isNull(roleId) ? roleManager.getAdminRole(orgId).id() : roleId);

            fieldPermissions.setOrgId(orgId);
            fieldPermissions.setRoleId(roleId);
            fieldPermissions.setPermissions(permissionBuilder.build(orgId, roleId, permissionJson, existingPermissions.getPermissions()));

            role.setFieldPermissions(fieldPermissions);
        }

        if (jsonObject.containsKey(usersKey)) {
            crmUserConverter.loadJsonDataToModel(jsonObject, crmUsers);
            role.setCrmUsers(crmUsers);
        }
        config.setDefault(false);
        config.setAdmin(false);
        config.setRenamable(true);
        role.setConfig(config);
    }

    /**
     * <p>
     * Builds the partial data into the {@link CrmRole}
     * </p>
     *
     * @param orgId      Represents the organization id
     * @param userId     Represents the user id
     * @param jsonObject Holds the role {@link JsonObject} details
     * @return The {@link CrmRole} details
     */
    private CrmRole buildRoleByPartialData(final Long orgId, final Long userId, final JsonObject jsonObject) {
        final CrmRole crmUserRole = roleManager.get(orgId, jsonObject.getLong(RoleFields.ID.getName()));
        final CrmUser updatedBy = crmUserRole.getUpdatedBy();

        if (Objects.isNull(updatedBy) || !Objects.equals(updatedBy.id(), userId)) {
            final CrmUser crmUser = new CrmUserEntity();

            crmUser.setId(userId);
            crmUserRole.setUpdatedBy(crmUser);
        }
        setJsonDataToModel(orgId, crmUserRole, jsonObject);

        return crmUserRole;
    }

    /**
     * <p>
     * Converts the {@link CrmRole} details into {@link JsonObject}
     * </p>
     *
     * @param role      Represents the {@link CrmRole} details
     * @param isPartial A flag that indicates whether the data is partial or not
     * @return Converted the role {@link JsonObject}
     */
    public JsonObject convert(final CrmRole role, final boolean isPartial) {
        final JsonObject jsonObject = Json.object();
        final Scope scope = role.getDefaultScope();

        jsonObject.put(RoleFields.ID.getName(), role.id());
        jsonObject.put(RoleFields.NAME.getName(), role.getName());

        if (Objects.nonNull(scope)) {
            jsonObject.put(RoleFields.DEFAULT_SCOPE.getName(), scope.getType());
        }

        if (!isPartial) {
            final CrmUser createdBy = role.getCreatedBy();
            final CrmUser updatedBy = role.getUpdatedBy();
            final Collection<CrmUser> crmUsers = role.getCrmUsers();

            if (Objects.nonNull(crmUsers)) {
                final JsonArray userJsonArray = Json.array();

                for (final CrmUser crmUser : crmUsers) {
                    userJsonArray.put(crmUserConverter.convert(crmUser, false, true));
                }
                jsonObject.put(RoleFields.USERS.getName(), userJsonArray);
            }

            if (Objects.nonNull(createdBy)) {
                jsonObject.put(RoleFields.CREATED_BY.getName(), crmUserConverter.convert(role.getCreatedBy(), true, true));
                jsonObject.put(RoleFields.CREATED_TIME.getName(), dateTimeUtil.getTimeStamp(role.getCreatedTime()));
            }

            if (Objects.nonNull(updatedBy)) {
                jsonObject.put(RoleFields.UPDATED_BY.getName(), crmUserConverter.convert(role.getUpdatedBy(), true, true));
                jsonObject.put(RoleFields.UPDATED_TIME.getName(), dateTimeUtil.getTimeStamp(role.getUpdatedTime()));
            }

            jsonObject.put(RoleFields.IS_DELETABLE.getName(), !role.getConfig().isDefault());
            jsonObject.put(RoleFields.USERS_COUNT.getName(), role.getUsersCount());
            jsonObject.put(RoleFields.IS_RENAMABLE.getName(), role.getConfig().isRenamable());
        }

        return jsonObject;
    }

    /**
     * <p>
     * Converts the {@link CrmRole} details into {@link JsonObject}
     * </p>
     *
     * @param crmRole Represents the {@link CrmRole} details
     * @return The {@link JsonObject} details
     */
    public JsonObject convert(final CrmRole crmRole) {
        final JsonObject jsonObject = Json.object();
        final Collection<CrmUser> crmUsers = crmRole.getCrmUsers();
        final JsonObject rolePermissions = crmRole.getPermissionJson();

        jsonObject.put(RoleFields.ID.getName(), crmRole.id());
        jsonObject.put(RoleFields.NAME.getName(), crmRole.getName());

        if (Objects.nonNull(crmUsers)) {
            final JsonArray userJsonArray = Json.array();

            for (final CrmUser crmUser : crmUsers) {
                userJsonArray.put(crmUserConverter.convertUserToJson(crmUser));
            }
            jsonObject.put(RoleFields.USERS.getName(), userJsonArray);
        }

        if (Objects.nonNull(rolePermissions)) {
            jsonObject.put(PermissionAttributes.PERMISSIONS.getName(), rolePermissions);
        }

        return jsonObject;
    }
}
