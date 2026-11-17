package com.tlc.crm.core.features.action.followers.user;

import com.tlc.attachment.model.profilePicture.DisplayPicture;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.AuthorizationController;
import com.tlc.crm.common.fields.ActionFields;
import com.tlc.crm.common.iam.api.manager.CrmUserManager;
import com.tlc.crm.common.iam.api.manager.RoleManager;
import com.tlc.crm.common.iam.api.manager.TeamManager;
import com.tlc.crm.common.iam.api.model.CrmRole;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.Team;
import com.tlc.crm.common.iam.api.model.TeamUser;
import com.tlc.crm.common.iam.api.model.converter.CrmUserConverter;
import com.tlc.crm.common.iam.api.model.converter.TeamConverter;
import com.tlc.crm.common.iam.field.CrmUserFields;
import com.tlc.crm.common.iam.internal.IamErrorCodes;
import com.tlc.crm.common.iam.service.OrganizationService;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.common.models.CriteriaDetails;
import com.tlc.crm.common.models.CriteriaDetailsHolder;
import com.tlc.crm.common.models.LimitConfig;
import com.tlc.sql.common.api.dml.Operator;
import com.tlc.crm.core.features.field.follower.UserFollowerFields;
import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.entity.followers.records.UserFollowerUpdateRecords;
import com.tlc.crm.core.features.api.followers.source.manager.SourceFollowersManager;
import com.tlc.crm.core.features.api.followers.user.manager.UserFollowerManager;
import com.tlc.crm.core.features.api.followers.model.Follower;
import com.tlc.crm.core.features.internal.followers.status.SourceFollowersErrorCode;
import com.tlc.crm.core.features.internal.followers.status.UserFollowerErrorCode;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.service.EntityBulkActionPublisher;
import com.tlc.crm.entity.store.AsyncBulkActionTopicStore;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.model.RolePermissions;
import com.tlc.crm.security.permission.service.RolePermissionService;
import com.tlc.iam.client.UserService;
import com.tlc.iam.client.dropdown.SearchFilter;
import com.tlc.queue.config.SubscriptionTopic;
import com.tlc.validator.ErrorKeys;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;
import com.tlc.web.WebAction;
import com.tlc.web.client.event.EventCallback;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Optional;
import java.util.Base64;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.time.Instant;

/**
 * Manages user follower related web actions
 *
 * @author Ryan
 * @version 1.0
 */
@WebAction(path = "/user/follower/mgmt")
public class UserFollowerManagement extends AuthorizationController {

    private static final int DEFAULT_START_VALUE = 1;
    private static final int DEFAULT_LIMIT = 20;
    private static final Collection<Source> ENTITY_SOURCES = List.of(Source.CONTACT, Source.COMPANY, Source.DEAL);

    private final UserFollowerManager userFollowerManager;
    private final SourceFollowersManager sourceFollowersManager;
    private final EntityBulkActionPublisher entityBulkActionPublisher;
    private final AsyncBulkActionTopicStore asyncBulkActionTopicStore;
    private final CrmUserManager secureCrmUserManager;
    private final TeamManager teamManager;
    private final UserService userService;
    private final OrganizationService organizationService;
    private final com.tlc.crm.common.iam.service.UserService crmUserService;
    private final TeamConverter teamConverter;
    private final CrmUserConverter crmUserConverter;
    private final RolePermissionService rolePermissionService;
    private final RoleManager roleManager;
    private final ContactManager contactManager;
    private final CompanyManager companyManager;
    private final DealManager dealManager;

