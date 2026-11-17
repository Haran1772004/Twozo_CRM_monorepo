package com.tlc.sql.common.api.dml.function;

public record NumberValue(Number number) implements ScalarValue {
    @Override
    public String toString() {
        return String.valueOf(number);
    }
}
