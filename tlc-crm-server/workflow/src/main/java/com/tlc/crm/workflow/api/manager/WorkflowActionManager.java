package com.tlc.crm.workflow.api.manager;

import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.crm.workflow.api.model.TriggerActionEvent;
import com.tlc.crm.workflow.api.model.TriggerActionField;
import com.tlc.crm.workflow.api.model.TriggerActionSource;
import com.tlc.crm.workflow.api.model.FieldGroup;
import com.tlc.crm.workflow.api.model.TriggerAction;
import com.tlc.crm.workflow.sql.resource.MWFLACTIONEVENT;
import com.tlc.crm.workflow.sql.resource.MWFLACTIONFIELDS;
import com.tlc.crm.workflow.sql.resource.MWFLACTIONSOURCE;
import com.tlc.crm.workflow.sql.resource.MWFLSTEPOPTIONFIELDS;
import com.tlc.crm.workflow.sql.resource.MWFLTRIGGERACTIONS;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.ReadableDataStore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * Implements the work flow action related DB actions
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 */
public final class WorkflowActionManager {

    private WorkflowActionManager() {

    }

    private static class Instance {
        private static final WorkflowActionManager INSTANCE = new WorkflowActionManager();
    }

    /**
     * <p>
     * Gets the instance of {@link WorkflowActionManager}
     * </p>
     *
     * @return The {@link WorkflowActionManager} instance
     */
    public static WorkflowActionManager getInstance() {
        return WorkflowActionManager.Instance.INSTANCE;
    }

    /**
     * Returns {@link ReadableDataStore}.
     *
     * @return {@link ReadableDataStore}.
     */
    private ReadableDataStore readableDataStore() {
        return DataStoreProvider.getReadableDataStore();
    }

    /**
     * Gets the {@link Table} of trigger action source.
     *
     * @return The trigger action source table.
     */
    private Table getActionSourceTable() {
        return Table.get(MWFLACTIONSOURCE.TABLE);
    }

    /**
     * Gets the {@link Table} of trigger action event.
     *
     * @return The trigger action event table.
     */
    private Table getActionEventTable() {
        return Table.get(MWFLACTIONEVENT.TABLE);
    }

    /**
     * Gets the {@link Table} of trigger actions table.
     *
     * @return The trigger action table.
     */
    private Table getTriggerActionsTable() {
        return Table.get(MWFLTRIGGERACTIONS.TABLE);
    }

    /**
     * Gets the {@link Table} of step option table.
     *
     * @return The step option table.
     */
    private Table getStepOptionFieldsTable() {
        return Table.get(MWFLSTEPOPTIONFIELDS.TABLE);
    }

    /**
     * Gets the {@link Table} of trigger action fields table.
     *
     * @return The trigger action fields table.
     */
    private Table getActionFieldsTable() {
        return Table.get(MWFLACTIONFIELDS.TABLE);
    }

    /**
     * Gets all the {@link TriggerAction}.
     *
     * @return The collection of {@link TriggerAction}.
     */
    public Collection<TriggerAction> getAllTriggerAction() {
        final DataContainer triggerActionContainer = readableDataStore().get(buildTriggerActionSelectQuery());

        return triggerActionContainer.isEmpty() ? List.of() : convertContainerIntoTriggerActions(triggerActionContainer);
    }

