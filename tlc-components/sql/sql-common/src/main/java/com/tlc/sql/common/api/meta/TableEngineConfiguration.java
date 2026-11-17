package com.tlc.sql.common.api.meta;

import lombok.Getter;

@Getter
public class TableEngineConfiguration {

    private final String name;

    public TableEngineConfiguration(final String name) {
        this.name = name;
    }
}