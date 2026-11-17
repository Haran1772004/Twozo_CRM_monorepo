package com.tlc.crm.core.action.deal.handler;

import com.tlc.crm.core.api.deal.model.Deal;


/**
 * <p>
 * The {@link DynamicDealFieldEvaluatorHandler} is responsible for evaluating and computing values for deal fields
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public interface DynamicDealFieldEvaluatorHandler {

    /**
     * <p>
     * Gets the {@link DynamicDealFieldEvaluatorHandler} instance
     * </p>
     *
     * @return The {@link DynamicDealFieldEvaluatorHandler}
     */
    static DynamicDealFieldEvaluatorHandler getInstance() {
        return DynamicDealFieldEvaluatorHandlerImpl.getInstance();
    }

    /**
     * <p>
     * Computes the deal fields for the given field name
     * </p>
     *
     * @param fieldName Represents the field name
     * @param deal      Represents the {@link Deal}
     */
    void computeFieldValue(final String fieldName, final Deal deal);
}
