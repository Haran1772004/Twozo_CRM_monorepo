package com.tlc.sql.oltp.internal.update;

import com.tlc.sql.common.api.ddl.update.impl.UpdateColumnNotNullable;
import com.tlc.sql.oltp.api.meta.OltpColumnDefinition;
import com.tlc.sql.oltp.api.meta.OltpTableDefinition;
import com.tlc.sql.oltp.internal.handler.OltpDDLHandler;

public class OltpUpdateColumnNotNullable extends UpdateColumnNotNullable<OltpColumnDefinition, OltpTableDefinition, OltpDDLHandler> {

    public OltpUpdateColumnNotNullable(final OltpTableDefinition tableDefinition, final OltpColumnDefinition columnDefinition) {
        super(tableDefinition, columnDefinition);
    }
}