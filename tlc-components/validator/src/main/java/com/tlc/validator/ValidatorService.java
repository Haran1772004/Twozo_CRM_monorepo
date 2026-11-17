package com.tlc.validator;

import com.tlc.commons.service.Service;

/**
 * <p>
 * Provides the validator related services
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public interface ValidatorService extends Service {

    /**
     * <p>
     * Gets the {@link ModelValidator} service for validating the given model
     * </p>
     *
     * @return {@link ModelValidator}
     */
    ModelValidator modelValidator();

    /**
     * <p>
     * Gets the {@link ExpressionEvaluator} service for evaluate the given expression
     * </p>
     *
     * @return {@link ExpressionEvaluator}
     */
    ExpressionEvaluator expressionEvaluator();
}
