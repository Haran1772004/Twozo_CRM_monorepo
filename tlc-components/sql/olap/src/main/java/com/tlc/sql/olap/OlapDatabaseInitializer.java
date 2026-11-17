package com.tlc.sql.olap;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.util.Configurations;
import com.tlc.sql.common.SQLService;
import com.tlc.sql.common.api.meta.DatabaseType;
import com.tlc.sql.common.registrar.TableResolverRegistry;
import com.tlc.sql.common.registrar.model.SqlRegistrarData;
import com.tlc.sql.olap.internal.ch.CHSqlService;
import com.tlc.sql.olap.internal.ds.OlapAdminDataStore;
import com.tlc.sql.olap.internal.ds.ch.CHSchemaDataStore;
import com.tlc.sql.olap.internal.meta.CHMetaDataHandler;
import com.tlc.sql.olap.internal.registrar.CHSqlResourceRegistrar;
import com.tlc.sql.olap.internal.update.ch.CHDDLUpdateManager;
import com.tlc.sql.olap.internal.update.ch.CHDMLUpdateManager;
import com.tlc.sql.olap.resolver.OlapTableResolver;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class OlapDatabaseInitializer {

    private final AtomicReference<CHSqlService> chSqlServiceReference;
    private final AtomicReference<CHSqlResourceRegistrar> sqlResourceRegistrarReference;
    private final AtomicReference<CHMetaDataHandler> oltpMetaDataHandlerReference;

    private OlapDatabaseInitializer() {
        this.sqlResourceRegistrarReference = new AtomicReference<>();
        this.chSqlServiceReference = new AtomicReference<>();
        this.oltpMetaDataHandlerReference = new AtomicReference<>();
    }

    private static class Instance {
        private static final OlapDatabaseInitializer DATABASE_INITIALIZER = new OlapDatabaseInitializer();
    }

    public static OlapDatabaseInitializer get() {
        return Instance.DATABASE_INITIALIZER;
    }

    public void initialize(final Configurations dbConfigurations, final Configurations dbResourceConfigurations) {

        if (Objects.nonNull(chSqlServiceReference.get())) {
            throw ErrorCode.get(ErrorCodes.ALREADY_INITIALIZED);
        }
        final CHSqlService chsqlService = new CHSqlService();

        chsqlService.start(dbConfigurations);

        final OlapAdminDataStore adminDataStore = chsqlService.getAdminDataStore();
        final CHSchemaDataStore schemaDataStore = chsqlService.getSchemaDataStore();
        final CHMetaDataHandler metaDataHandler = new CHMetaDataHandler(adminDataStore, chsqlService.getSchemaDataStore());
        final CHSqlResourceRegistrar sqlResourceRegistrar = new CHSqlResourceRegistrar(dbResourceConfigurations, metaDataHandler,
                new CHDDLUpdateManager(schemaDataStore), new CHDMLUpdateManager(metaDataHandler, adminDataStore));

        this.chSqlServiceReference.set(chsqlService);
        this.oltpMetaDataHandlerReference.set(metaDataHandler);
        this.sqlResourceRegistrarReference.set(sqlResourceRegistrar);
        TableResolverRegistry.getInstance().register(DatabaseType.OLAP, OlapTableResolver.get());
    }

    public CHSqlResourceRegistrar getResourceRegistrar() {
        final CHSqlResourceRegistrar sqlResourceRegistrar = sqlResourceRegistrarReference.get();

        if (Objects.isNull(sqlResourceRegistrar)) {
            throw ErrorCode.get(ErrorCodes.NOT_INITIALIZED);
        }
        return sqlResourceRegistrar;
    }

    public SQLService getSqlService() {
        final CHSqlService chsqlService = chSqlServiceReference.get();

        if (Objects.isNull(chsqlService)) {
            throw ErrorCode.get(ErrorCodes.NOT_INITIALIZED);
        }
        return chsqlService;
    }

    public CHMetaDataHandler getCHMetaDataHandler() {
        final CHMetaDataHandler metaDataHandler = oltpMetaDataHandlerReference.get();

        if (Objects.isNull(metaDataHandler)) {
            throw ErrorCode.get(ErrorCodes.NOT_INITIALIZED);
        }
        return metaDataHandler;
    }

    public void register(final SqlRegistrarData sqlRegistrarData) {
        getResourceRegistrar().register(sqlRegistrarData);
    }
}