package com.tlc.crm.common.iam.api.model;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.iam.field.RoleFields;
import com.tlc.crm.notification.app.AppNotificationEventType;
import com.tlc.crm.notification.app.Notifiable;
import com.tlc.crm.notification.app.CrmModelType;
import com.tlc.crm.security.iam.Role;
import com.tlc.crm.security.permission.model.FieldPermissions;
import com.tlc.crm.security.permission.model.RolePermissions;
import com.tlc.crm.security.permission.model.Scope;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * A model that represents the {@link CrmRole} details
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
@Notifiable(types = AppNotificationEventType.ROLE_CHANGED, modelName = CrmModelType.ROLE)
public class CrmRole implements Role, TlcModel {

    @NotNull(groups = {Group.Delete.class}, message = "i18n.role.id.required")
    private Long id;

    @NotNull(groups = {Group.Create.class}, message = "i18n.role.name.required")
    @Size(min = 3, max = 255, groups = {Group.Create.class, Group.Update.class}, message = "i18n.role.name.limit.invalid")
    private String name;

    @NotNull(groups = {Group.Create.class, Group.Delete.class}, message = "i18n.role.orgId.required")
    private Long orgId;

    @Valid
    @NotNull(groups = Group.Create.class, message = "i18n.role.permission.required")
    private RolePermissions rolePermissions;

    @Valid
    @NotNull(groups = Group.Create.class, message = "i18n.role.field.permission.required")
    private FieldPermissions fieldPermissions;

    private JsonObject permissionJson;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.role.permission.default.scope.required")
    private Scope defaultScope;

    private Collection<CrmUser> crmUsers;
    private DateTime createdTime;
    private DateTime updatedTime;
    private Long usersCount;
    private Collection<Long> userIds;

    @NotNull(groups = {Group.Create.class}, message = "i18n.createdBy.required")
    @Valid
    private CrmUser createdBy;

    @NotNull(groups = {Group.Create.class}, message = "i18n.updatedBy.required")
    @Valid
    private CrmUser updatedBy;
    private Config config;
    private String configJsonString;

    public CrmRole() {
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setUsersCount(final Long usersCount) {
        this.usersCount = usersCount;
    }

    public void setCrmUsers(final Collection<CrmUser> crmUsers) {
        this.crmUsers = crmUsers;
    }

    public void addUser(final CrmUser crmUser) {
        this.crmUsers = this.crmUsers == null ? new ArrayList<>() : this.crmUsers;

        this.crmUsers.add(crmUser);
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setUpdatedTime(final DateTime updatedTime) {
        this.updatedTime = updatedTime;
    }

    public void setCreatedTime(final DateTime createdTime) {
        this.createdTime = createdTime;
    }

    public void setCreatedBy(final CrmUser createdBy) {
        this.createdBy = createdBy;
    }

    public void setUpdatedBy(final CrmUser updatedBy) {
        this.updatedBy = updatedBy;
    }

    public DateTime getUpdatedTime() {
        return updatedTime;
    }

    public DateTime getCreatedTime() {
        return createdTime;
    }

    public Collection<CrmUser> getCrmUsers() {
        return crmUsers;
    }

    public Config getConfig() {
        return config;
    }

    public void setConfig(final Config config) {
        configJsonString = config.toString();
        this.config = config;
    }

    public CrmUser getCreatedBy() {
        return createdBy;
    }

    public CrmUser getUpdatedBy() {
        return updatedBy;
    }

    public Long getUsersCount() {
        return usersCount;
    }

    public RolePermissions getRolePermissions() {
        return rolePermissions;
    }

    public void setRolePermissions(final RolePermissions rolePermissions) {
        this.rolePermissions = rolePermissions;
    }

    public FieldPermissions getFieldPermissions() {
        return fieldPermissions;
    }

    public void setFieldPermissions(final FieldPermissions fieldPermissions) {
        this.fieldPermissions = fieldPermissions;
    }

    public JsonObject getPermissionJson() {
        return permissionJson = Objects.isNull(permissionJson) ? rolePermissions.toJson() : permissionJson;
    }

    public void setPermissionJson(final JsonObject permissionJson) {
        this.permissionJson = permissionJson;
    }

    @Override
    public Long id() {
        return id;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    @Override
    public Object identity() {
        return null;
    }

    public Collection<Long> getUserIds() {
        return userIds;
    }

    public void setUserIds(final Collection<Long> userIds) {
        this.userIds = userIds;
    }

    /**
     * {@inheritDoc}
     *
     * @return The crm user role name
     */
    @Override
    public String getName() {
        return name;
    }

    public Scope getDefaultScope() {
        return defaultScope;
    }

    public void setDefaultScope(final Scope scope) {
        this.defaultScope = scope;
    }

    public String getConfigJsonString() {
        return configJsonString;
    }

    /**
     * <p>
     * A record record that stores and manipulates {@link CrmRole} configuration data
     * </p>
     *
     * @author pandiarajkumarG
     * @version 1.0
     */
    public static class Config {

        private final JsonObject configJson;

        public Config() {
            configJson = Json.object();
        }

        public Config(final String configJsonString) {
            configJson = Json.object(configJsonString);
        }

        /**
         * <p>
         * Sets the renamable key
         * </p>
         *
         * @param value Represents the renameable value
         */
        public void setRenamable(final Object value) {
            configJson.put(RoleFields.IS_RENAMABLE.getName(), value);
        }

        /**
         * <p>
         * Sets the isAdmin key
         * </p>
         *
         * @param value Represents the isAdmin value
         */
        public void setAdmin(final Object value) {
            configJson.put(RoleFields.IS_ADMIN.getName(), value);
        }

        /**
         * <p>
         * Sets the default key in config json
         * </p>
         *
         * @param value Represents the default value
         */
        public void setDefault(final Object value) {
            configJson.put(RoleFields.IS_DEFAULT.getName(), value);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return configJson.toString();
        }

        /**
         * <p>
         * Checks if the role is renameable or not
         * </p>
         *
         * @return If role is renameable it will return true, otherwise it will return false
         */
        public boolean isRenamable() {
            return configJson.optBoolean(RoleFields.IS_RENAMABLE.getName());
        }

        /**
         * <p>
         * Checks if the role is admin or not
         * </p>
         *
         * @return If role is admin it will return true, otherwise it will return false
         */
        public boolean isAdmin() {
            return configJson.optBoolean(RoleFields.IS_ADMIN.getName());
        }

        /**
         * <p>
         * Checks if the role is default or not
         * </p>
         *
         * @return If role is default it will return true, otherwise it will return false
         */
        public boolean isDefault() {
            return configJson.optBoolean(RoleFields.IS_DEFAULT.getName());
        }
    }
}
