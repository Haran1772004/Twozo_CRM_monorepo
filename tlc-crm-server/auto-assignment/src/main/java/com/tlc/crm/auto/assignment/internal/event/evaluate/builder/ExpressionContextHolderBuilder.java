package com.tlc.crm.auto.assignment.internal.event.evaluate.builder;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.auto.assignment.api.model.AutoAssignmentCriteriaSet;
import com.tlc.crm.auto.assignment.internal.status.AutoAssignmentErrorCodes;

import com.tlc.crm.entity.fields.source.Source;

import com.tlc.validator.expression.model.ExpressionContextHolder;

import java.util.Collection;

/**
 * The builder that constructs the expression.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public interface ExpressionContextHolderBuilder<T> {

    /**
     * Gets the {@link ExpressionContextHolderBuilder}.
     *
     * @param source Refers the {@link Source}.
     * @return The {@link ExpressionContextHolderBuilder} instance.
     */
    static ExpressionContextHolderBuilder get(final Source source) {
        return switch (source) {
            case COMPANY -> CompanyExpressionContextHolderBuilder.getInstance();
            case CONTACT -> ContactExpressionContextHolderBuilder.getInstance();
            case DEAL -> DealExpressionContextHolderBuilder.getInstance();
            default -> throw ErrorCode.get(AutoAssignmentErrorCodes.INVALID_SOURCE);
        };
    }

    /**
     * Fetches the {@link ExpressionContextHolder} by constructing the expression of an entity.
     *
     * @param model        Refers to the model.
     * @param criteriaSets Refers the {@link AutoAssignmentCriteriaSet} to be checked.
     * @return The {@link ExpressionContextHolder}.
     */
    ExpressionContextHolder buildExpressionContextHolder(final T model, final Collection<AutoAssignmentCriteriaSet> criteriaSets);

}
