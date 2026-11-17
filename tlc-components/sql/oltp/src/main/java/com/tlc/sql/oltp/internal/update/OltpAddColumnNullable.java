package com.tlc.sql.oltp.internal.update;

import com.tlc.sql.common.api.ddl.update.impl.AddColumnNullable;
import com.tlc.sql.oltp.api.meta.OltpColumnDefinition;
import com.tlc.sql.oltp.api.meta.OltpTableDefinition;
import com.tlc.sql.oltp.internal.handler.OltpDDLHandler;

public class OltpAddColumnNullable extends AddColumnNullable<OltpColumnDefinition, OltpTableDefinition, OltpDDLHandler> {

    public OltpAddColumnNullable(final OltpTableDefinition tableDefinition, final OltpColumnDefinition columnDefinition) {
        super(tableDefinition, columnDefinition);
    }
}
