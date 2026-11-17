package com.tlc.sql.oltp.internal.update;

import com.tlc.sql.common.api.ddl.update.impl.CreateTable;
import com.tlc.sql.oltp.api.meta.OltpColumnDefinition;
import com.tlc.sql.oltp.api.meta.OltpTableDefinition;
import com.tlc.sql.oltp.internal.handler.OltpDDLHandler;

public class OltpCreateTable extends CreateTable<OltpColumnDefinition, OltpTableDefinition, OltpDDLHandler> {

    public OltpCreateTable(final OltpTableDefinition tableDefinition) {
        super(tableDefinition);
    }
}
