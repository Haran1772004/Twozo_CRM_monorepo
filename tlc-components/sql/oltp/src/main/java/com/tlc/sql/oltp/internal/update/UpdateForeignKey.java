package com.tlc.sql.oltp.internal.update;

import com.tlc.sql.oltp.api.meta.OltpColumnDefinition;
import com.tlc.sql.oltp.api.meta.OltpTableDefinition;
import com.tlc.sql.oltp.internal.handler.OltpDDLHandler;
import com.tlc.sql.common.ddl.AdvDDLAction;
import com.tlc.sql.common.api.meta.FKDefinition;

import java.util.Objects;

/**
 * @author Abishek
 * @version 1.0
 */
public class UpdateForeignKey implements AdvDDLAction<OltpColumnDefinition, OltpTableDefinition, OltpDDLHandler> {

    private final OltpTableDefinition tableDefinition;
    private final FKDefinition oldFkDefinition;
    private final FKDefinition fkDefinition;

    public UpdateForeignKey(OltpTableDefinition tableDefinition, FKDefinition oldFkDefinition, FKDefinition fkDefinition) {
        this.tableDefinition = Objects.requireNonNull(tableDefinition);
        this.oldFkDefinition = Objects.requireNonNull(oldFkDefinition);
        this.fkDefinition = Objects.requireNonNull(fkDefinition);
    }

    @Override
    public String[] getRevertSQL(final OltpDDLHandler ddlHandler) {
        return ddlHandler.getUpdateForeignKeySQL(tableDefinition, oldFkDefinition);
    }

    @Override
    public String[] getSQL(final OltpDDLHandler ddlHandler) {
        return ddlHandler.getUpdateForeignKeySQL(tableDefinition, fkDefinition);
    }
}