package com.tlc.crm.core.features.internal.dataimport.service;

import com.tlc.crm.core.features.api.dataimport.model.ImportInfo;
import com.tlc.crm.core.features.internal.dataimport.container.SourceModelContainer;
import com.tlc.crm.core.features.internal.dataimport.service.impl.DataImportServiceImpl;
import com.tlc.crm.core.features.internal.dataimport.tracker.ErrorTracker;
import com.tlc.sql.common.api.DataContainer;

import java.util.Map;

/**
 * Provides the services for performing import related actions.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public interface DataImportService {

    /**
     * Gets the instance of {@link DataImportService}.
     *
     * @return {@link DataImportService}.
     */
    static DataImportService getInstance() {
        return DataImportServiceImpl.getInstance();
    }

    /**
     * Fetches the data containers required for the import process grouped by indices of rows from the given {@link SourceModelContainer}.
     *
     * @param importInfo           Which holds the {@link ImportInfo} details.
     * @param sourceModelContainer Is a repository that encompasses all the model values extracted from the sourceRecordContainer.
     * @param errorTracker         Tracks the errors that arise while importing the data into the database.
     * @return The data containers for the import process grouped by the index of rows.
     */
    Map<Integer, DataContainer> fetchDataContainersByIndex(final ImportInfo importInfo,
                                                           final SourceModelContainer sourceModelContainer,
                                                           final ErrorTracker errorTracker);
}