    public UserFollowerManagement() {
        userFollowerManager = UserFollowerManager.getInstance();
        sourceFollowersManager = SourceFollowersManager.getInstance();
        entityBulkActionPublisher = EntityBulkActionPublisher.getInstance();
        asyncBulkActionTopicStore = AsyncBulkActionTopicStore.getInstance();
        secureCrmUserManager = CrmUserManager.getSecureInstance();
        teamManager = TeamManager.getInstance();
        userService = UserService.getInstance();
        organizationService = OrganizationService.getInstance();
        crmUserService = com.tlc.crm.common.iam.service.UserService.getInstance();
        teamConverter = TeamConverter.getInstance();
        crmUserConverter = CrmUserConverter.getInstance();
        rolePermissionService = RolePermissionService.getInstance();
        roleManager = RoleManager.getInstance();
        contactManager = ContactManager.getInstance();
        companyManager = CompanyManager.getInstance();
        dealManager = DealManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param crmRequest  Represents the {@link CrmRequest}
     * @param crmResponse Represents the {@link CrmResponse}
     */
    @Override
    protected void authorizedProcess(final CrmRequest crmRequest, final CrmResponse crmResponse) throws Exception {
        final JsonObject requestJson = crmRequest.getRequestJson();
        final JsonObject data = requestJson.getJsonObject(UserFollowerFields.DATA);

        switch (requestJson.getString(UserFollowerFields.TYPE)) {
            case UserFollowerFields.UPDATE_FOLLOWERS ->
                    update(crmRequest.orgId(), crmRequest.userId(), data, crmResponse);
            case UserFollowerFields.BULK_UPDATE_FOLLOWERS -> bUpdateFollowers(crmRequest, crmResponse);
            case UserFollowerFields.GET_TEAMS_AND_USERS_LIST ->
                    fetchAndDispatchTeamsAndUsers(crmRequest.orgId(), crmRequest.userId(), data, crmResponse);
            case UserFollowerFields.GET_FOLLOWERS ->
                    fetchAndDispatchFollowers(crmRequest.orgId(), crmRequest.userId(), data, crmResponse);
        }
    }

    /**
     * Updates the followers for the requesting sales owner by adding and removing follower ids
     *
     * @param orgId       Represents the organization id
     * @param userId      Represents the user id
     * @param data        Holds the request data
     * @param crmResponse Represents the {@link CrmResponse}
     */
    private void update(final long orgId, final long userId, final JsonObject data, final CrmResponse crmResponse) {
        final Collection<Long> addedFollowerIds = extractIds(data, UserFollowerFields.ADDED_FOLLOWER_IDS);
        final Collection<Long> removedFollowerIds = extractIds(data, UserFollowerFields.REMOVED_FOLLOWER_IDS);

        validateUserFollower(orgId, userId, addedFollowerIds, removedFollowerIds, crmResponse);
        addedFollowerIds.remove(userId);
        removedFollowerIds.remove(userId);
        userFollowerManager.update(orgId, userId, addedFollowerIds, removedFollowerIds);
        crmResponse.put(ActionFields.MESSAGE, "User followers updated successfully");
    }

    /**
     * Publishes owned entity follower updates using the same {@code bUpdate()} flow as {@code bAssignFollowers}.
     * Selected sales owners are resolved from {@code isAllSelected}; entity ids are published to the
     * contact, company, and deal consumers with {@code addedFollowerIds} and {@code removedFollowerIds}.
     *
     * @param crmRequest  Represents the {@link CrmRequest}
     * @param crmResponse Represents the {@link CrmResponse}
     */
    private void bUpdateFollowers(final CrmRequest crmRequest, final CrmResponse crmResponse) {
        final long orgId = crmRequest.orgId();
        final long userId = crmRequest.userId();
        final JsonObject data = crmRequest.getRequestJson().getJsonObject(UserFollowerFields.DATA);
        final Collection<Long> addedFollowerIds = extractIds(data, UserFollowerFields.ADDED_FOLLOWER_IDS);
        final Collection<Long> removedFollowerIds = extractIds(data, UserFollowerFields.REMOVED_FOLLOWER_IDS);
        final Collection<Long> followerIds = new HashSet<>();

        validateUserFollower(orgId, userId, addedFollowerIds, removedFollowerIds, crmResponse);
        followerIds.addAll(addedFollowerIds);
        followerIds.addAll(removedFollowerIds);

        if (!followerIds.isEmpty()) {
            final Collection<CrmUser> users = crmUserService.getUsers(orgId, followerIds);

            if (users.size() != followerIds.size()) {
                throw ErrorCode.get(SourceFollowersErrorCode.CRM_USERS_NOT_FOUND, "i18n.source.followers.crm.users.not.found");
            }
        }

        updateUserFollowers(orgId, userId, addedFollowerIds, removedFollowerIds);
        updateFollowersInEntities(addedFollowerIds, removedFollowerIds, crmRequest, crmResponse);
    }

    /**
     *  Validates the user follower Request
     *
     * @param orgId              Represents the organization id
     * @param userId             Represents the selected user id
     * @param addedFollowerIds   Represents the follower ids to be added
     * @param removedFollowerIds Represents the follower ids to be removed
     * @param crmResponse        Represents the {@link CrmResponse}
     */
    private void validateUserFollower(final long orgId, final long userId, final Collection<Long> addedFollowerIds, final Collection<Long> removedFollowerIds, final CrmResponse crmResponse) {
        if (addedFollowerIds.size() > NumericalConstant.TWENTY_FIVE) {
            setFollowerLimitExceededError(crmResponse);
        }

        if (removedFollowerIds.stream().anyMatch(addedFollowerIds::contains)) {
            setDuplicateFollowerError(crmResponse);
        }

        validateCrmUser(orgId, userId, addedFollowerIds, removedFollowerIds);
    }

    /**
     * Updates {@code OMUserFollowers} for each selected sales owner
     *
     * @param orgId              Represents the organization id
     * @param userId             Represents the selected user id
     * @param addedFollowerIds   Represents the follower ids to be added
     * @param removedFollowerIds Represents the follower ids to be removed
     */
    private void updateUserFollowers(final long orgId, final Long userId,
                                     final Collection<Long> addedFollowerIds, final Collection<Long> removedFollowerIds) {
        final Collection<Long> addedIds = new HashSet<>(addedFollowerIds);
        final Collection<Long> removedIds = new HashSet<>(removedFollowerIds);

        addedIds.remove(userId);
        removedIds.remove(userId);

        if (!addedIds.isEmpty() || !removedIds.isEmpty()) {
            userFollowerManager.update(orgId, userId, addedIds, removedIds);
        }
    }

    /**
     * Publishes follower updates for entities owned by the selected sales owners using {@code getFilteredIds}
     *
     * @param crmRequest  Represents the {@link CrmRequest}
     * @param crmResponse Represents the {@link CrmResponse}
     */
    private void updateFollowersInEntities(final Collection<Long> addedFollowerIds, final Collection<Long> removedFollowerIds, final CrmRequest crmRequest, final CrmResponse crmResponse) {
        final long orgId = crmRequest.orgId();
        final long userId = crmRequest.userId();

        for (final Source source : ENTITY_SOURCES) {
            final JsonObject requestJson = crmRequest.getRequestJson();
            final Collection<Long> permittedFollowersForAddition = filterFollowersWithSourceAccess(orgId, addedFollowerIds, source);
            final Collection<Long> permittedFollowersForRemoval = filterFollowersWithSourceAccess(orgId, removedFollowerIds, source);
            final Map<Long, UserFollowerUpdateRecords> modelIds = sourceFollowersManager.getSourceFollowerChanges(orgId, userId,
                    permittedFollowersForAddition, permittedFollowersForRemoval, source);

            requestJson.getJsonObject("data").put(UserFollowerFields.FOLLOWER_CHANGES, toFollowerUpdatesJson(modelIds));

            final Optional<SubscriptionTopic> subscriptionTopicOptional = asyncBulkActionTopicStore.getSubscriptionTopic(getConfigActionName(source), Action.UPDATE);

            subscriptionTopicOptional.ifPresent(subscriptionTopic -> entityBulkActionPublisher.publish(orgId, userId, requestJson, modelIds.keySet(), subscriptionTopic));
        }

        final String requestId = Base64.getEncoder().encodeToString(String.format("%s:%s:%d", orgId, userId, Instant.now().toEpochMilli()).getBytes());

        crmResponse.put(ActionFields.ID, requestId);
        crmResponse.put(ActionFields.MESSAGE, "Processing bulk update operation");
    }

    /**
     * Converts the follower update records into a JSON array that contains
     * the entity id along with the followers to be added and removed.
     *
     * @param modelIds Represents the entity ids mapped to their follower update records
     * @return The follower updates as a JSON array
     */
    private JsonArray toFollowerUpdatesJson(final Map<Long, UserFollowerUpdateRecords> modelIds) {
        final JsonArray followerUpdatesArray = Json.array();

        modelIds.forEach((entityId, records) -> {
            final JsonObject entry = Json.object();

            entry.put(ActionFields.ID, entityId);
            entry.put(UserFollowerFields.ADDED_FOLLOWER_IDS, Json.array(records.followersToAdd()));
            entry.put(UserFollowerFields.REMOVED_FOLLOWER_IDS, Json.array(records.followersToRemove()));

            followerUpdatesArray.put(entry);
        });

        return followerUpdatesArray;
    }

    /**
     * Returns the configuration action name for the given source.
     *
     * @param source The {@link Source} enum value.
     * @return The action name used in async bulk config topics ("contact", "company", or "deal").
     * @throws ErrorCode Thrown when the provided source is invalid.
     */
    private String getConfigActionName(final Source source) {
        return switch (source) {
            case CONTACT -> "contact";
            case COMPANY -> "company";
            case DEAL -> "deal";
            default -> throw ErrorCode.get(SourceFollowersErrorCode.INVALID_SOURCE, "i18n.source.followers.invalid.source");
        };
    }

    /**
     * Sets the error response when the number of followers exceeds the allowed limit.
     *
     * @param crmResponse Represents the {@link CrmResponse} to populate with the limit error.
     */
    private void setFollowerLimitExceededError(final CrmResponse crmResponse) {
        final ErrorCode errorCode = ErrorCode.get(UserFollowerErrorCode.USER_FOLLOWER_LIMIT_REACHED, "i18n.source.followers.user.limit.exceeded");

        crmResponse.setErrorCode(errorCode);
        crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode, ErrorKeys.UPDATE_ACTION_FAILED)));
        crmResponse.send();
    }

    /**
     * Sets the error response when a duplicate follower is detected in the update request.
     *
     * @param crmResponse Represents the {@link CrmResponse} to populate with the duplicate error.
     */
    private void setDuplicateFollowerError(final CrmResponse crmResponse) {
        final ErrorCode errorCode = ErrorCode.get(SourceFollowersErrorCode.DUPLICATE_FOLLOWER_FOUND, "i18n.source.followers.duplicate.user.found");

        crmResponse.setErrorCode(errorCode);
        crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode, ErrorKeys.UPDATE_ACTION_FAILED)));
        crmResponse.send();
    }

    /**
     * Fetches and dispatches the followers for a specific user within an organization.
     *
     * @param orgId    Represents the organization id
     * @param userId   Represents the user id
     * @param data     Holds the request data
     * @param response Represents the {@link CrmResponse}
     */
    private void fetchAndDispatchFollowers(final long orgId, final long userId, final JsonObject data, final CrmResponse response) {
        final int start = Math.max(data.optInt(UserFollowerFields.START, DEFAULT_START_VALUE), DEFAULT_START_VALUE);
        final int limit = Math.min(data.optInt(UserFollowerFields.LIMIT, DEFAULT_LIMIT), DEFAULT_LIMIT);
        final long totalRecords = userFollowerManager.totalRecords(orgId, userId);
        final JsonArray followersJson = Json.array();

        if (totalRecords == 0) {
            response.put(UserFollowerFields.TOTAL_RECORDS, totalRecords);
            response.put(UserFollowerFields.HAS_MORE, start + limit <= totalRecords);
            response.put(UserFollowerFields.LIST, followersJson);

            return;
        }

        final Collection<Follower> followers = userFollowerManager.fetchFollowers(orgId, userId, new LimitConfig(start, limit));
        final Map<Long, Follower> followerMap = followers.stream()
                .collect(Collectors.toMap(Follower::getUserId, Function.identity()));
        final Set<Long> iamIds = crmUserService.getUsers(orgId, followerMap.keySet()).stream()
                .map(CrmUser::getIamId).collect(Collectors.toSet());

        response.convertIntoAsync();
        userService.fetchActiveUsersThumbnail(organizationService.getIamOrgId(orgId), iamIds, new EventCallback<>() {
            @Override
            public void onSuccess(final Map<Long, DisplayPicture> userMap) {
                try {
                    final Collection<CrmUser> crmUsers = crmUserService.getUsersByIamId(orgId, userMap.keySet());

                    for (final CrmUser crmUser : crmUsers) {
                        final Follower follower = followerMap.get(crmUser.id());

                        if (Objects.nonNull(follower)) {
                            follower.setCrmUser(crmUser);
                            followersJson.put(convertFollowerToJson(orgId, follower, userMap));
                        }
                    }
                    response.put(UserFollowerFields.TOTAL_RECORDS, totalRecords);
                    response.put(UserFollowerFields.HAS_MORE, start + limit <= totalRecords);
                    response.put(UserFollowerFields.LIST, followersJson);
                } catch (final Throwable throwable) {
                    response.setErrorCode(ErrorCode.get(SourceFollowersErrorCode.FOLLOWERS_RETRIEVAL_ACTION_FAILED,
                            "i18n.source.followers.retrieval.action.failed"));
                } finally {
                    response.send();
                }
            }

            @Override
            public void onFailure(final Throwable cause) {
                response.setErrorCode(ErrorCode.get(IamErrorCodes.USER_RETRIEVAL_FAILED, "i18n.common.user.retrieval.failed", cause));
                response.send();
            }
        });
    }

    /**
     * <p>
     * Fetches and dispatches team users
     * </p>
     *
     * @param orgId             Represents the organization id
     * @param userId            Represents the user id
     * @param data              Holds the request data
     * @param response          Represents the {@link CrmResponse}
     */
    private void fetchAndDispatchTeamsAndUsers(final long orgId, final long userId, final JsonObject data,
                                               final CrmResponse response) {
        final int start = Math.max(data.optInt(UserFollowerFields.START, DEFAULT_START_VALUE), DEFAULT_START_VALUE);
        final int limit = Math.min(data.optInt(UserFollowerFields.LIMIT, DEFAULT_LIMIT), DEFAULT_LIMIT);
        final SearchFilter nameFilter = createNameFilter(start, limit, data);
        final CriteriaDetailsHolder userCriteriaDetailsHolder = buildUserCriteriaDetailsHolder(data);
        final Set<Long> excludedCrmUserIds = new HashSet<>(getRestrictedUserIds(orgId));

        excludedCrmUserIds.add(userId);

        final Set<Long> excludedIamIds = crmUserService.getUsers(orgId, excludedCrmUserIds).stream()
                .map(CrmUser::getIamId).collect(Collectors.toSet());
        final List<CrmUser> pageUsers;
        final long usersCount;

        try {
            usersCount = crmUserService.getUsersCount(orgId, userCriteriaDetailsHolder, excludedCrmUserIds);

            if (usersCount > NumericalConstant.ZERO) {
                pageUsers = crmUserService.getUsers(orgId, userCriteriaDetailsHolder, new LimitConfig(start, limit), excludedCrmUserIds).stream().toList();
            } else {
                pageUsers = Collections.emptyList();
            }
        } catch (final Throwable throwable) {
            response.setErrorCode(ErrorCode.get(IamErrorCodes.USER_RETRIEVAL_FAILED, "i18n.common.user.retrieval.failed"));
            response.send();
            return;
        }

        if (usersCount >= limit) {
            fetchActiveUsersAndDispatch(orgId, usersCount, start + limit <= usersCount, pageUsers, Collections.emptyList(), Collections.emptySet(), response);
            return;
        }

        final long teamCount = teamManager.getTeamsCount(orgId, nameFilter);
        final Collection<Team> teams = teamManager.getTeams(orgId, nameFilter);
        final long combinedTotal = Math.toIntExact(teamCount + usersCount);
        final boolean hasMore = start + limit <= combinedTotal;
        final Set<Long> pageIamIds = pageUsers.stream().map(CrmUser::getIamId).collect(Collectors.toSet());
        final Set<Long> teamIamIds = teams.stream().flatMap(team -> team.getUsers().stream())
                .map(TeamUser::getCrmUser).map(CrmUser::getIamId)
                .filter(iamId -> !excludedIamIds.contains(iamId) && !pageIamIds.contains(iamId))
                .collect(Collectors.toSet());

        fetchActiveUsersAndDispatch(orgId, combinedTotal, hasMore, pageUsers, teams, teamIamIds, response);
    }

    /**
     * Fetches the IAM user details for the active page and team users, then dispatches the combined payload
     * back to the caller.
     *
     *
     * @param orgId         Represents the organization id used to resolve the IAM organization context
     * @param pageUsers     Represents the CRM users included in the current page result set
     * @param teams         Represents the team list that should be included alongside page users in the response
     * @param totalRecords  Represents the total number of records available for the current request
     * @param hasMore       Indicates whether additional records are available beyond the current page
     * @param response      Represents the {@link CrmResponse} that receives the final payload
     * @param teamIamIds    Represents the IAM user IDs for the team members that are included in the response
     */
    private void fetchActiveUsersAndDispatch(final long orgId, final long totalRecords, final boolean hasMore,
                                             final List<CrmUser> pageUsers, final Collection<Team> teams,
                                             final Set<Long> teamIamIds, final CrmResponse response) {
        final Set<Long> iamUserIds = new HashSet<>(teamIamIds);

        if (iamUserIds.isEmpty() && pageUsers.isEmpty()) {
            dispatchTeamsAndUsers(orgId, Collections.emptyMap(), teams, NumericalConstant.ZERO, hasMore, response);
            return;
        }

        final long iamOrgId = organizationService.getIamOrgId(orgId);

        pageUsers.forEach(crmUser -> iamUserIds.add(crmUser.getIamId()));
        response.convertIntoAsync();

        userService.fetchActiveUsersThumbnail(iamOrgId, iamUserIds, new EventCallback<>() {
            @Override
            public void onSuccess(final Map<Long, DisplayPicture> userMap) {
                dispatchTeamsAndUsers(orgId, userMap, teams, totalRecords, hasMore, response);
            }

            @Override
            public void onFailure(final Throwable cause) {
                response.setErrorCode(ErrorCode.get(IamErrorCodes.USER_RETRIEVAL_FAILED, "i18n.common.user.retrieval.failed"));
                response.send();
            }
        });
    }

    /**
     * Creates a search filter for user lookup requests using the supplied criteria and paging values.
     *
     * @param data   Holds the request payload including optional search criteria
     * @param start  Represents the start index for the result set
     * @param limit  Represents the maximum number of users to return
     * @return The configured {@link SearchFilter}
     */
    private SearchFilter createNameFilter(final int start, final int limit, final JsonObject data) {
        final SearchFilter filter = new SearchFilter();

        if (data.containsKey(UserFollowerFields.CRITERIA)) {
            final JsonObject criteria = data.getJsonObject(UserFollowerFields.CRITERIA);

            filter.setType(criteria.getString(UserFollowerFields.TYPE));
            filter.setValue(criteria.get(UserFollowerFields.VALUE));
        }
        filter.setStart(start);
        filter.setLimit(limit);

        return filter;
    }

    /**
     * Builds filtering criteria for CRM user count requests from the request payload.
     *
     * @param data Holds the request payload including optional search criteria
     * @return A {@link CriteriaDetailsHolder} for user count queries
     */
    private CriteriaDetailsHolder buildUserCriteriaDetailsHolder(final JsonObject data) {
        final CriteriaDetailsHolder criteriaDetailsHolder = new CriteriaDetailsHolder();

        if (data.containsKey(UserFollowerFields.CRITERIA)) {
            final JsonObject criteria = data.getJsonObject(UserFollowerFields.CRITERIA);
            final Object value = criteria.get(UserFollowerFields.VALUE);

            if (Objects.nonNull(value)) {
                criteriaDetailsHolder.addCriteria(new CriteriaDetails(CrmUserFields.NAME.getName(), value,
                        Operator.getOperator(criteria.getString(UserFollowerFields.TYPE)), true));
            }
        }

        return criteriaDetailsHolder;
    }

    /**
     * Dispatches the combined users and team data for the current follower search response.
     *
     * @param orgId       Represents the organization id
     * @param userMap     Maps IAM user IDs to their user thumbnail
     * @param teams       Represents the team list to include in the response
     * @param totalRecords Represents the total number of records available
     * @param hasMore     Indicates whether more records are available
     * @param response    Represents the {@link CrmResponse}
     */
    private void dispatchTeamsAndUsers(final long orgId, final Map<Long, DisplayPicture> userMap, final Collection<Team> teams,
                                       final long totalRecords, final boolean hasMore, final CrmResponse response) {
        try {
            final Collection<CrmUser> crmUsers = userMap.isEmpty() ? Collections.emptyList()
                    : secureCrmUserManager.getUsersByIamId(orgId, userMap.keySet());

            response.put(UserFollowerFields.USERS, buildFollowersArray(orgId, crmUsers, userMap));
            response.put(UserFollowerFields.TEAMS, constructTeamJson(orgId, teams, userMap));
            response.put(UserFollowerFields.TOTAL_RECORDS, totalRecords);
            response.put(UserFollowerFields.HAS_MORE, hasMore);
        } catch (final Throwable throwable) {
            response.setErrorCode(ErrorCode.get(SourceFollowersErrorCode.FOLLOWERS_RETRIEVAL_ACTION_FAILED,
                    "i18n.source.followers.retrieval.action.failed"));
        } finally {
            response.send();
        }
    }

    /**
     * Builds the team payload with their member users mapped to IAM user details.
     *
     * @param orgId    Represents the organization id
     * @param teams    Represents the collection of teams
     * @param userMap  Maps IAM user IDs to their user thumbnail
     * @return The JSON array representing teams and their members
     */
    private JsonArray constructTeamJson(final long orgId, final Collection<Team> teams, final Map<Long, DisplayPicture> userMap) {
        final JsonArray teamArray = Json.array();

        for (final Team team : teams) {
            final JsonArray users = buildFollowersArray(orgId, team.getUsers().stream().map(TeamUser::getCrmUser).toList(), userMap);

            if (users.size() > 0) {
                teamArray.put(teamConverter.convertTeam(team).put(UserFollowerFields.USERS, users));
            }
        }
        return teamArray;
    }

    /**
     * Builds the JSON representation of follower users for the specified CRM users.
     *
     * @param orgId    Represents the organization id
     * @param crmUsers Represents the CRM users to map into follower payloads
     * @param userMap  Maps IAM user IDs to their user thumbnail
     * @return The JSON array containing follower data
     */
    private JsonArray buildFollowersArray(final long orgId, final Collection<CrmUser> crmUsers,
                                          final Map<Long, DisplayPicture> userMap) {
        final JsonArray users = Json.array();

        for (final CrmUser crmUser : crmUsers) {
            if (userMap.containsKey(crmUser.getIamId())) {
                final Follower follower = new Follower(crmUser.id());

                follower.setCrmUser(crmUser);
                users.put(convertFollowerToJson(orgId, follower, userMap));
            }
        }

        return users;
    }

    /**
     * Converts a follower model into its JSON output payload.
     *
     * @param orgId    Represents the organization id
     * @param follower Represents the follower being serialized
     * @param userMap  Maps IAM user IDs to their user thumbnail
     * @return The JSON object for a follower record
     */
    private JsonObject convertFollowerToJson(final long orgId, final Follower follower, final Map<Long, DisplayPicture> userMap) {
        final CrmUser crmUser = follower.getCrmUser();
        final CrmRole role = crmUser.getRole();
        final JsonObject json = Json.object();
        final JsonObject user = Json.object().put(UserFollowerFields.NAME, crmUser.getFullName())
                .put(UserFollowerFields.ID, follower.getUserId());
        final JsonObject roleJson = Json.object().put(UserFollowerFields.NAME, role.getName())
                .put(UserFollowerFields.ID, role.id()).put(UserFollowerFields.IS_RENAMABLE, role.getConfig().isRenamable());

        json.put(UserFollowerFields.USER, user);
        json.put(UserFollowerFields.ROLE, roleJson);
        json.put(UserFollowerFields.ADDED_AT, follower.getAddedAt());

        final DisplayPicture displayPicture = userMap.get(crmUser.getIamId());

        if (Objects.nonNull(displayPicture)) {
            json.put("thumbnail", crmUserConverter.fetchProfileJson(displayPicture, orgId, crmUser.id()));
        }

        return json;
    }

    /**
     * Keeps only followers whose role has ACCESS on the source-specific followers records permission.
     *
     * @param orgId       Represents the organization id
     * @param followerIds Represents the follower ids to evaluate
     * @param source      Represents the {@link Source}
     * @return The permitted follower ids
     */
    private Set<Long> filterFollowersWithSourceAccess(final long orgId, final Collection<Long> followerIds,
                                                      final Source source) {
        if (followerIds.isEmpty()) {
            return Collections.emptySet();
        }

        final SubCategory.Type followerSubCategory = SubCategory.Type.getFollowersTypeBySource(source.getName());
        final Set<Long> permittedFollowerIds = new HashSet<>();
        final Map<Long, Collection<Long>> roleIdToUserIdsMap = crmUserService.getUsers(orgId, followerIds)
                .stream().collect(Collectors.groupingBy(
                        CrmUser::getRoleId,
                        Collectors.mapping(CrmUser::id, Collectors.toCollection(ArrayList::new))
                ));
        final Map<Long, RolePermission> rolePermissionMap = rolePermissionService.getPermissions(orgId, roleIdToUserIdsMap.keySet(),
                Category.Type.FOLLOWERS, followerSubCategory.getRelativeSubCategory(),
                com.tlc.crm.security.permission.model.Action.Type.ACCESS);

        roleIdToUserIdsMap.forEach((roleId, userIds) -> {
            final RolePermission permission = rolePermissionMap.get(roleId);

            if (Objects.nonNull(permission) && permission.isGranted()) {
                permittedFollowerIds.addAll(userIds);
            }
        });

        return permittedFollowerIds;
    }

    /**
     * Resolves users restricted by follower-record permissions for the given organization.
     *
     * @param orgId Represents the organization id
     * @return The collection of user IDs restricted from follower record access
     */
    private Set<Long> getRestrictedUserIds(final long orgId) {
        final Set<Long> restrictedRoleIds = new HashSet<>();

        for (final RolePermissions permissions : rolePermissionService.getPermissions(orgId, roleManager.getRoleIds(orgId))) {

            for (final RolePermission permission : permissions.getPermissions()) {

                if (permission.getSubCategory().getType().hasPrimaryAction()
                        && permission.getSubCategory().getType() == SubCategory.Type.USER_FOLLOWERS_RECORDS
                        && permission.getAction().getType() == com.tlc.crm.security.permission.model.Action.Type.ACCESS
                        && !permission.isGranted()) {
                    restrictedRoleIds.add(permissions.getRoleId());
                }
            }
        }

        if (restrictedRoleIds.isEmpty()) {
            return Collections.emptySet();
        }

        return roleManager.getUserIds(orgId, restrictedRoleIds).values().stream().flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    /**
     * Validates that the sales owner and follower ids exist
     *
     * @param orgId              Represents the organization id
     * @param salesOwnerId       Represents the sales owner user id
     * @param addedFollowerIds   Represents the follower ids to be added
     * @param removedFollowerIds Represents the follower ids to be removed
     */
    private void validateCrmUser(final long orgId, final Long salesOwnerId, final Collection<Long> addedFollowerIds,
                                 final Collection<Long> removedFollowerIds) {
        final Collection<Long> userIds = new HashSet<>();

        userIds.add(salesOwnerId);
        userIds.addAll(addedFollowerIds);
        userIds.addAll(removedFollowerIds);

        final Collection<CrmUser> fetchedUsers = crmUserService.getUsers(orgId, userIds);

        if (fetchedUsers.size() != userIds.size()) {
            throw ErrorCode.get(SourceFollowersErrorCode.CRM_USERS_NOT_FOUND, "i18n.source.followers.crm.users.not.found");
        }
    }

    /**
     * Extracts a set of IDs from a {@link JsonObject} based on a specified key
     *
     * @param data The {@link JsonObject} containing the data
     * @param key  The key to look for in the {@link JsonObject}
     * @return A collection of IDs extracted from the {@link JsonObject}
     */
    private Collection<Long> extractIds(final JsonObject data, final String key) {
        if (data.containsKey(key)) {
            final JsonArray jsonArray = data.getJsonArray(key);
            final int size = jsonArray.size();
            final Collection<Long> ids = new HashSet<>(size);

            for (int index = NumericalConstant.ZERO; index < size; index++) {
                ids.add(jsonArray.getLong(index));
            }

            return ids;
        }

        return Collections.emptySet();
    }
}
