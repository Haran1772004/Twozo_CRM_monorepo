package com.tlc.sql.olap.internal.registrar;

import com.tlc.commons.util.Configurations;
import com.tlc.sql.common.registrar.SqlResourceRegistrar;
import com.tlc.sql.olap.api.meta.OlapColumnDefinition;
import com.tlc.sql.olap.internal.meta.CHMetaDataHandler;
import com.tlc.sql.olap.internal.meta.ch.CHTableDefinition;
import com.tlc.sql.olap.internal.update.ch.CHDDLUpdateManager;
import com.tlc.sql.olap.internal.update.ch.CHDMLUpdateManager;

public class CHSqlResourceRegistrar extends SqlResourceRegistrar<OlapColumnDefinition, CHTableDefinition> {

    public CHSqlResourceRegistrar(final Configurations sqlResourceConfig, final CHMetaDataHandler metaDataHandler,
                                  final CHDDLUpdateManager ddlUpdateManager, final CHDMLUpdateManager dmlUpdateManager) {
        super(sqlResourceConfig, metaDataHandler, ddlUpdateManager, dmlUpdateManager);
    }

    @Override
    protected boolean isStaticDataAllowed() {
        return false;
    }
}