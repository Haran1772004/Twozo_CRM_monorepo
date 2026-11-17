package com.tlc.sql.common;

import com.tlc.sql.common.api.meta.ColumnDefinition;
import com.tlc.sql.common.api.meta.DatabaseType;
import com.tlc.sql.common.api.meta.TableDefinition;
import com.tlc.sql.common.api.meta.handler.MetaDataHandler;
import com.tlc.sql.common.registrar.SqlServiceRegistry;
import com.tlc.sql.common.registrar.SqlResourceRegistrar;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * @author Abishek
 * @version 1.0
 */
public abstract class SQLAccess<C extends ColumnDefinition, T extends TableDefinition<C>> implements BundleActivator
{
    protected final SqlServiceRegistry sqlServiceRegistry;

    protected SQLAccess(final SqlServiceRegistry sqlServiceRegistry) {
        this.sqlServiceRegistry = sqlServiceRegistry;
    }

    @Override
    public void start(BundleContext bundleContext)
    {
        bundleContext.addBundleListener(getBundleListener());
        sqlServiceRegistry.register(getSqlService());
    }

    @Override
    public void stop(BundleContext bundleContext)
    {
        bundleContext.removeBundleListener(getBundleListener());

        for (final DatabaseType databaseType : DatabaseType.values()) {
            sqlServiceRegistry.unregister(databaseType).stop();
        }
    }

    public abstract <M extends MetaDataHandler<C, T>, S extends SqlResourceRegistrar<C, T>> SQLDevListener<C, T, S, M> getBundleListener();

    public abstract SQLService getSqlService();

    public abstract DatabaseType getDatabaseType();
}
