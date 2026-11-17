package com.tlc.sql.common.api.meta;

import java.util.Objects;


public enum ImmutableColumns
{
    ORG_ID("ORG_ID"),
    ID("ID");

    private final String name;
    ImmutableColumns(String name)
    {
        this.name = Objects.requireNonNull(name);
    }

    public String getName()
    {
        return name;
    }
}
