package com.tlc.crm.workflow.api.manager;

import com.tlc.crm.fields.manager.AbstractReadableConfigManager;
import com.tlc.crm.fields.service.FieldTypeService;

import com.tlc.crm.workflow.api.model.WorkflowField;
import com.tlc.crm.workflow.cache.service.TriggerActionSourceService;
import com.tlc.crm.workflow.sql.resource.MWFLFIELDS;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * Manages the {@link WorkflowField} related DB actions
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public final class WorkflowFieldManager extends AbstractReadableConfigManager<WorkflowField> {

    private final FieldTypeService fieldTypeService;
    private final TriggerActionSourceService triggerActionSourceService;

    private WorkflowFieldManager() {
        this.fieldTypeService = FieldTypeService.getInstance();
        this.triggerActionSourceService = TriggerActionSourceService.getInstance();
    }

    private static class Instance {
        private static final WorkflowFieldManager INSTANCE = new WorkflowFieldManager();
    }

    /**
     * <p>
     * Gets the {@link WorkflowFieldManager} Instance
     * </p>
     *
     * @return {@link WorkflowFieldManager}
     */
    public static WorkflowFieldManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @return the {@link Table} of workflow field table
     */
    @Override
    public Table getTable() {
        return Table.get(MWFLFIELDS.TABLE);
    }

    /**
     * <p>
     * Gets the {@link Collection} of {@link WorkflowField} for the given trigger action source id
     * </p>
     *
     * @param triggerActionSourceId Represent the trigger action source id
     * @return {@link Collection} of {@link WorkflowField}
     */
    public Collection<WorkflowField> getWorkflowFieldsBySource(final Long triggerActionSourceId) {
        final Table workflowFieldsTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(workflowFieldsTable);

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(workflowFieldsTable.getColumn(MWFLFIELDS.ACTION_SOURCE_ID), triggerActionSourceId)));
        final DataContainer dataContainer = readableDataStore().get(selectQuery);

        return dataContainer.getRows(workflowFieldsTable).map(this::convertRowToModel).toList();
    }

    /**
     * <p>
     * Gets all the {@link Collection} of {@link WorkflowField}
     * </p>
     *
     * @return {@link Collection} of all workflow fields
     */
    public Collection<WorkflowField> getAllWorkflowFields() {
        final Table workflowFieldsTable = getTable();

        return readableDataStore().get(SelectQuery.get(workflowFieldsTable)).getRows(workflowFieldsTable)
                .map(this::convertRowToModel).collect(Collectors.toList());
    }

    /**
     * <p>
     * Get the {@link WorkflowField} which mapped for given workflow field id
     * </p>
     *
     * @param workflowFieldId Represents the pk value of workflow field table
     * @return {@link WorkflowField}
     */
    public WorkflowField getWorkflowField(final Long workflowFieldId) {
        final Table workflowFieldsTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(workflowFieldsTable);

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(workflowFieldsTable.getPKColumn(), workflowFieldId)));

        return convertRowToModel(readableDataStore().get(selectQuery).getRow(workflowFieldsTable));
    }

    /**
     * <p>
     * Gets the {@link Map} of {@link WorkflowField} by given {@link Collection} of alias names
     * </p>
     *
     * @param aliasNames Represents the {@link Collection} of alias names
     * @return {@link Map} of WorkflowField
     */
    public Map<String, WorkflowField> getWorkflowFieldsByAliasNames(final Collection<String> aliasNames) {
        final Map<String, WorkflowField> workflowFields = new HashMap<>();
        final Table workflowFieldsTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(workflowFieldsTable);
        final Column aliasNameColumn = workflowFieldsTable.getColumn(MWFLFIELDS.ALIAS_NAME);

        selectQuery.setWhereClause(new WhereClause(Criteria.in(aliasNameColumn, aliasNames)));
        readableDataStore().get(selectQuery).getRows(workflowFieldsTable).forEach(row -> workflowFields.put(row.get(aliasNameColumn), convertRowToModel(row)));

        return workflowFields;
    }

    /**
     * <p>
     * Gets the {@link WorkflowField} for the given alias name
     * </p>
     *
     * @param aliasName Represents the {@link String} of alias name
     * @return {@link WorkflowField}
     */
    public WorkflowField getWorkflowFieldByAliasName(final String aliasName) {
        final Table workflowFieldsTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(workflowFieldsTable);

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(workflowFieldsTable.getColumn(MWFLFIELDS.ALIAS_NAME), aliasName)));

        return convertRowToModel(readableDataStore().get(selectQuery).getRow(workflowFieldsTable));
    }

    /**
     * {@inheritDoc}
     *
     * @param row Refers the workflow field row
     * @return The {@link WorkflowField}
     */
    @Override
    public WorkflowField convertRowToModel(final Row row) {
        final WorkflowField workflowField = new WorkflowField();

        workflowField.setTriggerActionSource(triggerActionSourceService.get(row.get(MWFLFIELDS.ACTION_SOURCE_ID)));
        workflowField.setAliasName(row.get(MWFLFIELDS.ALIAS_NAME));
        workflowField.setFieldType(fieldTypeService.get(row.get(MWFLFIELDS.FIELD_TYPE_ID)));
        workflowField.setName(row.get(MWFLFIELDS.NAME));
        workflowField.setConfig(row.get(MWFLFIELDS.CONFIG));
        workflowField.setId(row.getPKValue());
        workflowField.setAliasName(row.get(MWFLFIELDS.ALIAS_NAME));

        return workflowField;
    }
}
