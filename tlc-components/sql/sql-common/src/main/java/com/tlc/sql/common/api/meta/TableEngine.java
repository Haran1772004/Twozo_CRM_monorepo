package com.tlc.sql.common.api.meta;

public interface TableEngine {

    DatabaseType getDatabaseType();

    String getName();
}