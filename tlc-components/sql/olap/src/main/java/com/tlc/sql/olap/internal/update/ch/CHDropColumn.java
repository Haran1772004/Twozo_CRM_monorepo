package com.tlc.sql.olap.internal.update.ch;

import com.tlc.sql.common.api.ddl.update.impl.DropColumn;
import com.tlc.sql.olap.api.meta.OlapColumnDefinition;
import com.tlc.sql.olap.internal.handler.ch.CHDDLHandler;
import com.tlc.sql.olap.internal.meta.ch.CHTableDefinition;

public class CHDropColumn extends DropColumn<OlapColumnDefinition, CHTableDefinition, CHDDLHandler> {

    public CHDropColumn(final CHTableDefinition tableDefinition, final OlapColumnDefinition columnDefinition) {
        super(tableDefinition, columnDefinition);
    }
}
