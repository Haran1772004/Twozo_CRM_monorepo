package com.tlc.sql.oltp.internal.update;

import com.tlc.sql.common.api.ddl.update.impl.DropTable;
import com.tlc.sql.common.api.meta.PKDefinition;
import com.tlc.sql.common.api.meta.SequenceDefinition;
import com.tlc.sql.oltp.api.meta.OltpColumnDefinition;
import com.tlc.sql.oltp.api.meta.OltpPKDefinition;
import com.tlc.sql.oltp.api.meta.OltpTableDefinition;
import com.tlc.sql.oltp.internal.handler.OltpDDLHandler;

public class OltpDropTable extends DropTable<OltpColumnDefinition, OltpTableDefinition, OltpDDLHandler> {

    public OltpDropTable(final OltpTableDefinition tableDefinition) {
        super(tableDefinition);
    }

    @Override
    protected String getDropSequenceSQL(final OltpDDLHandler ddlHandler, final PKDefinition pkDefinition) {
        final OltpPKDefinition oltpPKDefinition = (OltpPKDefinition) pkDefinition;
        final SequenceDefinition sequenceDefinition = oltpPKDefinition.getSequenceDefinition();
        final String sequenceName = sequenceDefinition.getSequenceName();

        return ddlHandler.getDropSequenceSQL(sequenceName);
    }
}