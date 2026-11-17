package com.tlc.sql.oltp.internal.update;

import com.tlc.sql.common.api.ddl.update.impl.IncreaseDecimalColumnWeight;
import com.tlc.sql.oltp.api.meta.OltpColumnDefinition;
import com.tlc.sql.oltp.api.meta.OltpTableDefinition;
import com.tlc.sql.oltp.internal.handler.OltpDDLHandler;

public class OltpIncreaseDecimalColumnWeight extends IncreaseDecimalColumnWeight<OltpColumnDefinition, OltpTableDefinition, OltpDDLHandler> {

    public OltpIncreaseDecimalColumnWeight(final OltpTableDefinition tableDefinition, final OltpColumnDefinition columnDefinition, final OltpColumnDefinition oldC) {
        super(tableDefinition, columnDefinition, oldC);
    }
}