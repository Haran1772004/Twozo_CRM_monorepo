package com.tlc.crm.core.features.api.activity.model.converter.goal;

import com.tlc.attachment.model.profilePicture.DisplayPicture;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.iam.api.model.converter.CrmUserConverter;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.TeamUser;
import com.tlc.crm.common.iam.internal.CrmUserEntity;
import com.tlc.crm.common.converter.ConverterConfig;

import com.tlc.crm.core.features.api.activity.manager.goal.ActivityGoalManager;
import com.tlc.crm.core.features.api.activity.model.goal.ActivityGoal;
import com.tlc.crm.core.features.api.activity.model.goal.Attainment;
import com.tlc.crm.core.features.api.activity.model.goal.GoalPeriod;
import com.tlc.crm.core.features.api.activity.model.goal.GoalRange;
import com.tlc.crm.core.features.api.activity.model.goal.GoalType;
import com.tlc.crm.core.features.api.activity.model.goal.TeamGoal;
import com.tlc.crm.core.features.api.activity.model.goal.TeamUserGoal;
import com.tlc.crm.core.features.api.activity.model.goal.UserGoal;

import com.tlc.crm.entity.builder.EntityConverter;
import com.tlc.crm.entity.features.api.activity.model.ActivityType;
import com.tlc.crm.entity.features.field.activity.ActivityGoalFields;

