package com.tlc.crm.core.features.api.activity.manager.goal;

import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.common.iam.api.manager.TeamManager;
import com.tlc.crm.common.iam.api.model.Team;
import com.tlc.crm.common.iam.api.model.TeamUser;
import com.tlc.crm.common.iam.internal.CrmUserEntity;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.core.features.api.activity.model.goal.ActivityGoal;
import com.tlc.crm.core.features.api.activity.model.goal.Attainment;
import com.tlc.crm.core.features.api.activity.model.goal.GoalPeriod;
import com.tlc.crm.core.features.api.activity.model.goal.GoalRange;
import com.tlc.crm.core.features.api.activity.model.goal.GoalType;
import com.tlc.crm.core.features.api.activity.model.goal.TeamGoal;
import com.tlc.crm.core.features.api.activity.model.goal.TeamUserGoal;
import com.tlc.crm.core.features.api.activity.model.goal.UserGoal;

import com.tlc.crm.entity.features.api.activity.manager.ActivityManager;
import com.tlc.crm.entity.features.api.activity.manager.ActivityTypeManager;
import com.tlc.crm.entity.features.field.activity.ActivityGoalFields;
import com.tlc.crm.entity.internal.status.ActivityGoalErrorCodes;
import com.tlc.crm.entity.sql.resource.OMACACTIVITYGOAL;
import com.tlc.crm.entity.sql.resource.OMACACTIVITYTYPE;
import com.tlc.crm.entity.sql.resource.OMACTEAMACTIVITYGOAL;
import com.tlc.crm.entity.sql.resource.OMACTEAMUSERACTIVITYGOAL;
import com.tlc.crm.entity.sql.resource.OMACUSERACTIVITYGOAL;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import com.tlc.validator.ErrorKeys;
import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * Implements the {@link ActivityGoal} related DB actions
 * </p>
 *
 * @author Muthu Kumar V
 * @version 1.0
 */
public final class ActivityGoalManager extends AbstractConfigManager<ActivityGoal> {

    private final ActivityTypeManager activityTypeManager;
    private final UserService userService;
    private final TeamManager teamManager;
    private final ActivityManager activityManager;

    private ActivityGoalManager() {
        activityTypeManager = ActivityTypeManager.getInstance();
        userService = UserService.getInstance();
        teamManager = TeamManager.getInstance();
        activityManager = ActivityManager.getInstance();
    }

    private static final class Instance {
        private static final ActivityGoalManager ACTIVITY_GOAL_MANAGER = new ActivityGoalManager();
    }

    /**
     * <p>
     * Gets the {@link ActivityGoalManager} instance
     * </p>
     *
     * @return {@link ActivityGoalManager}
     */
    public static ActivityGoalManager getInstance() {
        return Instance.ACTIVITY_GOAL_MANAGER;
    }

    /**
     * {@inheritDoc}
     *
     * @return the {@link Table} of activity goal
     */
    @Override
    public Table getTable() {
        return Table.get(OMACACTIVITYGOAL.TABLE);
    }

