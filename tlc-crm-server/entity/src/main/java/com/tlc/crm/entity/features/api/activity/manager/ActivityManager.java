package com.tlc.crm.entity.features.api.activity.manager;

import com.tlc.commons.Counter;
import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.entity.sql.resource.OMNTNOTE;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.crm.entity.features.api.activity.model.Activity;
import com.tlc.crm.entity.features.api.activity.model.ActivityType;
import com.tlc.crm.entity.features.api.activity.model.Availability;
import com.tlc.crm.entity.features.api.activity.model.Priority;
import com.tlc.crm.entity.features.api.activity.model.Reminder;
import com.tlc.crm.entity.features.api.activity.model.Status;
import com.tlc.crm.entity.features.api.note.manager.NoteManager;
import com.tlc.crm.entity.features.api.note.model.Note;
import com.tlc.crm.entity.features.api.activity.model.CheckInOutRecord;
import com.tlc.crm.entity.filter.api.models.FilterCriteria;
import com.tlc.crm.entity.internal.status.ActivityErrorCodes;
import com.tlc.crm.entity.list.model.Provider;
import com.tlc.crm.entity.list.filter.handler.FilterCriteriaHandler;
import com.tlc.crm.entity.timeline.field.TimelineActionFields;
import com.tlc.crm.entity.timeline.model.ActivityTimeline;
import com.tlc.crm.icon.service.IconService;
import com.tlc.crm.entity.sql.resource.MACNOTEACTIVITY;
import com.tlc.crm.entity.sql.resource.OMACACTIVITY;
import com.tlc.crm.entity.sql.resource.OMACACTIVITYCOLLABORATOR;
import com.tlc.crm.entity.sql.resource.OMACACTIVITYTYPE;
import com.tlc.crm.entity.sql.resource.OMCALEXTERNALEVENTS;
import com.tlc.crm.entity.sql.resource.OMCALCALENDARSYNCSETTINGS;
import com.tlc.crm.entity.timeline.model.Timeline;
import com.tlc.crm.entity.timeline.model.TimelineType;

import com.tlc.l10n.temporal.interval.Interval;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.l10n.temporal.resolver.DateTimeUtil;
import com.tlc.notification.model.email.EmailAddress;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Connector;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.util.WhereClauseUtil;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.sql.common.api.ds.WritableDataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tlc.notification.model.sync.ConnectionStatus.ACTIVE;

/**
 * <p>
 * Implements the {@link Activity} related DB actions
 * </p>
 *
 * @author Kowsalya SP
 * @version 1.0
 */
