package com.tlc.crm.core.notification.email.inbox.api.model.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.iam.api.model.converter.CrmUserConverter;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.Team;
import com.tlc.crm.common.iam.api.model.Territory;
import com.tlc.crm.common.iam.service.UserService;

import com.tlc.crm.entity.builder.EntityConverter;
import com.tlc.crm.common.converter.ConverterConfig;

import com.tlc.crm.core.notification.email.inbox.api.model.TeamInbox;
import com.tlc.crm.core.notification.email.inbox.fields.TeamInboxFields;

import com.tlc.email.common.model.address.EmailAddress;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

/**
 * <p>
 * Provides different ways to convert the data from database into {@link TeamInbox}
 * </p>
 *
 * @author AswiniN
 * @version 1.0
 */
public class TeamInboxConverter extends EntityConverter<TeamInbox> {

    private final UserService userService;

    public TeamInboxConverter() {
        userService = UserService.getInstance();
    }

    private static class Instance {
        private static final TeamInboxConverter INSTANCE = new TeamInboxConverter();
    }

    /**
     * <p>
     * Gets the {@link TeamInboxConverter} Instance
     * </p>
     *
     * @return {@link TeamInboxConverter}
     */
    public static TeamInboxConverter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Builds the team inbox entity from the JSON object provided
     * </p>
     *
     * @param converterConfig Represents the converter configuration
     * @param jsonObject      Contains the JSON object
     * @return {@link TeamInbox}
     */
    @Override
    public TeamInbox convert(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final TeamInbox teamInbox = new TeamInbox();
        final String idKey = TeamInboxFields.ID.getName();
        final Long orgId = converterConfig.orgId();
        final String emailAddressKey = TeamInboxFields.EMAIL_ADDRESS.getName();

        if (jsonObject.containsKey(emailAddressKey)) {
            final EmailAddress emailAddress = new EmailAddress(jsonObject.getString(emailAddressKey.trim()));

            teamInbox.setAddress(emailAddress);
        }
        teamInbox.setOrgId(orgId);

        if (jsonObject.containsKey(idKey)) {
            teamInbox.setId(jsonObject.getLong(idKey));
        }
        teamInbox.setState(TeamInbox.State.ACTIVE);
        teamInbox.setOwnerId(converterConfig.ownerId());
        teamInbox.setVisibilityStatus(TeamInbox.VisibilityStatus.EVERYONE);

        return teamInbox;
    }

    /**
     * <p>
     * Transforms the team inbox entity to JSON object
     * </p>
     *
     * @param teamInbox An entity that holds team inbox attributes
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject convert(final TeamInbox teamInbox) {
        final JsonObject jsonObject = Json.object();
        final Long autoForwardedTime = teamInbox.getAutoForwardedTime();

        jsonObject.put(TeamInboxFields.ID.getName(), teamInbox.id());
        jsonObject.put(TeamInboxFields.ORG_ID.getName(), teamInbox.orgId());
        jsonObject.put(TeamInboxFields.EMAIL_ADDRESS.getName(), teamInbox.getAddress().getEmailAddress());
        jsonObject.put(TeamInboxFields.EMAIL_VERIFICATION_STATUS.getName(), teamInbox.getVerificationState());
        jsonObject.put(TeamInboxFields.DOMAIN_VERIFICATION_STATUS.getName(), teamInbox.getDomainVerificationStatus());

        if (Objects.nonNull(autoForwardedTime)) {
            jsonObject.put(TeamInboxFields.AUTO_FORWARDING_SYNC_TIME.getName(), teamInbox.getAutoForwardedTime());
        }
        final JsonObject visibilityJson = buildVisibilityJsonObject(teamInbox);

        if (visibilityJson.size() > 0) {
            jsonObject.put(TeamInboxFields.VISIBLE_TO.getName(), visibilityJson);
        }

        return jsonObject;
    }

    /**
     * Builds the {@link JsonObject} with visibility details for a given {@link TeamInbox}.
     *
     * @param teamInbox Represents the entity of {@link TeamInbox}
     * @return {@link JsonObject} containing visibility information.
     */
    public JsonObject buildVisibilityJsonObject(final TeamInbox teamInbox) {
        final JsonObject visibilityJson = Json.object();
        final TeamInbox.VisibilityStatus visibilityStatus = teamInbox.getVisibilityStatus();

        if (TeamInbox.VisibilityStatus.DESIGNATED == visibilityStatus) {
            final Collection<Team> teams = teamInbox.resolveTeams();
            final Collection<Territory> territories = teamInbox.resolveTerritories();
            final Collection<CrmUser> crmUsers = teamInbox.resolveUsers();

            if (Objects.nonNull(territories) && !territories.isEmpty()) {
                final JsonArray territoryArray = Json.array();

                for (final Territory territory : territories) {
                    final JsonObject territoryObject = Json.object();

                    territoryObject.put(TeamInboxFields.TERRITORY_ID.getName(), territory.id());
                    territoryObject.put(TeamInboxFields.TERRITORY_NAME.getName(), territory.getName());
                    territoryArray.put(territoryObject);
                }
                visibilityJson.put(TeamInboxFields.TERRITORIES.getName(), territoryArray);
            }

            if (Objects.nonNull(teams) && !teams.isEmpty()) {
                final JsonArray teamsArray = Json.array();

                for (final Team team : teams) {
                    final JsonObject teamObject = Json.object();

                    teamObject.put(TeamInboxFields.TEAM_ID.getName(), team.id());
                    teamObject.put(TeamInboxFields.TEAM_NAME.getName(), team.getName());
                    teamsArray.put(teamObject);
                }
                visibilityJson.put(TeamInboxFields.TEAMS.getName(), teamsArray);
            }

            if (Objects.nonNull(crmUsers) && !crmUsers.isEmpty()) {
                final JsonArray userJsonArray = Json.array();
                final CrmUserConverter crmUserConverter = CrmUserConverter.getInstance();

                for (final CrmUser crmUser : crmUsers) {
                    userJsonArray.put(crmUserConverter.convert(crmUser, true, false));
                }
                visibilityJson.put(TeamInboxFields.USERS.getName(), userJsonArray);
            }
        }

        return visibilityJson;
    }

