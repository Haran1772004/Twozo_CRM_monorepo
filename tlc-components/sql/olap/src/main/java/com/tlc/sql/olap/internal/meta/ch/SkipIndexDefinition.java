package com.tlc.sql.olap.internal.meta.ch;

import com.tlc.commons.util.constants.NumericalConstant;
import lombok.Getter;

@Getter
public abstract class SkipIndexDefinition {

    final short granularity;

    private final String name;
    private final String columnName;

    protected SkipIndexDefinition(final String name, final String columnName) {
        this(name, columnName, (short) NumericalConstant.ONE);
    }

    protected SkipIndexDefinition(final String name, final String columnName, final short granularity) {
        this.name = name;
        this.granularity = granularity;
        this.columnName = columnName;
    }

    public abstract String getSkipIndexExpression();
}