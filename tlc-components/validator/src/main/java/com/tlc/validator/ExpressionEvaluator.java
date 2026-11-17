package com.tlc.validator;

import com.tlc.validator.expression.model.ExpressionContextHolder;

import java.util.Collection;

/**
 * An evaluator which evaluates the expression with provided context.
 * <br/>
 * <span style="color:red"><b>
 * <ul>
 *     <li>
 *         Provides an empty {@link Collection} in case of multi value field.
 *     </li>
 *     <li>
 *         Provides an empty " " {@link String} in case of string field.
 *     </li>
 *     <li>
 *         {@link Number} must hold null in case of number field.
 *     </li>
 * </ul>
 * </b></span>
 * <br/>
 * <span style="color:orange"><b>For Reference:</b></span>
 * <ul>
 *     <li> <b>Correct Expression:</b>
 *         <ul>
 *             <li>
 *                 <b>{@link String}: ((firstName.equals('Karl') && lastName.endsWith('Marx')) || (country.contains('Italy') && city.equals('Rome')))</b>
 *             </li>
 *             <li>
 *                 <b>{@link Number}: ((amount == 50 && amount >= 10) && (amount <= 80 && amount != null))</b>
 *             </li>
 *             <li>
 *                 <b>{@link Collection}: (list.containsAll(values) && list.isEmpty)</b>
 *             </li>
 *         </ul>
 *    </li>
 *    <li> <b>Incorrect Expression:</b>
 *        <ul>
 *            <li>
 *                <b>{@link String}: ((firstName.equals(Karl) && lastName.endsWith('"arx"')) || (null.contains('Italy') && null.equals('Rome')))</b>
 *            </li>
 *            <li>
 *                <b>{@link Number}: ((amount.equals('50') && amount.contains(10)) && (amount <<< 80 && null != 700))</b>
 *            </li>
 *            <li>
 *                <b>{@link Collection}: (list.containsAll('values') && null.isEmpty)</b>
 *            </li>
 *        </ul>
 *    </li>
 * </ul>
 *
 * @author Kavinila E
 * @version 1.0
 */
public interface ExpressionEvaluator {

    /**
     * <p>
     * Evaluates the expression by comparing the context in the {@link ExpressionContextHolder}
     * </p>
     *
     * @param expressionContextHolder Represents the {@link ExpressionContextHolder}
     * @return true, if the expression matches to the given context
     */
    boolean evaluate(final ExpressionContextHolder expressionContextHolder);
}
