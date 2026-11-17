package com.tlc.sql.oltp.internal.update;

import com.tlc.sql.oltp.api.meta.OltpColumnDefinition;
import com.tlc.sql.oltp.api.meta.OltpTableDefinition;
import com.tlc.sql.oltp.internal.handler.OltpDDLHandler;
import com.tlc.sql.common.ddl.AdvDDLAction;

import java.util.Objects;

/**
 * @author Abishek
 * @version 1.0
 */
public class IncreaseColumnLength implements AdvDDLAction<OltpColumnDefinition, OltpTableDefinition, OltpDDLHandler> {

    private final OltpTableDefinition tableDefinition;
    private final OltpColumnDefinition columnDefinition;
    private final OltpColumnDefinition oldColumnDef;

    public IncreaseColumnLength(OltpTableDefinition tableDefinition, OltpColumnDefinition oldColumnDef, OltpColumnDefinition columnDefinition) {
        this.columnDefinition = Objects.requireNonNull(columnDefinition);
        this.oldColumnDef = Objects.requireNonNull(oldColumnDef);
        this.tableDefinition = Objects.requireNonNull(tableDefinition);
    }

    @Override
    public String[] getRevertSQL(final OltpDDLHandler ddlHandler) {
        return ddlHandler.getUpdateColumnLengthSQL(tableDefinition, oldColumnDef);
    }

    @Override
    public String[] getSQL(final OltpDDLHandler ddlHandler) {
        return ddlHandler.getUpdateColumnLengthSQL(tableDefinition, columnDefinition);
    }
}