import com.tlc.i18n.I18nResolver;
import com.tlc.iam.common.models.User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * Provides different ways to convert the data from / into {@link ActivityGoal}
 * </p>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public final class ActivityGoalConverter extends EntityConverter<ActivityGoal> {

    private final ActivityGoalManager activityGoalManager;
    private final CrmUserConverter crmUserConverter;

    private ActivityGoalConverter() {
        activityGoalManager = ActivityGoalManager.getInstance();
        crmUserConverter = CrmUserConverter.getInstance();
    }

    private static final class Instance {
        private static final ActivityGoalConverter ACTIVITY_GOAL_CONVERTER = new ActivityGoalConverter();
    }

    /**
     * <p>
     * Gets the {@link ActivityGoalConverter} Instance
     * </p>
     *
     * @return {@link ActivityGoalConverter}
     */
    public static ActivityGoalConverter getInstance() {
        return Instance.ACTIVITY_GOAL_CONVERTER;
    }

    /**
     * <p>
     * Converts the {@link JsonObject} into {@link ActivityGoal}
     * </p>
     *
     * @param converterConfig Represents the converter configuration
     * @param jsonObject      Which contains the request data
     * @return The {@link ActivityGoal}
     */
    @Override
    public ActivityGoal convert(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        if (converterConfig.isPartialData()) {
            return constructActivityGoalByPartialData(converterConfig, jsonObject);
        } else {
            return constructActivityGoal(converterConfig, jsonObject);
        }
    }

    /**
     * <p>
     * Converts {@link ActivityGoal} into {@link JsonObject}
     * </p>
     *
     * @param activityGoal A model that holds the attributes of activityGoal
     * @return {@link JsonObject} that contains the activity goal information
     */
    @Override
    public JsonObject convert(final ConverterConfig converterConfig, final ActivityGoal activityGoal) {
        final I18nResolver resolver = i18nService.getResolver(converterConfig.locale());
        final JsonObject activityGoalJson = Json.object();
        final GoalType activityGoalType = activityGoal.getGoalType();
        final GoalPeriod activityGoalPeriod = activityGoal.getGoalPeriod();
        final GoalRange activityGoalRange = activityGoal.getGoalRange();
        final ActivityType activityType = activityGoal.getActivityType();
        final Attainment attainment = activityGoal.getAttainment();
        final long target = attainment.getTarget();
        final TeamGoal teamGoal = activityGoal.getTeamGoal();
        final JsonObject attainmentJson = Json.object();
        final Long orgId = converterConfig.orgId();

        activityGoalJson.put(ActivityGoalFields.ID, activityGoal.id());
        activityGoalJson.put(ActivityGoalFields.START_TIME, activityGoal.getStartTime());
        activityGoalJson.put(ActivityGoalFields.END_TIME, activityGoal.getEndTime());
        activityGoalJson.put(ActivityGoalFields.IS_EDITABLE, activityGoal.isEditable());
        activityGoalJson.put(ActivityGoalFields.IS_DELETABLE, activityGoal.isDeletable());

        if (Objects.nonNull(activityGoalType)) {
            final JsonObject goalTypeJson = Json.object();

            goalTypeJson.put(ActivityGoalFields.ID, activityGoalType.getId());
            goalTypeJson.put(ActivityGoalFields.NAME, resolver.get(activityGoalType.getName()));

            activityGoalJson.put(ActivityGoalFields.GOAL_TYPE, goalTypeJson);
        }

        if (Objects.nonNull(activityGoalPeriod)) {
            final JsonObject goalPeriodJson = Json.object();

            goalPeriodJson.put(ActivityGoalFields.ID, activityGoalPeriod.getId());
            goalPeriodJson.put(ActivityGoalFields.NAME, resolver.get(activityGoalPeriod.getName()));

            activityGoalJson.put(ActivityGoalFields.GOAL_PERIOD, goalPeriodJson);
        }

        if (Objects.nonNull(activityGoalRange)) {
            activityGoalJson.put(ActivityGoalFields.GOAL_RANGE, activityGoalRange.getId());
        }

        if (Objects.nonNull(activityType)) {
            final JsonObject activityTypeJson = Json.object();

            activityTypeJson.put(ActivityGoalFields.ID, activityType.id());
            activityTypeJson.put(ActivityGoalFields.NAME, activityType.getName());
            activityGoalJson.put(ActivityGoalFields.ACTIVITY_TYPE, activityTypeJson);
        }
        attainmentJson.put(ActivityGoalFields.TARGET, target);
        attainmentJson.put(ActivityGoalFields.ACHIEVED, attainment.getAchieved());
        attainmentJson.put(ActivityGoalFields.ACHIEVEMENT_RATE, attainment.getAchievementRate());
        activityGoalJson.put(ActivityGoalFields.ATTAINMENT, attainmentJson);

        if (Objects.isNull(teamGoal)) {
             activityGoal.getUserGoals().stream().findFirst().ifPresent(userGoal -> {
                 final CrmUser crmUser = userGoal.getCrmUser();

                 if (Objects.nonNull(crmUser)) {
                     final JsonObject userJson = Json.object();
                     final Long crmUserId = crmUser.id();
                     final DisplayPicture thumbnail = crmUser.getThumbnail();

                     userJson.put(ActivityGoalFields.ID, crmUserId);
                     userJson.put(ActivityGoalFields.NAME, crmUser.getFullName());

                     if (Objects.nonNull(thumbnail)) {
                         userJson.put(ActivityGoalFields.THUMBNAIL, crmUserConverter.fetchProfileJson(thumbnail, orgId, crmUserId));
                     }

                     activityGoalJson.put(ActivityGoalFields.USER, userJson);
                     activityGoalJson.put(ActivityGoalFields.IS_ACTIVE_USER, Objects.equals(User.State.ACTIVE, crmUser.getState()));
                 }
            });
        } else {
            activityGoalJson.put(ActivityGoalFields.TEAM, getTeamJson(resolver, teamGoal, target));
            activityGoalJson.put(ActivityGoalFields.IS_ACTIVE, teamGoal.isActive());
        }
        return activityGoalJson;
    }

    /**
     * <p>
     * Converts the team activity goal into users activity goal
     * </p>
     *
     * @param i18nResolver     Represents the {@link I18nResolver}
     * @param activityGoalJson Represents the {@link JsonObject} contains team {@link ActivityGoal} data
     * @return The {@link JsonArray} containing team user goals
     */
    public JsonArray convertTeamGoalJson(final I18nResolver i18nResolver, final JsonObject activityGoalJson) {
        final JsonObject teamJson = activityGoalJson.getJsonObject(ActivityGoalFields.TEAM);
        final JsonArray usersArray = teamJson.getJsonArray(ActivityGoalFields.USERS);
        final JsonArray teamUserGoalArray = Json.array();

        for (int index = 0; index < usersArray.size(); index++) {
            final JsonObject teamUserJson = usersArray.getJsonObject(index);

            if (teamUserJson.getBoolean(ActivityGoalFields.IS_QUOTA_ASSIGNED)) {
                final JsonObject teamUserGoalJson = Json.object(activityGoalJson.getBytes());
                final JsonObject teamGoalJson = teamUserGoalJson.getJsonObject(ActivityGoalFields.TEAM);

                teamUserGoalJson.remove(ActivityGoalFields.TEAM);
                final JsonObject goalTypeJson = teamUserGoalJson.getJsonObject(ActivityGoalFields.GOAL_TYPE);
                final GoalType goalType = GoalType.PART_OF_TEAM_GOAL;

                goalTypeJson.put(ActivityGoalFields.ID, goalType.getId());
                goalTypeJson.put(ActivityGoalFields.NAME, i18nResolver.get(goalType.getName()));
                final JsonObject userJson = Json.object();

                userJson.put(ActivityGoalFields.ID, teamUserJson.getString(ActivityGoalFields.ID));
                userJson.put(ActivityGoalFields.NAME, teamUserJson.getString(ActivityGoalFields.NAME));

                teamUserGoalJson.put(ActivityGoalFields.USER, userJson);
                teamUserGoalJson.put(ActivityGoalFields.TEAM_NAME, teamGoalJson.optString(ActivityGoalFields.NAME, null));
                teamUserGoalJson.put(ActivityGoalFields.IS_REMOVED_FROM_TEAM, teamUserJson.getBoolean(ActivityGoalFields.IS_REMOVED_FROM_TEAM));
                teamUserGoalJson.put(ActivityGoalFields.IS_ACTIVE_USER, teamUserJson.getBoolean(ActivityGoalFields.IS_ACTIVE_USER));
                teamUserGoalJson.put(ActivityGoalFields.ATTAINMENT, teamUserJson.getJsonObject(ActivityGoalFields.ATTAINMENT));
                teamUserGoalJson.put(ActivityGoalFields.IS_EDITABLE, activityGoalJson.optBoolean(ActivityGoalFields.IS_EDITABLE));
                teamUserGoalJson.put(ActivityGoalFields.IS_DELETABLE, false);
                teamUserGoalArray.put(teamUserGoalJson);
            }
        }
        return teamUserGoalArray;
    }

    /**
     * <p>
     * Converts the partial json into the activity goal
     * </p>
     *
     * @param converterConfig Represents the converter configuration
     * @param jsonObject      Which contains the partial activity goal data
     * @return The {@link ActivityGoal}
     */
    private ActivityGoal constructActivityGoalByPartialData(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final ActivityGoal activityGoal = activityGoalManager.get(converterConfig.orgId(), jsonObject.getLong(ActivityGoalFields.ID));

        setJsonDataIntoModel(converterConfig, activityGoal, jsonObject);

        return activityGoal;
    }

    /**
     * <p>
     * Converts the {@link JsonObject} into {@link ActivityGoal}
     * </p>
     *
     * @param converterConfig Represents the converter configuration
     * @param jsonObject      Which contains the request data
     * @return The {@link ActivityGoal}
     */
    private ActivityGoal constructActivityGoal(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final ActivityGoal activityGoal = new ActivityGoal();
        final String idKey = ActivityGoalFields.ID;

        if (jsonObject.containsKey(idKey)) {
            activityGoal.setId(jsonObject.getLong(idKey));
        }
        setJsonDataIntoModel(converterConfig, activityGoal, jsonObject);

        return activityGoal;
    }

    /**
     * <p>
     * Sets the json data into the {@link ActivityGoal}
     * </p>
     *
     * @param converterConfig Represents the converter configuration
     * @param activityGoal    An entity that holds the activity goal attributes
     * @param jsonObject      Which contains the data
     */
    private void setJsonDataIntoModel(final ConverterConfig converterConfig, final ActivityGoal activityGoal, final JsonObject jsonObject) {
        final long orgId = converterConfig.orgId();

        activityGoal.setOrgId(orgId);
        activityGoal.setOwnerId(converterConfig.ownerId());

        if (jsonObject.containsKey(ActivityGoalFields.ACTIVITY_TYPE)) {
            final JsonObject activityTypeJson = jsonObject.getJsonObject(ActivityGoalFields.ACTIVITY_TYPE);
            final ActivityType activityType = new ActivityType();

            if (activityTypeJson.containsKey(ActivityGoalFields.ID)) {
                activityType.setId(activityTypeJson.getLong(ActivityGoalFields.ID));
            }

            if (activityTypeJson.containsKey(ActivityGoalFields.NAME)) {
                activityType.setName(activityTypeJson.getString(ActivityGoalFields.NAME));
            }
            activityGoal.setActivityType(activityType);
        }

        if (jsonObject.containsKey(ActivityGoalFields.GOAL_PERIOD)) {
            activityGoal.setGoalPeriod(GoalPeriod.getGoalPeriodById(jsonObject.getShort(ActivityGoalFields.GOAL_PERIOD)));
        }

        if (jsonObject.containsKey(ActivityGoalFields.GOAL_RANGE)) {
            activityGoal.setGoalRange(GoalRange.getGoalRangeById(jsonObject.getShort(ActivityGoalFields.GOAL_RANGE)));
        }

        if (jsonObject.containsKey(ActivityGoalFields.GOAL_TYPE)) {
            activityGoal.setGoalType(GoalType.getGoalTypeById(jsonObject.getShort(ActivityGoalFields.GOAL_TYPE)));
        }

        if (jsonObject.containsKey(ActivityGoalFields.START_TIME)) {
            activityGoal.setStartTime(jsonObject.getLong(ActivityGoalFields.START_TIME));
        }

        if (jsonObject.containsKey(ActivityGoalFields.END_TIME)) {
            activityGoal.setEndTime(jsonObject.getLong(ActivityGoalFields.END_TIME));
        }

        if (jsonObject.containsKey(ActivityGoalFields.TEAM)) {
            loadTeamGoalJsonIntoModel(activityGoal, jsonObject.getJsonObject(ActivityGoalFields.TEAM));
        }

        if (jsonObject.containsKey(ActivityGoalFields.USERS)) {
            final Collection<UserGoal> userGoals = new ArrayList<>();
            final JsonArray userArray = jsonObject.getJsonArray(ActivityGoalFields.USERS);

            if (0 < userArray.size()) {

                for (int index = 0; index < userArray.size(); index++) {
                    final UserGoal userGoal = new UserGoal();
                    final CrmUser crmUser = new CrmUserEntity();

                    crmUser.setId(userArray.getLong(index));
                    userGoal.setOrgId(orgId);
                    userGoal.setCrmUser(crmUser);
                    userGoals.add(userGoal);
                }
                activityGoal.setUserGoals(userGoals);
            }
        } else if (jsonObject.containsKey(ActivityGoalFields.USER)) {
            final JsonObject userJson = jsonObject.getJsonObject(ActivityGoalFields.USER);
            final UserGoal userGoal = new UserGoal();
            final CrmUser crmUser = new CrmUserEntity();

            if (userJson.containsKey(ActivityGoalFields.ID)) {
                crmUser.setId(userJson.getLong(ActivityGoalFields.ID));
            }
            userGoal.setOrgId(orgId);
            userGoal.setCrmUser(crmUser);

            activityGoal.setUserGoals(List.of(userGoal));
        }

        if (jsonObject.containsKey(ActivityGoalFields.ATTAINMENT)) {
            final JsonObject attainmentJson = jsonObject.getJsonObject(ActivityGoalFields.ATTAINMENT);

            if (attainmentJson.containsKey(ActivityGoalFields.TARGET)) {
                final Attainment attainment = new Attainment();

                attainment.setTarget(attainmentJson.getLong(ActivityGoalFields.TARGET));
                activityGoal.setAttainment(attainment);
            }
        }
    }

    /**
     * <p>
     * Loads the team goal json data into the {@link TeamGoal}
     * </p>
     *
     * @param activityGoal An entity that holds the activityGoal attributes
     * @param teamGoalJson Which contains the data
     */
    private void loadTeamGoalJsonIntoModel(final ActivityGoal activityGoal, final JsonObject teamGoalJson) {
        final long orgId = activityGoal.orgId();
        final TeamGoal teamGoal = new TeamGoal();

        if (teamGoalJson.containsKey(ActivityGoalFields.ID)) {
            teamGoal.setTeamId(teamGoalJson.getLong(ActivityGoalFields.ID));
        }
        teamGoal.setOrgId(orgId);
        teamGoal.setSplitEnabled(teamGoalJson.optBoolean(ActivityGoalFields.IS_SPLIT_ENABLED, true));

        if (teamGoalJson.containsKey(ActivityGoalFields.USERS)) {
            final Collection<TeamUserGoal> teamUserGoals = new ArrayList<>();
            final JsonArray userArray = teamGoalJson.getJsonArray(ActivityGoalFields.USERS);

            for (int index = 0; index < userArray.size(); index++) {
                final JsonObject userJson = userArray.getJsonObject(index);
                final TeamUserGoal teamUserGoal = new TeamUserGoal();

                if (userJson.containsKey(ActivityGoalFields.ID)) {
                    final TeamUser teamUser = new TeamUser();
                    final CrmUser crmUser = new CrmUserEntity();

                    crmUser.setId(userJson.getLong(ActivityGoalFields.ID));
                    teamUser.setCrmUser(crmUser);
                    teamUserGoal.setTeamUser(teamUser);
                }
                teamUserGoal.setOrgId(orgId);
                teamUserGoal.setQuotaEnabled(userJson.optBoolean(ActivityGoalFields.IS_QUOTA_ASSIGNED, true));
                teamUserGoal.setRemovedFromTeam(userJson.optBoolean(ActivityGoalFields.IS_REMOVED_FROM_TEAM, false));

                if (userJson.containsKey(ActivityGoalFields.ATTAINMENT)) {
                    final JsonObject attainmentJson = userJson.getJsonObject(ActivityGoalFields.ATTAINMENT);

                    if (attainmentJson.containsKey(ActivityGoalFields.TARGET)) {
                        final Attainment attainment = new Attainment();

                        attainment.setTarget(attainmentJson.getLong(ActivityGoalFields.TARGET));
                        teamUserGoal.setAttainment(attainment);
                    }
                }
                teamUserGoals.add(teamUserGoal);
            }
            teamGoal.setTeamUserGoals(teamUserGoals);
        }
        activityGoal.setTeamGoal(teamGoal);
    }

    /**
     * <p>
     * Loads {@link TeamGoal} into {@link JsonObject}
     * </p>
     *
     * @param resolver   Represents the {@link I18nResolver}
     * @param teamGoal   A model that holds the attributes of team goal
     * @param teamTarget Represents the team target
     * @return {@link JsonObject} that contains the team goal information
     */
    private JsonObject getTeamJson(final I18nResolver resolver, final TeamGoal teamGoal, final Long teamTarget) {
        final Collection<TeamUserGoal> teamUserGoals = teamGoal.getTeamUserGoals();
        final JsonObject teamJson = Json.object();
        final JsonArray userArray = Json.array();
        final Long orgId = teamGoal.orgId();

        teamJson.put(ActivityGoalFields.ID, teamGoal.getTeamId());
        teamJson.put(ActivityGoalFields.NAME, teamGoal.getTeamName());
        teamJson.put(ActivityGoalFields.IS_SPLIT_ENABLED, teamGoal.isSplitEnabled());
        teamJson.put(ActivityGoalFields.TOTAL_QUOTA_COUNT, teamTarget);
        teamJson.put(ActivityGoalFields.IS_ACTIVE, teamGoal.isActive());

        for (final TeamUserGoal teamUserGoal : teamUserGoals) {
            final TeamUser teamUser = teamUserGoal.getTeamUser();
            final CrmUser crmUser = teamUser.getCrmUser();

            if (Objects.nonNull(crmUser)) {
                final JsonObject userJson = Json.object();
                final Attainment attainment = teamUserGoal.getAttainment();
                final JsonObject attainmentJson = Json.object();
                final String userName = crmUser.getFullName();
                final DisplayPicture thumbnail = crmUser.getThumbnail();
                final Long crmUserId = crmUser.id();

                attainmentJson.put(ActivityGoalFields.ACHIEVED, attainment.getAchieved());
                attainmentJson.put(ActivityGoalFields.ACHIEVEMENT_RATE, attainment.getAchievementRate());
                attainmentJson.put(ActivityGoalFields.TARGET, attainment.getTarget());

                userJson.put(ActivityGoalFields.ID, crmUserId);
                userJson.put(ActivityGoalFields.IS_ACTIVE_USER, teamUserGoal.isActiveUser());
                userJson.put(ActivityGoalFields.NAME, userName);
                userJson.put(ActivityGoalFields.IS_REMOVED_FROM_TEAM, teamUserGoal.isRemovedFromTeam());
                userJson.put(ActivityGoalFields.ATTAINMENT, attainmentJson);
                userJson.put(ActivityGoalFields.IS_QUOTA_ASSIGNED, teamUserGoal.isQuotaEnabled());
                userJson.put(ActivityGoalFields.IS_MANAGER, teamUser.isManager());

                if (Objects.nonNull(thumbnail)) {
                    userJson.put(ActivityGoalFields.THUMBNAIL, crmUserConverter.fetchProfileJson(thumbnail, orgId, crmUserId));
                }
                userArray.put(userJson);
            }
        }
        teamJson.put(ActivityGoalFields.USERS, userArray);

        return teamJson;
    }
}