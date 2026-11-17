package com.tlc.crm.common.iam.action;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.fields.ListActionFields;
import com.tlc.crm.common.iam.api.manager.RoleManager;
import com.tlc.crm.common.iam.api.model.converter.RoleConverter;
import com.tlc.crm.common.iam.api.model.CrmRole;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.field.CrmUserFields;
import com.tlc.crm.common.iam.field.IamFields;
import com.tlc.crm.common.iam.field.RoleFields;
import com.tlc.crm.common.iam.field.TerritoryFields;
import com.tlc.crm.common.iam.internal.IamConfigAction;
import com.tlc.crm.common.iam.internal.IamErrorCodes;
import com.tlc.crm.common.list.pagination.ListPaginationState;
import com.tlc.crm.common.list.sort.models.ListSortConfig;
import com.tlc.crm.common.list.sort.models.SortViewType;
import com.tlc.crm.security.cache.service.FieldPermissionService;
import com.tlc.crm.security.iam.DefaultRole;
import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.FieldPermissions;
import com.tlc.crm.security.permission.model.RolePermissions;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.crm.security.permission.annotation.authorizer.SettingsAuthorizer;
import com.tlc.crm.common.iam.service.UserComparatorService;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.common.models.SortType;
import com.tlc.crm.notification.app.Event;
import com.tlc.crm.security.permission.service.RolePermissionService;
import com.tlc.iam.common.models.User;
import com.tlc.validator.TlcModel;
import com.tlc.web.WebAction;
import com.tlc.web.client.event.EventCallback;
import com.tlc.crm.common.list.model.ListType;
import com.tlc.crm.common.list.ListStateProcessor;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.tlc.crm.notification.app.event.NotificationEvent.APP_NOTIFICATION_EVENT;

/**
 * <p>
 * Handles the web action related to {@link CrmRole} details
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 * @see IamConfigAction
 */
@SettingsAuthorizer(category = Category.Type.ADMIN_ACCESS, subCategory = SubCategory.Type.MANAGE_ROLES, needActionHandling = true)
@WebAction(path = "/role/mgmt")
public class RoleManagement extends IamConfigAction<CrmRole> {

    private static final int MINIMUM_ADMINS_REQUIRED_COUNT = 1;
    private static final String PERMITTED_LIMIT = "permittedLimit";

    private final String userRetrievalFailedI18NKey = "i18n.common.user.retrieval.failed";
    private final RoleManager roleManager;
    private final RoleConverter roleConverter;
    private final UserComparatorService userComparatorService;
    private final FieldPermissionService fieldPermissionService;
    private final RolePermissionService rolePermissionService;
    private final ListStateProcessor listStateProcessor;

