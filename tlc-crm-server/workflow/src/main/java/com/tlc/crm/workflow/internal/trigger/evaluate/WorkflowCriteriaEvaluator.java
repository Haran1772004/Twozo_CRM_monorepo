package com.tlc.crm.workflow.internal.trigger.evaluate;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.pair.Pair;

import com.tlc.crm.workflow.internal.status.WorkflowErrorCodes;
import com.tlc.crm.workflow.internal.trigger.evaluate.builder.ActivityExpressionContextHolderBuilder;
import com.tlc.crm.workflow.internal.trigger.evaluate.builder.CompanyExpressionContextHolderBuilder;
import com.tlc.crm.workflow.internal.trigger.evaluate.builder.ContactExpressionContextHolderBuilder;
import com.tlc.crm.workflow.internal.trigger.evaluate.builder.DealExpressionContextHolderBuilder;
import com.tlc.crm.workflow.v2.api.model.ExecutionEntity;

import com.tlc.validator.ExpressionEvaluator;
import com.tlc.validator.TlcModel;
import com.tlc.validator.expression.model.ExpressionContextHolder;

import com.tlc.crm.workflow.api.model.TriggerSource;
import com.tlc.crm.workflow.api.model.history.WorkflowExecutionStatus.WorkflowCriteriaExecutionStatus;
import com.tlc.crm.workflow.api.model.WorkflowCriteriaSet;
import com.tlc.crm.workflow.internal.trigger.evaluate.builder.ExpressionContextHolderBuilder;
import com.tlc.validator.service.provider.ValidatorServiceProvider;

import java.util.Collection;

/**
 * Abstract way to evaluate the expression of a model.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class WorkflowCriteriaEvaluator {

    private final ExpressionEvaluator expressionEvaluator;

    private WorkflowCriteriaEvaluator() {
        this.expressionEvaluator = ValidatorServiceProvider.get().expressionEvaluator();
    }

    private static class Instance {
        private static final WorkflowCriteriaEvaluator INSTANCE = new WorkflowCriteriaEvaluator();
    }

    /**
     * Gets the instance of {@link WorkflowCriteriaEvaluator}.
     *
     * @return {@link WorkflowCriteriaEvaluator} instance.
     */
    public static WorkflowCriteriaEvaluator getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Evaluates the expression of the {@link WorkflowCriteriaSet} to ascertain the outcome.
     *
     * @param modelPair     Refers the {@link Pair} of models.
     * @param criteriaSets  Refers the {@link WorkflowCriteriaSet}.
     * @param triggerSource Refers the trigger source.
     * @param <T>           Refers the type of the model.
     * @return {@link WorkflowCriteriaExecutionStatus}
     */
    public <T extends TlcModel> WorkflowCriteriaExecutionStatus evaluate(final Pair<T, T> modelPair, final Collection<WorkflowCriteriaSet> criteriaSets,
                                                                         final TriggerSource triggerSource) {
        final ExpressionContextHolder expressionContextHolder = ExpressionContextHolderBuilder.get(triggerSource)
                .buildExpressionContextHolder(modelPair, criteriaSets);

        return expressionEvaluator.evaluate(expressionContextHolder)
                ? WorkflowCriteriaExecutionStatus.PASSED : WorkflowCriteriaExecutionStatus.FAILED;
    }

    /**
     * Evaluates the expression of the {@link WorkflowCriteriaSet} to ascertain the outcome.
     *
     * @param modelPair       Refers the {@link Pair} of models.
     * @param criteriaSets    Refers the {@link WorkflowCriteriaSet}.
     * @param executionSource Refers the execution source.
     * @param <T>             Refers the type of the model.
     * @return {@link WorkflowCriteriaExecutionStatus}
     */
    public <T extends TlcModel> WorkflowCriteriaExecutionStatus evaluate(final Pair<T, T> modelPair, final Collection<WorkflowCriteriaSet> criteriaSets, final ExecutionEntity.ExecutionSource executionSource) {
        final ExpressionContextHolderBuilder expressionContextHolderBuilder = switch (executionSource) {
            case COMPANY -> CompanyExpressionContextHolderBuilder.getInstance();
            case CONTACT -> ContactExpressionContextHolderBuilder.getInstance();
            case DEAL -> DealExpressionContextHolderBuilder.getInstance();
            case ACTIVITY -> ActivityExpressionContextHolderBuilder.getInstance();
            default -> throw ErrorCode.get(WorkflowErrorCodes.INVALID_TRIGGER_SOURCE);
        };

        final ExpressionContextHolder expressionContextHolder = expressionContextHolderBuilder.buildExpressionContextHolder(modelPair, criteriaSets);

        return expressionEvaluator.evaluate(expressionContextHolder)
                ? WorkflowCriteriaExecutionStatus.PASSED : WorkflowCriteriaExecutionStatus.FAILED;
    }
}