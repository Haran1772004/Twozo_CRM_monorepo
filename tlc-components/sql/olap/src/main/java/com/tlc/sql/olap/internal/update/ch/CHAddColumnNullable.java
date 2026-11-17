package com.tlc.sql.olap.internal.update.ch;

import com.tlc.sql.common.api.ddl.update.impl.AddColumnNullable;
import com.tlc.sql.olap.api.meta.OlapColumnDefinition;
import com.tlc.sql.olap.internal.handler.ch.CHDDLHandler;
import com.tlc.sql.olap.internal.meta.ch.CHTableDefinition;

public class CHAddColumnNullable extends AddColumnNullable<OlapColumnDefinition, CHTableDefinition, CHDDLHandler> {

    public CHAddColumnNullable(final CHTableDefinition tableDefinition, final OlapColumnDefinition columnDefinition) {
        super(tableDefinition, columnDefinition);
    }
}
