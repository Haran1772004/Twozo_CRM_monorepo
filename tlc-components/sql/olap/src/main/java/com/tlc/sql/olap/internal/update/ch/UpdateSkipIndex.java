package com.tlc.sql.olap.internal.update.ch;

import com.tlc.sql.common.ddl.AdvDDLAction;
import com.tlc.sql.olap.api.meta.OlapColumnDefinition;
import com.tlc.sql.olap.internal.handler.ch.CHDDLHandler;
import com.tlc.sql.olap.internal.meta.ch.CHTableDefinition;
import com.tlc.sql.olap.internal.meta.ch.SkipIndexDefinition;

public class UpdateSkipIndex implements AdvDDLAction<OlapColumnDefinition, CHTableDefinition, CHDDLHandler> {

    private final CHTableDefinition tableDefinition;
    private final SkipIndexDefinition oldSkipIndexDefinition;
    private final SkipIndexDefinition newSkipIndexDefinition;

    public UpdateSkipIndex(final CHTableDefinition tableDefinition, final SkipIndexDefinition oldSkipIndexDefinition,
                           final SkipIndexDefinition newSkipIndexDefinition) {
        this.tableDefinition = tableDefinition;
        this.oldSkipIndexDefinition = oldSkipIndexDefinition;
        this.newSkipIndexDefinition = newSkipIndexDefinition;
    }

    @Override
    public String[] getRevertSQL(final CHDDLHandler ddlHandler) {
        return ddlHandler.getUpdateSkipIndexSql(tableDefinition, oldSkipIndexDefinition);
    }

    @Override
    public String[] getSQL(final CHDDLHandler ddlHandler) {
        return ddlHandler.getUpdateSkipIndexSql(tableDefinition, newSkipIndexDefinition);
    }
}
