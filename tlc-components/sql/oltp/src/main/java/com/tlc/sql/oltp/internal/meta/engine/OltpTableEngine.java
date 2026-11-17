package com.tlc.sql.oltp.internal.meta.engine;

import com.tlc.sql.common.api.meta.TableEngine;
import com.tlc.sql.common.api.meta.DatabaseType;

public interface OltpTableEngine extends TableEngine {

    @Override
    default DatabaseType getDatabaseType() {
        return DatabaseType.OLTP;
    }
}