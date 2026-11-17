package com.tlc.crm.entity.features.api.activity.manager;

import com.tlc.commons.Counter;
import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.common.config.manager.AbstractConfigManager;

import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.features.api.activity.model.ActivityType;
import com.tlc.crm.entity.handler.OrderManager;
import com.tlc.crm.entity.internal.status.ActivityErrorCodes;
import com.tlc.crm.entity.resolver.OrderResolver;
import com.tlc.crm.entity.sql.resource.OMCALCALENDARSYNCSETTINGS;
import com.tlc.crm.icon.service.IconService;
import com.tlc.crm.entity.sql.resource.OMACACTIVITYTYPE;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Connector;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.util.WhereClauseUtil;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.model.ErrorSummary;
import com.tlc.validator.model.ErrorDescriptor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * <p>
 * Manages {@link ActivityType} related DB actions
 * </p>
 *
 * @author Kowsalya SP
 * @version 1.0
 * @see AbstractConfigManager
 */
public class ActivityTypeManager extends AbstractConfigManager<ActivityType> {

    private static final int ACTIVITY_TYPE_MAX_LIMIT = 50;

    private final IconService iconService;
    private final OrderResolver orderResolver;
    private final OrderManager orderManager;

    private static class Instance {
        private static final ActivityTypeManager INSTANCE = new ActivityTypeManager();
    }

    private ActivityTypeManager() {
        iconService = IconService.getInstance();
        orderResolver = OrderResolver.getInstance();
        orderManager = OrderManager.getInstance();
    }

    /**
     * <p>
     * Gets the {@link ActivityTypeManager} Instance
     * </p>
     *
     * @return {@link ActivityTypeManager}
     */
    public static ActivityTypeManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @return the {@link Table} of activityType
     */
    @Override
    public Table getTable() {
        return Table.get(OMACACTIVITYTYPE.TABLE);
    }

    /**
     * Gets the table of calendar sync.
     *
     * @return the {@link Table} of calendar sync
     */
    public Table getCalendarSyncTable() {
        return Table.get(OMCALCALENDARSYNCSETTINGS.TABLE);
    }

    /**
     * <p>
     * Fetches the {@link ActivityType} that has one of the id value mentioned in the parameter
     * </p>
     *
     * @param orgId Represents the organization id
     * @param ids   Represents the activityType ids
     * @return Collection of {@link ActivityType}
     */
    @Override
    public Collection<ActivityType> get(final Long orgId, final Collection<Long> ids) {
        final Table activityTypeTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(activityTypeTable);

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Stream<Row> rows = dataContainer.getRows(activityTypeTable, new WhereClause(Criteria.in(activityTypeTable.getPKColumn(), ids)));

        if (Objects.isNull(rows)) {
            throw ErrorCode.get(ActivityErrorCodes.ACTIVITY_TYPE_NOT_FOUND, "i18n.activity.type.not.found");
        } else {
            return rows.map(this::convertRowToModel).toList();
        }
    }

    /**
     * <p>
     * Converts row into {@link ActivityType}
     * </p>
     *
     * @param row Represents the activityType row to be converted
     * @return {@link ActivityType}
     */
    public ActivityType convertRowToModel(final Row row) {
        final ActivityType activityType = new ActivityType();

        activityType.setOrgId(row.getOrgId());
        activityType.setId(row.getPKValue());
        activityType.setName(row.get(OMACACTIVITYTYPE.NAME));
        activityType.setOwnerId(row.get(OMACACTIVITYTYPE.OWNER_ID));
        activityType.setActive(row.get(OMACACTIVITYTYPE.IS_ACTIVE));
        activityType.setActivityIcon(iconService.get(row.get(OMACACTIVITYTYPE.ICON_ID)));

        return activityType;
    }

