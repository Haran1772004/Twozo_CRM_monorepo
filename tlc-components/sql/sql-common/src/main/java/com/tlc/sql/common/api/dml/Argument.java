package com.tlc.sql.common.api.dml;

import java.util.function.Function;

@FunctionalInterface
public interface Argument {

    String resolve(final Function<Column, String> columnResolver);
}
