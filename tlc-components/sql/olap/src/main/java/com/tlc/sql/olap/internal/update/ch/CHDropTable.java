package com.tlc.sql.olap.internal.update.ch;

import com.tlc.sql.common.api.ddl.update.impl.DropTable;
import com.tlc.sql.common.api.meta.PKDefinition;
import com.tlc.sql.olap.api.meta.OlapColumnDefinition;
import com.tlc.sql.olap.internal.handler.ch.CHDDLHandler;
import com.tlc.sql.olap.internal.meta.ch.CHTableDefinition;

public class CHDropTable extends DropTable<OlapColumnDefinition, CHTableDefinition, CHDDLHandler> {

    public CHDropTable(final CHTableDefinition tableDefinition) {
        super(tableDefinition);
    }

    @Override
    protected String getDropSequenceSQL(final CHDDLHandler ddlHandler, final PKDefinition pkDefinition) {
        return "";
    }
}