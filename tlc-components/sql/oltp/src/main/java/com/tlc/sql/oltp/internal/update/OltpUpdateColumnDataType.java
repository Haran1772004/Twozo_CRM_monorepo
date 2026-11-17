package com.tlc.sql.oltp.internal.update;

import com.tlc.sql.common.api.ddl.update.impl.UpdateColumnDataType;
import com.tlc.sql.oltp.api.meta.OltpColumnDefinition;
import com.tlc.sql.oltp.api.meta.OltpTableDefinition;
import com.tlc.sql.oltp.internal.handler.OltpDDLHandler;

public class OltpUpdateColumnDataType extends UpdateColumnDataType<OltpColumnDefinition, OltpTableDefinition, OltpDDLHandler> {

    public OltpUpdateColumnDataType(final OltpTableDefinition tableDefinition, final OltpColumnDefinition oldC, final OltpColumnDefinition columnDefinition) {
        super(tableDefinition, oldC, columnDefinition);
    }
}