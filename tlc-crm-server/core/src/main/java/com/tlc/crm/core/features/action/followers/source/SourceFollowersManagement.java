package com.tlc.crm.core.features.action.followers.source;

import com.tlc.attachment.model.profilePicture.DisplayPicture;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.page.Page;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.AuthorizationController;
import com.tlc.crm.common.config.manager.search.IndexManager;
import com.tlc.crm.common.iam.api.manager.CrmUserManager;
import com.tlc.crm.common.iam.api.manager.RoleManager;
import com.tlc.crm.common.iam.api.manager.TeamManager;
import com.tlc.crm.common.iam.api.model.converter.CrmUserConverter;
import com.tlc.crm.common.iam.api.model.converter.TeamConverter;
import com.tlc.crm.common.iam.api.model.CrmRole;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.Team;
import com.tlc.crm.common.iam.api.model.TeamUser;
import com.tlc.crm.common.iam.field.CrmUserFields;
import com.tlc.crm.common.iam.internal.IamErrorCodes;
import com.tlc.crm.common.iam.service.OrganizationService;
import com.tlc.crm.common.models.SortType;
import com.tlc.crm.core.api.contact.manager.CompanyIndexManager;
import com.tlc.crm.core.api.contact.manager.ContactIndexManager;
import com.tlc.crm.core.api.deal.manager.DealIndexManager;
import com.tlc.crm.core.features.api.followers.source.manager.SourceFollowersManager;
import com.tlc.crm.core.features.api.followers.model.Follower;
import com.tlc.crm.core.features.api.followers.source.model.SourceFollowers;
import com.tlc.crm.core.features.internal.followers.status.SourceFollowersErrorCode;
import com.tlc.crm.core.features.field.follower.SourceFollowersFields;
import com.tlc.crm.core.holder.ConfigManagerHolder;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.manager.EntityConfigManager;
import com.tlc.crm.security.permission.annotation.authorizer.FollowerAuthorizer;
import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.model.RolePermissions;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.crm.security.permission.service.RolePermissionService;
import com.tlc.iam.client.UserService;
import com.tlc.iam.client.dropdown.SearchFilter;
import com.tlc.iam.common.models.User;
import com.tlc.iam.common.models.filter.UserStateFilter;
import com.tlc.web.WebAction;
import com.tlc.web.client.event.EventCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * <p>
 * Manages the {@link SourceFollowers} related web actions
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
@FollowerAuthorizer(action = Action.Type.MANAGE_FOLLOWERS, needCommonApiHandling = true)
@WebAction(path = "/followers/mgmt")
public class SourceFollowersManagement extends AuthorizationController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SourceFollowersManagement.class);
    private static final int DEFAULT_START_VALUE = 1;
    private static final int DEFAULT_LIMIT = 20;
    private static final String COMMA = ", ";
    private static final String EMPTY_STRING = "";

    private final SourceFollowersManager sourceFollowersManager;
    private final CrmUserManager crmUserManager;
    private final CrmUserManager secureCrmUserManager;
    private final TeamManager teamManager;
    private final UserService userService;
    private final OrganizationService organizationService;
    private final com.tlc.crm.common.iam.service.UserService crmUserService;
    private final TeamConverter teamConverter;
    private final RolePermissionService rolePermissionService;
    private final CrmUserConverter crmUserConverter;
    private final RoleManager roleManager;

    public SourceFollowersManagement() {
        sourceFollowersManager = SourceFollowersManager.getInstance();
        crmUserManager = CrmUserManager.getInstance();
        secureCrmUserManager = CrmUserManager.getSecureInstance();
        userService = UserService.getInstance();
        organizationService = OrganizationService.getInstance();
        teamManager = TeamManager.getInstance();
        rolePermissionService = RolePermissionService.getInstance();
        crmUserService = com.tlc.crm.common.iam.service.UserService.getInstance();
        teamConverter = TeamConverter.getInstance();
        crmUserConverter = CrmUserConverter.getInstance();
        roleManager = RoleManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param crmRequest  The request to be handled
     * @param crmResponse The response
     * @throws Exception if an error occurs during the request processing
     */
    @Override
    protected void authorizedProcess(final CrmRequest crmRequest, final CrmResponse crmResponse) throws Exception {
        final JsonObject requestJson = crmRequest.getRequestJson();
        final String type = requestJson.getString(SourceFollowersFields.TYPE);
        final JsonObject data = requestJson.getJsonObject(SourceFollowersFields.DATA);
        final long orgId = crmRequest.orgId();
        final long userId = crmRequest.userId();

        switch (type) {
            case SourceFollowersFields.UPDATE -> update(orgId, userId, data, crmResponse);
            case SourceFollowersFields.GET_TEAMS_AND_USERS_LIST ->
                    fetchAndDispatchTeamsAndUsers(orgId, userId, data, crmResponse);
            case SourceFollowersFields.GET_FOLLOWERS -> fetchAndDispatchFollowers(crmRequest, crmResponse);
            case SourceFollowersFields.FOLLOWER_USER_LIST -> fetchAndDispatchUsers(orgId, data, crmResponse);
        }
    }

    /**
     * <p>
     * Fetches and dispatch the users based on the given criteria
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param data        Holds the {@link JsonObject} details
     * @param crmResponse Represents the {@link CrmResponse} details
     */
    private void fetchAndDispatchUsers(final long orgId, final JsonObject data, final CrmResponse crmResponse) {
        final Source source = Source.get(data.getString(SourceFollowersFields.SOURCE));
        final SubCategory.Type subCategoryType = getSubCategory(source);
        final int start = Math.max(data.optInt(SourceFollowersFields.START, DEFAULT_START_VALUE), DEFAULT_START_VALUE);
        final int limit = Math.min(data.optInt(SourceFollowersFields.LIMIT, DEFAULT_LIMIT), DEFAULT_LIMIT);
        final SearchFilter nameSearchFilter = new SearchFilter();
        final SearchFilter stateSearchFilter = new SearchFilter();
        final Collection<SearchFilter> searchFilters = new HashSet<>();

        if (data.containsKey(SourceFollowersFields.CRITERIA)) {
            final JsonObject criteriaJson = data.getJsonObject(SourceFollowersFields.CRITERIA);

            nameSearchFilter.setType(criteriaJson.getString(SourceFollowersFields.TYPE));
            nameSearchFilter.setValue(criteriaJson.get(SourceFollowersFields.VALUE));
        }
        nameSearchFilter.setLimit(limit);
        nameSearchFilter.setStart(start);
        stateSearchFilter.setType(SourceFollowersFields.STATE);
        stateSearchFilter.setValue(UserStateFilter.ACTIVE);
        searchFilters.add(stateSearchFilter);
        searchFilters.add(nameSearchFilter);
        final Set<Long> restrictedFollowerRoleIds = getRestrictedFollowerRoleIds(orgId, subCategoryType);
        final Set<Long> restrictedUserIds = getRestrictedUserIds(orgId, restrictedFollowerRoleIds);

        if (!restrictedUserIds.isEmpty()) {
            final Set<Long> excludeIamUserIds = crmUserService.getUsers(orgId, restrictedUserIds).stream().map(CrmUser::getIamId).collect(Collectors.toSet());

            nameSearchFilter.setExcludeUserIds(excludeIamUserIds);
        }
        crmResponse.convertIntoAsync();
        final EventCallback<Page<User>> usersFetchCallBack = new EventCallback<>() {

            @Override
            public void onSuccess(final Page<User> userPage) {
                try {
                    final int totalRecords = userPage.getNumber();

                    if (totalRecords > 0 && start <= totalRecords) {
                        final Map<Long, User> userMap = userPage.getData().stream().collect(Collectors.toMap(User::id, Function.identity()));
                        final Collection<CrmUser> crmUsers = crmUserService.getUsersByIamId(orgId, userMap.keySet());
                        final JsonArray userArray = Json.array();

                        for (final CrmUser crmUser : crmUsers) {
                            final Long userId = crmUser.id();
                            final JsonObject jsonObject = Json.object();
                            final User user = userMap.get(crmUser.getIamId());
                            final DisplayPicture thumbnail = user.getThumbnail();

                            jsonObject.put(SourceFollowersFields.VALUE, userId);
                            jsonObject.put(SourceFollowersFields.NAME, user.getFullName());

                            if (Objects.nonNull(thumbnail)) {
                                jsonObject.put(FieldKeys.THUMBNAIL.getName(), crmUserConverter.fetchProfileJson(thumbnail, orgId, userId));
                            }
                            userArray.put(jsonObject);
                        }
                        crmResponse.put(FieldKeys.OPTIONS.getName(), userArray);
                    } else {
                        crmResponse.put(FieldKeys.OPTIONS.getName(), Collections.emptyList());
                    }
                    crmResponse.put(FieldKeys.TOTAL_RECORDS.getName(), totalRecords);
                    crmResponse.put(FieldKeys.HAS_MORE.getName(), start + limit <= totalRecords);
                    crmResponse.put(FieldKeys.SIZE.getName(), totalRecords);
                } catch (final Throwable throwable) {
                    LOGGER.error("Source Followers: Failed to users retrieved data. Reason: {}, Org Id: {}", throwable.getMessage(), orgId);
                    crmResponse.setErrorCode(ErrorCode.get(IamErrorCodes.USER_RETRIEVAL_FAILED, "i18n.common.user.retrieval.failed"));
                } finally {
                    crmResponse.send();
                }
            }

            @Override
            public void onFailure(final Throwable cause) {
                crmResponse.setErrorCode(ErrorCode.get(IamErrorCodes.USER_RETRIEVAL_FAILED, "i18n.common.user.retrieval.failed"));
                crmResponse.send();
            }
        };
        userService.fetchUserPage(false, organizationService.getIamOrgId(orgId), searchFilters, usersFetchCallBack);
    }

    /**
     * <p>
     * Builds a {@link SourceFollowers} from the provided organization id and {@link JsonObject}
     * </p>
     *
     * @param orgId      Represents the organization id
     * @param jsonObject Represent the followers {@link JsonObject}
     * @return The {@link SourceFollowers}
     */
    private SourceFollowers build(final long orgId, final JsonObject jsonObject) {
        final SourceFollowers sourceFollowers = new SourceFollowers();
        final Collection<Follower> followers = new HashSet<>();

        sourceFollowers.setOrgId(orgId);
        sourceFollowers.setFollowers(followers);
        sourceFollowers.setEntityId(jsonObject.getLong(SourceFollowersFields.ENTITY_ID));
        sourceFollowers.setSource(Source.get(jsonObject.getString(SourceFollowersFields.SOURCE)));

        return sourceFollowers;
    }

    /**
     * <p>
     * Validates the provided {@link SourceFollowers} to ensure it contains valid information
     * </p>
     *
     * @param sourceFollowers Represents the {@link SourceFollowers}
     */
    private void validate(final SourceFollowers sourceFollowers) {
        final long orgId = sourceFollowers.getOrgId();
        final Long entityId = sourceFollowers.getEntityId();
        final Set<Long> followerIds = sourceFollowers.getFollowers().stream().map(Follower::getUserId).collect(Collectors.toSet());
        final Collection<Long> entityIds = List.of(entityId);

        if (!followerIds.isEmpty() && !crmUserManager.exists(orgId, followerIds)) {
            throw ErrorCode.get(SourceFollowersErrorCode.CRM_USERS_NOT_FOUND, "i18n.source.followers.crm.users.not.found");
        }

        if (!((EntityConfigManager<?>) ConfigManagerHolder.getInstance().getManager(sourceFollowers.getSource())).exists(orgId, entityIds)) {
            throw ErrorCode.get(SourceFollowersErrorCode.ENTITY_NOT_FOUND, "i18n.source.followers.entity.not.found");
        }
    }

    /**
     * <p>
     * Fetches a list of followers for a specific entity within an organization
     * </p>
     * <p>
     *
     * @param crmRequest  Represents the {@link CrmRequest}
     * @param crmResponse Represents the {@link CrmResponse}
     */
    private void fetchAndDispatchFollowers(final CrmRequest crmRequest, final CrmResponse crmResponse) {
        final long orgId = crmRequest.orgId();
        final JsonObject data = crmRequest.getRequestJson().getJsonObject(SourceFollowersFields.DATA);
        final int start = Math.max(data.optInt(SourceFollowersFields.START, DEFAULT_START_VALUE), DEFAULT_START_VALUE);
        final int limit = Math.min(data.optInt(SourceFollowersFields.LIMIT, DEFAULT_LIMIT), DEFAULT_LIMIT);
        final Source source = Source.get(data.getString(SourceFollowersFields.SOURCE));
        final long entityId = data.getLong(SourceFollowersFields.ENTITY_ID);
        final long totalRecords = sourceFollowersManager.totalRecords(orgId, entityId, source);
        final JsonArray jsonArray = Json.array();

        if (0 == totalRecords) {
            crmResponse.put(SourceFollowersFields.TOTAL_RECORDS, totalRecords);
            crmResponse.put(SourceFollowersFields.HAS_MORE, (start + limit) <= totalRecords);
            crmResponse.put(SourceFollowersFields.LIST, jsonArray);
        } else {
            final SourceFollowers sourceFollowers = sourceFollowersManager.fetchSourceFollowers(orgId, entityId, source, start, limit).orElseGet(() -> null);
            final Collection<Follower> followers = Objects.isNull(sourceFollowers) ? Collections.emptyList() : sourceFollowers.getFollowers();
            final Map<Long, Follower> followersMap = followers.stream().collect(Collectors.toMap(Follower::getUserId, Function.identity()));
            final Set<Long> iamUserIds = crmUserService.getUsers(orgId, followersMap.keySet()).stream().map(CrmUser::getIamId).collect(Collectors.toSet());

            crmResponse.convertIntoAsync();
            final EventCallback<Collection<User>> usersFetchCallBack = new EventCallback<>() {

                @Override
                public void onSuccess(final Collection<User> users) {

                    try {
                        final SubCategory.Type subCategory = getSubCategory(source);
                        final Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::id, Function.identity()));
                        final Collection<CrmUser> crmUsers = crmUserService.getUsersByIamId(orgId, userMap.keySet());
                        final List<Follower> followers = new ArrayList<>();

                        for (final CrmUser crmUser : crmUsers) {
                            final Follower follower = followersMap.get(crmUser.id());

                            crmUser.setUser(userMap.get(crmUser.getIamId()));
                            follower.setCrmUser(crmUser);
                            follower.setPermissions(rolePermissionService.getPermissions(orgId, follower.getCrmUser().getRoleId(), Category.Type.FOLLOWERS, subCategory).stream().filter(rolePermission -> rolePermission.getAction().getType().isCrudAction()).toList());

                            followers.add(follower);
                        }
                        final String sortKey = CrmUserFields.SORT.getName();
                        final String thumbnailKey = FieldKeys.THUMBNAIL.getName();

                        followers.sort(data.containsKey(sortKey) ? createComparator(data.getJsonObject(sortKey), crmRequest.locale())
                                : Comparator.comparing(Follower::getAddedAt));
                        for (final Follower follower : followers) {
                            final JsonObject followersJson = convertUserToJson(follower);
                            final CrmUser crmUser = follower.getCrmUser();
                            final User user = userMap.get(crmUser.getIamId());
                            final DisplayPicture thumbnail = user.getThumbnail();

                            if (Objects.nonNull(thumbnail)) {
                                followersJson.put(thumbnailKey, crmUserConverter.fetchProfileJson(thumbnail, orgId, crmUser.id()));
                            }
                            jsonArray.put(followersJson);
                        }
                        crmResponse.put(SourceFollowersFields.TOTAL_RECORDS, totalRecords);
                        crmResponse.put(SourceFollowersFields.HAS_MORE, (start + limit) <= totalRecords);
                        crmResponse.put(SourceFollowersFields.LIST, jsonArray);
                    } catch (final Throwable throwable) {
                        LOGGER.error("Source Followers: Failed to followers retrieved data. Reason: {}, Org Id: {}", throwable.getMessage(), orgId);
                        crmResponse.setErrorCode(ErrorCode.get(SourceFollowersErrorCode.FOLLOWERS_RETRIEVAL_ACTION_FAILED, "i18n.source.followers.retrieval.action.failed"));
                    } finally {
                        crmResponse.send();
                    }
                }

                @Override
                public void onFailure(final Throwable throwable) {
                    crmResponse.setErrorCode(ErrorCode.get(IamErrorCodes.USER_RETRIEVAL_FAILED, "i18n.common.user.retrieval.failed", throwable));
                    crmResponse.send();
                }
            };
            userService.fetchUsers(organizationService.getIamOrgId(orgId), iamUserIds, usersFetchCallBack);
        }
    }

    /**
     * <p>
     * Fetches and dispatch the teams and users based on the given criteria
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param userId      Represents an user id
     * @param data        Holds the {@link JsonObject} details
     * @param crmResponse Represents the {@link CrmResponse} details
     */
    private void fetchAndDispatchTeamsAndUsers(final long orgId, final long userId, final JsonObject data, final CrmResponse crmResponse) {
        final String criteriaKey = SourceFollowersFields.CRITERIA;
        final int start = Math.max(data.optInt(SourceFollowersFields.START, DEFAULT_START_VALUE), DEFAULT_START_VALUE);
        final int limit = Math.min(data.optInt(SourceFollowersFields.LIMIT, DEFAULT_LIMIT), DEFAULT_LIMIT);
        final Source source = Source.get(data.getString(SourceFollowersFields.SOURCE));
        final SubCategory.Type subCategoryType = getSubCategory(source);
        final Long entityId = data.getNullableValue(SourceFollowersFields.ENTITY_ID);
        final SearchFilter nameSearchFilter = new SearchFilter();
        final SearchFilter stateSearchFilter = new SearchFilter();
        final Collection<SearchFilter> searchFilters = new HashSet<>();
        final Collection<Long> excludeUserIds = new HashSet<>();

        if (Objects.nonNull(entityId)) {
            final CrmUser crmUser = ((EntityConfigManager<?>) ConfigManagerHolder.getInstance().getManager(source))
                    .getSalesOwner(orgId, entityId).orElseGet(() -> null);

            if (Objects.nonNull(crmUser)) {
                excludeUserIds.add(crmUser.id());
            }
        }
        excludeUserIds.add(userId);
        excludeUserIds.addAll(getRestrictedUserIds(orgId, getRestrictedFollowerRoleIds(orgId, subCategoryType)));
        final Set<Long> excludeIamUserIds = crmUserService.getUsers(orgId, excludeUserIds).stream().map(CrmUser::getIamId).collect(Collectors.toSet());

        if (data.containsKey(criteriaKey)) {
            final JsonObject criteriaJson = data.getJsonObject(criteriaKey);

            nameSearchFilter.setType(criteriaJson.getString(SourceFollowersFields.TYPE));
            nameSearchFilter.setValue(criteriaJson.get(SourceFollowersFields.VALUE));
        }
        nameSearchFilter.setLimit(limit);
        nameSearchFilter.setStart(start);
        nameSearchFilter.setExcludeUserIds(excludeIamUserIds);
        stateSearchFilter.setType(SourceFollowersFields.STATE);
        stateSearchFilter.setValue(UserStateFilter.ACTIVE);
        searchFilters.add(stateSearchFilter);
        searchFilters.add(nameSearchFilter);

        crmResponse.convertIntoAsync();

        final EventCallback<Page<User>> usersFetchCallBack = new EventCallback<>() {

            @Override
            public void onSuccess(final Page<User> userPage) {
                final int totalRecords = userPage.getNumber();
                final Map<Long, User> userMap = userPage.getData().stream().collect(Collectors.toMap(User::id, Function.identity()));
                final SubCategory.Type subCategory = getSubCategory(source);

                if (totalRecords < limit) {
                    fetchAndDispatchTeamUsers(orgId, nameSearchFilter, excludeIamUserIds, subCategory, userMap, crmResponse);
                } else {

                    try {
                        final Collection<CrmUser> crmUsers = secureCrmUserManager.getUsersByIamId(orgId, userMap.keySet());

                        crmResponse.put(SourceFollowersFields.USERS, buildFollowersArray(orgId, crmUsers, userMap, subCategory));
                        crmResponse.put(SourceFollowersFields.TOTAL_RECORDS, totalRecords);
                        crmResponse.put(SourceFollowersFields.HAS_MORE, (start + limit) <= totalRecords);
                    } catch (final Throwable throwable) {
                        LOGGER.error("Source Followers: Failed to followers retrieved data. Reason: {}, Org Id: {}", throwable.getMessage(), orgId);
                        crmResponse.setErrorCode(ErrorCode.get(SourceFollowersErrorCode.FOLLOWERS_RETRIEVAL_ACTION_FAILED, "i18n.source.followers.retrieval.action.failed"));
                    } finally {
                        crmResponse.send();
                    }
                }
            }

            @Override
            public void onFailure(final Throwable cause) {
                crmResponse.setErrorCode(ErrorCode.get(IamErrorCodes.USER_RETRIEVAL_FAILED, "i18n.common.user.retrieval.failed"));
                crmResponse.send();
            }
        };
        userService.fetchUserPage(false, organizationService.getIamOrgId(orgId), searchFilters, usersFetchCallBack);
    }

    /**
     * <p>
     * Fetches and dispatches team users
     * </p>
     *
     * @param orgId             Represents the organization id
     * @param searchFilter      Represents the {@link SearchFilter}
     * @param excludeIamUserIds Which contains the collection of exclude iam user ids
     * @param subCategory       Represents the {@link SubCategory.Type}
     * @param userMap           A map associating user IDs with {@link User}
     * @param crmResponse       Represents the {@link CrmResponse}
     */
    private void fetchAndDispatchTeamUsers(final long orgId, final SearchFilter searchFilter, final Collection<Long> excludeIamUserIds,
                                           final SubCategory.Type subCategory, final Map<Long, User> userMap, final CrmResponse crmResponse) {
        final Collection<Long> iamUserIds = userMap.keySet();
        final int usersSize = iamUserIds.size();
        final int start = searchFilter.getStart();
        final int limit = searchFilter.getLimit();
        final int requiredSize = start + limit - 1;

        if (usersSize < requiredSize) {
            final long teamSize = teamManager.getTeamsCount(orgId, searchFilter);
            final int totalRecords = Math.toIntExact(teamSize + usersSize);
            final boolean hasMore = (start + limit) <= totalRecords;
            final int teamStart = usersSize == 0 ? start : start > usersSize ? start - usersSize : DEFAULT_START_VALUE;
            final int teamLimit = requiredSize - usersSize;

            if (teamStart <= teamSize) {
                searchFilter.setStart(teamStart);
                searchFilter.setLimit(teamLimit);
                final Collection<Team> teams = teamManager.getTeams(orgId, searchFilter);
                final Collection<Long> teamUserIds = new ArrayList<>();

                for (final Team team : teams) {
                    teamUserIds.addAll(team.getUsers().stream().map(TeamUser::getCrmUser).map(CrmUser::getIamId).toList());
                }
                final Set<Long> excludeUserIds = new HashSet<>(excludeIamUserIds);

                excludeUserIds.addAll(iamUserIds);
                teamUserIds.removeIf(excludeUserIds::contains);

                if (!teamUserIds.isEmpty()) {
                    final SearchFilter userSearchFilter = new SearchFilter();

                    userSearchFilter.setUserIds(teamUserIds);
                    userSearchFilter.setType(SourceFollowersFields.STATE);
                    userSearchFilter.setValue(UserStateFilter.ACTIVE);
                    userService.fetchUsers(false, organizationService.getIamOrgId(orgId), userSearchFilter, new EventCallback<>() {

                        @Override
                        public void onSuccess(final Collection<User> users) {
                            users.forEach(user -> userMap.put(user.id(), user));
                            loadAndDispatchTeamsAndUsersResponse(orgId, userMap, teams, subCategory, crmResponse, totalRecords, hasMore);
                        }

                        @Override
                        public void onFailure(final Throwable cause) {
                            crmResponse.setErrorCode(ErrorCode.get(IamErrorCodes.USER_RETRIEVAL_FAILED, "i18n.common.user.retrieval.failed"));
                            crmResponse.send();
                        }
                    });
                } else {
                    loadAndDispatchTeamsAndUsersResponse(orgId, userMap, teams, subCategory, crmResponse, totalRecords, hasMore);
                }
            } else {
                loadAndDispatchTeamsAndUsersResponse(orgId, userMap, Collections.emptyList(), subCategory, crmResponse, totalRecords, hasMore);
            }
        }
    }

    /**
     * <p>
     * Loads the users and teams into {@link CrmResponse}
     * </p>
     *
     * @param orgId        Represents the organization id
     * @param userMap      A map associating user IDs with {@link User}
     * @param teams        Which contains the collections of {@link Team}
     * @param subCategory  Represents the {@link SubCategory.Type}
     * @param crmResponse  The {@link CrmResponse} to populate with the data
     * @param totalRecords The total number of records to include in the response
     * @param hasMore      A flag indicating whether there are more records to retrieve
     */
    private void loadAndDispatchTeamsAndUsersResponse(final long orgId, final Map<Long, User> userMap, final Collection<Team> teams,
                                                      final SubCategory.Type subCategory, final CrmResponse crmResponse,
                                                      final int totalRecords, final boolean hasMore) {
        try {
            final Set<Long> iamUserIds = userMap.keySet();
            final Collection<CrmUser> crmUsers = iamUserIds.isEmpty() ? Collections.emptyList() : secureCrmUserManager.getUsersByIamId(orgId, iamUserIds);

            crmResponse.put(SourceFollowersFields.USERS, buildFollowersArray(orgId, crmUsers, userMap, subCategory));
            crmResponse.put(SourceFollowersFields.TEAMS, constructTeamJson(orgId, teams, userMap, subCategory));
            crmResponse.put(SourceFollowersFields.TOTAL_RECORDS, totalRecords);
            crmResponse.put(SourceFollowersFields.HAS_MORE, hasMore);
        } catch (final Throwable throwable) {
            LOGGER.error("Source Followers: Failed to followers retrieved data. Reason: {}, Org Id: {}", throwable.getMessage(), orgId);
            crmResponse.setErrorCode(ErrorCode.get(SourceFollowersErrorCode.FOLLOWERS_RETRIEVAL_ACTION_FAILED, "i18n.source.followers.retrieval.action.failed"));
        } finally {
            crmResponse.send();
        }
    }

    /**
     * <p>
     * Constructs a {@link JsonArray} representing a collection of teams
     * </p>
     *
     * @param orgId       Represents the organization id
     * @param teams       Which contains the collections of {@link Team}
     * @param userMap     A {@link Map} associating user IDs with {@link User}
     * @param subCategory Represents the {@link SubCategory.Type}
     * @return The teams {@link JsonArray}
     */
    private JsonArray constructTeamJson(final long orgId, final Collection<Team> teams, final Map<Long, User> userMap, final SubCategory.Type subCategory) {
        final JsonArray jsonArray = Json.array();

        for (final Team team : teams) {
            final Collection<CrmUser> crmUsers = team.getUsers().stream().map(TeamUser::getCrmUser).toList();
            final JsonArray userArray = buildFollowersArray(orgId, crmUsers, userMap, subCategory);

            if (userArray.size() > 0) {
                final JsonObject teamJson = teamConverter.convertTeam(team);

                teamJson.put(SourceFollowersFields.USERS, userArray);
                jsonArray.put(teamJson);
            }
        }

        return jsonArray;
    }

    /**
     * <p>
     * Builds the follower {@link JsonArray}
     * </p>
     *
     * @param orgId       Represents the organization id
     * @param crmUsers    Which contain the collections of {@link CrmUser}
     * @param userMap     A {@link Map} associating user IDs with {@link User}
     * @param subCategory Represents the {@link SubCategory.Type}
     * @return The follower {@link JsonArray}
     */
    private JsonArray buildFollowersArray(final long orgId, final Collection<CrmUser> crmUsers,
                                          final Map<Long, User> userMap, final SubCategory.Type subCategory) {
        final JsonArray userJsonArray = Json.array();
        final String thumbnailKey = FieldKeys.THUMBNAIL.getName();

        for (final CrmUser crmUser : crmUsers) {
            final Long iamUserId = crmUser.getIamId();

            if (userMap.containsKey(iamUserId)) {
                final Follower follower = new Follower(crmUser.id());
                final User user = userMap.get(iamUserId);
                final DisplayPicture thumbnail = user.getThumbnail();

                follower.setCrmUser(crmUser);
                follower.setPermissions(rolePermissionService.getPermissions(orgId, follower.getCrmUser().getRoleId(), Category.Type.FOLLOWERS, subCategory).stream().filter(rolePermission -> rolePermission.getAction().getType().isCrudAction()).toList());
                crmUser.setUser(user);
                final JsonObject followersJson = convertUserToJson(follower);

                if (Objects.nonNull(thumbnail)) {
                    followersJson.put(thumbnailKey, crmUserConverter.fetchProfileJson(thumbnail, orgId, crmUser.id()));
                }
                userJsonArray.put(followersJson);
            }
        }
        return userJsonArray;
    }

    /**
     * <p>
     * Fetches the {@link SubCategory.Type} based on {@link Source}
     * </p>
     *
     * @param source Represents the {@link Source}
     * @return The {@link SubCategory.Type}
     */
    private SubCategory.Type getSubCategory(final Source source) {
        return switch (source) {
            case CONTACT -> SubCategory.Type.CONTACT_FOLLOWERS_RECORDS;
            case COMPANY -> SubCategory.Type.COMPANY_FOLLOWERS_RECORDS;
            case DEAL -> SubCategory.Type.DEAL_FOLLOWERS_RECORDS;
            default -> throw ErrorCode.get(SourceFollowersErrorCode.INVALID_SOURCE, "i18n.source.followers.invalid.source");
        };
    }

    /**
     * <p>
     * Fetches the set of role IDs that do not have ACCESS permission for the given subcategory
     * </p>
     *
     * @param orgId           Represents the organization id
     * @param subCategoryType Represents the {@link SubCategory.Type}
     * @return A set of role IDs that are restricted (do not have ACCESS permission)
     */
    private Set<Long> getRestrictedFollowerRoleIds(final long orgId, final SubCategory.Type subCategoryType) {
        final Set<Long> roleIds = roleManager.getRoleIds(orgId);
        final Set<Long> restrictedFollowerRoleIds = new HashSet<>(roleIds.size());

        for (final RolePermissions rolePermissions : rolePermissionService.getPermissions(orgId, roleIds)) {

            for (final RolePermission permission : rolePermissions.getPermissions()) {
                final SubCategory.Type subCategory = permission.getSubCategory().getType();
                final Long roleId = rolePermissions.getRoleId();

                if (!restrictedFollowerRoleIds.contains(roleId) && subCategory.hasPrimaryAction() && subCategoryType == subCategory
                        && Action.Type.ACCESS == permission.getAction().getType() && !permission.isGranted()) {
                    restrictedFollowerRoleIds.add(roleId);
                }
            }
        }

        return restrictedFollowerRoleIds;
    }

    /**
     * <p>
     * Fetches the set of user IDs that belong to restricted roles
     * </p>
     *
     * @param orgId                     Represents the organization id
     * @param restrictedFollowerRoleIds A set of role IDs that are restricted
     * @return A set of user IDs that belong to restricted roles
     */
    private Set<Long> getRestrictedUserIds(final long orgId, final Set<Long> restrictedFollowerRoleIds) {
        if (restrictedFollowerRoleIds.isEmpty()) {
            return Collections.emptySet();
        }
        final Map<Long, Collection<Long>> roleUserIds = roleManager.getUserIds(orgId, restrictedFollowerRoleIds);

        if (roleUserIds.isEmpty()) {
            return Collections.emptySet();
        }

        return roleUserIds.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
    }

    /**
     * <p>
     * Converts user and role information into {@link JsonObject} for a given organization id and {@link SubCategory.Type}
     * </p>
     *
     * @param follower Represents the {@link Follower}
     * @return A {@link JsonObject} containing the user's name, role, and permissions for the specified {@link SubCategory.Type}.
     */
    private JsonObject convertUserToJson(final Follower follower) {
        final JsonObject jsonObject = Json.object();
        final CrmUser crmUser = follower.getCrmUser();
        final CrmRole crmUserRole = crmUser.getRole();
        final JsonObject userJson = Json.object();
        final JsonObject roleJson = Json.object();

        userJson.put(SourceFollowersFields.NAME, crmUser.getFullName());
        userJson.put(SourceFollowersFields.ID, follower.getUserId());

        roleJson.put(SourceFollowersFields.NAME, crmUserRole.getName());
        roleJson.put(SourceFollowersFields.ID, crmUserRole.id());
        roleJson.put(SourceFollowersFields.IS_RENAMABLE, crmUserRole.getConfig().isRenamable());

        jsonObject.put(SourceFollowersFields.USER, userJson);
        jsonObject.put(SourceFollowersFields.ROLE, roleJson);

        jsonObject.put(SourceFollowersFields.ADDED_AT, follower.getAddedAt());
        jsonObject.put(SourceFollowersFields.PERMISSIONS_ON_THIS_SOURCE, buildPermissionsString(follower.getPermissions()));

        return jsonObject;
    }

    /**
     * <p>
     * Builds the followers permissions based on action type
     * </p>
     *
     * @param permissions Which contains the collection of {@link RolePermission}
     * @return A string representing the granted permissions
     */
    private String buildPermissionsString(final Collection<RolePermission> permissions) {
        final Collection<Action.Type> grantedActions = permissions.stream()
                .filter(RolePermission::isGranted).map(rolePermission -> rolePermission.getAction().getType()).toList();
        final List<Action.Type> orderedActions = List.of(Action.Type.VIEW, Action.Type.EDIT, Action.Type.DELETE);
        final StringJoiner actionPermissions = new StringJoiner(COMMA, SourceFollowersFields.CAN, EMPTY_STRING);

        orderedActions.forEach(action -> {
            if (grantedActions.contains(action)) {
                actionPermissions.add(action.getName());
            }
        });

        return grantedActions.isEmpty() ? null : actionPermissions.toString();
    }

    /**
     * <p>
     * Creates a {@link Comparator} for sorting {@link Follower} based on a specified field and sorting order.
     * </p>
     *
     * @param jsonObject Represents the {@link JsonObject}
     * @param locale     Represents the {@link Locale}
     * @return A Comparator for sorting {@link Follower}
     */
    private Comparator<Follower> createComparator(final JsonObject jsonObject, final Locale locale) {
        final SortType ascendingType = SortType.ASCENDING;
        final String field = jsonObject.getString(SourceFollowersFields.FIELD);
        final SortType type = SortType.getSortType(jsonObject.getString(SourceFollowersFields.TYPE));
        final Supplier<Collator> collator = () -> Collator.getInstance(locale);

        return switch (field) {
            case SourceFollowersFields.USER -> {
                final Comparator<Follower> comparator = Comparator.comparing(Follower::getCrmUser, Comparator.nullsLast(
                        Comparator.comparing(CrmUser::getFullName, Comparator.nullsLast(collator.get()))));

                yield ascendingType.equals(type) ? comparator : comparator.reversed();
            }
            case SourceFollowersFields.PERMISSIONS_ON_THIS_SOURCE -> {
                final Comparator<Follower> comparator = (firstFollower, secondFollower) -> {
                    final List<RolePermission> firstPermissions = new ArrayList<>(firstFollower.getPermissions().stream().filter(RolePermission::isGranted).toList());
                    final List<RolePermission> secondPermissions = new ArrayList<>(secondFollower.getPermissions().stream().filter(RolePermission::isGranted).toList());

                    firstFollower.setPermissions(firstPermissions);
                    secondFollower.setPermissions(secondPermissions);
                    firstPermissions.sort((firstRolePermission, secondRolePermission) ->
                            Integer.compare(mapActionTypeToOrder(firstRolePermission.getAction().getType()),
                                    mapActionTypeToOrder(secondRolePermission.getAction().getType())));

                    secondPermissions.sort((firstRolePermission, secondRolePermission) ->
                            Integer.compare(mapActionTypeToOrder(firstRolePermission.getAction().getType()),
                                    mapActionTypeToOrder(secondRolePermission.getAction().getType())));

                    final int minSize = Math.min(firstPermissions.size(), secondPermissions.size());

                    for (int index = 0; index < minSize; index++) {
                        final int firstPriority = mapActionTypeToOrder(firstPermissions.get(index).getAction().getType());
                        final int secondPriority = mapActionTypeToOrder(secondPermissions.get(index).getAction().getType());
                        final int comparisonResult = Integer.compare(firstPriority, secondPriority);

                        if (comparisonResult != 0) {
                            return comparisonResult;
                        }
                    }

                    return Integer.compare(firstPermissions.size(), secondPermissions.size());
                };

                yield ascendingType.equals(type) ? comparator : comparator.reversed();
            }
            case SourceFollowersFields.ADDED_AT -> {
                final Comparator<Follower> comparator = Comparator.comparing(Follower::getAddedAt);

                yield ascendingType.equals(type) ? comparator : comparator.reversed();
            }
            case SourceFollowersFields.ROLE -> {
                final Comparator<Follower> comparator = Comparator.comparing(Follower::getCrmUser, Comparator.nullsLast(
                        Comparator.comparing(CrmUser::getRole, Comparator.nullsLast(Comparator.comparing(CrmRole::getName, Comparator.nullsLast(collator.get()))))));

                yield ascendingType.equals(type) ? comparator : comparator.reversed();
            }
            default ->
                    throw ErrorCode.get(SourceFollowersErrorCode.FIELD_IS_INVALID, "i18n.source.followers.field.is.invalid");
        };
    }

    /**
     * <p>
     * Maps an {@link Action.Type} to a corresponding order value
     * </p>
     *
     * @param type Represents the {@link Action.Type}
     * @return An integer representing the order of the {@link Action.Type}
     */
    private int mapActionTypeToOrder(final Action.Type type) {
        return switch (type) {
            case VIEW -> 1;
            case EDIT -> 2;
            case DELETE -> 3;
            default -> 0;
        };
    }

    /**
     * <p>
     * Updates followers in search.
     * </p>
     *
     * @param orgId       Organization's id.
     * @param id          Entity id.
     * @param source      {@link Source}.
     * @param followerIds Followers ids/.
     */
    private void updateFollowersInSearch(final Long orgId, final Long id, final Source source, final Collection<Long> followerIds) {
        final IndexManager<?> indexManager = switch (source) {
            case COMPANY -> CompanyIndexManager.getInstance();
            case CONTACT -> ContactIndexManager.getInstance();
            case DEAL -> DealIndexManager.getInstance();
            default -> null;
        };

        if (Objects.nonNull(indexManager)) {
            indexManager.updateFollowerIds(orgId, id, followerIds);
        }
    }

    /**
     * <p>
     * Updates the followers for a given source followers by adding and removing the followers based on the given follower ids
     * </p>
     *
     * @param orgId       Represents the organization id
     * @param userId      Represents the user id
     * @param data        Holds the {@link JsonObject} details
     * @param crmResponse Represents the {@link CrmResponse} details
     */
    private void update(final long orgId, final long userId, final JsonObject data, final CrmResponse crmResponse) {
        final SourceFollowers sourceFollowers = build(orgId, data);
        final Collection<Long> addedFollowerIds = extractIds(data, SourceFollowersFields.ADDED_FOLLOWER_IDS);
        final Collection<Long> removedFollowerIds = extractIds(data, SourceFollowersFields.REMOVED_FOLLOWER_IDS);

        sourceFollowers.setOwnerId(userId);
        validate(sourceFollowers);

        sourceFollowersManager.update(sourceFollowers, addedFollowerIds, removedFollowerIds);

        final Collection<Long> currentFollowerIds = sourceFollowersManager.getFollowerIdsByEntityId(orgId, sourceFollowers.getEntityId(), sourceFollowers.getSource());

        updateFollowersInSearch(orgId, sourceFollowers.getEntityId(), sourceFollowers.getSource(), currentFollowerIds);
        crmResponse.put(SourceFollowersFields.MESSAGE, crmResponse.resolveI18nString("i18n.source.followers.saved.successfully"));
    }

    /**
     * <p>
     * Extracts a set of IDs from a {@link JsonObject} based on a specified key
     * </p>
     *
     * @param data The {@link JsonObject} containing the data
     * @param key  The key to look for in the {@link JsonObject}
     * @return A collection of IDs extracted from the {@link JsonObject}, or an empty set if the key is not present or does not contain a valid array
     */
    private Collection<Long> extractIds(final JsonObject data, final String key) {

        if (data.containsKey(key)) {
            final JsonArray jsonArray = data.getJsonArray(key);
            final int size = jsonArray.size();
            final Collection<Long> ids = new HashSet<>(size);

            for (int index = 0; index < size; index++) {
                ids.add(jsonArray.getLong(index));
            }
            return ids;
        }
        return Collections.emptySet();
    }
}
