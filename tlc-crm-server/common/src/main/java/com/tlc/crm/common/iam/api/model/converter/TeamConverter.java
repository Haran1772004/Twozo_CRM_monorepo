package com.tlc.crm.common.iam.api.model.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.iam.api.manager.TeamManager;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.Team;
import com.tlc.crm.common.iam.api.model.TeamUser;
import com.tlc.crm.common.iam.field.CrmUserFields;
import com.tlc.crm.common.iam.field.IamFields;
import com.tlc.crm.common.iam.field.TeamFields;
import com.tlc.crm.common.iam.field.TerritoryFields;
import com.tlc.crm.common.iam.internal.CrmUserEntity;
import com.tlc.l10n.temporal.resolver.DateTimeUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Provides various methods to convert the data from / into {@link Team}
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public class TeamConverter {

    private final CrmUserConverter userConverter;
    private final TeamManager teamManager;
    private final DateTimeUtil dateTimeUtil;

    private TeamConverter() {
        userConverter = CrmUserConverter.getInstance();
        teamManager = TeamManager.getInstance();
        dateTimeUtil = DateTimeUtil.getInstance();
    }

    private static class Instance {
        private static final TeamConverter INSTANCE = new TeamConverter();
    }

    /**
     * <p>
     * Gets the {@link TeamConverter} Instance
     * </p>
     *
     * @return The instance of {@link TeamConverter}
     */
    public static TeamConverter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Converts the {@link JsonObject} into {@link Team}
     * </p>
     *
     * @param isPartialData A flag that indicates whether the data is partial or not
     * @param orgId         Represents an organization id
     * @param userId        Represents the user id
     * @param jsonObject    Holds the team {@link JsonObject} details
     * @return The {@link Team} details
     */
    public Team convert(final boolean isPartialData, final Long orgId, final Long userId, final JsonObject jsonObject, final boolean isCreateEvent) {
        return isPartialData ? buildTeamByPartialData(orgId, userId, jsonObject) : buildTeam(orgId, userId, jsonObject, isCreateEvent);
    }

    /**
     * <p>
     * Converts the collection of {@link Team} into {@link JsonArray}.
     * </p>
     *
     * @param teams Holds the details of the {@link Team}.
     * @return Converted {@link JsonArray}
     */
    public JsonArray convertTeamsIntoJson(final Collection<Team> teams) {
        final JsonArray jsonArray = Json.array();

        for (final Team team : teams) {
            jsonArray.put(Json.object()
                    .put(IamFields.VALUE, team.id())
                    .put(IamFields.NAME, team.getName())
                    .put(IamFields.TYPE, IamFields.TEAM));
        }

        return jsonArray;
    }

    /**
     * <p>
     * Builds the {@link Team} from the given {@link JsonObject}
     * </p>
     *
     * @param orgId      Represents an organization id
     * @param userId     Represents the user id
     * @param jsonObject Holds the team {@link JsonObject} details
     * @return The {@link Team} details
     */
    private Team buildTeam(final Long orgId, final Long userId, final JsonObject jsonObject, final boolean isCreateEvent) {
        final Team team = new Team();
        final CrmUser crmUser = new CrmUserEntity();
        final String teamIdKey = TeamFields.ID.getName();

        if (jsonObject.containsKey(teamIdKey)) {
            team.setId(jsonObject.getLong(teamIdKey));
        }
        crmUser.setId(userId);
        team.setCreatedBy(crmUser);
        team.setUpdatedBy(crmUser);
        team.setOrgId(orgId);
        setJsonDataToModel(jsonObject, team, isCreateEvent);

        return team;
    }

    /**
     * <p>
     * Builds the partial {@link Team} from the given {@link JsonObject}
     * </p>
     *
     * @param orgId      Represents the organization id
     * @param userId     Represents the user id
     * @param jsonObject Holds the team {@link JsonObject} details
     * @return The {@link Team} details
     */
    private Team buildTeamByPartialData(final Long orgId, final Long userId, final JsonObject jsonObject) {
        final Team team = teamManager.get(orgId, jsonObject.getLong(TeamFields.ID.getName()));

        if (!Objects.equals(team.getUpdatedBy().id(), userId)) {
            final CrmUser crmUser = new CrmUserEntity();

            crmUser.setId(userId);
            team.setUpdatedBy(crmUser);
        }
        setJsonDataToModel(jsonObject, team, false);
        team.getUpdatedBy().setId(userId);

        return team;
    }

    /**
     * <p>
     * Sets the {@link JsonObject} data into the {@link Team}
     * </p>
     *
     * @param jsonObject Holds the team {@link JsonObject} details
     * @param team       Represents the {@link Team} details
     */
    private void setJsonDataToModel(final JsonObject jsonObject, final Team team, final boolean isCreateEvent) {
        final Long orgId = team.orgId();
        final Long teamId = team.id();
        final String nameKey = TeamFields.NAME.getName();
        final String idKey = TeamFields.ID.getName();
        final String addedUsersKey = TeamFields.ADDED_USERS.getName();
        final String removedUsersKey = TeamFields.REMOVED_USERS.getName();
        final String updatedUsersKey = TeamFields.UPDATED_USERS.getName();
        final String assignAsManagerKey = TeamFields.ASSIGN_AS_MANAGER.getName();

        team.setName(jsonObject.optString(nameKey, team.getName()));

        if (isCreateEvent) {
            final Collection<TeamUser> teamUsers = new ArrayList<>();

            if (jsonObject.containsKey(TeamFields.USERS.getName())) {
                loadTeamUsers(jsonObject, teamUsers);
            }
            team.setUsers(teamUsers);
        } else {
            final Map<Long, TeamUser> teamUserMap = new HashMap<>();
            final Collection<Long> teamUserIds = teamManager.getTeamUserIds(orgId, List.of(teamId));
            final Collection<Long> teamManagerIds = teamManager.getTeamManagerIds(orgId, teamId);
            final Collection<Long> removedUsers = new HashSet<>();

            if (jsonObject.containsKey(removedUsersKey)) {
                final JsonArray removedUsersArray = jsonObject.getJsonArray(removedUsersKey);

                for (int index = 0; index < removedUsersArray.size(); index++) {
                    final JsonObject userJson = removedUsersArray.getJsonObject(index);

                    removedUsers.add(userJson.getLong(idKey));
                }
            }

            teamUserIds.forEach(userId -> {

                if (!removedUsers.contains(userId)) {
                    final TeamUser teamUser = new TeamUser();
                    final CrmUser crmUser = new CrmUserEntity();

                    crmUser.setId(userId);
                    teamUser.setCrmUser(crmUser);
                    teamUser.setManager(teamManagerIds.contains(userId));
                    teamUserMap.put(userId, teamUser);
                }
            });

            if (jsonObject.containsKey(addedUsersKey)) {
                final JsonArray addedUsersArray = jsonObject.getJsonArray(addedUsersKey);
                final int size = addedUsersArray.size();

                for (int index = 0; index < size; index++) {
                    final JsonObject userJson = addedUsersArray.getJsonObject(index);
                    final TeamUser teamUser = new TeamUser();
                    final CrmUser crmUser = new CrmUserEntity();
                    final long userId = userJson.getLong(idKey);

                    crmUser.setId(userId);
                    teamUser.setCrmUser(crmUser);
                    teamUser.setManager(userJson.optBoolean(assignAsManagerKey));
                    teamUserMap.put(userId, teamUser);
                }
            }

            if (jsonObject.containsKey(updatedUsersKey)) {
                final JsonArray updatedUsersArray = jsonObject.getJsonArray(updatedUsersKey);
                final int size = updatedUsersArray.size();

                for (int index = 0; index < size; index++) {
                    final JsonObject userJson = updatedUsersArray.getJsonObject(index);

                    teamUserMap.get(userJson.getLong(idKey)).setManager(userJson.optBoolean(assignAsManagerKey));
                }
            }
            team.setUsers(teamUserMap.values());
        }
    }

    /**
     * Loads the {@link TeamUser} details from the json.
     *
     * @param jsonObject Refers the {@link JsonObject} which holds the team data.
     * @param teamUsers  Refers the collection of {@link TeamUser}.
     */
    private void loadTeamUsers(final JsonObject jsonObject, final Collection<TeamUser> teamUsers) {
        final JsonArray jsonArray = jsonObject.getJsonArray(CrmUserFields.USERS.getName());
        final String idKey = TeamFields.ID.getName();
        final String assignAsManagerKey = TeamFields.ASSIGN_AS_MANAGER.getName();

        for (int index = 0; index < jsonArray.size(); index++) {
            final JsonObject teamUserJson = jsonArray.getJsonObject(index);
            final TeamUser teamUser = new TeamUser();
            final CrmUser crmUser = new CrmUserEntity();

            crmUser.setId(teamUserJson.getLong(idKey));
            teamUser.setCrmUser(crmUser);
            teamUser.setManager(teamUserJson.optBoolean(assignAsManagerKey));
            teamUsers.add(teamUser);
        }
    }

    /**
     * <p>
     * Loads the partial {@link JsonObject} data into the {@link Team}
     * </p>
     *
     * @param jsonObject Holds the team {@link JsonObject} details
     * @param teams      Represents the collection of {@link Team} details
     */
    public void loadPartialJsonDataToModel(final JsonObject jsonObject, final Collection<Team> teams) {
        final JsonArray jsonArray = jsonObject.getJsonArray(TerritoryFields.TEAMS.getName());

        jsonArray.forEach(teamId -> {
            final Team team = new Team();

            team.setId((Long) teamId);
            teams.add(team);
        });
    }

    /**
     * <p>
     * Converts the {@link Team} details into {@link JsonObject}
     * </p>
     *
     * @param team Represents the {@link Team} details
     * @return Converted the team {@link JsonObject}
     */
    public JsonObject convert(final Team team) {
        final JsonObject jsonObject = Json.object();
        final Collection<TeamUser> teamUsers = team.getUsers();

        jsonObject.put(TeamFields.ID.getName(), team.id());
        jsonObject.put(TeamFields.NAME.getName(), team.getName());
        jsonObject.put(TeamFields.CREATED_TIME.getName(), dateTimeUtil.getTimeStamp(team.getCreatedTime()));
        jsonObject.put(TeamFields.UPDATED_TIME.getName(), dateTimeUtil.getTimeStamp(team.getUpdatedTime()));
        jsonObject.put(TeamFields.CREATED_BY.getName(), userConverter.convert(team.getCreatedBy(), true, true));
        jsonObject.put(TeamFields.UPDATED_BY.getName(), userConverter.convert(team.getUpdatedBy(), true, true));
        jsonObject.put(TeamFields.USERS_COUNT.getName(), team.getUsersCount());
        jsonObject.put(TeamFields.TEAM_MANAGERS.getName(), team.getManagersCount());

        if (Objects.nonNull(teamUsers)) {
            final JsonArray userJsonArray = Json.array();

            for (final TeamUser teamUser : teamUsers) {
                final JsonObject userJson = userConverter.convert(teamUser.getCrmUser(), false, true);
                final boolean isManager = teamUser.isManager();

                if (isManager) {
                    userJson.put(TeamFields.ASSIGN_AS_MANAGER.getName(), isManager);
                }
                userJsonArray.put(userJson);
            }
            jsonObject.put(TeamFields.USERS.getName(), userJsonArray);
        }

        return jsonObject;
    }

    /**
     * <p>
     * Converts the {@link Team} details into {@link JsonObject}
     * </p>
     *
     * @param team Represents the {@link Team} details
     * @return Converted the territory {@link JsonObject}
     */
    public JsonObject convertTeam(final Team team) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(TeamFields.ID.getName(), team.id());
        jsonObject.put(TeamFields.NAME.getName(), team.getName());

        return jsonObject;
    }

    /**
     * <p>
     * Converts the {@link Team} details into {@link JsonObject}
     * </p>
     *
     * @param team Represents the {@link Team} details
     * @return The {@link JsonObject} details
     */
    public JsonObject convertTeamToJson(final Team team) {
        final JsonObject jsonObject = Json.object();
        final Collection<TeamUser> teamUsers = team.getUsers();

        jsonObject.put(TeamFields.ID.getName(), team.id());
        jsonObject.put(TeamFields.NAME.getName(), team.getName());

        if (Objects.nonNull(teamUsers)) {
            final JsonArray userJsonArray = Json.array();

            for (final TeamUser teamUser : teamUsers) {
                final JsonObject userJson = userConverter.convertUserToJson(teamUser.getCrmUser());

                if (teamUser.isManager()) {
                    userJson.put(TeamFields.ASSIGN_AS_MANAGER.getName(), true);
                }
                userJsonArray.put(userJson);
            }
            jsonObject.put(TeamFields.USERS.getName(), userJsonArray);
        }

        return jsonObject;
    }
}
