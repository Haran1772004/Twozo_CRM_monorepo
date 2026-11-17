package com.tlc.sql.oltp.internal.update;

import com.tlc.sql.oltp.api.meta.OltpColumnDefinition;
import com.tlc.sql.oltp.api.meta.OltpTableDefinition;
import com.tlc.sql.oltp.internal.handler.OltpDDLHandler;
import com.tlc.sql.common.ddl.AdvDDLAction;
import com.tlc.sql.common.api.meta.UKDefinition;

import java.util.Objects;

/**
 * @author Abishek
 * @version 1.0
 */
public class UpdateUniqueKey implements AdvDDLAction<OltpColumnDefinition, OltpTableDefinition, OltpDDLHandler> {

    private final UKDefinition oldUkDefinition;
    private final UKDefinition ukDefinition;
    private final OltpTableDefinition tableDefinition;

    public UpdateUniqueKey(OltpTableDefinition tableDefinition, UKDefinition oldUkDefinition, UKDefinition ukDefinition) {
        this.ukDefinition = Objects.requireNonNull(ukDefinition);
        this.oldUkDefinition = Objects.requireNonNull(oldUkDefinition);
        this.tableDefinition = Objects.requireNonNull(tableDefinition);
    }

    @Override
    public String[] getRevertSQL(final OltpDDLHandler ddlHandler) {
        return ddlHandler.getUpdateUniqueKeySQL(tableDefinition, oldUkDefinition);
    }

    @Override
    public String[] getSQL(final OltpDDLHandler ddlHandler) {
        return ddlHandler.getUpdateUniqueKeySQL(tableDefinition, ukDefinition);
    }
}