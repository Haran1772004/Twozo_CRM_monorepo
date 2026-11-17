package com.tlc.sql.common.api.dml.function;

import com.tlc.sql.common.api.dml.Argument;
import com.tlc.sql.common.api.dml.Column;
import lombok.Getter;

@Getter
public class FunctionArgument implements Argument {

    private final SqlFunction function;

    private FunctionArgument(final SqlFunction function) {
        this.function = function;
    }

    public static FunctionArgument create(final SqlFunction function) {
        return new FunctionArgument(function);
    }

    @Override
    public String resolve(final java.util.function.Function<Column, String> columnResolver) {
        return function.toString(columnResolver);
    }
}