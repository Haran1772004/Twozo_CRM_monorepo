package com.tlc.sql.oltp.internal.update;

import com.tlc.sql.oltp.api.meta.OltpColumnDefinition;
import com.tlc.sql.oltp.api.meta.OltpTableDefinition;
import com.tlc.sql.oltp.internal.handler.OltpDDLHandler;
import com.tlc.sql.common.ddl.AdvDDLAction;
import com.tlc.sql.common.api.meta.IndexDefinition;

import java.util.Objects;

/**
 * @author Abishek
 * @version 1.0
 */
public class CreateIndex implements AdvDDLAction<OltpColumnDefinition, OltpTableDefinition, OltpDDLHandler> {

    private final OltpTableDefinition tableDefinition;
    private final IndexDefinition indexDefinition;

    public CreateIndex(OltpTableDefinition tableDefinition, IndexDefinition indexDefinition) {
        this.indexDefinition = Objects.requireNonNull(indexDefinition);
        this.tableDefinition = Objects.requireNonNull(tableDefinition);
    }

    @Override
    public String[] getRevertSQL(final OltpDDLHandler ddlHandler) {
        final String sql = ddlHandler.getDropIndexSQL(tableDefinition, indexDefinition);
        return new String[]{sql};
    }

    @Override
    public String[] getSQL(final OltpDDLHandler ddlHandler) {
        return new String[]{ddlHandler.getAddIndexSQL(tableDefinition, indexDefinition)};
    }
}