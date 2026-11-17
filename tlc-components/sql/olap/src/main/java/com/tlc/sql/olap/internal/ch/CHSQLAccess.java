package com.tlc.sql.olap.internal.ch;

import com.tlc.commons.util.ConfHolder;
import com.tlc.sql.common.SQLAccess;
import com.tlc.sql.common.SQLService;
import com.tlc.sql.common.api.meta.DatabaseType;
import com.tlc.sql.common.registrar.SqlServiceRegistry;
import com.tlc.sql.olap.OlapDatabaseInitializer;
import com.tlc.sql.olap.api.meta.OlapColumnDefinition;
import com.tlc.sql.olap.internal.meta.ch.CHTableDefinition;
import org.osgi.framework.BundleContext;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class CHSQLAccess extends SQLAccess<OlapColumnDefinition, CHTableDefinition> {

    private final AtomicReference<OlapDatabaseInitializer> oltpDatabaseInitializerReference;

    public CHSQLAccess() {
        super(SqlServiceRegistry.getInstance());
        this.oltpDatabaseInitializerReference = new AtomicReference<>();
    }

    @Override
    public CHSQLListener getBundleListener() {
        final OlapDatabaseInitializer dbInitializer = getDBInitializer();

        return new CHSQLListener(dbInitializer.getCHMetaDataHandler(), dbInitializer.getResourceRegistrar());
    }

    @Override
    public SQLService getSqlService() {
        return getDBInitializer().getSqlService();
    }

    @Override
    public DatabaseType getDatabaseType() {
        return DatabaseType.OLAP;
    }

    private OlapDatabaseInitializer getDBInitializer() {
        final OlapDatabaseInitializer oltpDatabaseInitializer = oltpDatabaseInitializerReference.get();

        if (Objects.isNull(oltpDatabaseInitializer)) {
            final OlapDatabaseInitializer databaseInitializer = OlapDatabaseInitializer.get();
            final ConfHolder confHolder = ConfHolder.getInstance();

            databaseInitializer.initialize(confHolder.getProperties("database"), confHolder.getProperties("database.resource"));
            oltpDatabaseInitializerReference.set(databaseInitializer);

            return databaseInitializer;
        }
        return oltpDatabaseInitializer;
    }

    @Override
    public void stop(final BundleContext bundleContext) {
        bundleContext.removeBundleListener(getBundleListener());
        sqlServiceRegistry.unregister(DatabaseType.OLAP).stop();
    }
}