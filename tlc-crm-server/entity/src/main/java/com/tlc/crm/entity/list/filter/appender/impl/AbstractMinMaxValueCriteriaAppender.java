package com.tlc.crm.entity.list.filter.appender.impl;

import com.tlc.crm.entity.filter.fields.FilterOptions;
import com.tlc.crm.entity.list.filter.FilterAction;
import com.tlc.crm.entity.list.filter.appender.CriteriaAppender;
import com.tlc.crm.entity.list.filter.appender.model.MinMaxValueCriteriaAppenderRequisites;
import com.tlc.crm.entity.list.filter.handler.FilterCriteriaHandler;
import com.tlc.crm.entity.list.filter.internal.FilterActionContainer;
import com.tlc.crm.entity.list.filter.loader.JoinsLoader;
import com.tlc.crm.entity.list.filter.models.FilterCriteriaDetails;
import com.tlc.crm.entity.list.filter.source.FilterSourceType;

import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Connector;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.DerivedTable;
import com.tlc.sql.common.api.dml.GroupByClause;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.util.WhereClauseUtil;

import java.util.Collections;
import java.util.Objects;

/**
 * <p>
 * Implements of the {@link CriteriaAppender} interface to provides functionality to appends the min/max value criteria
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public abstract class AbstractMinMaxValueCriteriaAppender implements CriteriaAppender<MinMaxValueCriteriaAppenderRequisites> {

    private static final String ORG_ID = "ORG_ID";

    private final FilterAction providerAction;
    private final FilterSourceType sourceType;
    private final FilterOptions filterOption;
    private final JoinClause.JoinType joinType;
    private final FilterActionContainer filterActionContainer;
    private final FilterCriteriaHandler filterCriteriaHandler;
    private final Event event;

    private MinMaxValueCriteriaAppenderRequisites criteriaRequisites;

    protected AbstractMinMaxValueCriteriaAppender(final FilterAction providerAction, final FilterSourceType sourceType,
                                                  final FilterOptions filterOption, final JoinClause.JoinType joinType,
                                                  final Event event) {
        this.providerAction = providerAction;
        this.sourceType = sourceType;
        this.filterOption = filterOption;
        this.joinType = joinType;
        this.event = event;
        this.filterActionContainer = FilterActionContainer.getInstance();
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
     * <p>
     * Sets the {@link MinMaxValueCriteriaAppenderRequisites} to build criteria.
     * </p>
     *
     * @param requisites Represents the {@link MinMaxValueCriteriaAppenderRequisites}.
     */
    @Override
    public void setCriteriaRequisites(final MinMaxValueCriteriaAppenderRequisites requisites) {
        this.criteriaRequisites = requisites;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WhereClause append(final WhereClause existingCriteria) {
        final Table baseTable = providerAction.getTable();
        final Column baseTablePkColumn = baseTable.getPKColumn();
        final Column baseTableOrgIdColumn = baseTable.getColumn(ORG_ID);
        final Column column = criteriaRequisites.getColumn();
        final SelectQuery innerQuery = SelectQuery.get(baseTable);
        final DerivedTable derivedBaseTable = DerivedTable.get(baseTable.getName(), baseTable.getAlias(), innerQuery);
        final SelectQuery selectQuery = SelectQuery.get(derivedBaseTable);
        final JoinsLoader joinsLoader = filterCriteriaHandler.getJoinsLoader(innerQuery, joinType, null, null, Collections.emptySet());
        final FilterCriteriaDetails filterCriteriaDetails = new FilterCriteriaDetails(joinsLoader, criteriaRequisites.getJoinPriorityDetails());

        filterCriteriaDetails.setSourceHolder(filterCriteriaHandler.constructSourceHolder(sourceType, criteriaRequisites.getRelatedSourceType()));
        filterCriteriaDetails.setFilterOptions(filterOption);
        filterCriteriaDetails.setFieldInfo(criteriaRequisites.getFieldInfo());
        filterCriteriaDetails.setUseAliasTable(criteriaRequisites.useAliasTable());
        innerQuery.addSelectClause(Event.MIN == event ? column.min() : column.max());
        innerQuery.addSelectClause(baseTableOrgIdColumn);
        innerQuery.addSelectClause(baseTablePkColumn);
        providerAction.loadJoins(filterActionContainer.getSource(providerAction), filterCriteriaDetails);
        innerQuery.setWhereClause(Objects.requireNonNullElse(criteriaRequisites.getCriteria(), existingCriteria));
        innerQuery.addGroupByClause(new GroupByClause(baseTablePkColumn));
        innerQuery.addGroupByClause(new GroupByClause(baseTableOrgIdColumn));
        selectQuery.addSelectClause(derivedBaseTable.getPKColumn());
        selectQuery.addJoinClause(innerQuery.getJoinClause());
        selectQuery.setWhereClause(WhereClauseUtil.connect(new WhereClause(Criteria.eq(column,
                derivedBaseTable.getColumn(column.getColumnName()))), existingCriteria, Connector.AND));
        final WhereClause criteria = new WhereClause(Criteria.in(baseTablePkColumn, selectQuery));

        return WhereClauseUtil.connect(criteria, existingCriteria, Connector.AND);
    }

    /**
     * <p>
     * Represents the events which refer the min/max.
     * </p>
     */
    protected enum Event {
        MIN, MAX
    }
}