    public RoleManagement() {
        this.roleManager = RoleManager.getInstance();
        this.roleConverter = RoleConverter.getInstance();
        this.userComparatorService = UserComparatorService.getInstance();
        this.fieldPermissionService = FieldPermissionService.getInstance();
        this.rolePermissionService = RolePermissionService.getInstance();
        this.listStateProcessor = ListStateProcessor.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param crmRequest  Holds the {@link CrmRequest} details
     * @param crmResponse Represents the {@link CrmResponse}
     * @param eventData   Represents the {@link EventData}
     */
    @Override
    public void handle(final CrmRequest crmRequest, final CrmResponse crmResponse, final EventData eventData) {
        final JsonObject requestJson = crmRequest.getRequestJson();
        final Long orgId = crmRequest.orgId();
        final JsonObject data = requestJson.getJsonObject(CrmUserFields.DATA.getName());

        switch (requestJson.getString(CrmUserFields.TYPE.getName())) {
            case "linkUsers" -> {
                linkUsers(orgId, data, eventData);
                crmResponse.put(CrmUserFields.MESSAGE.getName(), crmResponse.resolveI18nString("i18n.common.successfully.assigned.role"));
            }
            case "get" -> fetchAndDispatchRole(orgId, data, crmResponse);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return The instance of {@link RoleManager}
     */
    @Override
    public RoleManager getIamConfigManger() {
        return roleManager;
    }

    /**
     * {@inheritDoc}
     *
     * @param request    Holds the {@link CrmRequest} details
     * @param jsonObject Holds the role {@link JsonObject} details
     * @return The {@link CrmRole} details
     */
    @Override
    public CrmRole convert(final CrmRequest request, final JsonObject jsonObject, final boolean isCreateEvent) {
        return roleConverter.convert(false, request.orgId(), request.userId(), jsonObject);
    }

    /**
     * {@inheritDoc}
     *
     * @param request    Holds the {@link CrmRequest} details
     * @param jsonObject Holds the role {@link JsonObject} details
     * @return The {@link CrmRole} details
     */
    @Override
    public CrmRole convertPartialData(final CrmRequest request, final JsonObject jsonObject) {
        return roleConverter.convert(true, request.orgId(), request.userId(), jsonObject);
    }

    /**
     * {@inheritDoc}
     *
     * @param crmRequest  Holds the {@link CrmRequest} details
     * @param crmResponse Represents the {@link CrmResponse}
     */
    @Override
    public void fetchAndDispatchList(final CrmRequest crmRequest, final CrmResponse crmResponse) {
        final Long orgId = crmRequest.orgId();
        final Locale locale = crmRequest.locale();
        final JsonObject jsonObject = crmRequest.getRequestJson().getJsonObject(IamFields.DATA);
        final JsonObject limitJson = listStateProcessor.processAndGetLimitJson(
                jsonObject, new ListPaginationState(orgId, crmRequest.userId(), ListType.ROLE_LIST)
        );
        final int start = limitJson.getInt(ListActionFields.START);
        final int count = limitJson.getInt(ListActionFields.COUNT);
        final long rolesCount = roleManager.getTotalCount(orgId);
        final Collection<CrmRole> roles = rolesCount > 0 && start <= rolesCount ? roleManager.get(orgId, start, count) : Collections.emptyList();
        final JsonArray jsonArray = Json.array();
        final ListSortConfig listSortConfig = new ListSortConfig(orgId, crmRequest.userId(), null);

        listSortConfig.setListType(ListType.ROLE_LIST);
        listSortConfig.setSortProvider(SortViewType.LIST_SORT);
        final JsonObject sortJson = listStateProcessor.processAndGetSortJson(jsonObject, listSortConfig);

        crmResponse.put(RoleFields.LIMIT.getName(), limitJson);
        crmResponse.put(RoleFields.TOTAL_RECORDS.getName(), rolesCount);
        crmResponse.put(RoleFields.HAS_MORE.getName(), (start + count) <= rolesCount);
        crmResponse.put(RoleFields.SORT.getName(), sortJson);

        if (roles.isEmpty()) {
            crmResponse.put(RoleFields.ROLES.getName(), jsonArray);
        } else {
            final Collection<Long> iamUserIds = new ArrayList<>();

            for (final CrmRole role : roles) {
                final CrmUser createdBy = role.getCreatedBy();
                final CrmUser updatedBy = role.getUpdatedBy();

                if (Objects.nonNull(createdBy)) {
                    iamUserIds.add(createdBy.getIamId());
                }

                if (Objects.nonNull(updatedBy)) {
                    iamUserIds.add(updatedBy.getIamId());
                }
            }

            if (iamUserIds.isEmpty()) {
                loadRolesIntoResponse(sortJson, roles, locale, jsonArray, crmResponse);
            } else {
                crmResponse.convertIntoAsync();
                final EventCallback<Collection<User>> usersFetchCallBack = new EventCallback<>() {

                    @Override
                    public void onSuccess(final Collection<User> users) {
                        try {
                            final Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::id, Function.identity()));

                            for (final CrmRole role : roles) {
                                final CrmUser createdBy = role.getCreatedBy();
                                final CrmUser updatedBy = role.getUpdatedBy();

                                if (Objects.nonNull(createdBy)) {
                                    createdBy.setUser(userMap.get(createdBy.getIamId()));
                                }

                                if (Objects.nonNull(updatedBy)) {
                                    updatedBy.setUser(userMap.get(updatedBy.getIamId()));
                                }
                            }
                            loadRolesIntoResponse(sortJson, roles, locale, jsonArray, crmResponse);
                            crmResponse.send();
                        } catch (Exception exception) {
                            sendError(crmResponse, null, exception);
                        }
                    }

                    @Override
                    public void onFailure(final Throwable throwable) {
                        sendError(crmResponse, null, ErrorCode.get(IamErrorCodes.USER_RETRIEVAL_FAILED, userRetrievalFailedI18NKey, throwable));
                    }
                };
                userService.fetchUsers(getIamOrgId(orgId), iamUserIds, usersFetchCallBack);
            }
        }
    }

    /**
     * <p>
     * Loads the roles into the response JSON array
     * </p>
     *
     * @param sortJson    The JSON object containing sort details
     * @param roles       The collection of CRM roles
     * @param locale      The locale for localization
     * @param jsonArray   The JSON array to populate
     * @param crmResponse The CRM response object
     */
    private void loadRolesIntoResponse(final JsonObject sortJson, final Collection<CrmRole> roles,
                                              final Locale locale, final JsonArray jsonArray, final CrmResponse crmResponse) {
        if (Objects.nonNull(sortJson)) {
            ((List<CrmRole>) roles).sort(getRoleComparator(sortJson, locale));
        }

        for (final CrmRole role : roles) {
            jsonArray.put(convert(role));
        }

        crmResponse.put(RoleFields.ROLES.getName(), jsonArray);
    }

    /**
     * <p>
     * Gets the comparator for sorting {@link CrmRole} objects based on field and order type
     * </p>
     *
     * @param jsonObject Holds the sort {@link Json} details
     * @param locale     Represents a locale.
     * @return The {@link CrmRole} comparator
     */
    private Comparator<CrmRole> getRoleComparator(final JsonObject jsonObject, final Locale locale) {
        final SortType ascendingType = SortType.ASCENDING;
        final String field = jsonObject.getString(RoleFields.FIELD.getName());
        final SortType type = SortType.getSortType(jsonObject.getString(RoleFields.TYPE.getName()));
        final Supplier<Collator> collator = () -> Collator.getInstance(locale);

        return switch (field) {
            case "name" -> {
                final Comparator<CrmRole> comparator = Comparator.comparing(CrmRole::getName, Comparator.nullsLast(collator.get()));

                yield ascendingType.equals(type) ? comparator : comparator.reversed();
            }
            case "createdBy" -> {
                final Comparator<CrmRole> comparator = Comparator.comparing(CrmRole::getCreatedBy, Comparator.nullsLast(
                        Comparator.comparing(CrmUser::getFirstName, Comparator.nullsLast(collator.get()))
                                .thenComparing(CrmUser::getLastName, Comparator.nullsLast(collator.get()))));

                yield ascendingType.equals(type) ? comparator : comparator.reversed();
            }
            case "updatedBy" -> {
                final Comparator<CrmRole> comparator = Comparator.comparing(CrmRole::getUpdatedBy, Comparator.nullsLast(
                        Comparator.comparing(CrmUser::getFirstName, Comparator.nullsLast(collator.get()))
                                .thenComparing(CrmUser::getLastName, Comparator.nullsLast(collator.get()))));

                yield ascendingType.equals(type) ? comparator : comparator.reversed();
            }
            case "users" -> {
                final Comparator<CrmRole> comparator = Comparator.comparing(CrmRole::getUsersCount);

                yield ascendingType.equals(type) ? comparator : comparator.reversed();
            }
            default -> throw ErrorCode.get(IamErrorCodes.FIELD_IS_INVALID, "i18n.common.field.is.invalid");
        };
    }

    /**
     * {@inheritDoc}
     *
     * @param crmRequest  Holds the {@link CrmRequest} details
     * @param crmResponse Represents the {@link CrmResponse}
     */
    @Override
    public void fetchAndDispatchUserList(final CrmRequest crmRequest, final CrmResponse crmResponse) {
        final Long orgId = crmRequest.orgId();
        final Locale locale = crmRequest.locale();
        final JsonObject jsonObject = crmRequest.getRequestJson().getJsonObject(IamFields.DATA);
        final Long roleId = jsonObject.getLong(RoleFields.ID.getName());

        if (roleManager.exists(orgId, List.of(roleId))) {
            final int userStartValue = Math.max(jsonObject.optInt(RoleFields.START.getName(), DEFAULT_START_VALUE), DEFAULT_START_VALUE);
            final int userLimit = Math.min(jsonObject.optInt(RoleFields.LIMIT.getName(), DEFAULT_LIMIT), DEFAULT_LIMIT);
            final long usersCount = roleManager.getUsersCount(orgId, roleId);
            final CrmRole role = usersCount > 0 && userStartValue <= usersCount ? roleManager.get(orgId, roleId, userStartValue, userLimit) : null;

            if (Objects.isNull(role)) {
                crmResponse.put(RoleFields.TOTAL_RECORDS.getName(), usersCount);
                crmResponse.put(RoleFields.HAS_MORE.getName(), (userStartValue + userLimit) <= usersCount);
                crmResponse.put(RoleFields.ROLE.getName(), Json.object());
            } else {
                final Collection<Long> iamUserIds = role.getCrmUsers().stream().map(CrmUser::getIamId).collect(Collectors.toSet());

                crmResponse.convertIntoAsync();
                final EventCallback<Collection<User>> usersFetchCallBack = new EventCallback<>() {

                    @Override
                    public void onSuccess(final Collection<User> users) {
                        try {
                            final Collection<CrmUser> crmUsers = role.getCrmUsers();
                            final Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::id, Function.identity()));

                            if (Objects.nonNull(crmUsers) && !crmUsers.isEmpty()) {
                                final Collection<CrmUser> crmUserList = new ArrayList<>(iamUserIds.size());

                                for (final CrmUser crmUser : crmUsers) {
                                    final User user = userMap.get(crmUser.getIamId());

                                    if (Objects.nonNull(user)) {
                                        crmUser.setUser(user);
                                        crmUserList.add(crmUser);
                                    }
                                }
                                role.setCrmUsers(crmUserList);
                                final String sortKey = CrmUserFields.SORT.getName();

                                if ((jsonObject.containsKey(sortKey))) {
                                    ((List<CrmUser>) crmUserList).sort(userComparatorService.getUserComparator(jsonObject.getJsonObject(sortKey), locale));
                                } else {
                                    ((List<CrmUser>) crmUserList).sort(Comparator.comparing(CrmUser::id));
                                }
                            }
                            crmResponse.put(RoleFields.TOTAL_RECORDS.getName(), usersCount);
                            crmResponse.put(RoleFields.HAS_MORE.getName(), (userStartValue + userLimit) <= usersCount);
                            crmResponse.put(RoleFields.ROLE.getName(), roleConverter.convert(role));
                            crmResponse.send();
                        } catch (Exception exception) {
                            sendError(crmResponse, null, exception);
                        }
                    }

                    @Override
                    public void onFailure(final Throwable throwable) {
                        sendError(crmResponse, null, ErrorCode.get(IamErrorCodes.USER_RETRIEVAL_FAILED, userRetrievalFailedI18NKey, throwable));
                    }
                };
                userService.fetchUser(getIamOrgId(orgId), iamUserIds, usersFetchCallBack);
            }
        } else {
            throw ErrorCode.get(IamErrorCodes.ROLE_NOT_FOUND, "i18n.role.not.found");
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId       Represents an organization id
     * @param jsonObject  Holds the role {@link JsonObject} details
     * @param crmResponse Represents the {@link CrmResponse}
     */
    @Override
    public void fetchAndDispatchPartialUserList(final Long orgId, final JsonObject jsonObject, final CrmResponse crmResponse) {
        final Long roleId = jsonObject.getLong(RoleFields.ID.getName());

        if (roleManager.exists(orgId, List.of(roleId))) {
            final int userStartValue = Math.max(jsonObject.optInt(RoleFields.START.getName(), DEFAULT_START_VALUE), DEFAULT_START_VALUE);
            final int userLimit = Math.min(jsonObject.optInt(RoleFields.LIMIT.getName(), DEFAULT_LIMIT), DEFAULT_LIMIT);
            final long usersCount = roleManager.getUsersCount(orgId, roleId);
            final Collection<CrmUser> crmUsers = usersCount > 0 && userStartValue <= usersCount ? roleManager.getUsers(orgId, roleId, userStartValue, userLimit) :
                    Collections.emptyList();
            final JsonArray jsonArray = Json.array();

            if (crmUsers.isEmpty()) {
                crmResponse.put(CrmUserFields.TOTAL_RECORDS.getName(), usersCount);
                crmResponse.put(CrmUserFields.HAS_MORE.getName(), (userStartValue + userLimit) <= usersCount);
                crmResponse.put(CrmUserFields.USERS.getName(), jsonArray);
            } else {
                final Collection<Long> iamUserIds = crmUsers.stream().map(CrmUser::getIamId).collect(Collectors.toUnmodifiableSet());

                crmResponse.convertIntoAsync();
                final EventCallback<Collection<User>> usersFetchCallBack = new EventCallback<>() {

                    @Override
                    public void onSuccess(final Collection<User> users) {
                        try {
                            final Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::id, Function.identity()));

                            for (final CrmUser crmUser : crmUsers) {
                                crmUser.setUser(userMap.get(crmUser.getIamId()));
                                jsonArray.put(crmUserConverter.convert(crmUser));
                            }
                            crmResponse.put(CrmUserFields.TOTAL_RECORDS.getName(), usersCount);
                            crmResponse.put(CrmUserFields.HAS_MORE.getName(), (userStartValue + userLimit) <= usersCount);
                            crmResponse.put(CrmUserFields.USERS.getName(), jsonArray);
                            crmResponse.send();
                        } catch (Exception exception) {
                            sendError(crmResponse, null, exception);
                        }
                    }

                    @Override
                    public void onFailure(final Throwable throwable) {
                        sendError(crmResponse, null, ErrorCode.get(IamErrorCodes.USER_RETRIEVAL_FAILED, userRetrievalFailedI18NKey, throwable));
                    }
                };
                userService.fetchUsers(getIamOrgId(orgId), iamUserIds, usersFetchCallBack);
            }
        } else {
            throw ErrorCode.get(IamErrorCodes.ROLE_NOT_FOUND, "i18n.role.not.found");
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param role Represents the {@link CrmRole} details
     * @return Converted the role {@link JsonObject}
     */
    public JsonObject convert(final CrmRole role) {
        return roleConverter.convert(role, false);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents the organization id
     * @param action Represents the {@link Action} for given details
     * @param roles  Represents the collection of {@link CrmRole}
     * @return if all the details are valid it will return true, otherwise it will throw an error
     */
    @Override
    protected final boolean isValidDetails(final Long orgId, final Action action, final Collection<CrmRole> roles) {
        switch (action) {
            case CREATE -> {
                final Collection<Long> userIds = new HashSet<>();
                final Collection<String> names = new HashSet<>();
                final Collection<Long> adminUsers = roleManager.fetchAdminUserIds(orgId);

                for (final CrmRole role : roles) {
                    names.add(role.getName());
                    final Collection<CrmUser> crmUsers = role.getCrmUsers();

                    if (Objects.nonNull(crmUsers)) {
                        userIds.addAll(crmUsers.stream().map(TlcModel::id).collect(Collectors.toSet()));
                    }

                    if (1 == adminUsers.size() && userIds.contains(adminUsers.stream().findFirst().get())) {
                        throw ErrorCode.get(IamErrorCodes.ROLE_CREATION_RESTRICTED, "i18n.admin.role.minimum.user.restricted");
                    }
                }

                if (roleManager.checkRolesAreExists(orgId, names)) {
                    throw ErrorCode.get(IamErrorCodes.ROLE_NAME_ALREADY_EXISTS, "i18n.role.creation.failed");
                }

                if (!userIds.isEmpty() && !crmUserManager.exists(orgId, userIds)) {
                    throw ErrorCode.get(IamErrorCodes.CRM_USERS_NOT_FOUND, "i18n.common.user.not.found");
                }
            }
            case UPDATE, PARTIAL_UPDATE -> {
                final Collection<Long> userIds = new HashSet<>();
                final Collection<String> names = new HashSet<>();
                final Collection<Long> roleIds = new HashSet<>();
                final Collection<Long> adminUsers = roleManager.fetchAdminUserIds(orgId);

                for (final CrmRole role : roles) {
                    roleIds.add(role.id());
                    names.add(role.getName());
                    final Collection<CrmUser> crmUsers = role.getCrmUsers();

                    if (Objects.nonNull(crmUsers)) {
                        userIds.addAll(crmUsers.stream().map(TlcModel::id).collect(Collectors.toSet()));
                    }

                    if (!Objects.equals(DefaultRole.ACCOUNT_ADMIN.getName(), role.getName()) &&
                            1 == adminUsers.size() && userIds.contains(adminUsers.stream().findFirst().get())) {
                        throw ErrorCode.get(IamErrorCodes.ROLE_UPDATE_RESTRICTED, "i18n.admin.role.minimum.user.restricted");
                    }
                }

                if (!roleManager.exists(orgId, roleIds)) {
                    throw ErrorCode.get(IamErrorCodes.ROLE_NOT_FOUND, "i18n.role.not.found");
                }
                final Collection<CrmRole> crmRoles = roleManager.get(orgId, roleIds);
                final Map<Long, CrmRole> existingRoles = crmRoles.stream().collect(Collectors.toMap(CrmRole::id, Function.identity()));

                for (final CrmRole role : roles) {
                    final CrmRole existingRole = existingRoles.get(role.id());

                    if (isAdminRole(existingRole) && hasPermissionChanges(orgId, role, existingRole)) {
                        throw ErrorCode.get(IamErrorCodes.ADMIN_ROLE_MODIFICATION_RESTRICTED, "i18n.admin.role.modification.restricted");
                    }
                }

                if (!names.isEmpty() && roleManager.checkRolesAreExists(orgId, roleIds, names)) {
                    throw ErrorCode.get(IamErrorCodes.ROLE_NAME_ALREADY_EXISTS, "i18n.role.updation.failed");
                }

                if (!userIds.isEmpty() && !crmUserManager.exists(orgId, userIds)) {
                    throw ErrorCode.get(IamErrorCodes.CRM_USERS_NOT_FOUND, "i18n.common.user.not.found");
                }
                final Collection<Long> adminUserIds = roleManager.fetchAdminUserIds(orgId);

                for (final CrmRole role : roles) {
                    final CrmRole existingRole = existingRoles.get(role.id());

                    if (isAdminRole(existingRole)) {
                        final Collection<CrmUser> updatedUser = role.getCrmUsers();

                        if (Objects.nonNull(updatedUser) && updatedUser.isEmpty() && adminUserIds.size() <= MINIMUM_ADMINS_REQUIRED_COUNT) {
                                throw ErrorCode.get(IamErrorCodes.ROLE_UPDATE_RESTRICTED, "i18n.admin.role.minimum.user.restricted");
                        }
                    }
                }
            }
            case DELETE -> {
                final Collection<Long> ids = roles.stream().map(CrmRole::id).collect(Collectors.toUnmodifiableSet());
                final Collection<CrmRole> crmRoles = roleManager.get(orgId, ids);

                if (crmRoles.stream().map(CrmRole::getConfig).anyMatch(CrmRole.Config::isDefault)) {
                    throw ErrorCode.get(IamErrorCodes.DEFAULT_ROLE_CANNOT_BE_DELETED, "i18n.default.role.delete.restricted");
                }

                if (crmUserManager.rolesHaveUsers(orgId, ids)) {
                    throw ErrorCode.get(IamErrorCodes.ROLE_CANNOT_BE_DELETED_DUE_TO_USER_MAPPING, "i18n.user.mapped.role.delete.restricted");
                }
            }
        }

        return true;
    }

    /**
     * <p>
     * Link a {@link CrmRole} is an account admin role.
     * </p>
     *
     * @param role Represents the {@link CrmRole}.
     * @return if the role is an admin it will return true, otherwise it will throw an error
     */
    private boolean isAdminRole(final CrmRole role) {
        return Objects.nonNull(role) && Objects.nonNull(role.getConfig()) && role.getConfig().isAdmin();
    }

    /**
     * <p>
     * Fetches if there are any changes in permissions or field permissions between the updated role and the existing role.
     * </p>
     *
     * @param orgId         Represents the organization id
     * @param updatedRole   Represents the {@link CrmRole}
     * @param existingRole  Represents the {@link CrmRole}
     * @return if any functional modifications are detected it will return true, otherwise it will throw an error
     */
    private boolean hasPermissionChanges(final Long orgId, final CrmRole updatedRole, final CrmRole existingRole) {
        if (Objects.isNull(updatedRole) || Objects.isNull(existingRole)) {
            return false;
        }

        boolean rolePermissionsChanged = false;
        final RolePermissions updatedRolePermissions = updatedRole.getRolePermissions();

        if (Objects.nonNull(updatedRolePermissions)) {
            final JsonObject updatedPermissionJson = updatedRolePermissions.toJson();
            final JsonObject existingPermissionJson = rolePermissionService.getRolePermissions(orgId, existingRole.id()).toJson();

            rolePermissionsChanged = isPermissionChangesDetected(existingPermissionJson, updatedPermissionJson);
        }

        boolean fieldPermissionsChanged = false;
        final FieldPermissions updatedFieldPermissions = updatedRole.getFieldPermissions();

        if (Objects.nonNull(updatedFieldPermissions)) {
            final JsonArray updatedFieldPermissionsJson = updatedFieldPermissions.toJson();
            final JsonArray existingFieldPermissionsJson = fieldPermissionService.getPermissions(orgId, existingRole.id()).toJson();

            fieldPermissionsChanged = !Objects.equals(updatedFieldPermissionsJson.toString(), existingFieldPermissionsJson.toString());
        }

        return rolePermissionsChanged || fieldPermissionsChanged;
    }

    /**
     * <p>
     * Fetching a comparison between the existing and updated permission JSON
     * </p>
     *
     * @param existingPermissionJson  Holds the current permissions {@link JsonObject} details
     * @param updatedPermissionJson Holds the updated permissions {@link JsonObject} details
     * @return if all updated permission values match the existing permission values it will return true, otherwise it will throw an error
     */
    private boolean isPermissionChangesDetected(final JsonObject existingPermissionJson, final JsonObject updatedPermissionJson) {
        if (Objects.isNull(updatedPermissionJson)) {
            return false;
        }

        if (Objects.isNull(existingPermissionJson)) {
            return true;
        }

        for (final String permissionKey : updatedPermissionJson.keySet()) {

            if (PERMITTED_LIMIT.equals(permissionKey)) {
                continue;
            }

            final JsonObject updatedJson = updatedPermissionJson.optJsonObject(permissionKey);
            final JsonObject existingJson = existingPermissionJson.optJsonObject(permissionKey);

            if (Objects.nonNull(updatedJson)) {
                if (isPermissionChangesDetected(existingJson, updatedJson)) {
                    return true;
                }
                continue;
            }

            final Object updatedValue = updatedPermissionJson.opt(permissionKey);
            final Object existingValue = existingPermissionJson.opt(permissionKey);

            if (!Objects.equals(String.valueOf(updatedValue), String.valueOf(existingValue))) {
                return true;
            }
        }

        return false;
    }

    /**
     * <p>
     * Link a {@link CrmRole} to the given users
     * </p>
     *
     * @param orgId     Represents an organisation id
     * @param roleJson  Holds the role {@link JsonObject} details
     * @param eventData Represents the {@link EventData}
     */
    private void linkUsers(final Long orgId, final JsonObject roleJson, final EventData eventData) {
        final long roleId = roleJson.getLong(CrmUserFields.ROLE_ID.getName());

        if (roleManager.exists(orgId, List.of(roleId))) {
            final Collection<Long> userIds = fetchAndValidateUserIds(orgId, roleJson.getJsonArray(RoleFields.USERS.getName()));
            final Collection<CrmRole> crmRoles = roleManager.fetchRolesForUsers(orgId, userIds);
            final CrmRole role = new CrmRole();
            final Collection<Long> adminUserIds = roleManager.fetchAdminUserIds(orgId);

            adminUserIds.removeAll(userIds);

            if (adminUserIds.isEmpty() && !roleManager.isAdminRole(orgId, roleId)) {
                throw ErrorCode.get(IamErrorCodes.ROLE_UPDATE_RESTRICTED, "i18n.admin.role.minimum.user.restricted");
            }
            role.setId(roleId);
            role.setOrgId(orgId);
            role.setUserIds(userIds);
            role.setUpdatedBy(LoggedInUserInfo.getUser());
            roleManager.linkUsers(role);

            crmRoles.forEach(crmRole -> crmRole.setId(roleId));
            eventData.setValues(crmRoles);
            eventData.setAction(Action.UPDATE);
            triggerEvent(APP_NOTIFICATION_EVENT, eventData, Event.Type.POST);
        } else {
            throw ErrorCode.get(IamErrorCodes.ROLE_NOT_FOUND, "i18n.role.not.found");
        }
    }

    /**
     * <p>
     * Fetches a CRM role along with its associated users and permissions.
     * </p>
     *
     * @param orgId       Represents the ID of the organization.
     * @param roleJson    The JSON object containing the role ID.
     * @param crmResponse The response object to send back the result.
     * @throws ErrorCode If the role is not found, an error code is thrown.
     */
    private void fetchAndDispatchRole(final Long orgId, final JsonObject roleJson, final CrmResponse crmResponse) {
        final long id = roleJson.getLong(RoleFields.ID.getName());

        if (roleManager.exists(orgId, Set.of(id))) {
            final CrmRole crmRole = roleManager.getRole(orgId, id);
            final Collection<CrmUser> crmUsers = crmRole.getCrmUsers();
            final Collection<Long> iamUserIds = Objects.isNull(crmUsers) ? Collections.emptySet() :
                    crmUsers.stream().map(CrmUser::getIamId).collect(Collectors.toSet());

            if (iamUserIds.isEmpty()) {
                crmResponse.put(RoleFields.DATA.getName(), roleConverter.convert(crmRole));
                crmResponse.send();
            } else {
                crmResponse.convertIntoAsync();
                final EventCallback<Collection<User>> usersFetchCallBack = new EventCallback<>() {

                    @Override
                    public void onSuccess(final Collection<User> users) {
                        try {
                            final Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::id, Function.identity()));

                            for (final CrmUser crmUser : crmUsers) {
                                crmUser.setUser(userMap.get(crmUser.getIamId()));
                            }

                            crmResponse.put(RoleFields.DATA.getName(), roleConverter.convert(crmRole));
                            crmResponse.send();
                        } catch (Exception exception) {
                            sendError(crmResponse, null, exception);
                        }
                    }

                    @Override
                    public void onFailure(final Throwable throwable) {
                        sendError(crmResponse, null, ErrorCode.get(IamErrorCodes.USER_RETRIEVAL_FAILED, userRetrievalFailedI18NKey, throwable));
                    }
                };
                userService.fetchUser(getIamOrgId(orgId), iamUserIds, usersFetchCallBack);
            }
        } else {
            throw ErrorCode.get(IamErrorCodes.ROLE_NOT_FOUND, "i18n.role.not.found");
        }
    }
}
