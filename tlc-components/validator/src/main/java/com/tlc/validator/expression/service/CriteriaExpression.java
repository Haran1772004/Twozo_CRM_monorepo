package com.tlc.validator.expression.service;

import com.tlc.commons.code.ErrorCode;

import com.tlc.validator.expression.Criteria;
import com.tlc.validator.expression.model.CriteriaData;
import com.tlc.validator.internal.status.ValidatorErrorCodes;

import java.util.Objects;

/**
 * An expression criteria that assembles the expression based on the provided criteria information.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class CriteriaExpression implements Criteria {

    private static final String LEFT_PARENTHESIS = "(";
    private static final String RIGHT_PARENTHESIS = ")";
    private static final String DOT = ".";
    private static final String APOSTROPHE = "'";
    private static final String NULL = "null";
    private static final String SIZE = "size";
    private static final String OR = " || ";
    private static final String AND = " && ";

    private final String field;
    private final Object value;
    private final StringBuilder criteriaBuilder;

    public CriteriaExpression() {
        this("field", "value", new StringBuilder());
    }

    public CriteriaExpression(final String field, final Object value, final StringBuilder criteriaBuilder) {
        this.field = Objects.requireNonNull(field);
        this.value = value;
        this.criteriaBuilder = criteriaBuilder;
    }

    /**
     * Gets the field.
     *
     * @return The field.
     */
    public String getField() {
        return field;
    }

    /**
     * Gets the value of a criteria.
     *
     * @return The value of a criteria.
     */
    public Object getValue() {
        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return criteriaBuilder.toString();
    }

    /**
     * Creates the new expression of a criteria.
     *
     * @param field             Refers the field name of the criteria.
     * @param value             Refers the value of a criteria.
     * @param fieldValueBuilder Refers the {@link StringBuilder} which holds the formatted string of an expression criteria.
     * @return The {@link Criteria}.
     */
    private static Criteria newExpressionCriteria(final String field, final Object value, final StringBuilder fieldValueBuilder) {
        return new CriteriaExpression(field, value, fieldValueBuilder);
    }

    /**
     * {@inheritDoc}
     *
     * @param criteriaData Refers the {@link CriteriaData}.
     * @return The {@link Criteria} of an expression.
     */
    @Override
    public Criteria eq(final CriteriaData criteriaData) {
        final StringBuilder criteriaBuilder = new StringBuilder();
        final String fieldKey = criteriaData.getFieldKey();
        final Object value = criteriaData.getValue();

        switch (criteriaData.getExpressionDataType()) {
            case STRING -> criteriaBuilder.append(fieldKey).append(DOT).append(Operator.EQUALS).append(LEFT_PARENTHESIS)
                    .append(APOSTROPHE).append(value).append(APOSTROPHE).append(RIGHT_PARENTHESIS);
            case NUMBER -> criteriaBuilder.append(fieldKey).append(Operator.NUMBER_EQUAL).append(value);
            default -> throw ErrorCode.get(ValidatorErrorCodes.INVALID_CRITERIA_INPUT);
        }

        return newExpressionCriteria(fieldKey, value, criteriaBuilder);
    }

    /**
     * {@inheritDoc}
     *
     * @param criteriaData Refers the {@link CriteriaData}.
     * @return The {@link Criteria} of an expression.
     */
    @Override
    public Criteria notEq(final CriteriaData criteriaData) {
        final StringBuilder criteriaBuilder = new StringBuilder();
        final String fieldKey = criteriaData.getFieldKey();
        final Object value = criteriaData.getValue();

        switch (criteriaData.getExpressionDataType()) {
            case STRING -> criteriaBuilder.append(Operator.NOT).append(fieldKey).append(DOT).append(Operator.EQUALS)
                    .append(LEFT_PARENTHESIS).append(APOSTROPHE).append(value).append(APOSTROPHE).append(RIGHT_PARENTHESIS);
            case NUMBER -> criteriaBuilder.append(fieldKey).append(Operator.NUMBER_NOT_EQUAL).append(value);
            default -> throw ErrorCode.get(ValidatorErrorCodes.INVALID_CRITERIA_INPUT);
        }

        return newExpressionCriteria(fieldKey, value, criteriaBuilder);
    }

    /**
     * {@inheritDoc}
     *
     * @param criteriaData Refers the {@link CriteriaData}.
     * @return The {@link Criteria} of an expression.
     */
    @Override
    public Criteria startsWith(final CriteriaData criteriaData) {
        final String fieldKey = criteriaData.getFieldKey();
        final Object value = criteriaData.getValue();

        return newExpressionCriteria(fieldKey, value, new StringBuilder().append(fieldKey).append(DOT).append(Operator.STARTS_WITH)
                .append(LEFT_PARENTHESIS).append(APOSTROPHE).append(value).append(APOSTROPHE).append(RIGHT_PARENTHESIS));
    }

    /**
     * {@inheritDoc}
     *
     * @param criteriaData Refers the {@link CriteriaData}.
     * @return The {@link Criteria} of an expression.
     */
    @Override
    public Criteria notStartsWith(final CriteriaData criteriaData) {
        final String fieldKey = criteriaData.getFieldKey();
        final Object value = criteriaData.getValue();

        return newExpressionCriteria(fieldKey, value, new StringBuilder().append(Operator.NOT).append(fieldKey)
                .append(DOT).append(Operator.STARTS_WITH).append(LEFT_PARENTHESIS)
                .append(APOSTROPHE).append(value).append(APOSTROPHE).append(RIGHT_PARENTHESIS));
    }

    /**
     * {@inheritDoc}
     *
     * @param criteriaData Refers the {@link CriteriaData}.
     * @return The {@link Criteria} of an expression.
     */
    @Override
    public Criteria endsWith(final CriteriaData criteriaData) {
        final String fieldKey = criteriaData.getFieldKey();
        final Object value = criteriaData.getValue();

        return newExpressionCriteria(fieldKey, value, new StringBuilder().append(fieldKey).append(DOT).append(Operator.ENDS_WITH)
                .append(LEFT_PARENTHESIS).append(APOSTROPHE).append(value).append(APOSTROPHE).append(RIGHT_PARENTHESIS));
    }

    /**
     * {@inheritDoc}
     *
     * @param criteriaData Refers the {@link CriteriaData}.
     * @return The {@link Criteria} of an expression.
     */
    @Override
    public Criteria notEndsWith(final CriteriaData criteriaData) {
        final String fieldKey = criteriaData.getFieldKey();
        final Object value = criteriaData.getValue();

        return newExpressionCriteria(fieldKey, value, new StringBuilder().append(Operator.NOT).append(fieldKey)
                .append(DOT).append(Operator.ENDS_WITH).append(LEFT_PARENTHESIS)
                .append(APOSTROPHE).append(value).append(APOSTROPHE).append(RIGHT_PARENTHESIS));
    }

    /**
     * {@inheritDoc}
     *
     * @param criteriaData Refers the {@link CriteriaData}.
     * @return The {@link Criteria} of an expression.
     */
    @Override
    public Criteria contains(final CriteriaData criteriaData) {
        final StringBuilder criteriaBuilder = new StringBuilder();
        final String fieldKey = criteriaData.getFieldKey();
        final Object value = criteriaData.getValue();

        switch (criteriaData.getExpressionDataType()) {
            case STRING -> criteriaBuilder.append(fieldKey).append(DOT).append(Operator.CONTAINS)
                    .append(LEFT_PARENTHESIS).append(APOSTROPHE).append(value).append(APOSTROPHE).append(RIGHT_PARENTHESIS);
            case NUMBER -> criteriaBuilder.append(fieldKey).append(DOT).append(Operator.CONTAINS)
                    .append(LEFT_PARENTHESIS).append(value).append(RIGHT_PARENTHESIS);
            case COLLECTION -> criteriaBuilder.append(criteriaData.getCompareFieldKey()).append(DOT)
                    .append(Operator.CONTAINS).append(LEFT_PARENTHESIS).append(fieldKey).append(RIGHT_PARENTHESIS);
            default -> throw ErrorCode.get(ValidatorErrorCodes.INVALID_CRITERIA_INPUT);

        }

        return newExpressionCriteria(fieldKey, value, criteriaBuilder);
    }

    /**
     * {@inheritDoc}
     *
     * @param criteriaData Refers the {@link CriteriaData}.
     * @return The {@link Criteria} of an expression.
     */
    @Override
    public Criteria notContains(final CriteriaData criteriaData) {
        final StringBuilder criteriaBuilder = new StringBuilder();
        final String fieldKey = criteriaData.getFieldKey();
        final Object value = criteriaData.getValue();

        switch (criteriaData.getExpressionDataType()) {
            case STRING -> criteriaBuilder.append(Operator.NOT).append(fieldKey).append(DOT).append(Operator.CONTAINS)
                    .append(LEFT_PARENTHESIS).append(APOSTROPHE).append(value).append(APOSTROPHE).append(RIGHT_PARENTHESIS);
            case NUMBER -> criteriaBuilder.append(Operator.NOT).append(fieldKey).append(DOT).append(Operator.CONTAINS)
                    .append(LEFT_PARENTHESIS).append(value).append(RIGHT_PARENTHESIS);
            default -> throw ErrorCode.get(ValidatorErrorCodes.INVALID_CRITERIA_INPUT);
        }

        return newExpressionCriteria(fieldKey, value, criteriaBuilder);
    }

    /**
     * {@inheritDoc}
     *
     * @param criteriaData Refers the {@link CriteriaData}.
     * @return The {@link Criteria} of an expression.
     */
    @Override
    public Criteria ge(final CriteriaData criteriaData) {
        final StringBuilder criteriaBuilder = new StringBuilder();
        final String fieldKey = criteriaData.getFieldKey();
        final Object value = criteriaData.getValue();

        criteriaBuilder.append(LEFT_PARENTHESIS).append(fieldKey).append(Operator.NUMBER_NOT_EQUAL).append(NULL)
                .append(AND).append(fieldKey).append(Operator.GREATER_EQUAL).append(value).append(RIGHT_PARENTHESIS);

        return newExpressionCriteria(fieldKey, value, criteriaBuilder);
    }

    /**
     * {@inheritDoc}
     *
     * @param criteriaData Refers the {@link CriteriaData}.
     * @return The {@link Criteria} of an expression.
     */
    @Override
    public Criteria le(final CriteriaData criteriaData) {
        final StringBuilder criteriaBuilder = new StringBuilder();
        final String fieldKey = criteriaData.getFieldKey();
        final Object value = criteriaData.getValue();

        criteriaBuilder.append(LEFT_PARENTHESIS).append(fieldKey).append(Operator.NUMBER_NOT_EQUAL).append(NULL)
                .append(AND).append(fieldKey).append(Operator.LESS_EQUAL).append(value).append(RIGHT_PARENTHESIS);

        return newExpressionCriteria(fieldKey, value, criteriaBuilder);
    }

    /**
     * {@inheritDoc}
     *
     * @param criteriaData Refers the {@link CriteriaData}.
     * @return The {@link Criteria} of an expression.
     */
    @Override
    public Criteria lt(final CriteriaData criteriaData) {
        final StringBuilder criteriaBuilder = new StringBuilder();
        final String fieldKey = criteriaData.getFieldKey();
        final Object value = criteriaData.getValue();

        criteriaBuilder.append(LEFT_PARENTHESIS).append(fieldKey).append(Operator.NUMBER_NOT_EQUAL).append(NULL)
                .append(AND).append(fieldKey).append(Operator.LESS_THAN).append(value).append(RIGHT_PARENTHESIS);

        return newExpressionCriteria(fieldKey, value, criteriaBuilder);
    }

    /**
     * {@inheritDoc}
     *
     * @param criteriaData Refers the {@link CriteriaData}.
     * @return The {@link Criteria} of an expression.
     */
    @Override
    public Criteria gt(final CriteriaData criteriaData) {
        final StringBuilder criteriaBuilder = new StringBuilder();
        final String fieldKey = criteriaData.getFieldKey();
        final Object value = criteriaData.getValue();

        criteriaBuilder.append(LEFT_PARENTHESIS).append(fieldKey).append(Operator.NUMBER_NOT_EQUAL).append(NULL)
                .append(AND).append(fieldKey).append(Operator.GREATER_THAN).append(value).append(RIGHT_PARENTHESIS);

        return newExpressionCriteria(fieldKey, value, criteriaBuilder);
    }

    /**
     * {@inheritDoc}
     *
     * @param criteriaData Refers the {@link CriteriaData}.
     * @return The {@link Criteria} of an expression.
     */
    @Override
    public Criteria empty(final CriteriaData criteriaData) {
        final StringBuilder criteriaBuilder = new StringBuilder();
        final String fieldKey = criteriaData.getFieldKey();
        final Object value = criteriaData.getValue();

        switch (criteriaData.getExpressionDataType()) {
            case STRING, COLLECTION -> criteriaBuilder.append(fieldKey).append(DOT).append(Operator.EMPTY)
                    .append(LEFT_PARENTHESIS).append(RIGHT_PARENTHESIS);
            case NUMBER -> criteriaBuilder.append(fieldKey).append(Operator.NUMBER_EQUAL).append(NULL);
            default -> throw ErrorCode.get(ValidatorErrorCodes.INVALID_CRITERIA_INPUT);
        }

        return newExpressionCriteria(fieldKey, value, criteriaBuilder);
    }

    /**
     * {@inheritDoc}
     *
     * @param criteriaData Refers the {@link CriteriaData}.
     * @return The {@link Criteria} of an expression.
     */
    @Override
    public Criteria notEmpty(final CriteriaData criteriaData) {
        final StringBuilder criteriaBuilder = new StringBuilder();
        final String fieldKey = criteriaData.getFieldKey();
        final Object value = criteriaData.getValue();

        switch (criteriaData.getExpressionDataType()) {
            case STRING, COLLECTION -> criteriaBuilder.append(Operator.NOT).append(fieldKey).append(DOT)
                    .append(Operator.EMPTY).append(LEFT_PARENTHESIS).append(RIGHT_PARENTHESIS);
            case NUMBER -> criteriaBuilder.append(fieldKey).append(Operator.NUMBER_NOT_EQUAL).append(NULL);
            default -> throw ErrorCode.get(ValidatorErrorCodes.INVALID_CRITERIA_INPUT);
        }

        return newExpressionCriteria(fieldKey, value, criteriaBuilder);
    }

    /**
     * {@inheritDoc}
     *
     * @param criteriaData Refers the {@link CriteriaData}.
     * @return The {@link Criteria} of an expression.
     */
    @Override
    public Criteria containsAll(final CriteriaData criteriaData) {
        final String fieldKey = criteriaData.getFieldKey();

        return newExpressionCriteria(fieldKey, value, new StringBuilder().append(fieldKey).append(DOT)
                .append(Operator.CONTAINS_ALL).append(LEFT_PARENTHESIS).append(criteriaData.getCompareFieldKey()).append(RIGHT_PARENTHESIS));
    }

    /**
     * {@inheritDoc}
     *
     * @param criteriaData Refers the {@link CriteriaData}.
     * @return The {@link Criteria} of an expression.
     */
    @Override
    public Criteria notContainsAll(final CriteriaData criteriaData) {
        final String fieldKey = criteriaData.getFieldKey();

        return newExpressionCriteria(fieldKey, value, new StringBuilder().append(Operator.NOT).append(fieldKey).append(DOT)
                .append(Operator.CONTAINS_ALL).append(LEFT_PARENTHESIS).append(criteriaData.getCompareFieldKey()).append(RIGHT_PARENTHESIS));
    }

    /**
     * {@inheritDoc}
     *
     * @param criteriaData Refers the {@link CriteriaData}.
     * @return The {@link Criteria} of an expression.
     * @see #notEq(CriteriaData) {@code notEq(final String fieldKey, final Object value)}
     */
    @Override
    public Criteria changed(final CriteriaData criteriaData) {
        final StringBuilder criteriaBuilder = new StringBuilder();
        final String fieldKey = criteriaData.getFieldKey();
        final Object value = criteriaData.getValue();

        switch (criteriaData.getExpressionDataType()) {
            case STRING -> criteriaBuilder.append(Operator.NOT).append(fieldKey).append(DOT).append(Operator.EQUALS)
                    .append(LEFT_PARENTHESIS).append(APOSTROPHE).append(value).append(APOSTROPHE).append(RIGHT_PARENTHESIS);
            case NUMBER -> criteriaBuilder.append(fieldKey).append(Operator.NUMBER_NOT_EQUAL).append(value);
            case COLLECTION -> {
                final String compareFieldKey = criteriaData.getCompareFieldKey();

                criteriaBuilder.append(LEFT_PARENTHESIS).append(SIZE).append(LEFT_PARENTHESIS).append(fieldKey).append(RIGHT_PARENTHESIS)
                        .append(Operator.NUMBER_NOT_EQUAL).append(SIZE).append(LEFT_PARENTHESIS).append(compareFieldKey).append(RIGHT_PARENTHESIS)
                        .append(OR).append(Operator.NOT).append(fieldKey).append(DOT).append(Operator.CONTAINS_ALL).append(LEFT_PARENTHESIS)
                        .append(compareFieldKey).append(RIGHT_PARENTHESIS).append(RIGHT_PARENTHESIS);
            }
            default -> throw ErrorCode.get(ValidatorErrorCodes.INVALID_CRITERIA_INPUT);
        }

        return newExpressionCriteria(fieldKey, value, criteriaBuilder);
    }

    /**
     * {@inheritDoc}
     *
     * @param criteriaData Refers the {@link CriteriaData}.
     * @return The {@link Criteria} of an expression.
     * @see #eq(CriteriaData) {@code eq(final String fieldKey, final Object value)}.
     */
    @Override
    public Criteria notChanged(final CriteriaData criteriaData) {
        final StringBuilder criteriaBuilder = new StringBuilder();
        final String fieldKey = criteriaData.getFieldKey();
        final Object value = criteriaData.getValue();

        switch (criteriaData.getExpressionDataType()) {
            case STRING -> criteriaBuilder.append(fieldKey).append(DOT).append(Operator.EQUALS).append(LEFT_PARENTHESIS)
                    .append(APOSTROPHE).append(value).append(APOSTROPHE).append(RIGHT_PARENTHESIS);
            case NUMBER -> criteriaBuilder.append(fieldKey).append(Operator.NUMBER_EQUAL).append(value);
            case COLLECTION -> {
                final String compareFieldKey = criteriaData.getCompareFieldKey();

                criteriaBuilder.append(LEFT_PARENTHESIS).append(SIZE).append(LEFT_PARENTHESIS).append(fieldKey).append(RIGHT_PARENTHESIS)
                        .append(Operator.NUMBER_EQUAL).append(SIZE).append(LEFT_PARENTHESIS).append(compareFieldKey).append(RIGHT_PARENTHESIS)
                        .append(AND).append(fieldKey).append(DOT).append(Operator.CONTAINS_ALL).append(LEFT_PARENTHESIS)
                        .append(compareFieldKey).append(RIGHT_PARENTHESIS).append(RIGHT_PARENTHESIS);
            }
            default -> throw ErrorCode.get(ValidatorErrorCodes.INVALID_CRITERIA_INPUT);
        }

        return newExpressionCriteria(fieldKey, value, criteriaBuilder);
    }

}
