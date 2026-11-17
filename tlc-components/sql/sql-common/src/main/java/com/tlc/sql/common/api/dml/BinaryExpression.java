package com.tlc.sql.common.api.dml;

import com.tlc.commons.code.ErrorCode;
import com.tlc.sql.common.internal.status.SQLErrorCodes;

import java.util.function.Function;

public class BinaryExpression implements Expression {

    private static final String EXPRESSION_FORMAT = "(%s %s %s)";

    private final Argument operand1;
    private final Argument operand2;
    private final Operator operator;

    private BinaryExpression(final Argument operand1, final Argument operand2, final Operator operator) {
        if (!operator.isBinary()) {
            throw ErrorCode.get(SQLErrorCodes.UNSUPPORTED_OPERATOR);
        }
        this.operand1 = operand1;
        this.operand2 = operand2;
        this.operator = operator;
    }

    public static BinaryExpression create(final Argument operand1, final Operator operator, final Argument operand2) {
        return new BinaryExpression(operand1, operand2, operator);
    }

    @Override
    public Operator getOperator() {
        return operator;
    }

    @Override
    public String resolve(final Function<Column, String> columnResolver) {
        return String.format(EXPRESSION_FORMAT, operand1.resolve(columnResolver), operator.toString(), operand2.resolve(columnResolver));
    }
}

