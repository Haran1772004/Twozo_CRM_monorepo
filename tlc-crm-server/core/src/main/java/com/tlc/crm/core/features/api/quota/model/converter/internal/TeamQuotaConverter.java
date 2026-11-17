package com.tlc.crm.core.features.api.quota.model.converter.internal;

import com.tlc.attachment.model.profilePicture.DisplayPicture;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.converter.CrmConverterData;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.Team;
import com.tlc.crm.common.iam.api.model.TeamUser;
import com.tlc.crm.core.features.api.quota.fields.QuotaFields;
import com.tlc.crm.core.features.api.quota.model.Quota;
import com.tlc.crm.core.features.api.quota.model.QuotaHolder;
import com.tlc.crm.core.features.api.quota.model.TeamQuota;
import com.tlc.crm.core.features.api.quota.model.TeamUserQuota;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * Converter class for transforming between {@link TeamQuota} and their JSON representations
 * for use in APIs and UI flows.
 *
 * @author Kishore J
 * @version 1.0
 */
public class TeamQuotaConverter extends AbstractQuotaConverter<TeamQuota, JsonObject> {

    private TeamQuotaConverter() {
    }

    private static class Instance {
        private static final TeamQuotaConverter INSTANCE = new TeamQuotaConverter();
    }

    /**
     * <p>
     * Gets the {@link TeamQuotaConverter}
     * </p>
     *
     * @return The {@link TeamQuotaConverter}
     */
    public static TeamQuotaConverter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param converterData the container encapsulating the input data to be converted (e.g., JsonObject or other source),
     *                      along with contextual information such as locale, user, or flags
     * @return The {@link TeamQuota}
     */
    @Override
    public TeamQuota convertToModel(final CrmConverterData<JsonObject> converterData) {
        final JsonObject dataJson = converterData.dataToBeConverted();

        final TeamQuota teamQuota;

        if (converterData.isPartialData()) {
            final QuotaHolder quotaHolder = quotaHolderManagerImpl.get(converterData.orgId(), dataJson.getLong(QuotaFields.ID));

            teamQuota = quotaHolder.getTeamQuota();
        } else {
            teamQuota = new TeamQuota();
        }
        loadQuotaFromJson(converterData, teamQuota);
        loadTeamQuotaJsonToModel(teamQuota, dataJson.getJsonObject(QuotaFields.TEAM));
        return teamQuota;
    }

    /**
     * {@inheritDoc}
     *
     * @param converterData the container encapsulating the domain model instance along with contextual metadata
     * @return The {@link JsonObject}
     */
    @Override
    public JsonObject convertToJson(final CrmConverterData<TeamQuota> converterData) {
        final TeamQuota teamQuota = converterData.dataToBeConverted();
        final JsonObject teamQuotaJson = convertModelToJson(teamQuota);

        teamQuotaJson.put(QuotaFields.TEAM, getTeamJson(teamQuota, teamQuota));
        teamQuotaJson.put(QuotaFields.IS_ACTIVE, teamQuota.isActive());
        return teamQuotaJson;
    }

    /**
     * <p>
     * Loads the team quota {@link JsonObject} to {@link TeamQuota}
     * </p>
     *
     * @param teamQuota     Represents the {@link TeamQuota}
     * @param teamQuotaJson Represents the team quota {@link JsonObject}
     */
    private void loadTeamQuotaJsonToModel(final TeamQuota teamQuota, final JsonObject teamQuotaJson) {
        final Long orgId = teamQuota.orgId();

        if (teamQuotaJson.containsKey(QuotaFields.TEAM_QUOTA_ID)) {
            teamQuota.setTeamQuotaId(teamQuotaJson.getLong(QuotaFields.TEAM_QUOTA_ID));
        }

        if (teamQuotaJson.containsKey(QuotaFields.ID)) {
            final Team team = new Team();

            team.setId(teamQuotaJson.getLong(QuotaFields.ID));
            teamQuota.setTeam(team);
        }
        teamQuota.setOrgId(orgId);

        if (teamQuotaJson.containsKey(QuotaFields.USERS)) {
            final JsonArray userArray = teamQuotaJson.getJsonArray(QuotaFields.USERS);
            final int size = userArray.size();
            final Collection<TeamUserQuota> teamUserQuotas = new ArrayList<>(size);

            for (int index = 0; index < size; index++) {
                final JsonObject userJson = userArray.getJsonObject(index);
                final TeamUserQuota teamUserQuota = new TeamUserQuota();
                final TeamUser teamUser = new TeamUser();
                final JsonObject attainment = userJson.getJsonObject(QuotaFields.ATTAINMENT);

                teamUserQuota.setOrgId(orgId);
                teamUserQuota.setEnabled(userJson.getBoolean(QuotaFields.IS_QUOTA_ASSIGNED));
                teamUser.setCrmUser(userService.getUser(orgId, userJson.getLong(QuotaFields.ID)));
                teamUserQuota.setTeamUser(teamUser);

                teamUserQuotas.add(teamUserQuota);
                teamUserQuota.setTarget(attainment.getDouble(QuotaFields.TARGET));
            }
            teamQuota.setTeamUserQuotas(teamUserQuotas);
        }
    }

