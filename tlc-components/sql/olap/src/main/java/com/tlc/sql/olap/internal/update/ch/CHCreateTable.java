package com.tlc.sql.olap.internal.update.ch;

import com.tlc.sql.common.api.ddl.update.impl.CreateTable;
import com.tlc.sql.olap.api.meta.OlapColumnDefinition;
import com.tlc.sql.olap.internal.handler.ch.CHDDLHandler;
import com.tlc.sql.olap.internal.meta.ch.CHTableDefinition;

public class CHCreateTable extends CreateTable<OlapColumnDefinition, CHTableDefinition, CHDDLHandler> {

    public CHCreateTable(final CHTableDefinition tableDefinition) {
        super(tableDefinition);
    }
}