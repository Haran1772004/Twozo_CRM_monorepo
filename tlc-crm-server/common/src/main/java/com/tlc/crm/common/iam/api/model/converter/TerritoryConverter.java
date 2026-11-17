package com.tlc.crm.common.iam.api.model.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.iam.api.manager.TerritoryManager;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.converter.OptionJsonConverter;
import com.tlc.crm.common.iam.api.model.Team;
import com.tlc.crm.common.iam.api.model.TeamUser;
import com.tlc.crm.common.iam.api.model.Territory;
import com.tlc.crm.common.iam.field.IamFields;
import com.tlc.crm.common.iam.field.TerritoryFields;
import com.tlc.crm.common.iam.internal.CrmUserEntity;
import com.tlc.l10n.temporal.resolver.DateTimeUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * Provides various methods to convert the data from / into {@link Territory}
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public class TerritoryConverter implements OptionJsonConverter<Territory> {

    private final TerritoryManager territoryManager;
    private final CrmUserConverter userConverter;
    private final TeamConverter teamConverter;
    private final DateTimeUtil dateTimeUtil;

    private static class Instance {
        private static final TerritoryConverter INSTANCE = new TerritoryConverter();
    }

    private TerritoryConverter() {
        territoryManager = TerritoryManager.getInstance();
        userConverter = CrmUserConverter.getInstance();
        teamConverter = TeamConverter.getInstance();
        dateTimeUtil = DateTimeUtil.getInstance();
    }

    /**
     * <p>
     * Gets the {@link TerritoryConverter} Instance
     * </p>
     *
     * @return The instance of {@link TerritoryConverter}
     */
    public static TerritoryConverter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Converts the {@link JsonObject} into {@link Territory}
     * </p>
     *
     * @param isPartialData A flag that indicates whether the data is partial or not
     * @param orgId         Represents an organization id
     * @param userId        Represents the user id
     * @param jsonObject    Holds the territory {@link JsonObject} details
     * @return The {@link Territory} details
     */
    public Territory convert(final boolean isPartialData, final Long orgId, final Long userId, final JsonObject jsonObject, final boolean isCreateEvent) {
        return isPartialData ? buildTerritoryByPartialData(orgId, userId, jsonObject) : buildTerritory(orgId, userId, jsonObject, isCreateEvent);
    }

    /**
     * <p>
     * Converts the collection of {@link Territory} into {@link JsonArray}.
     * </p>
     *
     * @param territories Holds the details of the {@link Territory}.
     * @return Converted {@link JsonArray}
     */
    public JsonArray convertTerritoriesIntoJson(final Collection<Territory> territories) {
        final JsonArray jsonArray = Json.array();

        for (final Territory territory : territories) {
            jsonArray.put(Json.object()
                    .put(IamFields.VALUE, territory.id())
                    .put(IamFields.NAME, territory.getName())
                    .put(IamFields.TYPE, IamFields.TERRITORY));
        }

        return jsonArray;
    }

    /**
     * <p>
     * Builds the {@link Territory} from the given {@link JsonObject}
     * </p>
     *
     * @param orgId      Represents an organization id
     * @param userId     Represents the user id
     * @param jsonObject Holds the territory {@link JsonObject} details
     * @return The {@link Territory} details
     */
    private Territory buildTerritory(final Long orgId, final Long userId, final JsonObject jsonObject, final boolean isCreateEvent) {
        final Territory territory = new Territory();
        final CrmUser crmUser = new CrmUserEntity();
        final String idKey = TerritoryFields.ID.getName();

        if (jsonObject.containsKey(idKey)) {
            territory.setId(jsonObject.getLong(idKey));
        }
        crmUser.setId(userId);
        territory.setCreatedBy(crmUser);
        territory.setUpdatedBy(crmUser);
        territory.setOrgId(orgId);
        setJsonDataToModel(jsonObject, territory, isCreateEvent);

        return territory;
    }

    /**
     * <p>
     * Builds the partial {@link Territory} from the given {@link JsonObject}
     * </p>
     *
     * @param orgId      Represents an organization id
     * @param userId     Represents the user id
     * @param jsonObject Holds the territory {@link JsonObject} details
     * @return The {@link Territory} details
     */
    private Territory buildTerritoryByPartialData(final Long orgId, final Long userId, final JsonObject jsonObject) {
        final Territory territory = territoryManager.get(orgId, jsonObject.getLong(TerritoryFields.ID.getName()));

        if (!Objects.equals(territory.getUpdatedBy().id(), userId)) {
            final CrmUser crmUser = new CrmUserEntity();

            crmUser.setId(userId);
            territory.setUpdatedBy(crmUser);
        }
        setJsonDataToModel(jsonObject, territory, false);
        territory.getUpdatedBy().setId(userId);

        return territory;
    }

    /**
     * <p>
     * Sets the {@link JsonObject} data into the {@link Territory}
     * </p>
     *
     * @param jsonObject Holds the territory {@link JsonObject} details
     * @param territory  Represents the {@link Territory} details
     */
    private void setJsonDataToModel(final JsonObject jsonObject, final Territory territory, final boolean isCreateEvent) {
        final String nameKey = TerritoryFields.NAME.getName();
        final String descriptionKey = TerritoryFields.DESCRIPTION.getName();
        final String addedUsersKey = TerritoryFields.ADDED_USERS.getName();
        final String removedUsersKey = TerritoryFields.REMOVED_USERS.getName();
        final Collection<CrmUser> crmUsers = new ArrayList<>();

        if (isCreateEvent) {

            if (jsonObject.containsKey(TerritoryFields.USERS.getName())) {
                userConverter.loadJsonDataToModel(jsonObject, crmUsers);
            }
        } else {
            final Collection<Long> territoryUserIds = territoryManager.getTerritoryUserIds(territory.orgId(), List.of(territory.id()));
            final Collection<Long> removedUsers = new HashSet<>();

            if (jsonObject.containsKey(removedUsersKey)) {
                final JsonArray removedUsersArray = jsonObject.getJsonArray(removedUsersKey);

                for (int index = 0; index < removedUsersArray.size(); index++) {
                    removedUsers.add(removedUsersArray.getLong(index));
                }
            }

            territoryUserIds.forEach(userId -> {

                if (!removedUsers.contains(userId)) {
                    final CrmUser crmUser = new CrmUserEntity();

                    crmUser.setId(userId);
                    crmUsers.add(crmUser);
                }
            });

            if (jsonObject.containsKey(addedUsersKey)) {
                final JsonArray addedUsersArray = jsonObject.getJsonArray(addedUsersKey);

                addedUsersArray.forEach(userId -> {
                    final CrmUser crmUser = new CrmUserEntity();

                    crmUser.setId((Long) userId);
                    crmUsers.add(crmUser);
                });
            }
        }
        territory.setCrmUsers(crmUsers);


        if (jsonObject.containsKey(TerritoryFields.TEAMS.getName())) {
            final Collection<Team> teams = new ArrayList<>();

            teamConverter.loadPartialJsonDataToModel(jsonObject, teams);
            territory.setTeams(teams);
        }
        territory.setName(jsonObject.containsKey(nameKey) ? jsonObject.getString(nameKey) : territory.getName());
        territory.setDescription(jsonObject.containsKey(descriptionKey) ? jsonObject.getString(descriptionKey) : territory.getDescription());
    }

    /**
     * <p>
     * Converts the {@link Territory} details into {@link JsonObject}
     * </p>
     *
     * @param territory Represents the {@link Territory} details
     * @return Converted the territory {@link JsonObject}
     */
    public JsonObject convert(final Territory territory) {
        final JsonObject jsonObject = Json.object();
        final Collection<CrmUser> crmUsers = territory.getCrmUsers();
        final Collection<Team> teams = territory.getTeams();

        jsonObject.put(TerritoryFields.ID.getName(), territory.id());
        jsonObject.put(TerritoryFields.NAME.getName(), territory.getName());
        jsonObject.put(TerritoryFields.DESCRIPTION.getName(), territory.getDescription());
        jsonObject.put(TerritoryFields.CREATED_TIME.getName(), dateTimeUtil.getTimeStamp(territory.getCreatedTime()));
        jsonObject.put(TerritoryFields.UPDATED_TIME.getName(), dateTimeUtil.getTimeStamp(territory.getUpdatedTime()));
        jsonObject.put(TerritoryFields.CREATED_BY.getName(), userConverter.convert(territory.getCreatedBy(), true, true));
        jsonObject.put(TerritoryFields.UPDATED_BY.getName(), userConverter.convert(territory.getUpdatedBy(), true, true));
        jsonObject.put(TerritoryFields.USERS_COUNT.getName(), territory.getUsersCount());

        if (Objects.nonNull(crmUsers)) {
            final JsonArray userJsonArray = Json.array();

            for (final CrmUser crmUser : crmUsers) {
                userJsonArray.put(userConverter.convert(crmUser, false, true));
            }
            jsonObject.put(TerritoryFields.USERS.getName(), userJsonArray);
        }

        if (Objects.nonNull(teams)) {
            final JsonArray teamJsonArray = Json.array();

            for (final Team team : teams) {
                teamJsonArray.put(teamConverter.convert(team));
            }
            jsonObject.put(TerritoryFields.TEAMS.getName(), teamJsonArray);
        }

        return jsonObject;
    }

    /**
     * <p>
     * Converts the {@link Territory} details into {@link JsonObject}
     * </p>
     *
     * @param territory Represents the {@link Territory} details
     * @return Converted the territory {@link JsonObject}
     */
    public JsonObject convertTerritoryToJson(final Territory territory) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(TerritoryFields.ID.getName(), territory.id());
        jsonObject.put(TerritoryFields.NAME.getName(), territory.getName());

        return jsonObject;
    }

    /**
     * <p>
     * Converts {@link Territory} details into {@link JsonObject}
     * </p>
     *
     * @param territory Represents the {@link Territory} details
     * @return The Territory {@link JsonObject} details
     */
    public JsonObject convertToJson(final Territory territory) {
        final JsonObject jsonObject = Json.object();
        final Collection<CrmUser> crmUsers = territory.getCrmUsers();
        final Collection<Team> teams = territory.getTeams();

        jsonObject.put(TerritoryFields.ID.getName(), territory.id());
        jsonObject.put(TerritoryFields.NAME.getName(), territory.getName());
        jsonObject.put(TerritoryFields.DESCRIPTION.getName(), territory.getDescription());

        if (Objects.nonNull(teams)) {
            crmUsers.addAll(teams.stream().flatMap(team -> team.getUsers().stream()).map(TeamUser::getCrmUser).toList());
        }

        if (Objects.nonNull(crmUsers)) {
            final JsonArray userJsonArray = Json.array();

            for (final CrmUser crmUser : crmUsers) {
                userJsonArray.put(userConverter.convertUserToJson(crmUser));
            }
            jsonObject.put(TerritoryFields.USERS.getName(), userJsonArray);
        }

        return jsonObject;
    }

    /**
     * Converts {@link Territory} into {@link JsonObject}
     *
     * @param territory The territory entity with the attributes of territory details.
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject convertModelToOptionJson(final Territory territory) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(TerritoryFields.VALUE.getName(), territory.id());
        jsonObject.put(TerritoryFields.NAME.getName(), territory.getName());

        return jsonObject;
    }
}
