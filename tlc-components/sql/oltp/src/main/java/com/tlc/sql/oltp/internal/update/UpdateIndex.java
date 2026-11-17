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
public class UpdateIndex implements AdvDDLAction<OltpColumnDefinition, OltpTableDefinition, OltpDDLHandler> {

    private final OltpTableDefinition tableDefinition;
    private final IndexDefinition oldIndexDefinition;
    private final IndexDefinition indexDefinition;

    public UpdateIndex(OltpTableDefinition tableDefinition, IndexDefinition oldIndexDefinition, IndexDefinition indexDefinition) {
        this.tableDefinition = Objects.requireNonNull(tableDefinition);
        this.oldIndexDefinition = Objects.requireNonNull(oldIndexDefinition);
        this.indexDefinition = Objects.requireNonNull(indexDefinition);
    }

    @Override
    public String[] getRevertSQL(final OltpDDLHandler ddlHandler) {
        return ddlHandler.getUpdateIndexSQL(tableDefinition, oldIndexDefinition);
    }

    @Override
    public String[] getSQL(final OltpDDLHandler ddlHandler) {
        return ddlHandler.getUpdateIndexSQL(tableDefinition, indexDefinition);
    }
}