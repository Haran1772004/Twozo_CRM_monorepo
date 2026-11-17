package com.tlc.crm.entity.list.filter.builder;

import com.tlc.crm.entity.filter.api.models.Comparator;
import com.tlc.sql.common.util.TableUtils;
import com.tlc.crm.entity.filter.api.models.CriteriaValue;
import com.tlc.crm.entity.list.filter.appender.CriteriaAppender;
import com.tlc.crm.entity.sql.resource.OMETFIELDS;

import com.tlc.crm.common.util.NumberUtils;
import com.tlc.commons.util.TypeChecker;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Connector;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.Operator;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.util.WhereClauseUtil;

import com.tlc.sql.oltp.resolver.OltpTableResolver;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <p>
 * It helps to build the field related criteria.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class FieldCriteriaBuilder {

    private static final String ENTITY_ID = "ENTITY_ID";

    private static final class Instance {
        private static final FieldCriteriaBuilder INSTANCE = new FieldCriteriaBuilder();
    }

    private FieldCriteriaBuilder() {
    }

    public static FieldCriteriaBuilder getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Builds the {@link WhereClause} for the default & custom fields.
     * </p>
     *
     * @param fieldId        Represents the field id.
     * @param remoteTable    Represents the remote table of the field.
     * @param mapperTable    Represents the mapper table of the field table.
     * @param column         Represents the main {@link Column} of the criteria.
     * @param comparator     Represents the {@link Comparator} to compare the value of given column and given value.
     * @param criteriaValues Represents the values to build criteria.
     * @return {@link WhereClause}.
     */
    public final WhereClause build(final Long fieldId, final Table remoteTable, final Table mapperTable,
                                   final Column column, final Comparator comparator, final Collection<CriteriaValue> criteriaValues) {
        final Column fieldIdColumn = TableUtils.groupFkColumnsByTable(mapperTable, OltpTableResolver.get()).get(getFieldTable());
        final WhereClause fieldCriteria = new WhereClause(Criteria.eq(fieldIdColumn, fieldId));
        final Operator operator = Operator.getOperator(comparator.getAliasName());

        if (Objects.isNull(criteriaValues)) {
            return Operator.EQUAL == operator
                    ? buildInvertedOperatorCriteria(remoteTable, mapperTable, fieldCriteria).or(Criteria.eq(fieldIdColumn, null))
                    : new WhereClause(new Criteria(fieldIdColumn, fieldId, Operator.getInvertedOperator(operator)));
        } else if (operator.isInvertedOperator()) {
            final WhereClause filter = buildCriteria(column, comparator.isMultiValueAllowed(), Operator.getInvertedOperator(operator), criteriaValues);

            return buildInvertedOperatorCriteria(remoteTable, mapperTable, filter.and(fieldCriteria)).or(Criteria.eq(fieldIdColumn, null));
        } else {
            return fieldCriteria.and(buildCriteria(column, comparator.isMultiValueAllowed(), operator, criteriaValues));
        }
    }

    /**
     * <p>
     * Builds the {@link WhereClause} for the multi select fields.
     * </p>
     *
     * @param fieldId        Represents the field id.
     * @param mapperTable    Represents the mapper table of the field table.
     * @param column         Represents the main {@link Column} of the criteria.
     * @param operator       Represents the {@link Operator} to compare the value of given column and given value.
     * @param valueEntityIds Represents the values to build criteria.
     * @return Multi select field {@link WhereClause}.
     */
    public final WhereClause buildMultiSelectFieldCriteria(final Long fieldId, final Table mapperTable, final Column column,
                                                           final Operator operator, final Collection<Long> valueEntityIds) {
        final Column entityIdColumn = mapperTable.getColumn(ENTITY_ID);

        if (Objects.isNull(valueEntityIds) || valueEntityIds.isEmpty()) {
            final Column fieldIdColumn = TableUtils.groupFkColumnsByTable(mapperTable, OltpTableResolver.get()).get(getFieldTable());
            final WhereClause whereClause = new WhereClause(Criteria.eq(fieldIdColumn, fieldId));

            return Operator.EQUAL == operator ? buildInvertedOperatorCriteria(null, mapperTable, whereClause)
                    .or(Criteria.eq(entityIdColumn, null)) : new WhereClause(new Criteria(fieldIdColumn, fieldId, Operator.getInvertedOperator(operator)));
        } else {
            return buildMultiSelectFieldCriteria(mapperTable, entityIdColumn, column, valueEntityIds, operator);
        }
    }

    /**
     * <p>
     * Appends the fieldId criteria for the case where the selected source differs from the provider source.
     * </p>
     *
     * @param isCountableField Represent whether it is countable field or not
     * @param fieldId          Represents the id of the field.
     * @param hasNoValue       {@link CriteriaAppender} to appends the criteria for inverted operators.
     * @param mapperTable      Represents the mapper table of the field table.
     * @param operator         It is used to compare the given value with the given respective column.
     * @param whereClause      {@link WhereClause} holds the details about retrieval criteria.
     * @return {@link WhereClause}.
     */
    public final WhereClause buildFieldIdCriteria(final boolean isCountableField, Long fieldId, final boolean hasNoValue, final Table mapperTable,
                                                  final Operator operator, final WhereClause whereClause) {
        final Column fieldIdColumn = TableUtils.groupFkColumnsByTable(mapperTable, OltpTableResolver.get()).get(getFieldTable());
        final WhereClause fieldCriteria = new WhereClause(Criteria.eq(fieldIdColumn, fieldId));

        if (hasNoValue && !isCountableField) {
            if (Operator.EQUAL == operator) {
                final Criteria criteria = getPrimaryCriteria(whereClause);
                final WhereClause filter = new WhereClause(new Criteria(criteria.getColumn(), criteria.getValue(), Operator.getInvertedOperator(criteria.getOperator())));

                return fieldCriteria.and(buildInvertedOperatorCriteria(null, mapperTable, filter));
            } else {
                return new WhereClause(new Criteria(fieldIdColumn, fieldId, Operator.getInvertedOperator(operator))).and(whereClause);
            }
        } else if (operator.isInvertedOperator() && !isCountableField) {
            final Criteria criteria = getPrimaryCriteria(whereClause);
            final WhereClause filter = new WhereClause(new Criteria(criteria.getColumn(), criteria.getValue(),
                    Operator.getInvertedOperator(criteria.getOperator())));

            return fieldCriteria.and(buildInvertedOperatorCriteria(null, mapperTable, filter));
        } else {
            return WhereClauseUtil.connect(fieldCriteria, whereClause, Connector.AND);
        }
    }

    /**
     * <p>
     * Gets the primary criteria from the given {@link WhereClause}
     * </p>
     *
     * @param whereClause {@link WhereClause} holds the details about retrieval criteria.
     * @return The {@link Criteria}.
     */
    private Criteria getPrimaryCriteria(final WhereClause whereClause) {
        return whereClause.isConnector() ? getPrimaryCriteria(whereClause.getLeft()) : whereClause.getCriteria();
    }

    /**
     * <p>
     * Builds the {@link WhereClause} for the multi select fields.
     * </p>
     *
     * @param column         Represents the main {@link Column} of the criteria.
     * @param operator       Represents the {@link Operator} to compare the value of given column and given value.
     * @param valueEntityIds Represents the values to build criteria.
     * @return Multi select field {@link WhereClause}.
     */
    public final WhereClause buildMultiSelectFieldCriteria(final Table mapperTable, final Column entityIdColumn, final Column column,
                                                           final Collection<Long> valueEntityIds, final Operator operator) {
        WhereClause whereClause = null;

        for (final Long valueEntityId : valueEntityIds) {
            final SelectQuery selectQuery = SelectQuery.get(mapperTable);

            selectQuery.addSelectClause(entityIdColumn);
            selectQuery.setWhereClause(new WhereClause(Criteria.eq(column, valueEntityId)));
            final Criteria criteria = switch (operator) {
                case NOT_EQUAL -> Criteria.notIn(entityIdColumn, selectQuery);
                default -> Criteria.in(entityIdColumn, selectQuery);
            };
            whereClause = Objects.isNull(whereClause) ? new WhereClause(criteria) : whereClause.and(criteria);
        }

        return whereClause;
    }

    /**
     * <p>
     * Builds the {@link WhereClause} of the inverted {@link Operator}.
     * </p>
     *
     * @param remoteTable Represents the remote table of the field.
     * @param mapperTable Represents the mapper table of the field table.
     * @param whereClause Represents the retrieval criteria
     * @return Inverted operator criteria.
     */
    public WhereClause buildInvertedOperatorCriteria(final Table remoteTable, final Table mapperTable, final WhereClause whereClause) {
        return buildInvertedOperatorCriteria(remoteTable, mapperTable, whereClause, mapperTable.getColumn(ENTITY_ID));
    }

    /**
     * <p>
     * Builds the {@link WhereClause} of the inverted {@link Operator}
     * </p>
     *
     * @param remoteAliasTable Represents the alias name for remote table of the field
     * @param mapperTable      Represents the mapper table of the field table
     * @param whereClause      Represents the retrieval criteria
     * @param column           Represents the column
     * @return Inverted operator criteria
     */
    public WhereClause buildInvertedOperatorCriteria(final Table remoteAliasTable, final Table mapperTable, final WhereClause whereClause, final Column column) {
        final SelectQuery selectQuery = SelectQuery.get(mapperTable);

        selectQuery.addSelectClause(column);
        selectQuery.setWhereClause(whereClause);

        if (Objects.nonNull(remoteAliasTable)) {
            final Table remoteTable = Table.get(remoteAliasTable.getName());

            selectQuery.addJoinClause(new JoinClause(TableUtils.groupFkColumnsByTable(mapperTable, OltpTableResolver.get()).get(remoteTable), remoteAliasTable.getPKColumn()));
        }

        return new WhereClause(Criteria.notIn(column, selectQuery));
    }

    /**
     * <p>
     * Builds the criteria for the given column, operator and value.
     * </p>
     *
     * @param column              Represents the main {@link Column} of the criteria.
     * @param isMultiValueAllowed Represents the flag that indicates the given operator has multiple values or single value.
     * @param operator            Represents the {@link Operator} to compare the value of given column and given value.
     * @param criteriaValues      Represents the values to build criteria.
     * @return Field {@link WhereClause}.
     */
    public WhereClause buildCriteria(final Column column, final boolean isMultiValueAllowed, final Operator operator,
                                           final Collection<CriteriaValue> criteriaValues) {
        final Optional<CriteriaValue> optionalCriteriaValue = Optional.ofNullable(criteriaValues)
                .flatMap(values -> values.stream().findAny());
        final Long valueEntityId;
        final Object value;

        if (optionalCriteriaValue.isEmpty()) {
            value = null;
            valueEntityId = null;
        } else if (isMultiValueAllowed) {
            valueEntityId = null;
            value = criteriaValues.stream().map(criteriaValue -> {
                final Long entityId = criteriaValue.getValueEntityId();

                return Objects.isNull(entityId) ? criteriaValue.getValue() : entityId;
            }).filter(Objects::nonNull).collect(Collectors.toSet());
        } else {
            final CriteriaValue criteriaValue = optionalCriteriaValue.get();

            value = criteriaValue.getValue();
            valueEntityId = criteriaValue.getValueEntityId();
        }
        final Object resolvedValue;

        if (Objects.isNull(valueEntityId)) {
            if (column.getColumnDefinition().getDataType().isNumeric()) {
                resolvedValue = TypeChecker.isNumber(value) ? NumberUtils.parseNumber(value) : value;
            } else {
                resolvedValue = value;
            }
        } else {
            resolvedValue = valueEntityId;
        }

        return new WhereClause(new Criteria(column, resolvedValue, operator));
    }

    /**
     * <p>
     * Fetches the table of the fields.
     * </p>
     *
     * @return OMETFields table.
     */
    private Table getFieldTable() {
        return Table.get(OMETFIELDS.TABLE);
    }
}