public class ActivityManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActivityManager.class);

    private final NoteManager noteManager;
    private final ActivityTypeManager activityTypeManager;
    private final WritableDataStore writableDataStore;
    private final IconService iconService;
    private final UserService userService;
    private final FilterCriteriaHandler filterCriteriaHandler;
    private final DateTimeUtil dateTimeUtil;

    private static class Instance {
        private static final ActivityManager INSTANCE = new ActivityManager();
    }

    /**
     * Gets the orgDataStore
     *
     * @param orgId Represents the organization
     * @return {@link OrgDataStore}
     */
    private OrgDataStore orgDataStore(final Long orgId) {
        return DataStoreProvider.getSecureOrgDataStore(orgId);
    }

    /**
     * <p>
     * Gets the {@link ActivityManager} Instance
     * </p>
     *
     * @return {@link ActivityManager}
     */
    public static ActivityManager getInstance() {
        return Instance.INSTANCE;
    }

    private ActivityManager() {
        noteManager = NoteManager.getInstance();
        activityTypeManager = ActivityTypeManager.getInstance();
        writableDataStore = DataStoreProvider.getWritableDataStore();
        iconService = IconService.getInstance();
        userService = UserService.getInstance();
        filterCriteriaHandler = FilterCriteriaHandler.getInstance();
        dateTimeUtil = DateTimeUtil.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @return the {@link Table} of activity
     */

    public Table getTable() {
        return Table.get(OMACACTIVITY.TABLE);
    }

    /**
     * <p>
     * Return noteActivity Table
     * </p>
     *
     * @return the {@link Table} of noteActivity
     */
    public Table getNoteActivityTable() {
        return Table.get(MACNOTEACTIVITY.TABLE);
    }

    /**
     * <p>
     * Fetches the table of the collaborator.
     * </p>
     *
     * @return OMACActivityCollaborator table
     */
    public Table getCollaboratorTable() {
        return Table.get(OMACACTIVITYCOLLABORATOR.TABLE);
    }

    /**
     * <p>
     * Fetches the table of the collaborator.
     * </p>
     *
     * @return OMACActivityCollaborator table
     */
    public Table getActivityCollaboratorTable() {
        return Table.get(OMACACTIVITYCOLLABORATOR.TABLE);
    }

    /**
     * <p>
     * Fetches the synced calendar table.
     * </p>
     *
     * @return {@link Table}.
     */
    private Table getCalendarSyncSettingsTable() {
        return Table.get(OMCALCALENDARSYNCSETTINGS.TABLE);
    }

    /**
     * <p>
     * Check whether the ids exists.
     * </p>
     *
     * @param orgId Represents an organization id.
     * @param ids   Represents activity ids.
     * @return whether the id exists.
     */
    public boolean exists(final Long orgId, final Collection<Long> ids) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);
        final WhereClause whereClause = new WhereClause(Criteria.in(table.getPKColumn(), ids));

        countQuery.setWhereClause(whereClause);

        return orgDataStore(orgId).get(countQuery) == ids.size();
    }

    /**
     * <p>
     * Gets the collection of {@link Activity} based on the given start and end time
     * </p>
     *
     * @param orgId              Represents an organization id
     * @param startTime          Represents start time of first day of week
     * @param endTime            Represents end time of last day of week
     * @param filterCriteriaList Which contains the collection of {@link FilterCriteria}
     * @return The Collection of {@link Activity}
     */
    public Collection<Activity> getActivities(final Long orgId, final Long startTime, final Long endTime,
                                              final Collection<FilterCriteria> filterCriteriaList) {
        final Collection<Activity> activities = new ArrayList<>();
        final Table activityTable = getTable();
        final Table activityTypeTable = activityTypeManager.getTable();
        final Column startTimeColumn = activityTable.getColumn(OMACACTIVITY.START_TIME);
        final Column endTimeColumn = activityTable.getColumn(OMACACTIVITY.END_TIME);
        final Column typeIdColumn = activityTable.getColumn(OMACACTIVITY.TYPE_ID);
        final Column isDoneColumn = activityTable.getColumn(OMACACTIVITY.IS_DONE);
        final SelectQuery selectQuery = SelectQuery.get(activityTable);

        selectQuery.addJoinClause(List.of(new JoinClause(activityTable.getColumn(OMACACTIVITY.TYPE_ID), activityTypeTable.getPKColumn())));
        selectQuery.addSelectClause(List.of(activityTable.getColumn(OMACACTIVITY.TITLE), typeIdColumn, startTimeColumn,
                endTimeColumn, isDoneColumn, activityTypeTable.getColumn(OMACACTIVITYTYPE.ICON_ID), activityTable.getColumn(OMACACTIVITY.STATUS)));
        final WhereClause whereClause = new WhereClause(Criteria.between(startTimeColumn, startTime, endTime))
                .or(Criteria.between(endTimeColumn, startTime, endTime)).or(new WhereClause(Criteria.lt(startTimeColumn, startTime))
                        .and(Criteria.gt(endTimeColumn, endTime)));

        selectQuery.setWhereClause(whereClause);

        filterCriteriaHandler.applyCriteria(orgId, Provider.ACTIVITY_LIST, selectQuery, filterCriteriaList);
        selectQuery.addOrderByClause(new OrderByClause(startTimeColumn, OrderByClause.OrderType.ASCENDING));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        dataContainer.getRows(activityTable).forEach(row -> {
            final Activity activity = new Activity();
            final Row activityTypeRow = dataContainer.getRow(activityTypeTable, (Long) row.get(OMACACTIVITY.TYPE_ID));

            if (Objects.nonNull(activityTypeRow)) {
                final ActivityType activityType = new ActivityType();

                activityType.setActivityIcon(iconService.get(activityTypeRow.get(OMACACTIVITYTYPE.ICON_ID)));
                activity.setType(activityType);
            }
            activity.setId(row.get(OMACACTIVITY.ID));
            activity.setTitle(row.get(OMACACTIVITY.TITLE));
            activity.setStartTime(row.get(OMACACTIVITY.START_TIME));
            activity.setEndTime(row.get(OMACACTIVITY.END_TIME));
            activity.setStatus(Status.get(row.get(OMACACTIVITY.STATUS)));
            activity.setDone(row.get(OMACACTIVITY.IS_DONE));

            activities.add(activity);
        });

        return activities;
    }

    /**
     * <p>
     * Gets the activity status {@link WhereClause}
     * </p>
     *
     * @param isDoneColumn  Represents the activity is done {@link Column}
     * @param status        Represents the activity {@link Status}
     * @param endTimeColumn Represents the activity end time {@link Column}
     * @return The activity status {@link WhereClause}
     */
    private WhereClause getStatusWhereClause(final Column isDoneColumn, final Status status, final Column endTimeColumn) {
        return switch (status) {
            case UPCOMING -> new WhereClause(Criteria.eq(isDoneColumn, false));
            case COMPLETED -> new WhereClause(Criteria.eq(isDoneColumn, true));
            case OVERDUE ->
                    new WhereClause(Criteria.eq(isDoneColumn, false)).and(Criteria.lt(endTimeColumn, Instant.now().toEpochMilli()));
        };
    }

    /**
     * <p>
     * Counts the number of collaborators given activity id
     * </p>
     *
     * @param orgId Represents an organization id
     * @param id    Represents the activity id
     * @return The total number of collaborators
     */
    public long getCollaboratorCount(final Long orgId, final Long id) {
        final Table collaboratorsTable = getActivityCollaboratorTable();
        final CountQuery countQuery = CountQuery.get(collaboratorsTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(collaboratorsTable.getColumn(OMACACTIVITYCOLLABORATOR.ACTIVITY_ID), id)));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Retrieves the collection of activity ids by the given collaborator id
     * </p>
     *
     * @param orgId          Represents an organisation id
     * @param collaboratorId Represents the collaborator id
     * @return The collection of activity ids
     */
    public Collection<Long> getCollaboratorActivityIds(final Long orgId, final Long collaboratorId) {
        final Table collaboratorsTable = getCollaboratorTable();
        final SelectQuery selectQuery = SelectQuery.get(collaboratorsTable);

        selectQuery.addSelectClause(collaboratorsTable.getColumn(OMACACTIVITYCOLLABORATOR.ACTIVITY_ID));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(collaboratorsTable.getColumn(OMACACTIVITYCOLLABORATOR.COLLABORATOR_ID), collaboratorId)));

        return orgDataStore(orgId).get(selectQuery).getRows(collaboratorsTable).map(row -> (Long) row.get(OMACACTIVITYCOLLABORATOR.ACTIVITY_ID)).collect(Collectors.toSet());
    }

    /**
     * <p>
     * Retrieves the collection of collaborator IDs by given activity id with start and limit criteria
     * </p>
     *
     * @param orgId Represents an organisation id
     * @param id    Represents the activity id
     * @param start Represents the starting index of collaborator to be fetched
     * @param limit Represents the ending index of collaborator to be fetched
     * @return The collection of collaborators IDs
     */
    public Collection<Long> getCollaborators(final Long orgId, final Long id, final int start, final int limit) {
        final Table collaboratorsTable = getActivityCollaboratorTable();
        final SelectQuery selectQuery = SelectQuery.get(collaboratorsTable);

        selectQuery.addSelectClause(collaboratorsTable.getColumn(OMACACTIVITYCOLLABORATOR.COLLABORATOR_ID));
        selectQuery.setLimitClause(new LimitClause(start, limit));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(collaboratorsTable.getColumn(OMACACTIVITYCOLLABORATOR.ACTIVITY_ID), id)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.getRows(collaboratorsTable).map(row -> (Long) row.get(OMACACTIVITYCOLLABORATOR.COLLABORATOR_ID)).collect(Collectors.toSet());
    }

    /**
     * <p>
     * Gets the assignee's id.
     * </p>
     *
     * @param orgId Represents an organization id.
     * @param id    Represents the activity id.
     * @return assignee's id.
     */
    public Long getAssigneeId(final Long orgId, final Long id) {
        final Table activityTable = getTable();
        final SelectQuery activityQuery = SelectQuery.get(activityTable);

        activityQuery.addSelectClause(activityTable.getColumn(OMACACTIVITY.ASSIGNEE_ID));
        activityQuery.setWhereClause(new WhereClause(Criteria.eq(activityTable.getPKColumn(), id)));
        final DataContainer dataContainer = orgDataStore(orgId).get(activityQuery);

        return dataContainer.isEmpty() ? null : dataContainer.getRow(activityTable).get(OMACACTIVITY.ASSIGNEE_ID);
    }

    /**
     * <p>
     * Converts row into {@link Activity}
     * </p>
     *
     * @param row Activity row to be converted
     * @return {@link Activity}
     */
    public Activity convertRowToModel(final Row row, final DataContainer dataContainer) {
        final Activity activity = new Activity();
        final Table noteActivityTable = getNoteActivityTable();
        final Table noteTable = noteManager.getTable();
        final Table collaboratorTable = getCollaboratorTable();
        final Long id = row.getPKValue();
        final Long orgId = row.getOrgId();
        final Collection<Row> collaboratorRows = dataContainer.getRows(collaboratorTable,
                new WhereClause(Criteria.eq(collaboratorTable.getColumn(OMACACTIVITYCOLLABORATOR.ACTIVITY_ID), id))).toList();
        final Object reminder = row.get(OMACACTIVITY.REMINDER);
        final Object priority = row.get(OMACACTIVITY.PRIORITY);
        final Object status = row.get(OMACACTIVITY.STATUS);
        final Object availability = row.get(OMACACTIVITY.AVAILABILITY);

        activity.setId(id);
        activity.setOrgId(orgId);
        activity.setTitle(row.get(OMACACTIVITY.TITLE));
        activity.setStatus(Objects.isNull(status) ? null : Status.get((int) status));
        activity.setAvailability(Objects.isNull(availability) ? null : Availability.get((int) availability));
        activity.setReminder(Objects.isNull(reminder) ? null : Reminder.get((int) reminder));
        activity.setPriority(Objects.isNull(priority) ? null : Priority.get(row.get(OMACACTIVITY.PRIORITY)));
        activity.setDescription(row.get(OMACACTIVITY.DESCRIPTION));
        activity.setDone(row.get(OMACACTIVITY.IS_DONE));
        activity.setEndTime(row.get(OMACACTIVITY.END_TIME));
        activity.setStartTime(row.get(OMACACTIVITY.START_TIME));
        activity.setCreatorId(row.get(OMACACTIVITY.CREATOR_ID));
        activity.setModifiedById(row.get(OMACACTIVITY.MODIFIED_BY));
        activity.setAssigneeId(row.get(OMACACTIVITY.ASSIGNEE_ID));
        activity.setDuration(row.get(OMACACTIVITY.DURATION));
        activity.setLastNotificationTime(row.get(OMACACTIVITY.LAST_NOTIFICATION_TIME));
        activity.setCompletedTime(row.get(OMACACTIVITY.COMPLETED_TIME));
        activity.setCreatedTime(row.get(OMACACTIVITY.CREATED_TIME));
        activity.setUpdatedTime(row.get(OMACACTIVITY.MODIFIED_TIME));
        activity.setLocation(row.get(OMACACTIVITY.LOCATION));

        final Row activityTypeRow = dataContainer.getRow(activityTypeManager.getTable(), (Long) row.get(OMACACTIVITY.TYPE_ID));
        final Long assigneeId = activity.getAssigneeId();
        final Long creatorId = activity.getCreatorId();

        if (Objects.nonNull(assigneeId)) {
            activity.setAssignee(userService.getUser(orgId, assigneeId));
        }

        if (Objects.nonNull(creatorId)) {
            activity.setCreator(userService.getUser(orgId, creatorId));
        }

        if (!collaboratorRows.isEmpty()) {
            activity.setCollaboratorIds(collectCollaboratorIdsFromRows(collaboratorRows));
            activity.setCollaborators(userService.getUsers(orgId, activity.getCollaboratorIds()));
        }

        if (Objects.nonNull(activityTypeRow)) {
            activity.setType(activityTypeManager.convertRowToModel(activityTypeRow));
        }

        final Row activityNoteRow = dataContainer.getRow(noteActivityTable, new WhereClause(Criteria.eq(noteActivityTable.
                getColumn(MACNOTEACTIVITY.ACTIVITY_ID), id)));

        if (Objects.nonNull(activityNoteRow)) {
            final Row noteRow = dataContainer.getRow(noteTable, new WhereClause(Criteria.eq(noteTable.getPKColumn(),
                    activityNoteRow.get(MACNOTEACTIVITY.NOTE_ID))));

            if (Objects.nonNull(noteRow)) {
                activity.setNote(noteManager.convertRowToPartialNote(noteRow));
            }
        }

        return activity;
    }

    /**
     * <p>
     * Converts a the {@link Row} into a {@link Activity}.
     * </p>
     *
     * @param row           Represents the entity of {@link Row}
     * @param dataContainer Represents the entity of {@link DataContainer}
     * @return Entity of {@link Activity}
     */
    public Activity convertRowToActivity(final Row row, final DataContainer dataContainer) {
        final Activity activity = new Activity();
        final Long creatorId = row.get(OMACACTIVITY.CREATOR_ID);
        final Long assigneeId = row.get(OMACACTIVITY.ASSIGNEE_ID);
        final Row activityTypeRow = dataContainer.getRow(activityTypeManager.getTable(), (Long) row.get(OMACACTIVITY.TYPE_ID));

        activity.setId(row.getPKValue());
        activity.setOrgId(row.get(OMACACTIVITY.ORG_ID));
        activity.setTitle(row.get(OMACACTIVITY.TITLE));
        activity.setDone(row.get(OMACACTIVITY.IS_DONE));
        activity.setStartTime(row.get(OMACACTIVITY.START_TIME));
        activity.setEndTime(row.get(OMACACTIVITY.END_TIME));
        activity.setCreatorId(creatorId);
        activity.setCreator(userService.getUser(row.getOrgId(), creatorId));

        if (Objects.nonNull(assigneeId)) {
            activity.setAssigneeId(assigneeId);
            activity.setAssignee(userService.getUser(row.getOrgId(), assigneeId));
        }

        if (Objects.nonNull(activityTypeRow)) {
            activity.setType(activityTypeManager.convertRowToModel(activityTypeRow));
        }

        return activity;
    }

    /**
     * <p>
     * Loads the activity details into the target container
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param activity        An entity that holds the activity attributes
     * @param entityContainer Holds the field values of existing activity
     * @param targetContainer Holds the updated information of field values provided by user
     */
    public void loadActivityIntoContainer(final boolean isCreateEvent, final Activity activity,
                                          final DataContainer entityContainer, final DataContainer targetContainer) {
        final Table activityTable = getTable();
        final Long id = activity.id();
        final Long orgId = activity.orgId();
        final ActivityType activityType = activity.getType();
        final Note note = activity.getNote();
        final Collection<Long> collaborators = activity.getCollaboratorIds();
        final Row activityRow = isCreateEvent ? new Row(activityTable)
                : Optional.ofNullable(entityContainer.getRow(activityTable, id)).map(Row::new).orElse(null);
        final DateTime createdTime = activity.getCreatedTime();

        if (!isCreateEvent && Objects.isNull(activityRow)) {
            throw ErrorCode.get(ActivityErrorCodes.ACTIVITY_NOT_FOUND, "i18n.activity.not.found");
        }
        final Long activityPkValue = activityRow.getPKValue();

        if (isCreateEvent) {
            activityRow.setOrgId(orgId);
            activity.setId(activityPkValue);
            activityRow.set(OMACACTIVITY.CREATED_TIME, Objects.isNull(createdTime) ? Instant.now().toEpochMilli() : createdTime);
        } else {

            if (Objects.nonNull(createdTime)) {
                activityRow.setIfModified(OMACACTIVITY.CREATED_TIME, createdTime);
            }
        }

        if (Objects.nonNull(note)) {
            loadNoteRowIntoContainer(isCreateEvent, activityPkValue, note, targetContainer, entityContainer);
        }

        if (Objects.nonNull(activityType)) {
            final Long typeId = activityType.id();
            final String title = activity.getTitle();
            final String activityTitle = Objects.isNull(title) ? activityTypeManager.fetchName(orgId, typeId) : title;

            activityRow.set(OMACACTIVITY.TYPE_ID, typeId);
            activityRow.set(OMACACTIVITY.TITLE, activityTitle);
            activity.setTitle(activityTitle);
        }

        if (!collaborators.isEmpty()) {
            loadCollaboratorsIntoContainer(isCreateEvent, activity, entityContainer, targetContainer);
        }
        loadActivityDetailsIntoRow(orgId, activity, activityRow);
        activity.setUpdatedTime(activityRow.get(OMACACTIVITY.MODIFIED_TIME));

        targetContainer.updateRow(activityRow);

        if (!isCreateEvent) {
            deleteUnmappedNoteActivity(id, orgId, note, targetContainer);
            deleteUnmappedCollaborators(id, collaborators, entityContainer, targetContainer);
        }
    }

    /**
     * <p>
     * Deletes the unmapped rows for the collaborators in the given activity
     * </p>
     *
     * @param id              Represents the activity id
     * @param collaborators   Represents the collaborators
     * @param entityContainer Represents the entity {@link DataContainer}
     * @param targetContainer Represents the target {@link DataContainer}
     */
    private void deleteUnmappedCollaborators(final Long id, final Collection<Long> collaborators, final DataContainer entityContainer,
                                             final DataContainer targetContainer) {
        final Table collaboratorTable = getCollaboratorTable();
        final Criteria criteria = Criteria.eq(collaboratorTable.getColumn(OMACACTIVITYCOLLABORATOR.ACTIVITY_ID), id);
        final WhereClause whereClause = Objects.isNull(collaborators) ? new WhereClause(criteria) :
                new WhereClause(Criteria.notIn(collaboratorTable.getColumn(OMACACTIVITYCOLLABORATOR.COLLABORATOR_ID), collaborators)).and(criteria);
        final Stream<Row> rows = entityContainer.getRows(collaboratorTable, whereClause);

        rows.forEach(targetContainer::deleteRow);
    }

    /**
     * <p>
     * Collects the collaborator ids from the given {@link Row}.
     * </p>
     *
     * @param collaboratorRows Holds the details of collaborators.
     * @return Collaborator ids.
     */
    private Collection<Long> collectCollaboratorIdsFromRows(final Collection<Row> collaboratorRows) {
        return collaboratorRows.stream().map(row -> (Long) row.get(OMACACTIVITYCOLLABORATOR.COLLABORATOR_ID)).collect(Collectors.toSet());
    }

    /**
     * <p>
     * Loads the collaborators into target container.
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param activity        An entity that holds the activity attributes
     * @param entityContainer Holds the field values of existing activity
     * @param targetContainer Holds the updated information of field values provided by user
     */
    private void loadCollaboratorsIntoContainer(final boolean isCreateEvent, final Activity activity,
                                                final DataContainer entityContainer, final DataContainer targetContainer) {
        final Table collaboratorTable = getCollaboratorTable();

        for (final Long collaborator : activity.getCollaboratorIds()) {
            final Row row;

            if (isCreateEvent) {
                row = new Row(collaboratorTable);
            } else {
                final Row oldRow = entityContainer.getRow(collaboratorTable,
                        new WhereClause(Criteria.eq(collaboratorTable.getColumn(OMACACTIVITYCOLLABORATOR.ACTIVITY_ID), activity.id()))
                                .and(Criteria.eq(collaboratorTable.getColumn(OMACACTIVITYCOLLABORATOR.COLLABORATOR_ID), collaborator)));

                row = Objects.isNull(oldRow) ? new Row(collaboratorTable) : oldRow;
            }

            if (row.isNewRow()) {
                row.setOrgId(activity.orgId());
                row.set(OMACACTIVITYCOLLABORATOR.ACTIVITY_ID, activity.id());
                row.set(OMACACTIVITYCOLLABORATOR.COLLABORATOR_ID, collaborator);

                targetContainer.addNewRow(row);
            }
        }
    }

    /**
     * <p>
     * Loads the note activity row into container
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param activityId      Represents the activity id
     * @param note            An entity that holds the note attributes
     * @param targetContainer Holds the updated information of field values provided by user
     */
    public void loadNoteRowIntoContainer(final boolean isCreateEvent, final Long activityId, final Note note,
                                         final DataContainer targetContainer, final DataContainer entityContainer) {
        final Table noteActivityTable = getNoteActivityTable();
        final Row noteActivityRow;

        if (isCreateEvent) {
            noteActivityRow = new Row(noteActivityTable);
        } else {
            final Row oldRow = entityContainer.getRow(noteActivityTable, new WhereClause(Criteria.eq(noteActivityTable.getColumn(MACNOTEACTIVITY.ACTIVITY_ID), activityId))
                    .and(Criteria.eq(noteActivityTable.getColumn(MACNOTEACTIVITY.NOTE_ID), note.id())));
            noteActivityRow = Objects.isNull(oldRow) ? new Row(noteActivityTable) : oldRow;
        }
        final boolean isNewRow = noteActivityRow.isNewRow();

        noteManager.loadRowIntoContainer(isNewRow, note, entityContainer, targetContainer);
        noteActivityRow.set(MACNOTEACTIVITY.ACTIVITY_ID, activityId);
        noteActivityRow.set(MACNOTEACTIVITY.NOTE_ID, note.id());
        targetContainer.updateRow(noteActivityRow);
    }

    /**
     * Delete the note that are not associated with the activity
     *
     * @param activityId      Represents activity id
     * @param orgId           Represents organisation id
     * @param note            Represents note id
     * @param targetContainer Holds the new/updated/removed field values
     */
    public void deleteUnmappedNoteActivity(final Long activityId, final Long orgId, final Note note, final DataContainer targetContainer) {
        final Table noteActivityTable = getNoteActivityTable();
        final Long noteId = Objects.nonNull(note) ? note.id() : null;
        final WhereClause activityIdWhereclause = new WhereClause(Criteria.eq(noteActivityTable
                .getColumn(MACNOTEACTIVITY.ACTIVITY_ID), activityId));
        final WhereClause whereClause = Objects.nonNull(noteId) ? activityIdWhereclause
                .and(Criteria.notEq(noteActivityTable.getColumn(MACNOTEACTIVITY.NOTE_ID), noteId)) : activityIdWhereclause;
        final Row noteActivityRow = orgDataStore(orgId).get(noteActivityTable, whereClause).getRow(noteActivityTable);

        if (Objects.nonNull(noteActivityRow)) {
            targetContainer.deleteRow(noteActivityRow);
        }
    }

    /**
     * <p>
     * Builds the timeline when any action happens related to activity
     * </p>
     *
     * @param isCreateEvent A flag which indicates whether the action is related to create or update event.
     * @param activity      An entity that holds the activity attributes
     * @return {@link Timeline}
     */
    public Timeline buildActivityTimeline(final boolean isCreateEvent, final Activity activity, final Row typeRow) {
        final Timeline timeline = new Timeline();
        final ActivityTimeline activityTimeline = new ActivityTimeline();

        if (isCreateEvent) {
            timeline.setAction(TimelineActionFields.ADDED);
            timeline.setTimelineType(TimelineType.ACTIVITY_ADDED);
        }
        timeline.setOwnerId(activity.getCreatorId());
        timeline.setCategory(Timeline.Category.ACTIVITY);
        timeline.setOrgId(activity.orgId());
        activityTimeline.setIconId(typeRow.get(OMACACTIVITYTYPE.ICON_ID));
        timeline.setAuditedTime(activity.getUpdatedTime());
        timeline.setEntityTimeline(activityTimeline);

        return timeline;
    }

    /**
     * <p>
     * Updates the status of the overdue activities.
     * </p>
     */
    public void updateStatusToOverdue() {
        final Table table = getTable();
        final Column statusColumn = table.getColumn(OMACACTIVITY.STATUS);
        final WhereClause whereClause = new WhereClause(Criteria.eq(table.getColumn(OMACACTIVITY.IS_DONE), false))
                .and(Criteria.lt(table.getColumn(OMACACTIVITY.END_TIME), Instant.now().toEpochMilli()))
                .and(Criteria.notEq(statusColumn, Status.OVERDUE.getId()));

        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(whereClause);
        final long totalCount = writableDataStore.get(countQuery);

        LOGGER.info("Activity overdue count: {}", totalCount);
        if (totalCount > 0) {
            final Column pkColumn = table.getPKColumn();
            final SelectQuery selectQuery = SelectQuery.get(table);

            selectQuery.setWhereClause(whereClause);
            selectQuery.addSelectClause(pkColumn);

            final int batchSize = 200;
            final Counter counter = new Counter(1);

            while (counter.getCount() <= totalCount) {
                final int start = counter.getCount();

                counter.setCount(start + 100);
                selectQuery.setLimitClause(new LimitClause(start, batchSize));

                final DataContainer dataContainer = writableDataStore.get(selectQuery);
                final Set<Long> ids = dataContainer.getRows(table).map(Row::getPKValue).collect(Collectors.toUnmodifiableSet());

                if (ids.isEmpty()) {
                    break;
                }

                final UpdateQuery updateQuery = UpdateQuery.get(table);

                updateQuery.addUpdateColumn(statusColumn, Status.OVERDUE.getId());
                updateQuery.setWhereClause(new WhereClause(Criteria.in(pkColumn, ids)));

                writableDataStore.update(updateQuery);
            }
        }
    }

    /**
     * <p>
     * Fetches the event id related to the activity.
     * </p>
     *
     * @param orgId      Represents an organization id.
     * @param activityId Represents the activity id.
     * @return event id related to the activity.
     */
    public String getEventId(final long orgId, final long activityId) {
        final Table calendarEventTable = Table.get(OMCALEXTERNALEVENTS.TABLE);
        final SelectQuery selectQuery = SelectQuery.get(calendarEventTable);

        selectQuery.addSelectClause(calendarEventTable.getColumn(OMCALEXTERNALEVENTS.EXTERNAL_EVENT_ID));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(calendarEventTable.getColumn(OMCALEXTERNALEVENTS.ACTIVITY_ID), activityId)));

        final Row row = orgDataStore(orgId).get(selectQuery).getRow(calendarEventTable);

        return Objects.isNull(row) ? null : row.get(OMCALEXTERNALEVENTS.EXTERNAL_EVENT_ID);
    }

    /**
     * <p>
     * Fetches the email address which is associated with synced calendar of the user.
     * </p>
     *
     * @param orgId  Represents an organization id.
     * @param userId Represents a user id.
     * @return the synced email address.
     */
    public String getSyncedCalendarEmailAddress(final long orgId, final long userId) {
        final Table calendarSyncSettingsTable = getCalendarSyncSettingsTable();
        final SelectQuery selectQuery = SelectQuery.get(calendarSyncSettingsTable);

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(calendarSyncSettingsTable.getColumn(OMCALCALENDARSYNCSETTINGS.USER_ID), userId))
                .and(Criteria.eq(calendarSyncSettingsTable.getColumn(OMCALCALENDARSYNCSETTINGS.IS_SYNC_ENABLED), true)));
        selectQuery.addSelectClause(calendarSyncSettingsTable.getColumn(OMCALCALENDARSYNCSETTINGS.EMAIL_ADDRESS));

        final Row row = orgDataStore(orgId).get(selectQuery).getRow(calendarSyncSettingsTable);

        return Objects.isNull(row) ? null : row.get(OMCALCALENDARSYNCSETTINGS.EMAIL_ADDRESS);
    }

    /**
     * <p>
     * Gets the calendar sync id for an respective organization and user.
     * </p>
     *
     * @param orgId    Represents an organization id.
     * @param userId   Represents a user id.
     * @param isSynced A flag which represents the sync enabled or not
     * @return the synced calendar id.
     */
    public Long getCalendarSyncId(final long orgId, final long userId, final boolean isSynced) {
        final Table calendarSyncSettingsTable = getCalendarSyncSettingsTable();
        final WhereClause whereClause = new WhereClause(Criteria.eq(calendarSyncSettingsTable.getColumn(OMCALCALENDARSYNCSETTINGS.USER_ID), userId))
                .and(Criteria.eq(calendarSyncSettingsTable.getColumn(OMCALCALENDARSYNCSETTINGS.CONNECTION_STATUS), ACTIVE.getId()));
        final Row row = orgDataStore(orgId).get(calendarSyncSettingsTable, isSynced ? whereClause
                        .and(Criteria.eq(calendarSyncSettingsTable.getColumn(OMCALCALENDARSYNCSETTINGS.IS_SYNC_ENABLED), true)) : whereClause,
                List.of(calendarSyncSettingsTable.getPKColumn())).getRow(calendarSyncSettingsTable);

        return Objects.isNull(row) ? null : row.getPKValue();
    }

    /**
     * <p>
     * Loads the activity details into the row.
     * </p>
     *
     * @param orgId       Represents an organization id.
     * @param activity    Represents the entity of {@link Activity}
     * @param activityRow Represents the entity of {@link Row}
     */
    public void loadActivityDetailsIntoRow(final Long orgId, final Activity activity, final Row activityRow) {
        final Long currentTime = Instant.now().toEpochMilli();
        final ActivityType activityType = activity.getType();
        final DateTime createdTime = activity.getCreatedTime();

        if (activityRow.isNewRow()) {
            activityRow.setOrgId(activity.orgId());
            activity.setId(activityRow.getPKValue());
            activityRow.set(OMACACTIVITY.CREATED_TIME, Objects.isNull(createdTime) ? currentTime : createdTime);
            activityRow.set(OMACACTIVITY.CREATOR_ID, activity.getCreatorId());
        } else {

            if (Objects.nonNull(createdTime)) {
                activityRow.setIfModified(OMACACTIVITY.CREATED_TIME, createdTime);
            }
        }

        if (Objects.nonNull(activityType)) {
            final Long typeId = activityType.id();
            final String title = activity.getTitle();

            activityRow.set(OMACACTIVITY.TYPE_ID, typeId);
            activityRow.set(OMACACTIVITY.TITLE, Objects.isNull(title) ? activityTypeManager.fetchName(orgId, typeId) : title);
        }
        activityRow.set(OMACACTIVITY.DESCRIPTION, activity.getDescription());
        activityRow.set(OMACACTIVITY.MODIFIED_TIME, currentTime);
        activityRow.set(OMACACTIVITY.STATUS, activity.getStatus().getId());
        activityRow.set(OMACACTIVITY.REMINDER, activity.getReminder().getId());
        activityRow.set(OMACACTIVITY.PRIORITY, activity.getPriority().getId());
        activityRow.set(OMACACTIVITY.AVAILABILITY, activity.getAvailability().getId());
        activityRow.set(OMACACTIVITY.START_TIME, activity.getStartTime());
        activityRow.set(OMACACTIVITY.END_TIME, activity.getEndTime());
        activityRow.set(OMACACTIVITY.ASSIGNEE_ID, activity.getAssigneeId());
        activityRow.set(OMACACTIVITY.DURATION, activity.getDuration());
        activityRow.set(OMACACTIVITY.COMPLETED_TIME, activity.getCompletedTime());
        activityRow.set(OMACACTIVITY.LAST_NOTIFICATION_TIME, activity.getLastNotificationTime());
        activityRow.set(OMACACTIVITY.IS_DONE, activity.getDone());
        activityRow.set(OMACACTIVITY.LOCATION, activity.getLocation());
        activityRow.set(OMACACTIVITY.MODIFIED_BY, activity.getModifiedById());
    }

    /**
     * <p>
     * Retrieves the calendar synchronization configuration based on the provided email address for a specific organization and user.
     * </p>
     *
     * @param orgId        Represents an organization id
     * @param userId       Represents the user id
     * @param emailAddress Represents the calendar synced email address
     * @return the calendar sync config
     */
    public String getCalendarSyncConfig(final long orgId, final long userId, final EmailAddress emailAddress) {
        final Table calendarSyncTable = Table.get(OMCALCALENDARSYNCSETTINGS.TABLE);
        final DataContainer dataContainer = orgDataStore(orgId).get(calendarSyncTable,
                new WhereClause(Criteria.eq(calendarSyncTable.getColumn(OMCALCALENDARSYNCSETTINGS.USER_ID), userId))
                        .and(Criteria.eq(calendarSyncTable.getColumn(OMCALCALENDARSYNCSETTINGS.EMAIL_ADDRESS), emailAddress.getEmailId())),
                List.of(calendarSyncTable.getColumn(OMCALCALENDARSYNCSETTINGS.SYNC_CONFIG)));
        final Row calendarSyncRow = dataContainer.getRow(calendarSyncTable);

        return Objects.isNull(calendarSyncRow) ? null : calendarSyncRow.get(OMCALCALENDARSYNCSETTINGS.SYNC_CONFIG);
    }

    /**
     * <p>
     * Fetches the time intervals for activities within the specified time range.
     * Retrieves the start and end times falling between the activities which is matched for given start and end time.
     * </p>
     *
     * @param orgId     Represents an organization id
     * @param startTime Represents the start time
     * @param endTime   Represents the end time
     * @return {@link Collection} of {@link Activity}
     */
    public Collection<Interval<Long, Long>> getActivityDurations(final long orgId, final Long startTime, final Long endTime) {
        final Collection<Interval<Long, Long>> activities = new ArrayList<>();
        final Table activityTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(activityTable);
        final Column startTimeColumn = activityTable.getColumn(OMACACTIVITY.START_TIME);
        final Column endTimeColumn = activityTable.getColumn(OMACACTIVITY.END_TIME);

        selectQuery.addSelectClause(List.of(startTimeColumn, endTimeColumn));
        selectQuery.setWhereClause(new WhereClause(Criteria.between(startTimeColumn, startTime, endTime)).or(Criteria.between(endTimeColumn, startTime, endTime)));
        orgDataStore(orgId).get(selectQuery).getRows(activityTable).forEach(row -> activities.add(new Interval<>(dateTimeUtil.getTimeStamp(row.get(OMACACTIVITY.START_TIME)), dateTimeUtil.getTimeStamp(row.get(OMACACTIVITY.END_TIME)))));

        return activities;
    }

    /**
     * <p>
     * Gets the total records for the given start and end time of activity
     * </p>
     *
     * @param orgId              Represents an organization id
     * @param startTime          Represents the start time
     * @param endTime            Represents the end time
     * @param filterCriteriaList Represents the {@link Collection} of {@link FilterCriteria}
     * @param activityTypeIds    Represents the {@link Collection} of activity type ids
     * @return the total records
     */
    public Long getTotalRecords(final long orgId, final Long startTime, final Long endTime,
                                final Collection<FilterCriteria> filterCriteriaList, final Collection<Long> activityTypeIds) {
        final Table activityTable = getTable();
        final CountQuery countQuery = CountQuery.get(activityTable);
        final Column startTimeColumn = activityTable.getColumn(OMACACTIVITY.START_TIME);
        final Column endTimeColumn = activityTable.getColumn(OMACACTIVITY.END_TIME);
        final Column typeIdColumn = activityTable.getColumn(OMACACTIVITY.TYPE_ID);
        final WhereClause whereClause = new WhereClause(Criteria.between(startTimeColumn, startTime, endTime)).or(Criteria.between(endTimeColumn, startTime, endTime))
                .or(new WhereClause(Criteria.lt(startTimeColumn, startTime)).and(Criteria.gt(endTimeColumn, endTime)));

        countQuery.addJoinClause(new JoinClause(typeIdColumn, activityTypeManager.getTable().getPKColumn()));
        countQuery.setWhereClause(activityTypeIds.isEmpty() ? whereClause : whereClause.and(new WhereClause(Criteria.in(typeIdColumn, activityTypeIds))));
        filterCriteriaHandler.applyCriteria(orgId, Provider.ACTIVITY_LIST, countQuery, filterCriteriaList);

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Retrieves the count of activities for the specified organization based on the provided status and filter criteria
     * </p>
     *
     * @param orgId              Represents an organization id
     * @param status             Represents the {@link Status}
     * @param filterCriteriaList Represents the {@link Collection} of {@link FilterCriteria}
     * @param activityTypeIds    Represents the {@link Collection} of activity type ids
     * @return the count of the activities
     */
    public Long getActivitiesCount(final long orgId, final Status status, final Collection<FilterCriteria> filterCriteriaList,
                                   final Collection<Long> activityTypeIds) {
        final Table activityTable = getTable();
        final CountQuery countQuery = CountQuery.get(activityTable);
        WhereClause whereClause = null;

        if (Objects.nonNull(status)) {
            whereClause = new WhereClause(Criteria.eq(activityTable.getColumn(OMACACTIVITY.STATUS), status.getId()));
        }

        if (!activityTypeIds.isEmpty()) {
            whereClause = WhereClauseUtil.connect(whereClause, new WhereClause(Criteria.in(activityTable.getColumn(OMACACTIVITY.TYPE_ID), activityTypeIds)), Connector.AND);
        }

        if (Objects.nonNull(whereClause)) {
            countQuery.setWhereClause(whereClause);
        }

        if (Objects.nonNull(filterCriteriaList) && !filterCriteriaList.isEmpty()) {
            filterCriteriaHandler.applyCriteria(orgId, Provider.ACTIVITY_LIST, countQuery, filterCriteriaList);
        }

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Retrieves the completed activity count based on the provided criteria.
     * </p>
     *
     * @param orgId          Represents an organization id
     * @param userIds        Represents the user id
     * @param activityTypeId Represents the activity type id
     * @param startTime      Represents the activity start time
     * @param endTime        Represents the activity end time
     * @return The completed activity count
     */
    public long getCompletedActivityCount(final Long orgId, final Collection<Long> userIds, final Long activityTypeId, final Long startTime,
                                          final Long endTime) {
        final Table activityTable = getTable();
        final CountQuery countQuery = CountQuery.get(activityTable);

        countQuery.setWhereClause(new WhereClause(Criteria.in(activityTable.getColumn(OMACACTIVITY.ASSIGNEE_ID), userIds))
                .and(Criteria.eq(activityTable.getColumn(OMACACTIVITY.TYPE_ID), activityTypeId))
                .and(Criteria.ge(activityTable.getColumn(OMACACTIVITY.START_TIME), startTime))
                .and(Criteria.le(activityTable.getColumn(OMACACTIVITY.END_TIME), endTime))
                .and(Criteria.eq(activityTable.getColumn(OMACACTIVITY.IS_DONE), true)));

        return DataStoreProvider.getOrgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Gets the private note of the activity
     * </p>
     *
     * @param orgId      Represents the organisation Id
     * @param activityId Represents the activity Id
     * @return The private note
     */
    public String getPrivateNote(final Long orgId, final Long activityId) {
        final Table activityTable = getTable();
        final Table noteActivityTable = getNoteActivityTable();
        final Table noteTable = noteManager.getTable();
        final SelectQuery selectQuery = SelectQuery.get(activityTable);

        selectQuery.addSelectClause(List.of(noteTable.getColumn(OMNTNOTE.CONTENT)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(activityTable.getPKColumn(), activityId)));
        selectQuery.addJoinClause(List.of(new JoinClause(activityTable.getPKColumn(), noteActivityTable.getColumn(MACNOTEACTIVITY.ACTIVITY_ID)),
                new JoinClause(noteActivityTable.getColumn(MACNOTEACTIVITY.NOTE_ID), noteTable.getPKColumn())));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Row row = dataContainer.getRow(noteTable);

        return Objects.isNull(row) ? null : row.get(OMNTNOTE.CONTENT);
    }

    /**
     * <p>
     * Stores the check-in and check-out record
     * </p>
     *
     * @param checkInOutRecord Represents the {@link CheckInOutRecord}
     */
    public void storeCheckInOutRecord(final CheckInOutRecord checkInOutRecord) {
        final Table activityTable = getTable();
        final Long orgId = checkInOutRecord.orgId();
        final Long activityId = checkInOutRecord.getActivityId();
        final UpdateQuery updateQuery = UpdateQuery.get(activityTable);

        if (checkInOutRecord.isCheckIn()) {
            updateQuery.addUpdateColumn(activityTable.getColumn(OMACACTIVITY.CHECKED_IN_TIME), checkInOutRecord.getTime());
            updateQuery.addUpdateColumn(activityTable.getColumn(OMACACTIVITY.CHECK_IN_INFO), checkInOutRecord.getDataAsString());
        } else {
            final CheckInOutRecord checkInRecord = getCheckInRecord(orgId, activityId);

            if (Objects.isNull(checkInRecord)) {
                throw ErrorCode.get(ActivityErrorCodes.CHECK_IN_REQUIRED, "i18n.activity.check.out.invalid");
            } else {
                final long checkedOutTime = checkInOutRecord.getTime();

                updateQuery.addUpdateColumn(activityTable.getColumn(OMACACTIVITY.CHECKED_OUT_TIME), checkedOutTime);
                updateQuery.addUpdateColumn(activityTable.getColumn(OMACACTIVITY.CHECK_OUT_INFO), checkInOutRecord.getDataAsString());
            }
        }
        updateQuery.addUpdateColumn(activityTable.getColumn(OMACACTIVITY.MODIFIED_TIME), Instant.now().toEpochMilli());
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(activityTable.getPKColumn(), activityId)));

        orgDataStore(orgId).update(updateQuery);
    }

    /**
     * <p>
     * Gets the check-in record of the activity
     * </p>
     *
     * @param orgId      Represents the organisation Id
     * @param activityId Represents the activity Id
     * @return The check-in record of the activity
     */
    public CheckInOutRecord getCheckInRecord(final Long orgId, final Long activityId) {
        final Table activityTable = getTable();
        final DataContainer dataContainer = orgDataStore(orgId).get(activityTable, new WhereClause(Criteria.eq(activityTable.getPKColumn(), activityId)),
                List.of(activityTable.getColumn(OMACACTIVITY.CHECK_IN_INFO), activityTable.getColumn(OMACACTIVITY.CHECKED_IN_TIME)));
        final Row row = dataContainer.getRow(activityTable);

        if (Objects.isNull(row)) {
            return null;
        } else {
            final CheckInOutRecord checkInOutRecord = new CheckInOutRecord();

            checkInOutRecord.setDataAsString(row.get(OMACACTIVITY.CHECK_IN_INFO));
            checkInOutRecord.setTime(dateTimeUtil.getTimeStamp(row.get(OMACACTIVITY.CHECKED_IN_TIME)));

            return checkInOutRecord;
        }
    }

    /**
     * <p>
     * Updates {@link OMACACTIVITY#IS_DONE} for the given activities from the global search tab.
     * </p>
     *
     * @param orgId       Represents an organization id.
     * @param activityIds Represents the activity ids to update.
     * @param isDone      Whether the activities are marked as done.
     * @return Ids of activities that were updated.
     */
    public Collection<Long> updateIsDone(final long orgId, final Collection<Long> activityIds, final boolean isDone) {
        final Collection<Long> updatedRecords = new ArrayList<>();
        final Table activityTable = getTable();
        final Column pkColumn = activityTable.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(activityTable);

        selectQuery.addSelectClause(pkColumn);
        selectQuery.setWhereClause(new WhereClause(Criteria.in(pkColumn, activityIds)));

        final List<Long> existingActivityIds = orgDataStore(orgId).get(selectQuery).getRows(activityTable).map(Row::getPKValue).toList();

        if (existingActivityIds.isEmpty()) {
            return updatedRecords;
        }
        final UpdateQuery updateQuery = UpdateQuery.get(activityTable);
        final Column isDoneColumn = activityTable.getColumn(OMACACTIVITY.IS_DONE);

        updateQuery.addUpdateColumn(isDoneColumn, isDone);
        updateQuery.setWhereClause(new WhereClause(Criteria.in(pkColumn, existingActivityIds)));

        orgDataStore(orgId).update(updateQuery);
        updatedRecords.addAll(existingActivityIds);

        return updatedRecords;
    }

    /**
     * <p>
     * Gets the total time spent of the activities
     * </p>
     *
     * @param orgId       Represents the org id
     * @param activityIds Represents the activity ids
     * @return the total time spent map
     */
    public Map<Long, Long> getTotalTimeSpentMap(final Long orgId, final Collection<Long> activityIds) {
        final Table activityTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(activityTable);

        selectQuery.addSelectClause(List.of(activityTable.getColumn(OMACACTIVITY.CHECKED_IN_TIME), activityTable.getColumn(OMACACTIVITY.CHECKED_OUT_TIME)));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(activityTable.getPKColumn(), activityIds)));

        return orgDataStore(orgId).get(selectQuery).getRows(activityTable).filter(row -> Objects.nonNull(row.get(OMACACTIVITY.CHECKED_OUT_TIME)))
                .collect(Collectors.toMap(Row::getPKValue, activityRow -> {
                    final DateTime checkedOutTime = activityRow.get(OMACACTIVITY.CHECKED_OUT_TIME);
                    final DateTime checkedInTime = activityRow.get(OMACACTIVITY.CHECKED_IN_TIME);

                    return checkedOutTime.getTimeStamp() - checkedInTime.getTimeStamp();
                }));
    }
}