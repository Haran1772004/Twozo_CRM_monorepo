package com.tlc.crm.workflow.internal.trigger.evaluate.builder;

import com.tlc.commons.pair.Pair;

import com.tlc.validator.TlcModel;
import com.tlc.validator.expression.model.ExpressionContextHolder;

import com.tlc.crm.workflow.api.model.TriggerSource;
import com.tlc.crm.workflow.api.model.WorkflowCriteriaSet;

import java.util.Collection;

/**
 * The builder that constructs the expression for an entity with the provided criteria information.
 *
 * @param <T> Refers the type of the model.
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public interface ExpressionContextHolderBuilder<T extends TlcModel> {

    /**
     * Gets the {@link ExpressionContextHolderBuilder} of given trigger source.
     *
     * @param triggerSource Refers the {@link TriggerSource}
     * @return The {@link ExpressionContextHolderBuilder} instance.
     */
    static ExpressionContextHolderBuilder get(final TriggerSource triggerSource) {
        return switch (triggerSource) {
            case ACTIVITY -> ActivityExpressionContextHolderBuilder.getInstance();
            case COMPANY -> CompanyExpressionContextHolderBuilder.getInstance();
            case CONTACT -> ContactExpressionContextHolderBuilder.getInstance();
            case DEAL -> DealExpressionContextHolderBuilder.getInstance();
        };
    }

    /**
     * Fetches the {@link ExpressionContextHolder} by constructing the expression of an entity.
     *
     * @param modelPair    Refers to the {@link Pair} consisting of an old and current model of the same entity.
     * @param criteriaSets Refers the {@link WorkflowCriteriaSet} to be checked.
     * @return The {@link ExpressionContextHolder}.
     */
    ExpressionContextHolder buildExpressionContextHolder(final Pair<T, T> modelPair,
                                                         final Collection<WorkflowCriteriaSet> criteriaSets);
}
