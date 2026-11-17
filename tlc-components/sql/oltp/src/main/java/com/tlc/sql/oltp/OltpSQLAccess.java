package com.tlc.sql.oltp;

import com.tlc.commons.util.ConfHolder;
import com.tlc.sql.common.SQLAccess;
import com.tlc.sql.common.SQLService;
import com.tlc.sql.common.api.meta.DatabaseType;
import com.tlc.sql.common.registrar.SqlServiceRegistry;
import org.osgi.framework.BundleContext;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class OltpSQLAccess extends SQLAccess {

    private final AtomicReference<OltpDatabaseInitializer> oltpDatabaseInitializerReference;

    public OltpSQLAccess() {
        super(SqlServiceRegistry.getInstance());
        this.oltpDatabaseInitializerReference = new AtomicReference<>();
    }

    @Override
    public SQLListener getBundleListener() {
        final OltpDatabaseInitializer dbInitializer = getDBInitializer();

        return new SQLListener(dbInitializer.getOltpMetaDataHandler(), dbInitializer.getResourceRegistrar());
    }

    @Override
    public SQLService getSqlService() {
        return getDBInitializer().getSqlService();
    }

    @Override
    public DatabaseType getDatabaseType() {
        return DatabaseType.OLTP;
    }

    private OltpDatabaseInitializer getDBInitializer() {
        final OltpDatabaseInitializer oltpDatabaseInitializer = oltpDatabaseInitializerReference.get();

        if (Objects.isNull(oltpDatabaseInitializer)) {
            final OltpDatabaseInitializer databaseInitializer = OltpDatabaseInitializer.get();
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
        sqlServiceRegistry.unregister(DatabaseType.OLTP).stop();
    }
}