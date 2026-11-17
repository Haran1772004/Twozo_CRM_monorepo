package com.tlc.sql.common.api.dml.function;

import com.tlc.sql.common.api.dml.Argument;
import com.tlc.sql.common.api.dml.Column;
import lombok.Getter;

import java.util.Objects;
import java.util.function.Function;

@Getter
public class SimpleArgument implements Argument {

    private final Column column;
    private final ScalarValue value;

    private SimpleArgument(final Column column, final ScalarValue value) {
        this.column = column;
        this.value = value;
    }

    public static SimpleArgument of(final ScalarValue value) {
        return new SimpleArgument(null, value);
    }

    public static SimpleArgument of(final Column column) {
        return new SimpleArgument(column, null);
    }

    public boolean isColumn() {
        return Objects.nonNull(column);
    }

    @Override
    public String resolve(final Function<Column, String> resolver) {

        if (isColumn()) {
            return resolver.apply(column);
        } else {
            return value.toString();
        }
    }
}