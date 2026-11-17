package com.tlc.crm.entity.list.filter.appender.impl;

import com.tlc.crm.entity.filter.api.models.Comparator;
import com.tlc.crm.entity.list.filter.appender.CriteriaAppender;
import com.tlc.crm.entity.list.filter.appender.model.NullableFieldCriteriaAppenderRequisites;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Operator;
import com.tlc.sql.common.api.dml.WhereClause;

import java.util.Objects;
import java.util.function.Function;

/**
 * <p>
 * Builds the {@link NullableFieldCriteriaAppender} to appends the criteria for inverted operators
 * (Ex: If we choose a field named 'designation' present in Contact, and chosen 'is not' comparator,
 * for this case when we give any value(developer) for 'designation' the output should include the nullable values
 * in 'designation' field and should also exclude the given value(developer))
 * </p>
 *
 * @author Sathish
 */
public class NullableFieldCriteriaAppender implements CriteriaAppender<NullableFieldCriteriaAppenderRequisites> {

    public static final Type TYPE = () -> "NULLABLE_FIELD_CRITERIA";

    private final boolean hasNoValue;
    private final Comparator comparator;

    private NullableFieldCriteriaAppenderRequisites criteriaRequisites;

    public NullableFieldCriteriaAppender(final Comparator comparator, final boolean hasNoValue) {
        this.hasNoValue = hasNoValue;
        this.comparator = comparator;
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
     * @param requisites Represents the {@link NullableFieldCriteriaAppenderRequisites}.
     */
    @Override
    public void setCriteriaRequisites(final NullableFieldCriteriaAppenderRequisites requisites) {
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
        final Function<WhereClause, WhereClause> nullCriteriaAppender = (baseCriteria) -> {
            final Criteria criteria = Criteria.eq(criteriaRequisites.getColumn(), null);

            return Objects.isNull(baseCriteria) ? new WhereClause(criteria) : baseCriteria.or(criteria);
        };

        WhereClause whereClause = existingCriteria;

        if (comparator.isValueNeeded()) {
            whereClause = switch (operator) {
                case NOT_EQUAL, NOT_ENDS_WITH, NOT_CONTAINS, NOT_STARTS_WITH, NOT_BETWEEN, NOT_IN, LESS_EQUAL, LESS_THAN ->
                        nullCriteriaAppender.apply(whereClause);
                default -> whereClause;
            };
        } else if (criteriaRequisites.validateEmptyValueCriteria() && operator == Operator.EQUAL && hasNoValue) {
            whereClause = nullCriteriaAppender.apply(whereClause);
        }
        return whereClause;
    }
}