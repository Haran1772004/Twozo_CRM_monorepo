package com.tlc.sql.common.api.dml.function;

public record StringValue(String string) implements ScalarValue {
    @Override
    public String toString() {
        return String.format("'%s'", string);
    }
}
