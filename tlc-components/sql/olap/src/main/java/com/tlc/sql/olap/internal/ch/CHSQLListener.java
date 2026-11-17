package com.tlc.sql.olap.internal.ch;

import com.tlc.sql.common.SQLDevListener;
import com.tlc.sql.olap.api.meta.OlapColumnDefinition;
import com.tlc.sql.olap.internal.meta.CHMetaDataHandler;
import com.tlc.sql.olap.internal.meta.ch.CHTableDefinition;
import com.tlc.sql.olap.internal.registrar.CHSqlResourceRegistrar;

public class CHSQLListener extends SQLDevListener<OlapColumnDefinition, CHTableDefinition, CHSqlResourceRegistrar, CHMetaDataHandler> {

    public CHSQLListener(final CHMetaDataHandler metaDataHandler, final CHSqlResourceRegistrar sqlRegistrar) {
        super(metaDataHandler, sqlRegistrar);
    }
}