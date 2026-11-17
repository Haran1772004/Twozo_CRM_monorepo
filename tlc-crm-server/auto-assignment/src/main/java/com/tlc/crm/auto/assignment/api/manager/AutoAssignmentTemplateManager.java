package com.tlc.crm.auto.assignment.api.manager;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import com.tlc.crm.auto.assignment.api.model.AssignmentModule;
import com.tlc.crm.auto.assignment.api.model.AutoAssignmentCriteria;
import com.tlc.crm.auto.assignment.api.model.AutoAssignmentCriteriaSet;
import com.tlc.crm.auto.assignment.api.model.AutoAssignmentCriteriaValue;
import com.tlc.crm.auto.assignment.api.model.AutoAssignmentTemplate;
import com.tlc.crm.auto.assignment.api.model.CriteriaConnector;
import com.tlc.crm.auto.assignment.sql.resource.MASRTEMPLATE;
import com.tlc.crm.auto.assignment.sql.resource.MASRTEMPLATECRITERIA;
import com.tlc.crm.auto.assignment.sql.resource.MASRTEMPLATECRITERIASET;
import com.tlc.crm.auto.assignment.sql.resource.MASRTEMPLATECRITERIAVALUE;
import com.tlc.crm.auto.assignment.cache.service.AutoAssignmentComparatorService;

