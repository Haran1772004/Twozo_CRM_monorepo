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
public class AddForeignKey implements AdvDDLAction<OltpColumnDefinition, OltpTableDefinition, OltpDDLHandler> {
    private final FKDefinition fkDefinition;
    private final OltpTableDefinition tableDefinition;

    public AddForeignKey(OltpTableDefinition tableDefinition, FKDefinition fkDefinition) {
        this.fkDefinition = Objects.requireNonNull(fkDefinition);
        this.tableDefinition = Objects.requireNonNull(tableDefinition);
    }

    @Override
    public String[] getRevertSQL(final OltpDDLHandler ddlHandler) {
        return new String[]{ddlHandler.getDropForeignKeySQL(tableDefinition, fkDefinition)};
    }

    @Override
    public String[] getSQL(final OltpDDLHandler ddlHandler) {
        return new String[]{ddlHandler.getAddForeignKeySQL(tableDefinition, fkDefinition)};
    }
}