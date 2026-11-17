package com.tlc.validator.expression.model;

import com.tlc.validator.expression.service.Expression;

import java.util.Map;

/**
 * <p>
 * A model which represents the {@link ExpressionContextHolder}.
 * </p>
 *
 * @param expression Represents the {@link Expression} which has the string expression to be evaluated.
 * @param context    Represents the {@link Map} of context which holds the information to evaluates.
 * @author Kavinila E
 * @version 1.0
 */
public record ExpressionContextHolder(Expression expression, Map<String, Object> context) {
}