import com.tlc.crm.fields.manager.AbstractReadableConfigManager;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Manages the {@link AutoAssignmentTemplate} related DB actions.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class AutoAssignmentTemplateManager extends AbstractReadableConfigManager<AutoAssignmentTemplate> {

    private final AutoAssignmentComparatorService assignmentComparatorService;

    private AutoAssignmentTemplateManager() {
        this.assignmentComparatorService = AutoAssignmentComparatorService.getInstance();
    }

    private static class Instance {
        private static final AutoAssignmentTemplateManager TEMPLATE_MANAGER = new AutoAssignmentTemplateManager();
    }

    public static AutoAssignmentTemplateManager getInstance() {
        return Instance.TEMPLATE_MANAGER;
    }

    /**
     * Gets the {@link Table} of template.
     *
     * @return The template criteria table.
     */
    @Override
    public Table getTable() {
        return Table.get(MASRTEMPLATE.TABLE);
    }

    /**
     * Gets the {@link Table} of template criteria.
     *
     * @return The template criteria table.
     */
    private Table getTemplateCriteriaTable() {
        return Table.get(MASRTEMPLATECRITERIA.TABLE);
    }

    /**
     * Gets the {@link Table} of template criteria set.
     *
     * @return The template criteria set table.
     */
    private Table getTemplateCriteriaSetTable() {
        return Table.get(MASRTEMPLATECRITERIASET.TABLE);
    }

    /**
     * Gets the {@link Table} of template criteria value.
     *
     * @return The template criteria value table.
     */
    private Table getTemplateCriteriaValueTable() {
        return Table.get(MASRTEMPLATECRITERIAVALUE.TABLE);
    }

    /**
     * <p>
     * Converts the row data into model entity
     * </p>
     *
     * @param row Row that holds the information of model
     * @return Converted entity
     */
    @Override
    public AutoAssignmentTemplate convertRowToModel(final Row row) {
        return null;
    }

    /**
     * Gets all the {@link AutoAssignmentTemplate} of auto-assignment.
     *
     * @return The collection of {@link AutoAssignmentTemplate}.
     */
    public Collection<AutoAssignmentTemplate> getAllTemplates() {
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);
        final Table criteriaTable = getTemplateCriteriaTable();
        final Table criteriaSetTable = getTemplateCriteriaSetTable();
        final Table criteriaValueTable = getTemplateCriteriaValueTable();
        final Column templatePkColumn = table.getPKColumn();
        final Column criteriaPkColumn = criteriaTable.getPKColumn();

        selectQuery.addSelectClause(table, criteriaSetTable, criteriaTable, criteriaValueTable);
        selectQuery.addJoinClause(List.of(new JoinClause(templatePkColumn, criteriaSetTable.getColumn(MASRTEMPLATECRITERIASET.TEMPLATE_ID)),
                new JoinClause(templatePkColumn, criteriaTable.getColumn(MASRTEMPLATECRITERIA.TEMPLATE_ID)),
                new JoinClause(criteriaPkColumn, criteriaValueTable.getColumn(MASRTEMPLATECRITERIAVALUE.CRITERIA_ID))));
        selectQuery.addOrderByClause(new OrderByClause(templatePkColumn));

        final DataContainer templateContainer = readableDataStore().get(selectQuery);

        return templateContainer.getRows(table).map(row -> convertRowToModel(row, templateContainer)).toList();
    }

    /**
     * Converts the row into template.
     *
     * @param row           Refers the template row.
     * @param dataContainer The container that holds the template related information.
     * @return The {@link AutoAssignmentTemplate}.
     */
    private AutoAssignmentTemplate convertRowToModel(final Row row, final DataContainer dataContainer) {
        final Long templateId = row.getPKValue();
        final Table criteriaSetTable = getTemplateCriteriaSetTable();
        final Stream<Row> criteriaSetRows = dataContainer.getRows(criteriaSetTable, new WhereClause(Criteria.eq(criteriaSetTable.getColumn(MASRTEMPLATECRITERIASET.TEMPLATE_ID), templateId)));
        final Collection<AutoAssignmentCriteriaSet> assignmentCriteriaSets = criteriaSetRows.map(criteriaSet -> convertRowToCriteriaSet(criteriaSet, dataContainer)).toList();

        return new AutoAssignmentTemplate(templateId, row.get(MASRTEMPLATE.NAME), row.get(MASRTEMPLATE.DESCRIPTION),
                AssignmentModule.get((row.get(MASRTEMPLATE.MODULE))), assignmentCriteriaSets);
    }

    /**
     * Converts the row into {@link AutoAssignmentCriteriaSet}.
     *
     * @param criteriaSetRow Refers the row of the criteria set.
     * @param dataContainer  The container that holds the template related information.
     * @return The {@link AutoAssignmentCriteriaSet} details.
     */
    private AutoAssignmentCriteriaSet convertRowToCriteriaSet(final Row criteriaSetRow, final DataContainer dataContainer) {
        final Table criteriaTable = getTemplateCriteriaTable();
        final Table criteriaValueTable = getTemplateCriteriaValueTable();
        final Stream<Row> criteriaRows = dataContainer.getRows(criteriaTable,
                new WhereClause(Criteria.eq(criteriaTable.getColumn(MASRTEMPLATECRITERIA.SET_ID), criteriaSetRow.getPKValue())));
        final Short connectorId = criteriaSetRow.get(MASRTEMPLATECRITERIASET.CONNECTOR_ID);
        final AutoAssignmentCriteriaSet autoAssignmentCriteriaSet = new AutoAssignmentCriteriaSet();

        if (Objects.nonNull(connectorId)) {
            autoAssignmentCriteriaSet.setConnector(CriteriaConnector.get(connectorId));
        }
        autoAssignmentCriteriaSet.setOrder(criteriaSetRow.get(MASRTEMPLATECRITERIASET.ORDER));
        autoAssignmentCriteriaSet.setCriteriaList(criteriaRows.map(criteriaRow -> {
            final AutoAssignmentCriteria autoAssignmentCriteria = new AutoAssignmentCriteria();
            final Long criteriaId = criteriaRow.getPKValue();
            final Short criteriaConnectorId = criteriaRow.get(MASRTEMPLATECRITERIA.CONNECTOR_ID);
            final Stream<Row> criteriaValues = dataContainer.getRows(criteriaValueTable,
                    new WhereClause(Criteria.eq(criteriaValueTable.getColumn(MASRTEMPLATECRITERIAVALUE.CRITERIA_ID), criteriaId)));

            if (Objects.nonNull(criteriaConnectorId)) {
                autoAssignmentCriteria.setConnector(CriteriaConnector.get(criteriaConnectorId));
            }
            autoAssignmentCriteria.setFieldId(criteriaRow.get(MASRTEMPLATECRITERIA.FIELD_ID));
            autoAssignmentCriteria.setOrder(criteriaRow.get(MASRTEMPLATECRITERIA.ORDER));
            autoAssignmentCriteria.setId(criteriaId);
            autoAssignmentCriteria.setComparator(assignmentComparatorService.get(criteriaRow.get(MASRTEMPLATECRITERIA.COMPARATOR_ID)));
            autoAssignmentCriteria.setCriteriaValues(criteriaValues.map(valueRow -> {
                final AutoAssignmentCriteriaValue assignmentCriteriaValue = new AutoAssignmentCriteriaValue();

                assignmentCriteriaValue.setId(valueRow.getPKValue());
                assignmentCriteriaValue.setValue(valueRow.get(MASRTEMPLATECRITERIAVALUE.VALUE));
                return assignmentCriteriaValue;
            }).toList());

            return autoAssignmentCriteria;
        }).toList());

        return autoAssignmentCriteriaSet;
    }
}
