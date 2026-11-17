package com.tlc.crm.workflow.api.manager;

import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.crm.workflow.cache.service.TriggerActionEventService;
import com.tlc.crm.workflow.cache.service.TriggerActionSourceService;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.ReadableDataStore;

import com.tlc.crm.workflow.api.model.WorkflowCriteriaField;
import com.tlc.crm.workflow.api.model.FieldGroup;
import com.tlc.crm.workflow.api.model.WorkflowCriteriaSet;
import com.tlc.crm.workflow.api.model.WorkflowCriteriaValue;
import com.tlc.crm.workflow.api.model.TriggerSource;
import com.tlc.crm.workflow.api.model.TriggerSource.TriggerEvent;
import com.tlc.crm.workflow.api.model.CriteriaConnector;
import com.tlc.crm.workflow.api.model.WorkflowCriteria;
import com.tlc.crm.workflow.api.model.TemplateCategory;
import com.tlc.crm.workflow.api.model.WorkflowAction;
import com.tlc.crm.workflow.api.model.WorkflowTemplate;
import com.tlc.crm.workflow.cache.service.WorkflowComparatorService;
import com.tlc.crm.workflow.sql.resource.MWFLTEMPLATE;
import com.tlc.crm.workflow.sql.resource.MWFLTEMPLATECRITERIA;
import com.tlc.crm.workflow.sql.resource.MWFLTEMPLATECRITERIASET;
import com.tlc.crm.workflow.sql.resource.MWFLTEMPLATECRITERIAVALUE;
import com.tlc.crm.workflow.sql.resource.MWFLTEMPLATEACTIONS;
import com.tlc.crm.workflow.sql.resource.MWFLTEMPLATEFIELDCRITERIA;
import com.tlc.crm.workflow.sql.resource.MWFLTEMPLATESOURCEFIELDCRITERIA;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Manages the workflow templates related DB actions.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class WorkflowTemplateManager {

    private final WorkflowComparatorService comparatorService;
    private final TriggerActionSourceService triggerActionSourceService;
    private final TriggerActionEventService triggerActionEventService;

    private WorkflowTemplateManager() {
        this.comparatorService = WorkflowComparatorService.getInstance();
        this.triggerActionSourceService = TriggerActionSourceService.getInstance();
        this.triggerActionEventService = TriggerActionEventService.getInstance();
    }

    private static class Instance {
        private static final WorkflowTemplateManager INSTANCE = new WorkflowTemplateManager();
    }

    public static WorkflowTemplateManager getInstance() {
        return Instance.INSTANCE;
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
     * Gets the {@link Table} of template.
     *
     * @return The template table.
     */
    private Table getTable() {
        return Table.get(MWFLTEMPLATE.TABLE);
    }

    /**
     * Gets the {@link Table} of template criteria.
     *
     * @return The template criteria table.
     */
    private Table getTemplateCriteriaTable() {
        return Table.get(MWFLTEMPLATECRITERIA.TABLE);
    }

    /**
     * Gets the {@link Table} of template criteria set.
     *
     * @return The template criteria set table.
     */
    private Table getTemplateCriteriaSetTable() {
        return Table.get(MWFLTEMPLATECRITERIASET.TABLE);
    }

    /**
     * Gets the {@link Table} of template criteria value.
     *
     * @return The template criteria value table.
     */
    private Table getTemplateCriteriaValueTable() {
        return Table.get(MWFLTEMPLATECRITERIAVALUE.TABLE);
    }

    /**
     * Gets the {@link Table} of template field criteria.
     *
     * @return The template field criteria table.
     */
    private Table getTemplateSourceFieldCriteriaTable() {
        return Table.get(MWFLTEMPLATESOURCEFIELDCRITERIA.TABLE);
    }

    /**
     * Gets the {@link Table} of template unregister field criteria.
     *
     * @return The template unregister field criteria table.
     */
    private Table getTemplateFieldCriteriaTable() {
        return Table.get(MWFLTEMPLATEFIELDCRITERIA.TABLE);
    }

    /**
     * Gets the {@link Table} of template action.
     *
     * @return The template action table.
     */
    private Table getTemplateActionTable() {
        return Table.get(MWFLTEMPLATEACTIONS.TABLE);
    }

    /**
     * Gets all the {@link WorkflowTemplate} of workflow.
     *
     * @return The collection of {@link WorkflowTemplate}.
     */
    public Collection<WorkflowTemplate> getAllTemplates() {
        final Table table = getTable();
        final SelectQuery selectQuery = getTemplateSelectQuery();
        final DataContainer templateContainer = readableDataStore().get(selectQuery);

        return templateContainer.getRows(table).map(row -> convertRowToModel(row, templateContainer)).toList();
    }

    /**
     * Converts the row into template.
     *
     * @param row           Refers the template row.
     * @param dataContainer The container that holds the template related information.
     * @return The {@link WorkflowTemplate}.
     */
    private WorkflowTemplate convertRowToModel(final Row row, final DataContainer dataContainer) {
        final Long templateId = row.getPKValue();

        return new WorkflowTemplate(templateId, row.get(MWFLTEMPLATE.NAME), row.get(MWFLTEMPLATE.DESCRIPTION),
                TriggerSource.get(row.get(MWFLTEMPLATE.TRIGGER_SOURCE)), TriggerEvent.get(row.get(MWFLTEMPLATE.TRIGGER_EVENT)),
                TemplateCategory.get(row.get(MWFLTEMPLATE.CATEGORY)), getCriteriaSets(templateId, dataContainer), getTemplateActions(templateId, dataContainer));
    }

    /**
     * Gets the collection of {@link WorkflowCriteriaSet} of the template.
     *
     * @param templateId    Refers the template id.
     * @param dataContainer The container that holds the template related information.
     * @return The collection of {@link WorkflowCriteriaSet}.
     */
    private List<WorkflowCriteriaSet> getCriteriaSets(final Long templateId, final DataContainer dataContainer) {
        final Table criteriaSetTable = getTemplateCriteriaSetTable();
        final Stream<Row> criteriaSetRows = dataContainer.getRows(criteriaSetTable,
                new WhereClause(Criteria.eq(criteriaSetTable.getColumn(MWFLTEMPLATECRITERIASET.TEMPLATE_ID), templateId)));

        return criteriaSetRows.map(criteriaSet -> convertRowToCriteriaSet(criteriaSet, dataContainer)).toList();
    }

    /**
     * Gets the collection of {@link WorkflowAction} of the template.
     *
     * @param templateId    Refers the template id.
     * @param dataContainer Refers the container that holds the template related information.
     * @return The collection of {@link WorkflowAction}.
     */
    private Collection<WorkflowAction> getTemplateActions(final Long templateId, final DataContainer dataContainer) {
        final Table actionTable = getTemplateActionTable();
        final WorkflowAction workflowAction = new WorkflowAction();
        final Row actionRow = dataContainer.getRow(actionTable,
                new WhereClause(Criteria.eq(actionTable.getColumn(MWFLTEMPLATEACTIONS.TEMPLATE_ID), templateId)));

        workflowAction.setId(actionRow.getPKValue());
        workflowAction.setTriggerActionSource(triggerActionSourceService.get(actionRow.get(MWFLTEMPLATEACTIONS.ACTION_SOURCE)));
        workflowAction.setTriggerActionEvent(triggerActionEventService.get(actionRow.get(MWFLTEMPLATEACTIONS.ACTION_EVENT)));
        workflowAction.setFieldMapping(actionRow.get(MWFLTEMPLATEACTIONS.FIELD_MAPPING));

        return List.of(workflowAction);
    }

    /**
     * Converts the row into {@link WorkflowCriteriaSet}.
     *
     * @param criteriaSetRow Refers the row of the criteria set.
     * @param dataContainer  The container that holds the template related information.
     * @return The {@link WorkflowCriteria} details.
     */
    private WorkflowCriteriaSet convertRowToCriteriaSet(final Row criteriaSetRow, final DataContainer dataContainer) {
        final Table criteriaTable = getTemplateCriteriaTable();
        final Table criteriaValueTable = getTemplateCriteriaValueTable();
        final Table sourceFieldCriteriaTable = getTemplateSourceFieldCriteriaTable();
        final Table fieldCriteriaTable = getTemplateFieldCriteriaTable();
        final Stream<Row> criteriaRows = dataContainer.getRows(criteriaTable,
                new WhereClause(Criteria.eq(criteriaTable.getColumn(MWFLTEMPLATECRITERIA.SET_ID), criteriaSetRow.getPKValue())));
        final Short connectorId = criteriaSetRow.get(MWFLTEMPLATECRITERIASET.CONNECTOR_ID);
        final WorkflowCriteriaSet workflowCriteriaSet = new WorkflowCriteriaSet();

        if (Objects.nonNull(connectorId)) {
            workflowCriteriaSet.setConnector(CriteriaConnector.get(connectorId));
        }
        workflowCriteriaSet.setOrder(criteriaSetRow.get(MWFLTEMPLATECRITERIASET.ORDER));
        workflowCriteriaSet.setCriteriaList(criteriaRows.map(criteriaRow -> {
            final WorkflowCriteria workflowCriteria = new WorkflowCriteria();
            final WorkflowCriteriaField workflowCriteriaField = new WorkflowCriteriaField();
            final Long criteriaId = criteriaRow.getPKValue();
            final Short criteriaConnectorId = criteriaRow.get(MWFLTEMPLATECRITERIA.CONNECTOR_ID);
            final Stream<Row> criteriaValues = dataContainer.getRows(criteriaValueTable,
                    new WhereClause(Criteria.eq(criteriaValueTable.getColumn(MWFLTEMPLATECRITERIAVALUE.CRITERIA_ID), criteriaId)));
            final Row fieldCriteriaRow = dataContainer.getRow(sourceFieldCriteriaTable,
                    new WhereClause(Criteria.eq(sourceFieldCriteriaTable.getColumn(MWFLTEMPLATESOURCEFIELDCRITERIA.CRITERIA_ID), criteriaId)));

            if (Objects.isNull(fieldCriteriaRow)) {
                final Row workflowFieldCriteriaRow = dataContainer.getRow(fieldCriteriaTable,
                        new WhereClause(Criteria.eq(fieldCriteriaTable.getColumn(MWFLTEMPLATEFIELDCRITERIA.CRITERIA_ID), criteriaId)));

                if (Objects.nonNull(workflowFieldCriteriaRow)) {
                    workflowCriteriaField.setId(workflowFieldCriteriaRow.getPKValue());
                    workflowCriteriaField.setFieldId(workflowFieldCriteriaRow.get(MWFLTEMPLATEFIELDCRITERIA.FIELD_ID));
                    workflowCriteriaField.setFieldGroup(FieldGroup.WORKFLOW_RELATED_FIELD);

                    workflowCriteria.setCriteriaField(workflowCriteriaField);
                }
            } else {
                workflowCriteriaField.setId(fieldCriteriaRow.getPKValue());
                workflowCriteriaField.setFieldId(fieldCriteriaRow.get(MWFLTEMPLATESOURCEFIELDCRITERIA.FIELD_ID));
                workflowCriteriaField.setFieldGroup(FieldGroup.SOURCE_RELATED_FIELD);

                workflowCriteria.setCriteriaField(workflowCriteriaField);
            }

            if (Objects.nonNull(criteriaConnectorId)) {
                workflowCriteria.setConnector(CriteriaConnector.get(criteriaConnectorId));
            }
            workflowCriteria.setOrder(criteriaRow.get(MWFLTEMPLATECRITERIA.ORDER));
            workflowCriteria.setId(criteriaId);
            workflowCriteria.setComparator(comparatorService.get(criteriaRow.get(MWFLTEMPLATECRITERIA.COMPARATOR_ID)));
            workflowCriteria.setCriteriaValues(criteriaValues.map(valueRow -> {
                final WorkflowCriteriaValue workflowCriteriaValue = new WorkflowCriteriaValue();

                workflowCriteriaValue.setId(valueRow.getPKValue());
                workflowCriteriaValue.setValue(valueRow.get(MWFLTEMPLATECRITERIAVALUE.VALUE));
                workflowCriteriaValue.setValueId(valueRow.get(MWFLTEMPLATECRITERIAVALUE.VALUE_ID));

                return workflowCriteriaValue;
            }).toList());

            return workflowCriteria;
        }).toList());

        return workflowCriteriaSet;
    }

    /**
     * Gets the {@link WorkflowTemplate} of workflow by its id provided.
     *
     * @param id Refers the template id.
     * @return The {@link WorkflowTemplate}.
     */
    public WorkflowTemplate getTemplate(final Long id) {
        final Table table = getTable();
        final SelectQuery selectQuery = getTemplateSelectQuery();

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(table.getPKColumn(), id)));
        final DataContainer templateContainer = readableDataStore().get(selectQuery);
        final Row row = templateContainer.getRow(table);

        return Objects.isNull(row) ? null : convertRowToModel(row, templateContainer);
    }

    /**
     * Gets all the {@link WorkflowTemplate} of workflow for the given category id.
     *
     * @param categoryId Refers the category id of the template.
     * @return The collection of {@link WorkflowTemplate}.
     */
    public Collection<WorkflowTemplate> getTemplateByCategory(final Short categoryId) {
        final Table table = getTable();
        final SelectQuery selectQuery = getTemplateSelectQuery();

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(MWFLTEMPLATE.CATEGORY), categoryId)));
        final DataContainer templateContainer = readableDataStore().get(selectQuery);

        return templateContainer.getRows(table).map(row -> convertRowToModel(row, templateContainer)).toList();
    }

    /**
     * Gets the selectQuery of the template.
     *
     * @return The template {@link SelectQuery}.
     */
    private SelectQuery getTemplateSelectQuery() {
        final Table table = getTable();
        final Table criteriaTable = getTemplateCriteriaTable();
        final Table criteriaSetTable = getTemplateCriteriaSetTable();
        final Table criteriaValueTable = getTemplateCriteriaValueTable();
        final Table sourceFieldCriteriaTable = getTemplateSourceFieldCriteriaTable();
        final Table fieldCriteriaTable = getTemplateFieldCriteriaTable();
        final Table actionTable = getTemplateActionTable();
        final Column templatePkColumn = table.getPKColumn();
        final Column criteriaPkColumn = criteriaTable.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(table);
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;

        selectQuery.addSelectClause(table, criteriaSetTable, criteriaTable, criteriaValueTable, sourceFieldCriteriaTable, fieldCriteriaTable, actionTable);
        selectQuery.addJoinClause(List.of(new JoinClause(templatePkColumn, criteriaSetTable.getColumn(MWFLTEMPLATECRITERIASET.TEMPLATE_ID)),
                new JoinClause(templatePkColumn, criteriaTable.getColumn(MWFLTEMPLATECRITERIA.TEMPLATE_ID)),
                new JoinClause(criteriaPkColumn, criteriaValueTable.getColumn(MWFLTEMPLATECRITERIAVALUE.CRITERIA_ID), leftJoin),
                new JoinClause(criteriaPkColumn, sourceFieldCriteriaTable.getColumn(MWFLTEMPLATESOURCEFIELDCRITERIA.CRITERIA_ID), leftJoin),
                new JoinClause(criteriaPkColumn, fieldCriteriaTable.getColumn(MWFLTEMPLATEFIELDCRITERIA.CRITERIA_ID), leftJoin),
                new JoinClause(templatePkColumn, actionTable.getColumn(MWFLTEMPLATEACTIONS.TEMPLATE_ID))));
        selectQuery.addOrderByClause(new OrderByClause(templatePkColumn));

        return selectQuery;
    }
}
