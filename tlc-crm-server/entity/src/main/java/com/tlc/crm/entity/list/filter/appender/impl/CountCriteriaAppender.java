package com.tlc.crm.entity.list.filter.appender.impl;

import com.tlc.crm.entity.filter.api.models.Comparator;
import com.tlc.crm.entity.filter.api.models.FilterCriteria;
import com.tlc.crm.entity.filter.fields.FilterOptions;
import com.tlc.crm.entity.list.filter.FilterAction;
import com.tlc.crm.entity.list.filter.appender.CriteriaAppender;
import com.tlc.crm.entity.list.filter.appender.model.CountCriteriaAppenderRequisites;
import com.tlc.crm.entity.list.filter.handler.FilterCriteriaHandler;
import com.tlc.crm.entity.list.filter.internal.FilterActionContainer;
import com.tlc.crm.entity.list.filter.loader.JoinsLoader;
import com.tlc.crm.entity.list.filter.models.FilterCriteriaDetails;
import com.tlc.crm.entity.list.filter.models.JoinPriorityDetails;
import com.tlc.crm.entity.list.filter.source.FilterSourceType;
import com.tlc.crm.entity.list.filter.source.SourceTableMappings;
import com.tlc.crm.entity.list.model.Provider;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Connector;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.GroupByClause;
import com.tlc.sql.common.api.dml.HavingClause;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.Operator;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.util.WhereClauseUtil;
import com.tlc.sql.common.util.TableUtils;
import com.tlc.sql.oltp.resolver.OltpTableResolver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * Implements of the {@link CriteriaAppender} interface to provides functionality for handling count criteria to retrieve count related fields
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class CountCriteriaAppender implements CriteriaAppender<CountCriteriaAppenderRequisites> {

    public static final Type TYPE = () -> "COUNTABLE_FIELD_CRITERIA";
    private static final Long ZERO = 0L;

    private final FilterCriteria filterCriteria;
    private final FilterAction providerAction;
    private final FilterSourceType selectedFilterSourceType;
    private final FilterOptions filterOption;
    private final JoinClause.JoinType joinType;
    private final FilterActionContainer filterActionContainer;
    private final FilterCriteriaHandler filterCriteriaHandler;
    private final Provider provider;
    private final boolean hasNoValue;

    private CountCriteriaAppenderRequisites criteriaRequisites;

    public CountCriteriaAppender(final boolean hasNoValue, final FilterCriteria filterCriteria, final FilterAction providerAction,
                                 final FilterSourceType selectedFilterSourceType, final FilterOptions filterOption,
                                 final JoinClause.JoinType joinType, final Provider provider) {
        this.hasNoValue = hasNoValue;
        this.filterCriteria = filterCriteria;
        this.providerAction = providerAction;
        this.selectedFilterSourceType = selectedFilterSourceType;
        this.filterOption = filterOption;
        this.joinType = joinType;
        this.filterActionContainer = FilterActionContainer.getInstance();
        this.filterCriteriaHandler = FilterCriteriaHandler.getInstance();
        this.provider = provider;
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
     * @param requisites Represents the {@link CountCriteriaAppenderRequisites}.
     */
    @Override
    public void setCriteriaRequisites(final CountCriteriaAppenderRequisites requisites) {
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
    public WhereClause append(final WhereClause existing) {
        final Comparator comparator = filterCriteria.getComparator();
        final Operator operator = Operator.getOperator(comparator.getAliasName());
        WhereClause whereClause = null;

        for (final JoinPriorityDetails joinPriorityDetails : criteriaRequisites.getPriorityDetailHolders()) {
            final FilterSourceType providerSource = filterActionContainer.getSource(providerAction);
            final Table mainTable = providerAction.getTable();
            final SelectQuery selectQuery = SelectQuery.get(mainTable);
            final Column pkColumn = mainTable.getPKColumn();
            final long count = criteriaRequisites.getCount();
            final Column column = criteriaRequisites.getColumn();
            final FilterSourceType relatedSourceType = criteriaRequisites.getRelatedSourceType();
            final boolean isMapperSourcesNotUnique = Objects.equals(providerSource, selectedFilterSourceType) || Objects.equals(selectedFilterSourceType, relatedSourceType);
            final FilterAction selectedSourceAction = filterActionContainer.fetchSourceAction(selectedFilterSourceType);
            final Table selectedSourceTable = selectedSourceAction.getTable();
            Optional<SelectQuery> optionalNestedSelectQuery = Optional.empty();
            final boolean isEmptyCount = Objects.equals(Operator.EQUAL, operator) && count == 0;

            loadMapperTable(providerSource, selectedFilterSourceType, joinPriorityDetails);

            if (isMapperSourcesNotUnique) {
                loadMapperTable(selectedFilterSourceType, relatedSourceType, joinPriorityDetails);

                selectQuery.addGroupByClause(new GroupByClause(pkColumn));

                if (!isEmptyCount) {
                    selectQuery.addHavingClause(new HavingClause(new Criteria(column.count(), count, operator)));
                }
            } else {
                final Column selectedSourcePkColumn = selectedSourceTable.getPKColumn();
                final JoinPriorityDetails selectedSourceJoinPriorityDetails = new JoinPriorityDetails(joinPriorityDetails.getJoinPriorityType());
                final SelectQuery nestedSelectQuery = SelectQuery.get(selectedSourceTable);

                loadMapperTable(selectedFilterSourceType, relatedSourceType, selectedSourceJoinPriorityDetails);
                loadJoins(selectedSourceAction, relatedSourceType, null, nestedSelectQuery, selectedSourceJoinPriorityDetails, joinType);

                nestedSelectQuery.addSelectClause(selectedSourcePkColumn);
                nestedSelectQuery.setWhereClause(WhereClauseUtil.connect(existing, nestedSelectQuery.getWhereClause(), Connector.AND));
                nestedSelectQuery.addGroupByClause(new GroupByClause(selectedSourcePkColumn));
                nestedSelectQuery.addHavingClause(new HavingClause(new Criteria(column.count(), count, operator)));

                optionalNestedSelectQuery = Optional.of(nestedSelectQuery);
            }

            if (!Objects.equals(providerSource, selectedFilterSourceType) || !Objects.equals(providerSource, relatedSourceType)) {
                loadJoins(providerAction, selectedFilterSourceType, isMapperSourcesNotUnique ? relatedSourceType : null, selectQuery, joinPriorityDetails,
                        isMapperSourcesNotUnique ? joinType : JoinClause.JoinType.INNER);
            }
            final List<JoinClause> joinClauses = new ArrayList<>(selectQuery.getJoinClause());
            final WhereClause fieldIdCriteria = providerAction.appendFieldIdCriteria(true, provider,
                    selectedFilterSourceType, hasNoValue, operator, whereClause);
            final WhereClause innerCriteria = WhereClauseUtil.connect(existing, fieldIdCriteria, Connector.AND);

            optionalNestedSelectQuery.ifPresent(optionalSelectQuery -> joinClauses.addAll(optionalSelectQuery.getJoinClause()));

            if (Objects.nonNull(innerCriteria) || optionalNestedSelectQuery.isPresent()) {
                selectQuery.setWhereClause(isMapperSourcesNotUnique ? innerCriteria :
                        WhereClauseUtil.connect(fieldIdCriteria,
                                new WhereClause(getCriteria(selectedSourceTable.getPKColumn(), optionalNestedSelectQuery.get(), isEmptyCount)), Connector.AND));
            }
            selectQuery.addSelectClause(pkColumn);
            final Criteria criteria = getCriteria(pkColumn, selectQuery, isEmptyCount);
            final WhereClause countCriteria = new WhereClause(criteria);
            final WhereClause countFilter = buildOperatorBasedCriteria(pkColumn, operator, existing, selectQuery, joinClauses,
                    fieldIdCriteria).map(countCriteria::or).orElseGet(() -> countCriteria);
            whereClause = WhereClauseUtil.connect(whereClause, countFilter, Connector.AND);
        }

        if (operator.isInvertedOperator() == comparator.isValueNeeded() || compareCountWithZero(operator)) {
            final Table selectedSourceTable = filterActionContainer.fetchSourceAction(selectedFilterSourceType).getTable();
            final WhereClause selectedSourceClause = new WhereClause(Criteria.notEq(selectedSourceTable.getPKColumn(), null));
            whereClause = WhereClauseUtil.connect(whereClause, selectedSourceClause, Connector.AND);
        }

        return whereClause;
    }

    /**
     * <p>
     * Gets the criteria based on the operator
     * </p>
     *
     * @param column      Represents the {@link Column}
     * @param selectQuery Represents the {@link SelectQuery}
     * @return The {@link Criteria}
     */
    private Criteria getCriteria(final Column column, final SelectQuery selectQuery, final boolean isEmptyCount) {

        if (isEmptyCount) {
            return Criteria.notIn(column, selectQuery);
        }
        return Criteria.in(column, selectQuery);
    }

    /**
     * <p>
     * Loads a mapper table based on the provided source types if they are different.
     * If a mapping is found, the mapper table is added to the join priority details.
     * </p>
     *
     * @param primarySource       The primary filter source type.
     * @param secondarySource     The secondary filter source type.
     * @param joinPriorityDetails The object managing join priority details.
     */
    private void loadMapperTable(final FilterSourceType primarySource, final FilterSourceType secondarySource, final JoinPriorityDetails joinPriorityDetails) {
        if (!Objects.equals(primarySource, secondarySource)) {
            final Optional<SourceTableMappings> sourceTableMappings = Optional.ofNullable(SourceTableMappings.get(primarySource, secondarySource));

            sourceTableMappings.ifPresent(sourceTableMapping -> joinPriorityDetails.addMapperTable(sourceTableMapping.getMapperTable()));
        }
    }

    /**
     * <p>
     * Loads join details based on the provided filter action, source types, and select query.
     * Creates filter criteria details and loads joins using the provided action.
     * </p>
     *
     * @param providerAction           The filter action for loading joins.
     * @param selectedFilterSourceType The selected filter source type.
     * @param relatedSourceType        The related filter source type.
     * @param selectQuery              The select query object used for constructing joins.
     * @param joinPriorityDetails      The object managing join priority details.
     */
    private void loadJoins(final FilterAction providerAction, final FilterSourceType selectedFilterSourceType, final FilterSourceType relatedSourceType,
                           final SelectQuery selectQuery, final JoinPriorityDetails joinPriorityDetails, final JoinClause.JoinType joinType) {
        final JoinsLoader joinsLoader = filterCriteriaHandler.getJoinsLoader(selectQuery, joinType, null, null, Collections.emptySet());
        final FilterCriteriaDetails filterCriteriaDetails = new FilterCriteriaDetails(joinsLoader, joinPriorityDetails);

        filterCriteriaDetails.setFilterOptions(filterOption);
        filterCriteriaDetails.setSourceHolder(filterCriteriaHandler.constructSourceHolder(selectedFilterSourceType, relatedSourceType));
        filterCriteriaDetails.setFieldInfo(criteriaRequisites.getFieldInfo());
        filterCriteriaDetails.setUseAliasTable(false);
        providerAction.loadJoins(filterActionContainer.getSource(providerAction), filterCriteriaDetails);
    }

    /**
     * <p>
     * Checks whether the value lies with the zero.
     * </p>
     *
     * @param operator Represents the {@link Operator}
     * @return A flag that indicates whether the operator is comparison type or not
     */
    private boolean compareCountWithZero(final Operator operator) {
        final long count = criteriaRequisites.getCount();

        return switch (operator) {
            case NOT_EQUAL -> ZERO != count;
            case EQUAL -> ZERO == count;
            case LESS_THAN -> ZERO < count;
            case LESS_EQUAL -> ZERO <= count;
            case GREATER_THAN -> ZERO > count;
            case GREATER_EQUAL -> ZERO >= count;
            default -> false;
        };
    }

    /**
     * <p>
     * Builds the {@link Criteria} based on operator
     * </p>
     *
     * @param pkColumn         Represents the column which is to be filtered
     * @param existingCriteria Represents the existing criteria
     * @param operator         Represents the {@link Operator} which is used to compare the column and value
     * @param selectQuery      Represents the selectQuery
     * @return Retrieval {@link WhereClause}
     */
    private Optional<WhereClause> buildOperatorBasedCriteria(final Column pkColumn, final Operator operator,
                                                             final WhereClause existingCriteria, final SelectQuery selectQuery,
                                                             final List<JoinClause> joinClauses, final WhereClause innerCriteria) {
        final long count = criteriaRequisites.getCount();
        final boolean isZero = Objects.equals(0L, count);

        if (isZero) {
            return switch (operator) {
                case GREATER_EQUAL, LESS_EQUAL ->
                        buildNestedCriteria(pkColumn, existingCriteria, selectQuery, joinClauses, innerCriteria);
                default -> Optional.empty();
            };
        } else if (operator.isRelationalOperator() && !operator.isEqualityOperator()) {
            final boolean isNegativeNumber = count < 0;

            return switch (operator) {
                case GREATER_THAN, GREATER_EQUAL -> {
                    if (isNegativeNumber) {
                        yield buildNullableCriteria(buildNestedCriteria(pkColumn, existingCriteria, selectQuery, joinClauses, innerCriteria));
                    }
                    yield Optional.empty();
                }
                case LESS_THAN, LESS_EQUAL -> {
                    if (isNegativeNumber) {
                        yield Optional.empty();
                    }
                    yield buildNullableCriteria(buildNestedCriteria(pkColumn, existingCriteria, selectQuery, joinClauses, innerCriteria));
                }
                default -> Optional.empty();
            };
        } else if (Operator.NOT_EQUAL == operator) {
            return buildNullableCriteria(buildNestedCriteria(pkColumn, existingCriteria, selectQuery, joinClauses, innerCriteria));
        }

        return Optional.empty();
    }

    /**
     * <p>
     * Builds the {@link Criteria} based on operator
     * </p>
     *
     * @param whereClause Refers the whereclause
     * @return Retrieval {@link WhereClause}
     */
    private Optional<WhereClause> buildNullableCriteria(final Optional<WhereClause> whereClause) {

        if (criteriaRequisites.isNullableCriteriaRequired()) {
            final Criteria nullableCriteria = Criteria.eq(criteriaRequisites.getColumn(), null);

            return whereClause.isPresent() ? whereClause.map(criteria -> criteria.or(nullableCriteria)) :
                    Optional.of(new WhereClause(nullableCriteria));
        } else {
            return whereClause;
        }
    }

    /**
     * <p>
     * Builds the {@link Criteria}
     * </p>
     *
     * @param pkColumn         Represents the column which is to be filtered
     * @param existingCriteria Represents the existing criteria
     * @param selectQuery      Represents the selectQuery
     * @return A new {@link WhereClause} containing the "NOT IN" criteria based on the `pkColumn`
     * and inner query, or null if `existingCriteria` is null.
     */
    private Optional<WhereClause> buildNestedCriteria(final Column pkColumn, final WhereClause existingCriteria,
                                                      final SelectQuery selectQuery, final List<JoinClause> joinClauses,
                                                      final WhereClause innerCriteria) {
        if (Objects.nonNull(existingCriteria)) {
            final SelectQuery innerQuery = SelectQuery.get(selectQuery.getBaseTable());

            innerQuery.addSelectClause(selectQuery.getSelectClause());
            innerQuery.addJoinClause(joinClauses);
            innerQuery.setWhereClause(WhereClauseUtil.connect(existingCriteria, innerCriteria, Connector.AND));
            return Optional.of(new WhereClause(Criteria.notIn(pkColumn, innerQuery)));
        }

        return Optional.empty();
    }
}