    /**
     * <p>
     * Gets the team {@link JsonObject} for the given {@link Quota}
     * </p>
     *
     * @param quota     Represents the {@link Quota}
     * @param teamQuota Represents the {@link TeamQuota}
     * @return The team {@link JsonObject}
     */
    private JsonObject getTeamJson(final Quota quota, final TeamQuota teamQuota) {
        final Collection<TeamUserQuota> teamUserQuotas = teamQuota.getTeamUserQuotas();
        final JsonObject teamQuotaJson = Json.object();
        final JsonArray userArray = Json.array();
        final Long orgId = teamQuota.orgId();
        final Team team = teamQuota.getTeam();
        final boolean isDealValue = quota.getQuotaMetric().isDealValue();
        final double totalTargetValue = teamQuota.getTotalTargetValue();

        teamQuotaJson.put(QuotaFields.ID, team.id());
        teamQuotaJson.put(QuotaFields.TEAM_QUOTA_ID, teamQuota.getTeamQuotaId());
        teamQuotaJson.put(QuotaFields.NAME, team.getName());
        teamQuotaJson.put(QuotaFields.TOTAL_QUOTA_COUNT, isDealValue ? totalTargetValue : (long) totalTargetValue);
        teamQuotaJson.put(QuotaFields.IS_ACTIVE, teamQuota.isActive());

        for (final TeamUserQuota teamUserQuota : teamUserQuotas) {
            final TeamUser teamUser = teamUserQuota.getTeamUser();
            final CrmUser crmUser = teamUser.getCrmUser();

            if (Objects.nonNull(crmUser)) {
                final JsonObject userJson = Json.object();
                final DisplayPicture thumbnail = crmUser.getThumbnail();
                final Long crmUserId = crmUser.id();
                final JsonObject attainmentJson = Json.object();
                final Object targetValue = isDealValue ? teamUserQuota.getTargetValue()
                        : (long) teamUserQuota.getTargetValue();

                attainmentJson.put(QuotaFields.TARGET, targetValue);
                userJson.put(QuotaFields.ATTAINMENT, attainmentJson);

                userJson.put(QuotaFields.ID, crmUserId);
                userJson.put(QuotaFields.IS_ACTIVE_USER, teamUserQuota.isActiveUser());
                userJson.put(QuotaFields.NAME, crmUser.getFullName());
                userJson.put(QuotaFields.IS_REMOVED_FROM_TEAM, teamUserQuota.isExcludedFromTeam());
                userJson.put(QuotaFields.IS_NOT_PART_OF_TEAM_GOAL, teamUserQuota.isNotPartOfTeamGoal());
                userJson.put(QuotaFields.IS_QUOTA_ASSIGNED, teamUserQuota.isEnabled());
                userJson.put(QuotaFields.IS_MANAGER, teamUser.isManager());

                if (Objects.nonNull(thumbnail)) {
                    userJson.put(QuotaFields.THUMBNAIL, crmUserConverter.fetchProfileJson(thumbnail, orgId, crmUserId));
                }
                userArray.put(userJson);
            }
        }
        teamQuotaJson.put(QuotaFields.USERS, userArray);

        return teamQuotaJson;
    }
}
