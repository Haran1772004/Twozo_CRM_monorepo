package com.tlc.crm.auto.assignment.internal.event.evaluate;

import com.tlc.crm.auto.assignment.api.model.AutoAssignmentCriteriaSet;
import com.tlc.crm.auto.assignment.internal.event.evaluate.builder.ExpressionContextHolderBuilder;

import com.tlc.crm.entity.fields.source.Source;

import com.tlc.validator.ExpressionEvaluator;
import com.tlc.validator.expression.model.ExpressionContextHolder;
import com.tlc.validator.service.provider.ValidatorServiceProvider;

import java.util.Collection;

/**
 * Abstract way to evaluate the expression of a model.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class AutoAssignmentCriteriaEvaluator {

    private final ExpressionEvaluator expressionEvaluator;

    private AutoAssignmentCriteriaEvaluator() {
        this.expressionEvaluator = ValidatorServiceProvider.get().expressionEvaluator();
    }

    private static class Instance {
        private static final AutoAssignmentCriteriaEvaluator INSTANCE = new AutoAssignmentCriteriaEvaluator();
    }

    /**
     * Gets the instance of {@link AutoAssignmentCriteriaEvaluator}.
     *
     * @return {@link AutoAssignmentCriteriaEvaluator} instance.
     */
    public static AutoAssignmentCriteriaEvaluator getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Evaluates the expression of the {@link AutoAssignmentCriteriaSet} to ascertain the outcome.
     *
     * @param model        Refers the model.
     * @param criteriaSets Refers the {@code AutoAssignmentCriteriaSet}.
     * @param <T>          Refers the type of the model.
     * @return true, if successful otherwise false.
     */
    public <T> boolean evaluate(final T model, final Collection<AutoAssignmentCriteriaSet> criteriaSets, final Source source) {
        final ExpressionContextHolder expressionContextHolder = ExpressionContextHolderBuilder.get(source)
                .buildExpressionContextHolder(model, criteriaSets);

        return expressionEvaluator.evaluate(expressionContextHolder);
    }
}