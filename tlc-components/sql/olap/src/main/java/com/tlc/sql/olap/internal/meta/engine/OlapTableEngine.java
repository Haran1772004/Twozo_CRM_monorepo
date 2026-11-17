package com.tlc.sql.olap.internal.meta.engine;

import com.tlc.sql.common.api.meta.DatabaseType;
import com.tlc.sql.common.api.meta.TableEngine;

public interface OlapTableEngine extends TableEngine {

    @Override
    default DatabaseType getDatabaseType() {
        return DatabaseType.OLAP;
    }
}