    /**
     * <p>
     * Converts partial type row to activity type
     * </p>
     *
     * @param activityTypeRow Represents the row of activity type
     * @return {@link ActivityType}
     */
    private ActivityType convertPartialRowToModel(final Row activityTypeRow) {
        final ActivityType activityType = new ActivityType();

        activityType.setId(activityTypeRow.getPKValue());
        activityType.setName(activityTypeRow.get(OMACACTIVITYTYPE.NAME));

        return activityType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<ActivityType> partialGet(final Long orgId, final Collection<Long> ids) {
        final Table activityTypeTable = getTable();
        final SelectQuery query = SelectQuery.get(activityTypeTable);
        final Column pkColumn = activityTypeTable.getPKColumn();

        query.addSelectClause(List.of(pkColumn, activityTypeTable.getColumn(OMACACTIVITYTYPE.NAME)));
        query.setWhereClause(new WhereClause(Criteria.in(pkColumn, ids)));
        final Stream<Row> activityTypeRows = orgDataStore(orgId).get(query).getRows(activityTypeTable);

        return activityTypeRows.map(this::convertPartialRowToModel).toList();
    }

    /**
     * <p>
     * Saves the attributes of {@link ActivityType} into the database
     * </p>
     *
     * @param activityType An entity that holds the activityType attributes which have to be saved into database
     * @return {@link ActivityType}
     */
    @Override
    public ActivityType create(final ActivityType activityType) {
        final DataContainer targetContainer = DataContainer.create();

        loadActivityTypeIntoContainer(true, activityType, targetContainer);
        orgDataStore(activityType.orgId()).commitChanges(targetContainer);

        return activityType;
    }

    /**
     * <p>
     * Checks whether the activity type limit is reached or not
     * </p>
     *
     * @param orgId Represents the organisation ID
     * @return True if the limit reached, false otherwise
     */
    public boolean isMaxLimitReached(final Long orgId) {
        return ACTIVITY_TYPE_MAX_LIMIT <= orgDataStore(orgId).get(CountQuery.get(getTable()));
    }

    /**
     * <p>
     * Loads the activityType details into the target container
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param activityType    An entity that holds the activityType attributes
     * @param targetContainer Holds the updated information of field values
     */
    private void loadActivityTypeIntoContainer(final boolean isCreateEvent, final ActivityType activityType, final DataContainer targetContainer) {
        final DataContainer entityContainer = isCreateEvent ? null :
                orgDataStore(activityType.orgId()).get(getTable(), List.of(activityType.id()));

        loadActivityTypeIntoContainer(isCreateEvent, activityType, entityContainer, targetContainer);
    }

    /**
     * <p>
     * Loads the activityType details into the target container
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param activityType    An entity that holds the activityType attributes
     * @param entityContainer Holds the field values of existing activityType
     * @param targetContainer Holds the updated information of field values
     */
    private void loadActivityTypeIntoContainer(final boolean isCreateEvent, final ActivityType activityType,
                                               final DataContainer entityContainer, final DataContainer targetContainer) {
        final Table activityTypeTable = getTable();
        final Long id = activityType.id();
        final Long orgId = activityType.orgId();
        final Row activityTypeRow = isCreateEvent ? new Row(activityTypeTable)
                : entityContainer.getRow(activityTypeTable, id);

        if (!isCreateEvent && Objects.isNull(activityTypeRow)) {
            throw ErrorCode.get(ActivityErrorCodes.ENTITY_NOT_FOUND, "i18n.entity.not.found");
        }

        if (activityTypeRow.isNewRow()) {
            activityTypeRow.setOrgId(orgId);
            activityType.setId(activityTypeRow.getPKValue());
            activityTypeRow.set(OMACACTIVITYTYPE.OWNER_ID, activityType.getOwnerId());
            activityTypeRow.set(OMACACTIVITYTYPE.IS_ACTIVE, true);
            activityTypeRow.set(OMACACTIVITYTYPE.ORDER, orderResolver.getNextOrder(orgId, null, activityTypeTable));
        }
        activityTypeRow.set(OMACACTIVITYTYPE.NAME, activityType.getName());
        activityTypeRow.set(OMACACTIVITYTYPE.ICON_ID, activityType.getActivityIcon().id());

        targetContainer.updateRow(activityTypeRow);
    }

    /**
     * <p>
     * Updates the given activityType
     * </p>
     *
     * @param orgId         Represents the organization id
     * @param activityTypes Represents the collection of activityType that have to be updated
     * @return Updated status
     */
    @Override
    public ProcessOutcome update(final Long orgId, final Collection<ActivityType> activityTypes) {
        final OrgDataStore orgDataStore = orgDataStore(orgId);
        final Collection<Long> ids = activityTypes.stream().map(ActivityType::id).filter(Objects::nonNull).toList();
        final ProcessOutcome processOutcome = new ProcessOutcome();
        final List<ErrorDescriptor> failedEntities = new ArrayList<>();
        final Table activityTypeTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(activityTypeTable);

        selectQuery.setWhereClause(new WhereClause(Criteria.in(activityTypeTable.getPKColumn(), ids)));
        final DataContainer entityContainer = orgDataStore.get(selectQuery);

        activityTypes.forEach(activityType -> {
            try {
                final DataContainer targetContainer = DataContainer.create();

                loadActivityTypeIntoContainer(false, activityType, entityContainer, targetContainer);
                orgDataStore.commitChanges(targetContainer);
                processOutcome.addProcessedRecord(activityType.id());// TODO: need to move activity holder to entity or activity to core
            } catch (ErrorCode errorCode) {
                LOGGER.error("Update action: Activity type update failed Org ID : {} ID : {} reason : {}", orgId, activityType.id(), errorCode.getMessage(), errorCode);
                failedEntities.add(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED,
                        "i18n.core.update.failed"), ActivityErrorCodes.UPDATE_ACTION_FAILED.name(), activityType.id().toString()));
            }
        });

