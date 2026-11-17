package com.tlc.sql.olap.internal.update.ch;

import com.tlc.sql.common.api.ddl.update.impl.UpdateColumnDataType;
import com.tlc.sql.olap.api.meta.OlapColumnDefinition;
import com.tlc.sql.olap.internal.handler.ch.CHDDLHandler;
import com.tlc.sql.olap.internal.meta.ch.CHTableDefinition;

public class CHUpdateColumnDataType extends UpdateColumnDataType<OlapColumnDefinition, CHTableDefinition, CHDDLHandler> {

    public CHUpdateColumnDataType(final CHTableDefinition tableDefinition, final OlapColumnDefinition oldC, final OlapColumnDefinition columnDefinition) {
        super(tableDefinition, oldC, columnDefinition);
    }
}