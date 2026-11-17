package com.tlc.sql.olap.internal.meta.ch;

import lombok.Getter;

@Getter
public class SetIndex extends SkipIndexDefinition {

    private final int maxDistinctValues;

    public SetIndex(final String name, final String columnName, final int maxDistinctValues) {
        super(name, columnName);
        this.maxDistinctValues = maxDistinctValues;
    }

    public SetIndex(final String name, final String columnName, final int maxDistinctValues, final short granularity) {
        super(name, columnName, granularity);
        this.maxDistinctValues = maxDistinctValues;
    }

    @Override
    public String getSkipIndexExpression() {
        return String.format("set(%d) GRANULARITY %d", maxDistinctValues, granularity);
    }
}