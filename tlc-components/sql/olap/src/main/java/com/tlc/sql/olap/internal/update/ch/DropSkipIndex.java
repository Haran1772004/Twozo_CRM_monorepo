package com.tlc.sql.olap.internal.update.ch;

import com.tlc.sql.common.ddl.AdvDDLAction;
import com.tlc.sql.olap.api.meta.OlapColumnDefinition;
import com.tlc.sql.olap.internal.handler.ch.CHDDLHandler;
import com.tlc.sql.olap.internal.meta.ch.CHTableDefinition;
import com.tlc.sql.olap.internal.meta.ch.SkipIndexDefinition;

public class DropSkipIndex implements AdvDDLAction<OlapColumnDefinition, CHTableDefinition, CHDDLHandler> {

    private final CHTableDefinition tableDefinition;
    private final SkipIndexDefinition skipIndexDefinition;

    public DropSkipIndex(final CHTableDefinition tableDefinition, final SkipIndexDefinition skipIndexDefinition) {
        this.tableDefinition = tableDefinition;
        this.skipIndexDefinition = skipIndexDefinition;
    }

    @Override
    public String[] getRevertSQL(final CHDDLHandler ddlHandler) {
        return new String[] {ddlHandler.getCreateSkipIndexSql(tableDefinition, skipIndexDefinition)};
    }

    @Override
    public String[] getSQL(final CHDDLHandler ddlHandler) {
        return new String[] {ddlHandler.getDropSkipIndexSql(tableDefinition, skipIndexDefinition)};
    }
}
