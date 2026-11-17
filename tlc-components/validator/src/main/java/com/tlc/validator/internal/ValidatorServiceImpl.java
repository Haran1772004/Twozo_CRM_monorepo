package com.tlc.validator.internal;

import com.tlc.commons.util.Configurations;
import com.tlc.validator.ExpressionEvaluator;
import com.tlc.validator.ModelValidator;
import com.tlc.validator.ValidatorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Provides the validator related services
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public class ValidatorServiceImpl implements ValidatorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ValidatorServiceImpl.class);

    private ModelValidator modelValidator;
    private ExpressionEvaluator expressionEvaluator;

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(final Configurations configurations) {
        this.modelValidator = new ModelValidatorImpl();
        this.expressionEvaluator = new ExpressionEvaluatorImpl();

        LOGGER.info("Validator services activated");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        this.modelValidator = null;
        this.expressionEvaluator = null;

        LOGGER.info("Validator services deactivated");
    }

    /**
     * {@inheritDoc}
     *
     * @return the {@link ModelValidator}
     */
    @Override
    public ModelValidator modelValidator() {
        return modelValidator;
    }

    /**
     * {@inheritDoc}
     *
     * @return the {@link ExpressionEvaluator}
     */
    @Override
    public ExpressionEvaluator expressionEvaluator() {
        return expressionEvaluator;
    }
}
