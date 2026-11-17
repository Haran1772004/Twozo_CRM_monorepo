package com.tlc.crm.core.features.internal.dataimport.async.service;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;
import com.tlc.crm.core.features.internal.dataimport.async.impl.DataImporter;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Holder for the services utilized within the data import feature
 *
 * @author sundar
 * @version 1.0
 */
public final class ImportServiceHolder implements Service {

    private static final AtomicReference<DataImporter> DATA_IMPORTER_SERVICE = new AtomicReference<>();

    /**
     * Gets the data importer
     *
     * @return {@link DataImporter}
     */
    public static DataImporter getDataImporter() {
        final DataImporter dataImporter = DATA_IMPORTER_SERVICE.get();

        if (dataImporter == null) {
            throw ErrorCode.get(ErrorCodes.INVALID_ACCESS, "Service not initialized");
        }
        return dataImporter;
    }

    /**
     * Registers the data importer service
     *
     * @param service Represents the {@link DataImporter}
     */
    private static void registerDataImporter(final DataImporter service) {

        if (!DATA_IMPORTER_SERVICE.compareAndSet(null, service)) {
            throw ErrorCode.get(ErrorCodes.INVALID_ACCESS, "Service already initialized");
        }
    }

    /**
     * Unregisters the {@link DataImporter} service
     *
     * @return {@link DataImporter}
     */
    private DataImporter unregisterDataImporter() {
        return DATA_IMPORTER_SERVICE.getAndSet(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(final Configurations configurations) {
        final DataImporter dataImporter = new DataImporter();

        registerDataImporter(dataImporter);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        final DataImporter dataImporter = unregisterDataImporter();

        if (Objects.nonNull(dataImporter)) {
            dataImporter.stop();
        }
    }
}