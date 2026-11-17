package com.tlc.crm.workflow.api.manager;

import com.tlc.crm.fields.manager.AbstractReadableConfigManager;

import com.tlc.crm.workflow.api.model.FieldGroup;
import com.tlc.crm.workflow.api.model.StepOption;
import com.tlc.crm.workflow.sql.resource.MWFLSTEPOPTIONS;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * Manages the {@link StepOption} related DB actions
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public final class StepOptionManager extends AbstractReadableConfigManager<StepOption> {

    private StepOptionManager() {
    }

    private static class Instance {
        private static final StepOptionManager INSTANCE = new StepOptionManager();
    }

    /**
     * <p>
     * Gets the {@link StepOptionManager} Instance
     * </p>
     *
     * @return {@link StepOptionManager}
     */
    public static StepOptionManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @return the step option table
     */
    @Override
    public Table getTable() {
        return Table.get(MWFLSTEPOPTIONS.TABLE);
    }

    /**
     * <p>
     * Gets the {@link StepOption} for the given id
     * </p>
     *
     * @param stepOptionId Represents the {@link StepOption} id
     * @return {@link StepOption}
     */
    public StepOption getStepOption(final Long stepOptionId) {
        final Table stepOptionTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(stepOptionTable);

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(stepOptionTable.getPKColumn(), stepOptionId)));
        final DataContainer stepOptionContainer = readableDataStore().get(selectQuery);

        return convertRowToModel(stepOptionContainer.getRow(stepOptionTable));
    }

    /**
     * <p>
     * Gets the {@link Collection} of {@link StepOption} for the given ids
     * </p>
     *
     * @param stepOptionIds Represents the {@link Collection} of step option id
     * @return {@link Collection} of {@link StepOption}
     */
    public Collection<StepOption> getStepOptions(final Collection<Long> stepOptionIds) {
        final Table stepOptionTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(stepOptionTable);

        selectQuery.setWhereClause(new WhereClause(Criteria.in(stepOptionTable.getPKColumn(), stepOptionIds)));
        final DataContainer stepOptionContainer = readableDataStore().get(selectQuery);

        return stepOptionContainer.getRows(stepOptionTable).map(this::convertRowToModel).toList();
    }

    /**
     * {@inheritDoc}
     *
     * @param row Represents the row of the step option table
     * @return {@link StepOption}
     */
    @Override
    public StepOption convertRowToModel(final Row row) {
        final Long fieldId = row.get(MWFLSTEPOPTIONS.FIELD_ID);
        final boolean isSourceField = Objects.nonNull(fieldId);

        return new StepOption(row.getPKValue(), row.get(MWFLSTEPOPTIONS.NAME),
                isSourceField ? fieldId : row.get(MWFLSTEPOPTIONS.WORKFLOW_FIELD_ID),
                row.get(MWFLSTEPOPTIONS.ALIAS_NAME), isSourceField ? FieldGroup.SOURCE_RELATED_FIELD : FieldGroup.WORKFLOW_RELATED_FIELD);
    }

    /**
     * <p>
     * Gets all the {@link Collection} of {@link StepOption}
     * </p>
     *
     * @return {@link Collection} of all step option fields
     */
    public Collection<StepOption> getAllStepOptions() {
        final Table stepOptionTable = getTable();

        return readableDataStore().get(SelectQuery.get(stepOptionTable)).getRows(stepOptionTable)
                .map(this::convertRowToModel).toList();
    }

    /**
     * <p>
     * Gets the step options which is mapped for given {@link Collection} of alias name
     * </p>
     *
     * @param aliasNames Represents the {@link Collection} of alias name
     * @return {@link Map} of {@link StepOption}
     */
    public Map<String, StepOption> getStepOptionsByAliasName(final Collection<String> aliasNames) {
        final Table stepOptionTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(stepOptionTable);

        selectQuery.setWhereClause(new WhereClause(Criteria.in(stepOptionTable.getColumn(MWFLSTEPOPTIONS.ALIAS_NAME), aliasNames)));

        return readableDataStore().get(selectQuery).getRows(stepOptionTable).map(this::convertRowToModel)
                .collect(Collectors.toMap(StepOption::aliasName, Function.identity()));
    }
}
