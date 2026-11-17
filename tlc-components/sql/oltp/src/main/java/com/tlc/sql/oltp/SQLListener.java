package com.tlc.sql.oltp;

import com.tlc.sql.common.SQLDevListener;
import com.tlc.sql.oltp.api.meta.OltpColumnDefinition;
import com.tlc.sql.oltp.api.meta.OltpTableDefinition;
import com.tlc.sql.oltp.internal.meta.OltpMetaDataHandler;
import com.tlc.sql.oltp.internal.registrar.OltpSqlResourceRegistrar;

public class SQLListener extends SQLDevListener<OltpColumnDefinition, OltpTableDefinition, OltpSqlResourceRegistrar, OltpMetaDataHandler> {

    public SQLListener(final OltpMetaDataHandler metaDataHandler, final OltpSqlResourceRegistrar sqlRegistrar) {
        super(metaDataHandler, sqlRegistrar);
    }
}