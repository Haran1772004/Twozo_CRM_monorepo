package com.tlc.sql.oltp.internal.update;

import com.tlc.sql.common.api.ddl.update.impl.DropColumn;
import com.tlc.sql.oltp.api.meta.OltpColumnDefinition;
import com.tlc.sql.oltp.api.meta.OltpTableDefinition;
import com.tlc.sql.oltp.internal.handler.OltpDDLHandler;

public class OltpDropColumn extends DropColumn<OltpColumnDefinition, OltpTableDefinition, OltpDDLHandler> {

    public OltpDropColumn(final OltpTableDefinition tableDefinition, final OltpColumnDefinition columnDefinition) {
        super(tableDefinition, columnDefinition);
    }
}
