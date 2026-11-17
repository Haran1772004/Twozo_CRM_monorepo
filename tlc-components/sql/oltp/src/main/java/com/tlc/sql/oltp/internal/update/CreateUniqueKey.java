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
public class CreateUniqueKey implements AdvDDLAction<OltpColumnDefinition, OltpTableDefinition, OltpDDLHandler> {

    private final UKDefinition ukDefinition;
    private final OltpTableDefinition tableDefinition;

    public CreateUniqueKey(OltpTableDefinition tableDefinition, UKDefinition ukDefinition) {
        this.ukDefinition = Objects.requireNonNull(ukDefinition);
        this.tableDefinition = Objects.requireNonNull(tableDefinition);
    }

    @Override
    public String[] getRevertSQL(final OltpDDLHandler ddlHandler) {
        final String sql = ddlHandler.getDropUniqueKeySQL(tableDefinition, ukDefinition);
        return new String[]{sql};
    }

    @Override
    public String[] getSQL(final OltpDDLHandler ddlHandler) {
        final String sql = ddlHandler.getAddUniqueKeySQL(tableDefinition, ukDefinition);
        return new String[]{sql};
    }
}