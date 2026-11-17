package com.tlc.sql.olap.internal.meta.engine.ch;

public class MergeTree implements CHTableEngine {

    private static final String NAME = "MergeTree";

    @Override
    public String getName() {
        return NAME;
    }
}