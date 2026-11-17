package com.tlc.sql.common.api.dml;

import com.tlc.commons.code.ErrorCode;
import com.tlc.sql.common.internal.status.SQLErrorCodes;

import java.util.function.Function;

public class UnaryExpression implements Expression {

    private static final String EXPRESSION_FORMAT = "(%s%s)";

    private final Argument operand;
    private final Operator operator;
    private final boolean isPost;

    public UnaryExpression(final Argument operand, final Operator operator, final boolean isPost) {
        if (!operator.isUnary()) {
            throw ErrorCode.get(SQLErrorCodes.UNSUPPORTED_OPERATOR);
        }
        this.operand = operand;
        this.operator = operator;
        this.isPost = isPost;
    }

    public static UnaryExpression create(final Argument operand, final Operator operator, final boolean isPost) {
        return new UnaryExpression(operand, operator, isPost);
    }

    public static UnaryExpression create(final Argument operand, final Operator operator) {
        return new UnaryExpression(operand, operator, true);
    }

    @Override
    public Operator getOperator() {
        return operator;
    }

    @Override
    public String resolve(final Function<Column, String> columnResolver) {
        final String resolvedArgument = operand.resolve(columnResolver);
        final String operatorString = operator.toString();

        if (isPost) {
            return String.format(EXPRESSION_FORMAT, resolvedArgument, operatorString);
        } else {
            return String.format(EXPRESSION_FORMAT, operatorString, resolvedArgument);
        }
    }
}

