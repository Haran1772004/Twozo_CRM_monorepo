package com.tlc.sql.olap.internal.update.ch;

import com.tlc.sql.common.api.ddl.update.impl.IncreaseDecimalColumnWeight;
import com.tlc.sql.olap.api.meta.OlapColumnDefinition;
import com.tlc.sql.olap.internal.handler.ch.CHDDLHandler;
import com.tlc.sql.olap.internal.meta.ch.CHTableDefinition;

public class CHIncreaseDecimalColumnWeight extends IncreaseDecimalColumnWeight<OlapColumnDefinition, CHTableDefinition, CHDDLHandler> {

    public CHIncreaseDecimalColumnWeight(final CHTableDefinition tableDefinition, final OlapColumnDefinition columnDefinition, final OlapColumnDefinition oldC) {
        super(tableDefinition, columnDefinition, oldC);
    }
}