package com.tlc.sql.oltp;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.util.Configurations;
import com.tlc.sql.common.SQLService;
import com.tlc.sql.common.api.meta.DatabaseType;
import com.tlc.sql.common.registrar.SqlServiceRegistry;
import com.tlc.sql.common.registrar.TableResolverRegistry;
import com.tlc.sql.common.registrar.model.SqlRegistrarData;
import com.tlc.sql.oltp.internal.ds.OltpAdminDataStore;
import com.tlc.sql.oltp.internal.ds.OltpSchemaDataStore;
import com.tlc.sql.oltp.internal.meta.OltpMetaDataHandler;
import com.tlc.sql.oltp.internal.pgsql.PgsqlService;
import com.tlc.sql.oltp.internal.update.OltpDDLUpdateManager;
import com.tlc.sql.oltp.internal.update.OltpDMLUpdateManager;
import com.tlc.sql.oltp.internal.registrar.OltpSqlResourceRegistrar;
import com.tlc.sql.oltp.resolver.OltpTableResolver;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class OltpDatabaseInitializer {

    private final AtomicReference<PgsqlService> pgSqlServiceReference;
    private final AtomicReference<OltpSqlResourceRegistrar> sqlResourceRegistrarReference;
    private final AtomicReference<OltpMetaDataHandler> oltpMetaDataHandlerReference;

    private OltpDatabaseInitializer() {
        this.sqlResourceRegistrarReference = new AtomicReference<>();
        this.pgSqlServiceReference = new AtomicReference<>();
        this.oltpMetaDataHandlerReference = new AtomicReference<>();
    }

    private static class Instance {
        private static final OltpDatabaseInitializer DATABASE_INITIALIZER = new OltpDatabaseInitializer();
    }

    public static OltpDatabaseInitializer get() {
        return Instance.DATABASE_INITIALIZER;
    }


    public void initialize(final Configurations dbConfigurations, final Configurations dbResourceConfigurations) {

        if (Objects.nonNull(pgSqlServiceReference.get())) {
            throw ErrorCode.get(ErrorCodes.ALREADY_INITIALIZED);
        }
        final PgsqlService pgsqlService = new PgsqlService();

        pgsqlService.start(dbConfigurations);

        final OltpAdminDataStore adminDataStore = pgsqlService.getAdminDataStore();
        final OltpSchemaDataStore schemaDataStore = pgsqlService.getSchemaDataStore();
        final OltpMetaDataHandler metaDataHandler = new OltpMetaDataHandler(adminDataStore, schemaDataStore);
        final OltpSqlResourceRegistrar oltpSqlResourceRegistrar = new OltpSqlResourceRegistrar(dbResourceConfigurations, metaDataHandler,
                new OltpDDLUpdateManager(schemaDataStore), new OltpDMLUpdateManager(metaDataHandler, adminDataStore));

        pgsqlService.checkExtensions();

        this.pgSqlServiceReference.set(pgsqlService);
        this.oltpMetaDataHandlerReference.set(metaDataHandler);
        this.sqlResourceRegistrarReference.set(oltpSqlResourceRegistrar);
        TableResolverRegistry.getInstance().register(DatabaseType.OLTP, OltpTableResolver.get());
    }

    public OltpSqlResourceRegistrar getResourceRegistrar() {
        final OltpSqlResourceRegistrar oltpSqlResourceRegistrar = sqlResourceRegistrarReference.get();

        if (Objects.isNull(oltpSqlResourceRegistrar)) {
            throw ErrorCode.get(ErrorCodes.NOT_INITIALIZED);
        }
        return oltpSqlResourceRegistrar;
    }

    public SQLService getSqlService() {
        final PgsqlService pgsqlService = pgSqlServiceReference.get();

        if (Objects.isNull(pgsqlService)) {
            throw ErrorCode.get(ErrorCodes.NOT_INITIALIZED);
        }
        return pgsqlService;
    }

    public OltpMetaDataHandler getOltpMetaDataHandler() {
        final OltpMetaDataHandler metaDataHandler = oltpMetaDataHandlerReference.get();

        if (Objects.isNull(metaDataHandler)) {
            throw ErrorCode.get(ErrorCodes.NOT_INITIALIZED);
        }
        return metaDataHandler;
    }

    public void register(final SqlRegistrarData sqlRegistrarData) {
        getResourceRegistrar().register(sqlRegistrarData);
    }
}