    /**
     * <p>
     * Saves the attributes of {@link ActivityGoal} into the database
     * </p>
     *
     * @param activityGoal The {@link ActivityGoal}
     */
    @Override
    public ActivityGoal create(final ActivityGoal activityGoal) {
        final DataContainer targetContainer = DataContainer.create();

        loadActivityGoalIntoContainer(true, activityGoal, targetContainer);
        orgDataStore(activityGoal.orgId()).commitChanges(targetContainer);

        return activityGoal;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId           Represents the organization id
     * @param activityGoalIds Collection of activity goal ids
     * @return The collection of {@link ActivityGoal}
     */
    @Override
    public Collection<ActivityGoal> get(final Long orgId, final Collection<Long> activityGoalIds) {
        final DataContainer activityGoalDataContainer = getActivityGoalContainer(orgId, activityGoalIds);
        final Stream<Row> activityGoalRows = activityGoalDataContainer.getRows(getTable());

        return activityGoalRows.map(row -> convertRowIntoModel(row, activityGoalDataContainer, false)).toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<ActivityGoal> partialGet(final Long orgId, final Collection<Long> ids) {
        return Collections.emptyList();
    }

    /**
     * Gets the count of total activity goal of given orgId.
     *
     * @param orgId Represents the organization id.
     * @return The total activity goals count.
     */
    public long getActivityGoalCount(final long orgId) {
        final CountQuery countQuery = CountQuery.get(getTable());

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * Retrieves the Collection of {@link ActivityGoal} for given orgId.
     *
     * @param orgId     Refers the organization id.
     * @param start     Refers the start value of activity goal to be fetched.
     * @param limit     Refers the limit value of activity goal to be fetched.
     * @param sortField Refers the field by which the data is sorted.
     * @param sortType  Refers the sort type.
     * @return Collection of {@link ActivityGoal}.
     */
    public Collection<ActivityGoal> getActivityGoalList(final long orgId, final int start, final int limit,
                                                        final String sortField, final String sortType) {
        final Table activityGoalTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(activityGoalTable);
        final Column activityGoalPkColumn = activityGoalTable.getPKColumn();
        final boolean isAsc = ActivityGoalFields.ASC.equalsIgnoreCase(sortType);
        final Table activityTypeTable = activityTypeManager.getTable();
        final OrderByClause.OrderType orderType = isAsc
                ? OrderByClause.OrderType.ASCENDING : OrderByClause.OrderType.DESCENDING;

        selectQuery.addSelectClause(activityGoalPkColumn);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(activityGoalTable.getColumn(OMACACTIVITYGOAL.ORG_ID), orgId)));
        selectQuery.setLimitClause(new LimitClause(start, limit));

        switch (sortField) {
            case "dateRange" -> selectQuery.addOrderByClause(new OrderByClause(activityGoalTable.getColumn(OMACACTIVITYGOAL.START_TIME), orderType));
            case "activity" -> {
                selectQuery.addJoinClause(new JoinClause(activityGoalTable.getColumn(OMACACTIVITYGOAL.ACTIVITY_TYPE_ID), activityTypeTable.getPKColumn()));
                selectQuery.addOrderByClause(new OrderByClause(activityTypeTable.getColumn(OMACACTIVITYTYPE.NAME), orderType));
            }
            default -> selectQuery.addOrderByClause(new OrderByClause(activityGoalTable.getColumn(OMACACTIVITYGOAL.CREATED_TIME), orderType));
        }
        final Collection<Long> pkValues = orgDataStore(orgId).get(selectQuery).getRows(activityGoalTable).map(Row::getPKValue).toList();
        final DataContainer dataContainer = getUserGoalDataContainer(orgId, pkValues);

        dataContainer.append(getTeamGoalDataContainer(orgId, pkValues));

        final List<ActivityGoal> activityGoals = pkValues.stream().map(id -> dataContainer.getRow(activityGoalTable, id))
                .filter(Objects::nonNull).map(row -> convertRowIntoModel(row, dataContainer, true)).toList();

        final Comparator<ActivityGoal> comparator = switch (sortField.toLowerCase()) {
            case "attainment" -> Comparator.comparingInt(activityGoal -> activityGoal.getAttainment().getAchievementRate());
            case "type" -> Comparator.comparing(activityGoal -> activityGoal.getGoalType().getName());
            case "period" -> Comparator.comparing(activityGoal -> activityGoal.getGoalPeriod().getName());
            default -> null;
        };

        if (Objects.nonNull(comparator)) {

            if (isAsc) {
                return activityGoals.stream().sorted(comparator).toList();
            } else {
                return activityGoals.stream().sorted(comparator.reversed()).toList();
            }
        }
        return activityGoals;
    }

    /**
     * <p>
     * Checks whether the user is the owner of the {@link ActivityGoal} or not
     * </p>
     *
     * @param orgId          Represents the organisation ID
     * @param userId         Represents the user ID
     * @param activityGoalId Represents the {@link ActivityGoal} id
     * @return True if the user is the owner of the activity goal, false otherwise
     */
    public boolean isGoalOwner(final Long orgId, final Long userId, final Long activityGoalId) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(table.getPKColumn(), activityGoalId))
                .and(Criteria.eq(table.getColumn(OMACACTIVITYGOAL.OWNER_ID), userId)));

        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * <p>
     * Gets the activity goal ownership map
     * </p>
     *
     * @param orgId           Represents the organisation ID
     * @param userId          Represents the user ID
     * @param activityGoalIds Represents the {@link ActivityGoal} ids
     * @return The map containing key as activity goal id and value as the user is owner or not
     */
    public Map<Long, Boolean> getGoalOwnerMap(final Long orgId, final Long userId, final Collection<Long> activityGoalIds) {
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(table.getColumn(OMACACTIVITYGOAL.OWNER_ID));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(table.getPKColumn(), activityGoalIds)));

        return orgDataStore(orgId).get(selectQuery).getRows(table).collect(Collectors.toMap(Row::getPKValue,
                row -> Objects.equals(row.get(OMACACTIVITYGOAL.OWNER_ID), userId)));
    }

    /**
     * <p>
     *  Checks whether the user exists in team user goal
     * </p>
     *
     * @param orgId          Represents the organisation ID
     * @param userId         Represents the user ID
     * @param activityGoalId Represents the {@link ActivityGoal} id
     * @return True if the user exists in the team user activity goal, false otherwise
     */
    public boolean isTeamUserGoalExists(final Long orgId, final Long userId, final Long activityGoalId) {
        final Table teamGoalTable = getTeamGoalTable();
        final Table teamUserGoalTable = getTeamUserGoalTable();
        final CountQuery countQuery = CountQuery.get(teamGoalTable);

        countQuery.addJoinClause(new JoinClause(teamGoalTable.getPKColumn(), teamUserGoalTable.getColumn(OMACTEAMUSERACTIVITYGOAL.TEAM_ACTIVITY_GOAL_ID)));
        countQuery.setWhereClause(new WhereClause(Criteria.eq(teamGoalTable.getColumn(OMACTEAMACTIVITYGOAL.ACTIVITY_GOAL_ID), activityGoalId)));

        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * <p>
     *  Checks whether the user exists in user activity goal
     * </p>
     *
     * @param orgId          Represents the organisation ID
     * @param userId         Represents the user ID
     * @param activityGoalId Represents the {@link ActivityGoal} id
     * @return True if the user exists in the user activity goal, false otherwise
     */
    public boolean isUserGoalExists(final Long orgId, final Long userId, final Long activityGoalId) {
        final Table userGoalTable = getUserGoalTable();
        final CountQuery countQuery = CountQuery.get(userGoalTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(userGoalTable.getColumn(OMACUSERACTIVITYGOAL.ACTIVITY_GOAL_ID), activityGoalId))
                .and(Criteria.eq(userGoalTable.getColumn(OMACUSERACTIVITYGOAL.USER_ID), userId)));

        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * <p>
     * Gets the {@link UserGoal} data container
     * </p>
     *
     * @param orgId           Represents the organisation Id
     * @param activityGoalIds Represents the collection of activity goal ids
     * @return The {@link UserGoal} data container
     */
    private DataContainer getUserGoalDataContainer(final Long orgId, final Collection<Long> activityGoalIds) {
        final Table activityGoalTable = getTable();
        final Table userGoalTable = getUserGoalTable();
        final Table activityTypeTable = activityTypeManager.getTable();
        final Column activityGoalPkColumn = activityGoalTable.getPKColumn();
        final Column activityTypeColumn = activityGoalTable.getColumn(OMACACTIVITYGOAL.ACTIVITY_TYPE_ID);
        final SelectQuery userGoalSelectQuery = SelectQuery.get(activityGoalTable);

        userGoalSelectQuery.addSelectClause(activityGoalTable, userGoalTable);
        userGoalSelectQuery.addSelectClause(List.of(activityTypeTable.getPKColumn(), activityTypeTable.getColumn(OMACACTIVITYTYPE.NAME),
                activityTypeTable.getColumn(OMACACTIVITYTYPE.IS_ACTIVE)));
        userGoalSelectQuery.addJoinClause(List.of(new JoinClause(activityTypeColumn, activityTypeTable.getPKColumn()),
                new JoinClause(activityGoalPkColumn, userGoalTable.getColumn(OMACUSERACTIVITYGOAL.ACTIVITY_GOAL_ID))));
        userGoalSelectQuery.setWhereClause(new WhereClause(Criteria.in(activityGoalPkColumn, activityGoalIds)));

        return orgDataStore(orgId).get(userGoalSelectQuery);
    }

    /**
     * <p>
     * Gets the {@link TeamGoal} data container
     * </p>
     *
     * @param orgId           Represents the organisation Id
     * @param activityGoalIds Represents the collection of activity goal ids
     * @return The {@link TeamGoal} data container
     */
    private DataContainer getTeamGoalDataContainer(final Long orgId, final Collection<Long> activityGoalIds) {
        final Table activityGoalTable = getTable();
        final Table activityTypeTable = activityTypeManager.getTable();
        final Table teamGoalTable = getTeamGoalTable();
        final Table teamUserGoalTable = getTeamUserGoalTable();
        final Column activityGoalPkColumn = activityGoalTable.getPKColumn();
        final Column activityTypeColumn = activityGoalTable.getColumn(OMACACTIVITYGOAL.ACTIVITY_TYPE_ID);
        final SelectQuery teamGoalSelectQuery = SelectQuery.get(activityGoalTable);

        teamGoalSelectQuery.addSelectClause(activityGoalTable, teamGoalTable, teamUserGoalTable);
        teamGoalSelectQuery.addSelectClause(List.of(activityTypeTable.getPKColumn(), activityTypeTable.getColumn(OMACACTIVITYTYPE.NAME),
                activityTypeTable.getColumn(OMACACTIVITYTYPE.IS_ACTIVE)));
        teamGoalSelectQuery.addJoinClause(List.of(new JoinClause(activityTypeColumn, activityTypeTable.getPKColumn()),
                new JoinClause(activityGoalPkColumn, teamGoalTable.getColumn(OMACTEAMACTIVITYGOAL.ACTIVITY_GOAL_ID)),
                new JoinClause(teamGoalTable.getPKColumn(), teamUserGoalTable.getColumn(OMACTEAMUSERACTIVITYGOAL.TEAM_ACTIVITY_GOAL_ID))));
        teamGoalSelectQuery.setWhereClause(new WhereClause(Criteria.in(activityGoalPkColumn, activityGoalIds)));

        return orgDataStore(orgId).get(teamGoalSelectQuery);
    }

    /**
     * <p>
     * Gets the {@link Attainment} for the {@link ActivityGoal}
     * </p>
     *
     * @param orgId          Represents the organisation Id
     * @param activityGoalId Represents the activity goal Id
     * @return The {@link Attainment}
     */
    public Attainment getAttainment(final Long orgId, final Long activityGoalId, final Long teamUserId) {
        final DataContainer dataContainer = getActivityGoalContainer(orgId, List.of(activityGoalId));
        final Row activityGoalRow = dataContainer.getRow(getTable());
        final Short goalTypeId = activityGoalRow.get(OMACACTIVITYGOAL.TYPE);
        final Collection<Long> userIds = new HashSet<>();
        final Attainment attainment = new Attainment();
        final long targetCount;

        if (Objects.equals(GoalType.USER_GOAL.getId(), goalTypeId)) {
            final Row userGoalRow = dataContainer.getRow(getUserGoalTable());
            final Long userId = userGoalRow.get(OMACUSERACTIVITYGOAL.USER_ID);
            targetCount = activityGoalRow.get(OMACACTIVITYGOAL.TARGET);

            userIds.add(userId);
        } else if (Objects.nonNull(teamUserId)) {
            final Table teamUserGoalTable = getTeamUserGoalTable();
            targetCount = dataContainer.getRow(teamUserGoalTable,
                    new WhereClause(Criteria.eq(teamUserGoalTable.getColumn(OMACTEAMUSERACTIVITYGOAL.USER_ID), teamUserId))).get(OMACTEAMUSERACTIVITYGOAL.QUOTA);

            userIds.add(teamUserId);
        } else {
            dataContainer.getRows(getTeamUserGoalTable()).forEach(row -> {
                final Long userId = row.get(OMACTEAMUSERACTIVITYGOAL.USER_ID);

                userIds.add(userId);
            });
            targetCount = activityGoalRow.get(OMACACTIVITYGOAL.TARGET);
        }
        final long achievedCount = activityManager.getCompletedActivityCount(orgId, userIds, activityGoalRow.get(OMACACTIVITYGOAL.ACTIVITY_TYPE_ID),
                activityGoalRow.get(OMACACTIVITYGOAL.START_TIME), activityGoalRow.get(OMACACTIVITYGOAL.END_TIME));

        attainment.setTarget(targetCount);
        attainment.setAchieved(achievedCount);
        attainment.setAchievementRate(attainment.calculateAchievementRate());

        return attainment;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId         Represents the organisation id
     * @param activityGoals Represents the {@link Collection<ActivityGoal>}
     * @return {@link Map} Which contains the response data
     */
    @Override
    protected ProcessOutcome update(final Long orgId, final Collection<ActivityGoal> activityGoals) {
        final ProcessOutcome processOutcome = new ProcessOutcome();
        final List<ErrorDescriptor> failedEntities = new ArrayList<>();
        final Collection<Long> activityGoalIds = activityGoals.stream().map(ActivityGoal::id).filter(Objects::nonNull).collect(Collectors.toSet());
        final DataContainer existingActivityGoalContainer = getActivityGoalContainer(orgId, activityGoalIds);

        for (final ActivityGoal activityGoal : activityGoals) {
            final Long activityGoalId = activityGoal.id();
            final TeamGoal teamGoal = activityGoal.getTeamGoal();
            final DataContainer targetContainer = DataContainer.create();

            loadActivityGoalIntoContainer(false, activityGoal, existingActivityGoalContainer, targetContainer);

            if (Objects.isNull(teamGoal)) {
                activityGoal.getUserGoals().stream().findFirst().ifPresent(userGoal -> loadUserGoalIntoContainer(orgId,
                        userGoal.getCrmUser().id(), false, activityGoalId, existingActivityGoalContainer, targetContainer));
            } else {
                loadTeamGoalIntoContainer(false, teamGoal, activityGoalId, existingActivityGoalContainer, targetContainer);
            }

            try {
                orgDataStore(orgId).commitChanges(targetContainer);
                processOutcome.addProcessedRecord(activityGoalId);
            } catch (final ErrorCode errorCode) {
                LOGGER.error("Activity goal update failed", errorCode);
                failedEntities.add(new ErrorDescriptor(errorCode, ErrorKeys.UPDATE_ACTION_FAILED, activityGoal.id().toString()));
            }
        }

        if (!failedEntities.isEmpty()) {
            processOutcome.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED,
                    "i18n.core.update.failed")), failedEntities));
        }
        return processOutcome;
    }

    /**
     * <p>
     * Loads the {@link ActivityGoal} details into the data container.
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event.
     * @param activityGoal    An entity that holds the {@link ActivityGoal} attributes.
     * @param targetContainer Holds the updated information of user defined and system field values.
     */
    private void loadActivityGoalIntoContainer(final boolean isCreateEvent, final ActivityGoal activityGoal, final DataContainer targetContainer) {
        final long orgId = activityGoal.orgId();
        final TeamGoal teamGoal = activityGoal.getTeamGoal();
        final DataContainer existingActivityGoalContainer = isCreateEvent ? null : orgDataStore(orgId).get(getTable(), List.of(activityGoal.id()));

        if (Objects.isNull(teamGoal)) {
            final Collection<UserGoal> userGoals = activityGoal.getUserGoals();

            for (final UserGoal userGoal : userGoals) {
                loadActivityGoalIntoContainer(isCreateEvent, activityGoal, existingActivityGoalContainer, targetContainer);
                loadUserGoalIntoContainer(orgId, userGoal.getCrmUser().id(), isCreateEvent, activityGoal.id(), existingActivityGoalContainer, targetContainer);
            }
        } else {
            loadActivityGoalIntoContainer(isCreateEvent, activityGoal, existingActivityGoalContainer, targetContainer);
            loadTeamGoalIntoContainer(isCreateEvent, teamGoal, activityGoal.id(), existingActivityGoalContainer, targetContainer);
        }
    }

    /**
     * <p>
     * Loads the {@link ActivityGoal} details into the data container.
     * </p>
     *
     * @param isCreateEvent                 A flag which indicates whether the action is related to create or update event.
     * @param activityGoal                  An entity that holds the {@link ActivityGoal} attributes.
     * @param existingActivityGoalContainer Holds the user defined and system field values of existing activity goal.
     * @param targetContainer               Holds the updated information of user defined and system field values.
     */
    private void loadActivityGoalIntoContainer(final boolean isCreateEvent, final ActivityGoal activityGoal,
                                               final DataContainer existingActivityGoalContainer, final DataContainer targetContainer) {
        final Table activityGoalTable = getTable();
        final long currentTime = Instant.now().toEpochMilli();
        final Row activityGoalRow = isCreateEvent ? new Row(activityGoalTable) : existingActivityGoalContainer.getRow(activityGoalTable, activityGoal.id());

        if (!isCreateEvent && Objects.isNull(activityGoalRow)) {
            throw ErrorCode.get(ActivityGoalErrorCodes.ACTIVITY_GOAL_NOT_FOUND, "i18n.activity.goal.not.found");
        }

        if (activityGoalRow.isNewRow()) {
            activityGoal.setId(activityGoalRow.getPKValue());
            activityGoalRow.setOrgId(activityGoal.orgId());
            activityGoalRow.set(OMACACTIVITYGOAL.OWNER_ID, activityGoal.getOwnerId());
            activityGoalRow.set(OMACACTIVITYGOAL.CREATED_TIME, currentTime);
        }
        activityGoalRow.set(OMACACTIVITYGOAL.UPDATED_TIME, currentTime);
        activityGoalRow.set(OMACACTIVITYGOAL.TYPE, activityGoal.getGoalType().getId());
        activityGoalRow.set(OMACACTIVITYGOAL.ACTIVITY_TYPE_ID, activityGoal.getActivityType().id());
        activityGoalRow.set(OMACACTIVITYGOAL.PERIOD, activityGoal.getGoalPeriod().getId());
        activityGoalRow.set(OMACACTIVITYGOAL.PERIOD_CATEGORY, activityGoal.getGoalRange().getId());
        activityGoalRow.set(OMACACTIVITYGOAL.START_TIME, activityGoal.getStartTime());
        activityGoalRow.set(OMACACTIVITYGOAL.END_TIME, activityGoal.getEndTime());
        activityGoalRow.set(OMACACTIVITYGOAL.TARGET, activityGoal.getAttainment().getTarget());

        targetContainer.updateRow(activityGoalRow);
    }

    /**
     * <p>
     * Loads the {@link TeamGoal} details into the data container.
     * </p>
     *
     * @param isCreateEvent                 A flag which indicates whether the action is related to create or update event.
     * @param teamGoal                      An entity that holds the {@link TeamGoal} attributes.
     * @param existingActivityGoalContainer Holds the user defined and system field values of existing teamGoal.
     * @param targetContainer               Holds the updated information of user defined and system field values.
     */
    private void loadTeamGoalIntoContainer(final boolean isCreateEvent, final TeamGoal teamGoal, final long activityGoalId,
                                           final DataContainer existingActivityGoalContainer, final DataContainer targetContainer) {
        final Table activityGoalTable = getTeamGoalTable();
        final Long teamId = teamGoal.getTeamId();
        final Row teamGoalRow;

        if (isCreateEvent) {
            teamGoalRow = new Row(activityGoalTable);

            teamGoal.setId(teamGoalRow.getPKValue());
            teamGoalRow.setOrgId(teamGoal.orgId());
            teamGoalRow.set(OMACTEAMACTIVITYGOAL.TEAM_ID, teamId);
            teamGoalRow.set(OMACTEAMACTIVITYGOAL.ACTIVITY_GOAL_ID, activityGoalId);
            loadTeamUserGoalIntoContainer(true, teamGoal, existingActivityGoalContainer, targetContainer);
        } else {
            teamGoalRow = existingActivityGoalContainer.getRow(activityGoalTable,
                    new WhereClause(Criteria.eq(activityGoalTable.getColumn(OMACTEAMACTIVITYGOAL.ACTIVITY_GOAL_ID), activityGoalId))
                            .and(Criteria.eq(activityGoalTable.getColumn(OMACTEAMACTIVITYGOAL.TEAM_ID), teamId)));

            teamGoal.setId(teamGoalRow.getPKValue());
            loadTeamUserGoalIntoContainer(false, teamGoal, existingActivityGoalContainer, targetContainer);
        }
        teamGoalRow.set(OMACTEAMACTIVITYGOAL.IS_SPLIT_ENABLED, teamGoal.isSplitEnabled());

        targetContainer.updateRow(teamGoalRow);
    }

    /**
     * <p>
     * Loads the {@link TeamUserGoal} details into the data container.
     * </p>
     *
     * @param isCreateEvent                 A flag which indicates whether the action is related to create or update event.
     * @param teamGoal                      An entity that holds the {@link TeamGoal} attributes.
     * @param existingActivityGoalContainer Holds the user defined and system field values of existing team user goal.
     * @param targetContainer               Holds the updated information of user defined and system field values.
     */
    private void loadTeamUserGoalIntoContainer(final boolean isCreateEvent, final TeamGoal teamGoal,
                                               final DataContainer existingActivityGoalContainer, final DataContainer targetContainer) {
        final Table teamUserGoalTable = getTeamUserGoalTable();
        final Collection<TeamUserGoal> teamUserGoals = teamGoal.getTeamUserGoals();
        final Long teamGoalId = teamGoal.id();

        for (final TeamUserGoal teamUserGoal : teamUserGoals) {
            final Long teamUserId = teamUserGoal.getTeamUser().getCrmUser().id();
            Row teamUserGoalRow;

            if (isCreateEvent) {
                teamUserGoalRow = createTeamUserRow(teamGoal, teamUserGoalTable, teamUserId);

                teamUserGoal.setId(teamUserGoalRow.getPKValue());
            } else {
                teamUserGoalRow = existingActivityGoalContainer.getRow(teamUserGoalTable,
                        new WhereClause(Criteria.eq(teamUserGoalTable.getColumn(OMACTEAMUSERACTIVITYGOAL.TEAM_ACTIVITY_GOAL_ID), teamGoalId))
                                .and(Criteria.eq(teamUserGoalTable.getColumn(OMACTEAMUSERACTIVITYGOAL.USER_ID), teamUserId)));

                if (Objects.isNull(teamUserGoalRow)) {
                    teamUserGoalRow = createTeamUserRow(teamGoal, teamUserGoalTable, teamUserId);
                    teamUserGoal.setId(teamUserGoalRow.getPKValue());
                }
            }
            teamUserGoalRow.set(OMACTEAMUSERACTIVITYGOAL.IS_QUOTA_ENABLED, teamUserGoal.isQuotaEnabled());
            teamUserGoalRow.set(OMACTEAMUSERACTIVITYGOAL.QUOTA, teamUserGoal.getAttainment().getTarget());

            targetContainer.updateRow(teamUserGoalRow);
        }
    }

    /**
     * <p>
     * Creates the new team user row
     * </p>
     *
     * @param teamGoal           Represents the {@link TeamGoal}
     * @param teamUserGoalTable  Represents the team user goal table
     * @param teamUserId         Represents the team user id
     * @return The team user row
     */
    private Row createTeamUserRow(final TeamGoal teamGoal, final Table teamUserGoalTable, final Long teamUserId) {
        final Row teamUserGoalRow = new Row(teamUserGoalTable);

        teamUserGoalRow.setOrgId(teamGoal.orgId());
        teamUserGoalRow.set(OMACTEAMUSERACTIVITYGOAL.USER_ID, teamUserId);
        teamUserGoalRow.set(OMACTEAMUSERACTIVITYGOAL.TEAM_ACTIVITY_GOAL_ID, teamGoal.id());

        return teamUserGoalRow;
    }

    /**
     * <p>
     * Gets the {@link ActivityGoal} dataContainer
     * </p>
     *
     * @param orgId           Represents the organisation id
     * @param activityGoalIds Collection of ids that have to be gets the activity goal details
     * @return The activity goal {@link DataContainer}
     */
    private DataContainer getActivityGoalContainer(final long orgId, final Collection<Long> activityGoalIds) {
        final Table activityGoalTable = getTable();
        final Column activityGoalPkColumn = activityGoalTable.getPKColumn();
        final Table userGoalTable = getUserGoalTable();
        final Table teamGoalTable = getTeamGoalTable();
        final Table teamUserGoalTable = getTeamUserGoalTable();
        final Table activityTypeTable = activityTypeManager.getTable();
        final SelectQuery selectQuery = SelectQuery.get(activityGoalTable);
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;

        selectQuery.addJoinClause(List.of(new JoinClause(activityGoalTable.getColumn(OMACACTIVITYGOAL.ACTIVITY_TYPE_ID), activityTypeTable.getPKColumn()),
                new JoinClause(activityGoalPkColumn, userGoalTable.getColumn(OMACUSERACTIVITYGOAL.ACTIVITY_GOAL_ID), leftJoin),
                new JoinClause(activityGoalPkColumn, teamGoalTable.getColumn(OMACTEAMACTIVITYGOAL.ACTIVITY_GOAL_ID), leftJoin),
                new JoinClause(teamGoalTable.getPKColumn(), teamUserGoalTable.getColumn(OMACTEAMUSERACTIVITYGOAL.TEAM_ACTIVITY_GOAL_ID), leftJoin)));
        selectQuery.addSelectClause(activityGoalTable, activityTypeTable, userGoalTable, teamGoalTable, teamUserGoalTable);
        selectQuery.setWhereClause(new WhereClause(Criteria.in(activityGoalPkColumn, activityGoalIds)));

        return orgDataStore(orgId).get(selectQuery);
    }

    /**
     * <p>
     * Loads the user activity goal details into the data container.
     * </p>
     *
     * @param isCreateEvent                 A flag which indicates whether the action is related to create or update event.
     * @param userId                        Represents the crm user id
     * @param orgId                         Represents the organisation id
     * @param existingActivityGoalContainer Holds the user defined and system field values of existing teamGoal.
     * @param targetContainer               Holds the updated information of user defined and system field values.
     */
    private void loadUserGoalIntoContainer(final long orgId, final Long userId, final boolean isCreateEvent, final Long activityGoalId,
                                           final DataContainer existingActivityGoalContainer, final DataContainer targetContainer) {
        final Table userGoalTable = getUserGoalTable();
        final Row userGoalRow;

        if (isCreateEvent) {
            userGoalRow = new Row(userGoalTable);

            userGoalRow.setOrgId(orgId);
            userGoalRow.set(OMACUSERACTIVITYGOAL.USER_ID, userId);
            userGoalRow.set(OMACUSERACTIVITYGOAL.ACTIVITY_GOAL_ID, activityGoalId);
        } else {
            userGoalRow = existingActivityGoalContainer.getRow(userGoalTable,
                    new WhereClause(Criteria.eq(userGoalTable.getColumn(OMACUSERACTIVITYGOAL.ACTIVITY_GOAL_ID), activityGoalId))
                            .and(Criteria.eq(userGoalTable.getColumn(OMACUSERACTIVITYGOAL.USER_ID), userId)));
        }

        targetContainer.updateRow(userGoalRow);
    }

    /**
     * <p>
     * Converts row into {@link ActivityGoal}
     * </p>
     *
     * @param activityGoalRow Activity goal row to be converted
     * @param dataContainer   A container that holds the user defined field values and system values
     * @return {@link ActivityGoal}
     */
    private ActivityGoal convertRowIntoModel(final Row activityGoalRow, final DataContainer dataContainer,
                                             final boolean isPartial) {
        final long orgId = activityGoalRow.getOrgId();
        final Table teamGoalTable = getTeamGoalTable();
        final Table userGoalTable = getUserGoalTable();
        final Long activityGoalId = activityGoalRow.getPKValue();
        final Long startTime = activityGoalRow.get(OMACACTIVITYGOAL.START_TIME);
        final Long endTime = activityGoalRow.get(OMACACTIVITYGOAL.END_TIME);
        final Row activityTypeRow = dataContainer.getRow(activityTypeManager.getTable(),
                (Long) activityGoalRow.get(OMACACTIVITYGOAL.ACTIVITY_TYPE_ID));
        final ActivityGoal activityGoal = new ActivityGoal();
        final Collection<Long> userIds = new HashSet<>();

        activityGoal.setId(activityGoalId);
        activityGoal.setOrgId(orgId);
        activityGoal.setGoalType(GoalType.getGoalTypeById(activityGoalRow.get(OMACACTIVITYGOAL.TYPE)));
        activityGoal.setGoalPeriod(GoalPeriod.getGoalPeriodById(activityGoalRow.get(OMACACTIVITYGOAL.PERIOD)));
        activityGoal.setStartTime(startTime);
        activityGoal.setEndTime(endTime);
        activityGoal.setActivityType(activityTypeManager.convertRowToModel(activityTypeRow));

        if (!isPartial) {
            activityGoal.setGoalRange(GoalRange.getGoalRangeById(activityGoalRow.get(OMACACTIVITYGOAL.PERIOD_CATEGORY)));
            activityGoal.setCreatedTime(activityGoalRow.get(OMACACTIVITYGOAL.CREATED_TIME));
            activityGoal.setUpdatedTime(activityGoalRow.get(OMACACTIVITYGOAL.UPDATED_TIME));
        }

        if (0 < dataContainer.size(teamGoalTable)) {
            final Row teamGoalRow = dataContainer.getRow(teamGoalTable, new WhereClause(Criteria.eq(userGoalTable
                    .getColumn(OMACTEAMACTIVITYGOAL.ACTIVITY_GOAL_ID), activityGoalId)));

            if (Objects.nonNull(teamGoalRow)) {
                activityGoal.setTeamGoal(convertRowIntoTeamGoal(teamGoalRow, activityGoal, userIds, dataContainer));
            }
        }

        if (0 < dataContainer.size(userGoalTable)) {
            final Stream<Row> userGoalRows = dataContainer.getRows(userGoalTable,
                    new WhereClause(Criteria.eq(userGoalTable.getColumn(OMACUSERACTIVITYGOAL.ACTIVITY_GOAL_ID), activityGoalId)));
            final Collection<UserGoal> userGoals = new ArrayList<>();

            userGoalRows.forEach(userGoalRow -> {
                final long userId = userGoalRow.get(OMACUSERACTIVITYGOAL.USER_ID);
                final UserGoal userGoal = new UserGoal();

                userIds.add(userId);
                userGoal.setId(userGoalRow.getPKValue());
                userGoal.setOrgId(orgId);
                userGoal.setCrmUser(userService.getUser(orgId, userId));
                userGoals.add(userGoal);
            });
            activityGoal.setUserGoals(userGoals);
        }
        activityGoal.setUserIds(userIds);
        final Attainment attainment = new Attainment();
        final long achievedCount = activityManager.getCompletedActivityCount(orgId, userIds, activityTypeRow.getPKValue(), startTime, endTime);
        final long targetCount = activityGoalRow.get(OMACACTIVITYGOAL.TARGET);

        attainment.setTarget(targetCount);
        attainment.setAchieved(achievedCount);
        attainment.setAchievementRate(attainment.calculateAchievementRate());
        activityGoal.setAttainment(attainment);

        return activityGoal;
    }

    /**
     * <p>
     * Converts row into {@link TeamGoal}
     * </p>
     *
     * @param teamActivityGoalRow Team activity goal row to be converted
     * @param dataContainer       A container that holds the user defined field values and system values
     * @return {@link TeamGoal}
     */
    private TeamGoal convertRowIntoTeamGoal(final Row teamActivityGoalRow, final ActivityGoal activityGoal,
                                            final Collection<Long> userIds, final DataContainer dataContainer) {
        final long orgId = activityGoal.orgId();
        final Table teamUserGoalTable = getTeamUserGoalTable();
        final Long teamGoalId = teamActivityGoalRow.getPKValue();
        final Stream<Row> teamUserGoalRows = dataContainer.getRows(teamUserGoalTable,
                new WhereClause(Criteria.eq(teamUserGoalTable.getColumn(OMACTEAMUSERACTIVITYGOAL.TEAM_ACTIVITY_GOAL_ID), teamGoalId)));
        final Collection<TeamUserGoal> teamUserGoals = new ArrayList<>();
        final TeamGoal teamGoal = new TeamGoal();
        final Long teamId = teamActivityGoalRow.get(OMACTEAMACTIVITYGOAL.TEAM_ID);
        final Map<Long, TeamUser> users;

        if (Objects.nonNull(teamId)) {
            final Team team = teamManager.getTeam(orgId, teamId);
            final Collection<TeamUser> teamUsers = team.getUsers();
            users = teamUsers.stream().collect(Collectors.toMap(teamUser -> teamUser.getCrmUser().id(), Function.identity()));

            teamGoal.setTeamId(teamId);
            teamGoal.setTeamName(team.getName());
            teamGoal.setActive(true);
        } else {
            users = new HashMap<>();
            teamGoal.setActive(false);
        }

        teamUserGoalRows.forEach(teamUserRow -> {
            final Long crmUserId = teamUserRow.get(OMACTEAMUSERACTIVITYGOAL.USER_ID);
            final TeamUserGoal teamUserGoal;

            if (users.containsKey(crmUserId)) {
                teamUserGoal = convertRowIntoTeamUserGoal(teamUserRow, users.get(crmUserId), userIds, activityGoal);
            } else {
                final TeamUser teamUser = new TeamUser();
                final CrmUserEntity crmUser = new CrmUserEntity();

                crmUser.setId(crmUserId);
                crmUser.setIamId(userService.getUser(orgId, crmUserId).getIamId());
                teamUser.setCrmUser(crmUser);
                teamUserGoal = convertRowIntoTeamUserGoal(teamUserRow, teamUser, userIds, activityGoal);
                teamUserGoal.setRemovedFromTeam(true);
            }
            users.remove(crmUserId);
            teamUserGoals.add(teamUserGoal);
        });

        users.values().forEach(teamuser -> {
            final TeamUserGoal teamUserGoal = new TeamUserGoal();

            teamUserGoal.setOrgId(orgId);
            teamUserGoal.setTeamUser(teamuser);
            teamUserGoal.setAttainment(new Attainment());
            teamUserGoals.add(teamUserGoal);
            userIds.add(teamuser.getCrmUser().id());
        });
        teamGoal.setId(teamGoalId);
        teamGoal.setOrgId(orgId);
        teamGoal.setSplitEnabled(teamActivityGoalRow.get(OMACTEAMACTIVITYGOAL.IS_SPLIT_ENABLED));
        teamGoal.setTeamUserGoals(teamUserGoals);

        return teamGoal;
    }

    /**
     * <p>
     * Converts row into {@link TeamUserGoal}
     * </p>
     *
     * @param teamUserGoalRow Team user activity goal row to be converted
     * @param teamUser        Represents the {@link TeamUser}
     * @param userIds         Collection of user ids
     * @return {@link TeamUserGoal}
     */
    private TeamUserGoal convertRowIntoTeamUserGoal(final Row teamUserGoalRow, final TeamUser teamUser,
                                                    final Collection<Long> userIds, final ActivityGoal activityGoal) {
        final long orgId = activityGoal.orgId();
        final Long crmUserId = teamUserGoalRow.get(OMACTEAMUSERACTIVITYGOAL.USER_ID);
        final boolean isQuotaEnabled = teamUserGoalRow.get(OMACTEAMUSERACTIVITYGOAL.IS_QUOTA_ENABLED);
        final long targetCount = teamUserGoalRow.get(OMACTEAMUSERACTIVITYGOAL.QUOTA);
        final TeamUserGoal teamUserGoal = new TeamUserGoal();
        final Attainment attainment = new Attainment();

        teamUserGoal.setOrgId(orgId);
        teamUserGoal.setId(teamUserGoalRow.getPKValue());
        teamUserGoal.setTeamUser(teamUser);
        teamUserGoal.setQuotaEnabled(isQuotaEnabled);
        userIds.add(crmUserId);
        attainment.setTarget(targetCount);

        if (isQuotaEnabled) {
            final long achievedCount = activityManager.getCompletedActivityCount(orgId, List.of(crmUserId), activityGoal.getActivityType().id(),
                    activityGoal.getStartTime(), activityGoal.getEndTime());

            attainment.setAchieved(achievedCount);
            attainment.setAchievementRate(attainment.calculateAchievementRate());
        }
        teamUserGoal.setAttainment(attainment);

        return teamUserGoal;
    }

    /**
     * <p>
     * Fetches the table of team activity goal
     * </p>
     *
     * @return OMACTeamActivityGoal table
     */
    private Table getTeamGoalTable() {
        return Table.get(OMACTEAMACTIVITYGOAL.TABLE);
    }

    /**
     * <p>
     * Fetches the table of user activity goal
     * </p>
     *
     * @return OMACUserActivityGoal table
     */
    private Table getUserGoalTable() {
        return Table.get(OMACUSERACTIVITYGOAL.TABLE);
    }

    /**
     * <p>
     * Fetches the table of team user activity goal
     * </p>
     *
     * @return OMACTeamUserActivityGoal table
     */
    private Table getTeamUserGoalTable() {
        return Table.get(OMACTEAMUSERACTIVITYGOAL.TABLE);
    }
}