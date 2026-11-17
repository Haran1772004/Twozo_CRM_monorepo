package com.tlc.sql.olap.internal.meta.ch;

public class MinmaxIndex extends SkipIndexDefinition {

    public MinmaxIndex(final String name, final String columnName) {
        super(name, columnName);
    }

    @Override
    public String getSkipIndexExpression() {
        return String.format("minmax GRANULARITY %d", granularity);
    }

    public MinmaxIndex(final String name, final short granularity, final String columnName) {
        super(name, columnName, granularity);
    }
}