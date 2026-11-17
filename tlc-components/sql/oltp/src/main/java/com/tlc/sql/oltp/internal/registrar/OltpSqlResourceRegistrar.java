package com.tlc.sql.oltp.internal.registrar;

import com.tlc.commons.util.Configurations;
import com.tlc.sql.common.api.dml.update.DMLUpdateManager;
import com.tlc.sql.oltp.api.meta.OltpColumnDefinition;
import com.tlc.sql.oltp.api.meta.OltpTableDefinition;
import com.tlc.sql.oltp.internal.meta.OltpMetaDataHandler;
import com.tlc.sql.oltp.internal.update.OltpDDLUpdateManager;

public class OltpSqlResourceRegistrar extends com.tlc.sql.common.registrar.SqlResourceRegistrar<OltpColumnDefinition, OltpTableDefinition> {

    public OltpSqlResourceRegistrar(final Configurations sqlResourceConfig, final OltpMetaDataHandler metaDataHandler,
                                    final OltpDDLUpdateManager ddlUpdateManager, final DMLUpdateManager dmlUpdateManager) {
        super(sqlResourceConfig, metaDataHandler, ddlUpdateManager, dmlUpdateManager);
    }

    @Override
    protected boolean isStaticDataAllowed() {
        return true;
    }
}