        if (!failedEntities.isEmpty()) {
            processOutcome.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(ActivityErrorCodes.UPDATE_ACTION_FAILED,
                    "i18n.core.update.failed")), failedEntities));
        }
        return processOutcome;
    }

    /**
     * <p>
     * Fetch the activity type name by the given type id
     * </p>
     *
     * @param orgId  Represents the organisation id
     * @param typeId Represents the activity type id
     * @return activityType name
     */
    public String fetchName(final Long orgId, final Long typeId) {
        final Table activityTypeTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(activityTypeTable);

        selectQuery.addSelectClause(activityTypeTable.getColumn(OMACACTIVITYTYPE.NAME));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(activityTypeTable.getPKColumn(), typeId)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Row row = dataContainer.getRow(activityTypeTable);

        if (Objects.nonNull(row)) {
            return row.get(OMACACTIVITYTYPE.NAME);
        } else {
            throw ErrorCode.get(ActivityErrorCodes.ACTIVITY_TYPE_NOT_FOUND, "i18n.activity.type.not.found");
        }
    }

    /**
     * Checks whether all the given activity type ids are exists or not
     *
     * @param orgId   Represents the Organisation ID
     * @param typeIds Which contains the collection of activity type IDs
     * @return true if all the activity types are exists
     */
    public boolean exists(final Long orgId, final Set<Long> typeIds) {

        if (Objects.nonNull(typeIds) && !typeIds.isEmpty()) {
            final Table typeTable = getTable();
            final CountQuery countQuery = CountQuery.get(typeTable);

            countQuery.setWhereClause(new WhereClause(Criteria.in(typeTable.getPKColumn(), typeIds)));

            return typeIds.size() == orgDataStore(orgId).get(countQuery);
        }
        return false;
    }

    /**
     * Gets the list of activity type
     *
     * @param orgId Represents the organization id
     * @return {@link Collection} of activity type
     */
    public Collection<ActivityType> getTypeList(final Long orgId, final int start, final int limit, final Boolean isActive,
                                                final DropdownCriteria dropdownCriteria) {
        final Table activityTypeTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(activityTypeTable);
        final WhereClause nameCriteria = Objects.isNull(dropdownCriteria) ? null :
                new WhereClause(dropdownCriteria.build((activityTypeTable.getColumn(OMACACTIVITYTYPE.NAME))));

        if (Objects.nonNull(isActive)) {
            final WhereClause isActiveCriteria = new WhereClause(Criteria.eq(activityTypeTable.getColumn(OMACACTIVITYTYPE.IS_ACTIVE), isActive));

            selectQuery.setWhereClause(WhereClauseUtil.connect(isActiveCriteria, nameCriteria, Connector.AND));
        } else {
            selectQuery.setWhereClause(nameCriteria);
        }
        selectQuery.setLimitClause(new LimitClause(start, limit));
        selectQuery.addOrderByClause(new OrderByClause(activityTypeTable.getColumn(OMACACTIVITYTYPE.ORDER), OrderByClause.OrderType.DESCENDING));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Stream<Row> activityTypeRows = dataContainer.getRows(activityTypeTable);

        return activityTypeRows.filter(Objects::nonNull).map(this::convertRowToModel).toList();
    }

    /**
     * <p>
     * Gets the {@link Collection} of {@link ActivityType} based on isActive field.
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param isActive A flag which indicates whether the {@link ActivityType} is active or not
     * @return {@link Collection} of {@link ActivityType}
     */
    public Collection<ActivityType> getActivityTypes(final long orgId, final boolean isActive) {
        final Table activityTypeTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(activityTypeTable);

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(activityTypeTable.getColumn(OMACACTIVITYTYPE.IS_ACTIVE), isActive)));
        selectQuery.addOrderByClause(new OrderByClause(activityTypeTable.getColumn(OMACACTIVITYTYPE.ORDER), OrderByClause.OrderType.DESCENDING));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Stream<Row> activityTypeRows = dataContainer.getRows(activityTypeTable);

        return activityTypeRows.filter(Objects::nonNull).map(this::convertRowToModel).toList();
    }

    /**
     * <p>
     * Fetches the activity count for given organization
     * </p>
     *
     * @param orgId    Represents the organization id
     * @param isActive Represents which is active or not
     * @return activity type count
     */
    public Long getActivityTypeCount(final Long orgId, final Boolean isActive) {
        final Table activityTypeTable = getTable();
        final CountQuery countQuery = CountQuery.get(activityTypeTable);

        if (Objects.nonNull(isActive)) {
            countQuery.setWhereClause(new WhereClause(Criteria.eq(activityTypeTable.getColumn(OMACACTIVITYTYPE.IS_ACTIVE), isActive)));
        }

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Updates the status of a specific activity type for a given organization.
     * </p>
     *
     * @param orgId      Represents the organization id
     * @param typeId     Represents the activity type id
     * @param isActive   A flag indicating whether the activity type should be set as active or inactive
     * @param syncConfig Represents the calendar sync config
     */
    public void updateStatus(final Long orgId, final Long userId, final Long typeId, final boolean isActive, final String syncConfig) {
        final Table activityTypeTable = getTable();
        final List<UpdateQuery> updateQueries = new ArrayList<>();
        final UpdateQuery updateQuery = UpdateQuery.get(activityTypeTable);

        updateQuery.addUpdateColumn(activityTypeTable.getColumn(OMACACTIVITYTYPE.IS_ACTIVE), isActive);
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(activityTypeTable.getPKColumn(), typeId)));

        updateQueries.add(updateQuery);

        if (!isActive && Objects.nonNull(syncConfig)) {
            final Table table = getCalendarSyncTable();
            final UpdateQuery calendarSyncUpdateQuery = UpdateQuery.get(table);

            calendarSyncUpdateQuery.addUpdateColumn(table.getColumn(OMCALCALENDARSYNCSETTINGS.SYNC_CONFIG), syncConfig);
            calendarSyncUpdateQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(OMCALCALENDARSYNCSETTINGS.USER_ID), userId))
                    .and(Criteria.eq(table.getColumn(OMCALCALENDARSYNCSETTINGS.IS_SYNC_ENABLED), true)));
            updateQueries.add(calendarSyncUpdateQuery);
        }

        orgDataStore(orgId).update(updateQueries);
    }

    /**
     * <p>
     * Gets the calendar sync config.
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @return calendar sync config
     */
    public String getCalendarSyncConfig(final Long orgId, final Long userId) {
        final Table table = getCalendarSyncTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(table.getColumn(OMCALCALENDARSYNCSETTINGS.SYNC_CONFIG));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(OMCALCALENDARSYNCSETTINGS.USER_ID), userId))
                .and(Criteria.eq(table.getColumn(OMCALCALENDARSYNCSETTINGS.IS_SYNC_ENABLED), true)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Row row = dataContainer.getRow(table);

        return Objects.isNull(row) ? null : row.get(table.getColumn(OMCALCALENDARSYNCSETTINGS.SYNC_CONFIG));
    }

    /**
     * <p>
     * Updates the order of activity types
     * </p>
     *
     * @param orgId           Represents the organization id
     * @param activityTypeIds Represents the collection of activity type ids
     */
    public void updateActivityTypesOrder(final Long orgId, final Collection<Long> activityTypeIds) {
        orderManager.update(orgId, new Counter(), activityTypeIds, getTable());
    }

    /**
     * Fetches the {@link Collection} of {@link ActivityType} for the provided organization and activity type names.
     *
     * @param orgId             Represents an organization id
     * @param activityTypeNames Represents the {@link Collection} of activity type name
     * @return {@link Collection} of {@link ActivityType}
     */
    public Collection<ActivityType> fetchActivityTypeByName(final long orgId, final Collection<String> activityTypeNames) {
        final Table activityTypeTable = getTable();
        final Column typeNameColumn = activityTypeTable.getColumn(OMACACTIVITYTYPE.NAME);
        final Column isActiveColumn = activityTypeTable.getColumn(OMACACTIVITYTYPE.IS_ACTIVE);
        final DataContainer dataContainer = orgDataStore(orgId).get(activityTypeTable, new WhereClause(Criteria.in(typeNameColumn, activityTypeNames))
                .and(Criteria.eq(isActiveColumn, true)), List.of(typeNameColumn, isActiveColumn));

        return dataContainer.getRows(activityTypeTable).map(this::convertRowToModel).toList();
    }

    /**
     * Gets the default {@link ActivityType}
     *
     * @param orgId Represents an organization id
     * @return {@link ActivityType}
     */
    public ActivityType getDefaultActivityType(final long orgId) {
        final Table activityTypeTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(activityTypeTable);

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(activityTypeTable.getColumn(OMACACTIVITYTYPE.IS_ACTIVE), true)));
        selectQuery.addOrderByClause(new OrderByClause(activityTypeTable.getColumn(OMACACTIVITYTYPE.ORDER), OrderByClause.OrderType.DESCENDING));
        selectQuery.setLimitClause(new LimitClause(1));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Row activityTypeRow = dataContainer.getRow(activityTypeTable);

        return convertRowToModel(activityTypeRow);
    }

    /**
     * <p>
     * Check whether the activity type already exists based on name and icon ID.
     * </p>
     *
     * @param isCreateEvent Indicates whether the check is for a create event.
     * @param orgId         Represents the ID of the organization.
     * @param activityType  The activity type object containing the name, icon ID, and ID.
     * @return true if the activity type with the specified name and icon ID already exists, otherwise false.
     */
    public final boolean exists(final boolean isCreateEvent, final Long orgId, final ActivityType activityType) {
        final Table activityTypeTable = getTable();
        final CountQuery countQuery = CountQuery.get(activityTypeTable);
        final WhereClause whereClause = new WhereClause(Criteria.eq(activityTypeTable.getColumn(OMACACTIVITYTYPE.NAME), activityType.getName())).
                and(Criteria.eq(activityTypeTable.getColumn(OMACACTIVITYTYPE.ICON_ID), activityType.getActivityIcon().id()));

        countQuery.setWhereClause(isCreateEvent ? whereClause : whereClause.and(Criteria.notEq(activityTypeTable.getPKColumn(), activityType.id())));

        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * <p>
     * Retrieves the total activity type total count
     * </p>
     *
     * @param orgId            Represents an organization id
     * @param dropdownCriteria Represent the dropdown criteria
     * @return The count of entities
     */
    public long getTotalCount(final Long orgId, final DropdownCriteria dropdownCriteria) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);

        if (Objects.nonNull(dropdownCriteria)) {
            countQuery.setWhereClause(new WhereClause(dropdownCriteria.build(table.getColumn(OMACACTIVITYTYPE.NAME))));
        }

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Gets the default active {@link ActivityType} based on excluding the given deactivate type id.
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param typeId Represents to deactivate activity type id
     * @return {@link ActivityType}
     */
    public ActivityType getActiveDefaultActivityType(final long orgId, final Long typeId) {
        final Table activityTypeTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(activityTypeTable);

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(activityTypeTable.getColumn(OMACACTIVITYTYPE.IS_ACTIVE), true))
                .and(Criteria.notEq(activityTypeTable.getPKColumn(), typeId)));
        selectQuery.addOrderByClause(new OrderByClause(activityTypeTable.getColumn(OMACACTIVITYTYPE.ORDER), OrderByClause.OrderType.DESCENDING));
        selectQuery.setLimitClause(new LimitClause(1));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Optional<Row> activityTypeRow = Optional.ofNullable(dataContainer.getRow(activityTypeTable));

        return activityTypeRow.map(this::convertRowToModel).orElseGet(() -> null);
    }
}