    /**
     * <p>
     * Sets the visibility details for the {@link TeamInbox} based on the provided data.
     * </p>
     *
     * @param teamInbox  An entity that holds team inbox attributes
     * @param jsonObject Represents the {@link JsonObject} provided by user
     */
    public void setVisibilityDetails(final TeamInbox teamInbox, final JsonObject jsonObject) {
        final JsonArray jsonArray = jsonObject.getJsonArray(TeamInboxFields.ACCESS_TO.getName());

        for (int index = 0; index < jsonArray.size(); index++) {
            final JsonObject userJson = jsonArray.getJsonObject(index);

            setUserJsonToModel(teamInbox, userJson);
            setTeamJsonToModel(teamInbox, userJson);
            setTerritoryJsonToModel(teamInbox, userJson);
        }
    }

    /**
     * <p>
     * Converts a collection of email addresses into a {@link JsonArray}.
     * </p>
     *
     * @param teamInboxes A {@link Collection} of email addresses to convert
     * @return A {@link JsonArray} containing the email addresses
     */
    public JsonArray convert(final Collection<TeamInbox> teamInboxes) {
        final JsonArray jsonArray = Json.array();

        if (Objects.nonNull(teamInboxes)) {
            teamInboxes.forEach(teamInbox -> jsonArray.put(
                    Json.object()
                            .put(TeamInboxFields.ID.getName(), teamInbox.id())
                            .put(TeamInboxFields.EMAIL.getName(), teamInbox.getAddress().getEmailAddress())
            ));
        }

        return jsonArray;
    }

    /**
     * <p>
     * Sets the user JSON to {@link TeamInbox} model
     * </p>
     *
     * @param teamInbox An entity that holds team inbox attributes
     * @param userJson  Represents the user JSON
     */
    private void setUserJsonToModel(final TeamInbox teamInbox, final JsonObject userJson) {
        if (userJson.containsKey(TeamInboxFields.USERS.getName())) {
            final JsonArray users = userJson.getJsonArray(TeamInboxFields.USERS.getName());
            final Collection<CrmUser> crmUsers = new HashSet<>();

            for (int index = 0; index < users.size(); index++) {
                final CrmUser crmUser = userService.getUser(teamInbox.orgId(), users.getLong(index));

                crmUsers.add(crmUser);
            }
            teamInbox.setUsers(crmUsers);
        }
    }

    /**
     * <p>
     * Sets the team JSON to {@link TeamInbox} model
     * </p>
     *
     * @param teamInbox An entity that holds team inbox attributes
     * @param teamJson  Represents the team JSON
     */
    private void setTeamJsonToModel(final TeamInbox teamInbox, final JsonObject teamJson) {
        if (teamJson.containsKey(TeamInboxFields.TEAMS.getName())) {
            final JsonArray teamArray = teamJson.getJsonArray(TeamInboxFields.TEAMS.getName());
            final Collection<Team> teams = new HashSet<>();
            final int size = teamArray.size();

            for (int teamIndex = 0; teamIndex < size; teamIndex++) {
                final Team team = new Team();

                team.setId(teamArray.getLong(teamIndex));
                teams.add(team);
            }
            teamInbox.setTeams(teams);
        }
    }

    /**
     * <p>
     * Sets the territory JSON to {@link TeamInbox} model
     * </p>
     *
     * @param teamInbox     An entity that holds team inbox attributes
     * @param territoryJson Represents the territory JSON
     */
    private void setTerritoryJsonToModel(final TeamInbox teamInbox, final JsonObject territoryJson) {
        if (territoryJson.containsKey(TeamInboxFields.TERRITORIES.getName())) {
            final JsonArray territoryArray = territoryJson.getJsonArray(TeamInboxFields.TERRITORIES.getName());
            final Collection<Territory> territories = new HashSet<>();

            for (int territoryIndex = 0; territoryIndex < territoryArray.size(); territoryIndex++) {
                final Territory territory = new Territory();

                territory.setId(territoryArray.getLong(territoryIndex));
                territories.add(territory);
            }
            teamInbox.setTerritories(territories);
        }
    }
}