    /**
     * Gets the {@link TriggerAction} by given trigger source id
     *
     * @param triggerSourceId Represents the trigger source id
     * @return The {@link TriggerAction} details.
     */
    public TriggerAction getTriggerAction(final long triggerSourceId) {
        final SelectQuery selectQuery = buildTriggerActionSelectQuery();

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(getTriggerActionsTable().getColumn(MWFLTRIGGERACTIONS.TRIGGER_SOURCE_ID), triggerSourceId)));

        final DataContainer triggerActionContainer = readableDataStore().get(selectQuery);

        return triggerActionContainer.isEmpty() ? null : convertContainerIntoTriggerActions(triggerActionContainer).stream().findFirst().orElseGet(() -> null);
    }

    /**
     * <p>
     * Builds and returns a {@link SelectQuery} to retrieve {@link TriggerAction} data from the database.
     * </p>>
     *
     * @return A {@link SelectQuery} representing the query to fetch trigger action data
     */
    private SelectQuery buildTriggerActionSelectQuery() {
        final Table actionSourceTable = getActionSourceTable();
        final Table actionEventTable = getActionEventTable();
        final Table triggerActionTable = getTriggerActionsTable();
        final SelectQuery selectQuery = SelectQuery.get(triggerActionTable);

        selectQuery.addJoinClause(List.of(new JoinClause(triggerActionTable.getColumn(MWFLTRIGGERACTIONS.ACTION_SOURCE_ID), actionSourceTable.getPKColumn()),
                new JoinClause(triggerActionTable.getColumn(MWFLTRIGGERACTIONS.ACTION_EVENT_ID), actionEventTable.getPKColumn())));
        selectQuery.addSelectClause(actionEventTable, actionSourceTable, triggerActionTable);

        return selectQuery;
    }

    /**
     * <p>
     * Converts the provided {@link DataContainer} representing trigger action sources to a collection of {@link TriggerAction}
     * </p>
     *
     * @param dataContainer A container which holds the details of trigger action information
     * @return The collection of {@link TriggerAction}
     */
    private Collection<TriggerAction> convertContainerIntoTriggerActions(final DataContainer dataContainer) {
        final Table actionSourceTable = getActionSourceTable();
        final Table actionEventTable = getActionEventTable();
        final Table triggerActionTable = getTriggerActionsTable();
        final Collection<TriggerAction> triggerActions = new ArrayList<>();
        final Map<Long, Row> actionSourceRows = dataContainer.getRows(actionSourceTable).collect(Collectors.toMap(Row::getPKValue, Function.identity()));
        final Map<Long, Row> actionEventRows = dataContainer.getRows(actionEventTable).collect(Collectors.toMap(Row::getPKValue, Function.identity()));
        final Map<Short, Map<Long, List<Row>>> groupedRows = dataContainer.getRows(triggerActionTable).collect(Collectors.groupingBy(triggerActionRow ->
                triggerActionRow.get(MWFLTRIGGERACTIONS.TRIGGER_SOURCE_ID), Collectors.groupingBy(triggerActionRow ->
                triggerActionRow.get(MWFLTRIGGERACTIONS.ACTION_SOURCE_ID))));

        for (final Map.Entry<Short, Map<Long, List<Row>>> triggerActionEntry : groupedRows.entrySet()) {
            final short triggerSourceId = triggerActionEntry.getKey();
            final Collection<TriggerActionSource> triggerActionSources = new ArrayList<>();

            for (final Map.Entry<Long, List<Row>> actionSourceEntry : triggerActionEntry.getValue().entrySet()) {
                final long triggerActionSourceId = actionSourceEntry.getKey();
                final Row actionSourceRow = actionSourceRows.get(triggerActionSourceId);
                final Collection<TriggerActionEvent> triggerActionEvents = new ArrayList<>();

                for (final Row triggerActionRow : actionSourceEntry.getValue()) {
                    final long triggerActionEventId = triggerActionRow.get(MWFLTRIGGERACTIONS.ACTION_EVENT_ID);
                    final Row actionEventRow = actionEventRows.get(triggerActionEventId);

                    triggerActionEvents.add(new TriggerActionEvent.Builder(actionEventRow.getPKValue(),
                            actionEventRow.get(MWFLACTIONEVENT.ALIAS_NAME), actionEventRow.get(MWFLACTIONEVENT.NAME))
                            .isEnabled(triggerActionRow.get(MWFLTRIGGERACTIONS.IS_ENABLED)).build());
                }
                triggerActionSources.add(new TriggerActionSource.Builder(triggerActionSourceId, actionSourceRow.get(MWFLACTIONSOURCE.NAME),
                        actionSourceRow.get(MWFLACTIONSOURCE.ALIAS_NAME)).setTriggerActionEvents(triggerActionEvents).build());
            }
            triggerActions.add(new TriggerAction(triggerSourceId, triggerActionSources));
        }
        return triggerActions;
    }

    /**
     * <p>
     * Retrieves all {@link TriggerActionSource} entities
     * </p>
     *
     * @return A Collection of {@link TriggerActionSource} entities
     */
    public Collection<TriggerActionSource> getAllTriggerActionSources() {
        final Table actionSourceTable = getActionSourceTable();
        final DataContainer dataContainer = readableDataStore().get(SelectQuery.get(actionSourceTable));

        return dataContainer.isEmpty() ? List.of() : dataContainer.getRows(actionSourceTable).map(row -> new TriggerActionSource.Builder(row.getPKValue(), row.get(MWFLACTIONSOURCE.NAME),
                row.get(MWFLACTIONSOURCE.ALIAS_NAME)).build()).toList();
    }

    /**
     * <p>
     * Gets the {@link TriggerActionSource} by given id
     * </p>
     *
     * @param triggerActionSourceId Represents the trigger action source id
     * @return The {@link TriggerActionSource}
     */
    public TriggerActionSource getTriggerActionSource(final long triggerActionSourceId) {
        final Table actionSourceTable = getActionSourceTable();
        final DataContainer dataContainer = readableDataStore().get(actionSourceTable, new WhereClause(Criteria.eq(actionSourceTable.getPKColumn(), triggerActionSourceId)));

        if (!dataContainer.isEmpty()) {
            final Row actionSourceRow = dataContainer.getRow(actionSourceTable);

            return new TriggerActionSource.Builder(actionSourceRow.getPKValue(), actionSourceRow.get(MWFLACTIONSOURCE.NAME),
                    actionSourceRow.get(MWFLACTIONSOURCE.ALIAS_NAME)).build();
        }

        return null;
    }

    /**
     * <p>
     * Retrieves all {@link TriggerActionEvent} entities
     * </p>
     *
     * @return A Collection of {@link TriggerActionEvent} entities
     */
    public Collection<TriggerActionEvent> getAllTriggerActionEvents() {
        final Table actionEventTable = getActionEventTable();
        final DataContainer dataContainer = readableDataStore().get(SelectQuery.get(actionEventTable));

        return dataContainer.isEmpty() ? List.of() : dataContainer.getRows(actionEventTable).map(row -> new TriggerActionEvent.Builder(row.getPKValue(), row.get(MWFLACTIONSOURCE.ALIAS_NAME),
                row.get(MWFLACTIONSOURCE.NAME)).build()).toList();
    }

    /**
     * <p>
     * Gets the {@link TriggerActionEvent} by trigger action event id
     * </p>
     *
     * @param triggerActionEventId Represents the trigger action event id
     * @return The {@link TriggerActionEvent}
     */
    public TriggerActionEvent getTriggerActionEvent(final long triggerActionEventId) {
        final Table actionEventTable = getActionEventTable();
        final DataContainer dataContainer = readableDataStore().get(actionEventTable, new WhereClause(Criteria.eq(actionEventTable.getPKColumn(), triggerActionEventId)));

        if (!dataContainer.isEmpty()) {
            final Row actionEventRow = dataContainer.getRow(actionEventTable);

            return new TriggerActionEvent.Builder(actionEventRow.getPKValue(), actionEventRow.get(MWFLACTIONSOURCE.ALIAS_NAME),
                    actionEventRow.get(MWFLACTIONSOURCE.NAME)).build();
        }

        return null;
    }

    /**
     * <p>
     * Retrieves all collection of {@link TriggerActionField} from the database
     * </p>
     *
     * @return The collection of {@link TriggerActionField}
     */
    public Collection<TriggerActionField> getAllTriggerActionFields() {
        final DataContainer actionFieldContainer = readableDataStore().get(buildTriggerActionFieldSelectQuery());

        return actionFieldContainer.isEmpty() ? List.of() : actionFieldContainer.getRows(getActionFieldsTable()).map(actionFieldRow ->
                convertRowToTriggerActionField(actionFieldRow, actionFieldContainer)).toList();
    }

    /**
     * <p>
     * Gets the {@link TriggerActionField} by given trigger action id
     * </p>
     *
     * @return The {@link TriggerActionField}
     */
    public Collection<TriggerActionField> getTriggerActionField(final long triggerActionId) {
        final Table actionFieldsTable = getActionFieldsTable();
        final SelectQuery selectQuery = buildTriggerActionFieldSelectQuery();

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(actionFieldsTable.getColumn(MWFLACTIONFIELDS.TRIGGER_ACTION_ID), triggerActionId)));

        final DataContainer actionFieldContainer = readableDataStore().get(selectQuery);

        return actionFieldContainer.isEmpty() ? null : actionFieldContainer.getRows(actionFieldsTable).map(actionFieldRow ->
                convertRowToTriggerActionField(actionFieldRow, actionFieldContainer)).toList();
    }

    /**
     * <p>
     * Builds and returns a {@link SelectQuery} to retrieve {@link TriggerActionField} data from the database.
     * </p>>
     *
     * @return A {@link SelectQuery} representing the query to fetch trigger action field data
     */
    private SelectQuery buildTriggerActionFieldSelectQuery() {
        final Table stepOptionFieldsTable = getStepOptionFieldsTable();
        final Table actionFieldsTable = getActionFieldsTable();
        final SelectQuery selectQuery = SelectQuery.get(actionFieldsTable);

        selectQuery.addJoinClause(new JoinClause(actionFieldsTable.getPKColumn(),
                stepOptionFieldsTable.getColumn(MWFLSTEPOPTIONFIELDS.ACTION_FIELD_ID), JoinClause.JoinType.LEFT));
        selectQuery.addSelectClause(stepOptionFieldsTable, actionFieldsTable);

        return selectQuery;
    }

    /**
     * <p>
     * Converts the trigger action field row into {@link TriggerActionField} entity
     * </p>
     *
     * @param actionFieldRow The database row containing {@link TriggerActionField} information
     * @param dataContainer  A container that holds the user defined field values and system values
     * @return The {@link TriggerActionField}
     */
    private TriggerActionField convertRowToTriggerActionField(final Row actionFieldRow, final DataContainer dataContainer) {
        final Table stepOptionTable = getStepOptionFieldsTable();
        final Collection<Long> stepOptionIds = dataContainer.getRows(stepOptionTable,
                        new WhereClause(Criteria.eq(stepOptionTable.getColumn(MWFLSTEPOPTIONFIELDS.ACTION_FIELD_ID), actionFieldRow.getPKValue())))
                .map(stepOptionFieldsRow -> (Long) stepOptionFieldsRow.get(MWFLSTEPOPTIONFIELDS.STEP_OPTION_ID)).collect(Collectors.toSet());
        final Long sourceFieldId = actionFieldRow.get(MWFLACTIONFIELDS.FIELD_ID);
        final Long fieldId;
        final FieldGroup fieldGroup;

        if (Objects.nonNull(sourceFieldId)) {
            fieldId = sourceFieldId;
            fieldGroup = FieldGroup.SOURCE_RELATED_FIELD;
        } else {
            fieldId = actionFieldRow.get(MWFLACTIONFIELDS.WORKFLOW_FIELD_ID);
            fieldGroup = FieldGroup.WORKFLOW_RELATED_FIELD;
        }
        return new TriggerActionField(actionFieldRow.getPKValue(), fieldId, actionFieldRow.get(MWFLACTIONFIELDS.TRIGGER_ACTION_ID),
                actionFieldRow.get(MWFLACTIONFIELDS.CONFIG), fieldGroup, stepOptionIds);
    }

    /**
     * <p>
     * Retrieves the trigger action id
     * </p>
     *
     * @param triggerSourceId Represents the trigger source id
     * @param triggerActionSourceId  Represents the trigger action source id
     * @param triggerActionEventId   Represents the trigger action event id
     * @return The trigger action id if found, or null if not found
     */
    public Long getTriggerActionId(final Short triggerSourceId, final Long triggerActionSourceId, final Long triggerActionEventId) {
        final Table triggerActionsTable = getTriggerActionsTable();
        final WhereClause whereClause = new WhereClause(Criteria.eq(triggerActionsTable.getColumn(MWFLTRIGGERACTIONS.TRIGGER_SOURCE_ID), triggerSourceId))
                .and(Criteria.eq(triggerActionsTable.getColumn(MWFLTRIGGERACTIONS.ACTION_SOURCE_ID), triggerActionSourceId))
                .and(Criteria.eq(triggerActionsTable.getColumn(MWFLTRIGGERACTIONS.ACTION_EVENT_ID), triggerActionEventId));
        final DataContainer triggerActionContainer = readableDataStore().get(triggerActionsTable, whereClause, List.of(triggerActionsTable.getPKColumn()));

        if (!triggerActionContainer.isEmpty()) {
            final Row row = triggerActionContainer.getRow(triggerActionsTable);

            return Objects.isNull(row) ? null : row.getPKValue();
        }

        return null;
    }
}
