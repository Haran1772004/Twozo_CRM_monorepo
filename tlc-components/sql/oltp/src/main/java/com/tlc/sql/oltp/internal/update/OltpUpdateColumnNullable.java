package com.tlc.sql.oltp.internal.update;

import com.tlc.sql.common.api.ddl.update.impl.UpdateColumnNullable;
import com.tlc.sql.oltp.api.meta.OltpColumnDefinition;
import com.tlc.sql.oltp.api.meta.OltpTableDefinition;
import com.tlc.sql.oltp.internal.handler.OltpDDLHandler;

public class OltpUpdateColumnNullable extends UpdateColumnNullable<OltpColumnDefinition, OltpTableDefinition, OltpDDLHandler> {

    public OltpUpdateColumnNullable(final OltpTableDefinition tableDefinition, final OltpColumnDefinition columnDefinition) {
        super(tableDefinition, columnDefinition);
    }
}