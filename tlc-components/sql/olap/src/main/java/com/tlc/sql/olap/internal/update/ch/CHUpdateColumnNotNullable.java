package com.tlc.sql.olap.internal.update.ch;

import com.tlc.sql.common.api.ddl.update.impl.UpdateColumnNotNullable;
import com.tlc.sql.olap.api.meta.OlapColumnDefinition;
import com.tlc.sql.olap.internal.handler.ch.CHDDLHandler;
import com.tlc.sql.olap.internal.meta.ch.CHTableDefinition;

public class CHUpdateColumnNotNullable extends UpdateColumnNotNullable<OlapColumnDefinition, CHTableDefinition, CHDDLHandler> {

    public CHUpdateColumnNotNullable(final CHTableDefinition tableDefinition, final OlapColumnDefinition columnDefinition) {
        super(tableDefinition, columnDefinition);
    }
}