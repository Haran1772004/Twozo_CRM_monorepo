package com.tlc.sql.common;

import com.tlc.commons.service.BundleContainer;

import com.tlc.sql.common.api.meta.ColumnDefinition;
import com.tlc.sql.common.api.meta.TableDefinition;
import com.tlc.sql.common.registrar.SqlResourceRegistrar;
import com.tlc.sql.common.registrar.model.SqlRegistrarData;
import com.tlc.sql.common.api.meta.handler.MetaDataHandler;

import org.osgi.framework.Bundle;

import java.io.File;

/**
 * @author Abishek
 * @version 1.0
 */
public abstract class SQLDevListener<C extends ColumnDefinition, T extends TableDefinition<C>, S extends SqlResourceRegistrar<C, T>, M extends MetaDataHandler<C, T>> extends BundleListener<C, T> {

    private static final String DATA_FILE_NAME = "data.xml";
    private static final String SEPARATOR = File.separator;

    private final S sqlRegistrar;
    private final BundleContainer bundleContainer;

    protected SQLDevListener(final M metaDataHandler, final S sqlRegistrar) {
        super(metaDataHandler);
        this.sqlRegistrar = sqlRegistrar;
        this.bundleContainer = BundleContainer.getInstance();
    }

    @Override
    public final synchronized void bundleStarted(final Bundle bundle) {
        final SqlRegistrarData sqlRegistrarData = new SqlRegistrarData();

        sqlRegistrarData.setModuleId((int) bundle.getBundleId());
        sqlRegistrarData.setModuleName(bundle.getSymbolicName());
        sqlRegistrarData.setLastModifiedTime(bundle.getLastModified());
        sqlRegistrarData.setMetaFileUrl(bundle.getResource(String.join(SEPARATOR, SQL_FILES_DIR, META_FILE_NAME)));
        sqlRegistrarData.setDataFileUrl(bundle.getResource(String.join(SEPARATOR, SQL_FILES_DIR, DATA_FILE_NAME)));

        sqlRegistrar.register(sqlRegistrarData);
        bundleContainer.addBundleListenedBySqlListener(bundle);
    }

    /**
     * <p>
     * Unloads the metadata files on bundle stopped event.
     * </p>
     *
     * @param bundle Holds the information of the bundle.
     */
    @Override
    public final void bundleStopped(final Bundle bundle) {
        final SqlRegistrarData sqlRegistrarData = new SqlRegistrarData();

        sqlRegistrarData.setModuleId((int) bundle.getBundleId());
        sqlRegistrarData.setModuleName(bundle.getSymbolicName());

        sqlRegistrar.unregister(sqlRegistrarData);
    }
}
