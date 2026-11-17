package com.tlc.crm.entity.list.filter.appender.impl;

import com.tlc.crm.entity.filter.api.models.Comparator;
import com.tlc.crm.entity.filter.fields.FilterOptions;
import com.tlc.crm.entity.list.filter.FilterAction;
import com.tlc.crm.entity.list.filter.appender.CriteriaAppender;
import com.tlc.crm.entity.list.filter.appender.model.DynamicFieldEmptyValueCriteriaAppenderRequisites;
import com.tlc.crm.entity.list.filter.handler.FilterCriteriaHandler;
import com.tlc.crm.entity.list.filter.loader.impl.DefaultJoinLoader;
import com.tlc.crm.entity.list.filter.models.FilterCriteriaDetails;
import com.tlc.crm.entity.list.filter.models.JoinPreference;
import com.tlc.crm.entity.list.filter.models.JoinPriorityDetails;
import com.tlc.crm.entity.list.filter.source.FilterSourceType;

import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Connector;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.Operator;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.util.WhereClauseUtil;

import java.util.Objects;

/**
 * <p>
 * Appends the empty value criteria of the dynamic fields to retrieve empty fields.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class DynamicFieldEmptyValueCriteriaAppender implements CriteriaAppender<DynamicFieldEmptyValueCriteriaAppenderRequisites> {

    public static final Type TYPE = () -> "EMPTY_VALUE_CRITERIA";

    private final FilterAction filterAction;
    private final Comparator comparator;
    private final FilterSourceType sourceType;
    private final FilterOptions filterOption;
    private final FilterCriteriaHandler filterCriteriaHandler;

    private DynamicFieldEmptyValueCriteriaAppenderRequisites criteriaRequisites;

    public DynamicFieldEmptyValueCriteriaAppender(final FilterAction filterAction, final FilterSourceType sourceType,
                                                  final FilterOptions filterOption, final Comparator comparator) {
        this.filterAction = filterAction;
        this.comparator = comparator;
        this.sourceType = sourceType;
        this.filterOption = filterOption;
        this.filterCriteriaHandler = FilterCriteriaHandler.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasColumn() {
        return Objects.nonNull(criteriaRequisites) && Objects.nonNull(criteriaRequisites.getColumn());
    }

    /**
     * {@inheritDoc}
     *
     * @param requisites Represents the {@link DynamicFieldEmptyValueCriteriaAppenderRequisites}.
     */
    @Override
    public void setCriteriaRequisites(final DynamicFieldEmptyValueCriteriaAppenderRequisites requisites) {
        this.criteriaRequisites = requisites;
    }

    /**
     * {@inheritDoc}
     *
     * @return Appender {@link Type}.
     */
    @Override
    public Type getType() {
        return TYPE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WhereClause append(final WhereClause existingCriteria) {
        final Operator operator = Operator.getOperator(comparator.getAliasName());

        if (!comparator.isValueNeeded()) {
            return switch (operator) {
                case EQUAL -> {
                    final Table baseTable = filterAction.getTable();
                    final Column baseTablePKColumn = baseTable.getPKColumn();
                    final SelectQuery positiveCriteriaQuery = buildSelectQuery(criteriaRequisites.getPositiveCriteria());
                    final SelectQuery negativeCriteriaQuery = buildSelectQuery(criteriaRequisites.getNegativeCriteria());
                    final WhereClause emptyValueCriteria = new WhereClause(Criteria.in(baseTablePKColumn, positiveCriteriaQuery))
                            .and(Criteria.notIn(baseTablePKColumn, negativeCriteriaQuery));

                    yield WhereClauseUtil.connect(existingCriteria, emptyValueCriteria, Connector.OR);
                }
                case NOT_EQUAL -> {
                    if (Objects.isNull(existingCriteria)) {
                        yield null;
                    }
                    final Table baseTable = filterAction.getTable();
                    final SelectQuery selectQuery = buildSelectQuery(existingCriteria);

                    yield new WhereClause(Criteria.in(baseTable.getPKColumn(), selectQuery));
                }
                default -> existingCriteria;
            };
        }
        return existingCriteria;
    }

    /**
     * <p>
     * Builds the {@link SelectQuery} for the positive/negative criteria.
     * </p>
     *
     * @param criteria Positive/Negative criteria.
     * @return {@link SelectQuery}.
     */
    private SelectQuery buildSelectQuery(final WhereClause criteria) {
        final Table baseTable = filterAction.getTable();
        final Column baseTablePKColumn = baseTable.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(baseTable);
        final DefaultJoinLoader joinLoader = new DefaultJoinLoader(selectQuery, JoinClause.JoinType.INNER);
        final FilterCriteriaDetails filterCriteriaDetails = new FilterCriteriaDetails(joinLoader, new JoinPriorityDetails(JoinPreference.ANY));

        filterCriteriaDetails.setSourceHolder(filterCriteriaHandler.constructSourceHolder(sourceType, criteriaRequisites.getRelatedSourceType()));
        filterCriteriaDetails.setFilterOptions(filterOption);
        filterCriteriaDetails.setFieldInfo(criteriaRequisites.getFieldInfo());
        filterAction.appendJoins(filterCriteriaDetails);
        selectQuery.addSelectClause(baseTablePKColumn);
        selectQuery.setWhereClause(criteria);

        return selectQuery;
    }
}