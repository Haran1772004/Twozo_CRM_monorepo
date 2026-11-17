package com.tlc.validator.internal;

import com.tlc.commons.code.ErrorCode;

import com.tlc.validator.internal.status.ValidatorErrorCodes;
import com.tlc.validator.expression.model.ExpressionContextHolder;
import com.tlc.validator.ExpressionEvaluator;

import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.MapContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * An evaluator which evaluates the expression with provided context
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public class ExpressionEvaluatorImpl implements ExpressionEvaluator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExpressionEvaluatorImpl.class);

    private JexlEngine jexlEngine;

    public ExpressionEvaluatorImpl() {
        createJexlEngine();
    }

    /**
     * <p>
     * Builds the JexlEngine with the specified configuration
     * </p>
     * <p>
     * The configuration options are:
     * - <code>strict</code> : If set to true, enables strict error reporting; if set to false, allows expressions to be evaluated as null
     * - <code>silent</code> : If set to true, suppresses JexlExceptions; if set to false, allows them
     * </p>
     */
    private void createJexlEngine() {
        this.jexlEngine = new JexlBuilder().strict(true).silent(false).create();

        LOGGER.info("Validator: Expression evaluator service initiated");
    }

    /**
     * {@inheritDoc}
     *
     * @param expressionContextHolder Represents the {@link ExpressionContextHolder}
     * @return true, if the expression matches to the given condition
     */
    @Override
    public boolean evaluate(final ExpressionContextHolder expressionContextHolder) {
        final JexlContext jexlContext = new MapContext(expressionContextHolder.context());

        try {
            final JexlExpression jexlExpression = jexlEngine.createExpression(expressionContextHolder.expression().toString());

            return (Boolean) jexlExpression.evaluate(jexlContext);
        } catch (JexlException exception) {
            throw ErrorCode.get(ValidatorErrorCodes.EXPRESSION_EVALUATION_FAILED);
        }
    }
}
