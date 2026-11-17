package com.tlc.crm.core.features.internal.dataimport.loader;

import com.tlc.crm.core.features.api.dataimport.model.SourceRecord;
import com.tlc.crm.core.features.internal.dataimport.container.SourceModelContainer;
import com.tlc.crm.core.features.internal.dataimport.container.SourceRecordContainer;
import com.tlc.crm.core.features.internal.dataimport.loader.impl.SourceModelLoaderImpl;
import com.tlc.crm.core.features.internal.dataimport.tracker.ErrorTracker;

/**
 * Provides service to parse the entity data within the {@link SourceRecord} and the loads into the {@link SourceModelContainer}
 *
 * @author sundar
 * @version 1.0
 */
public interface SourceModelLoader {

    /**
     * Gets the instance of {@link SourceModelContainer}
     *
     * @return {@link SourceModelContainer}
     */
    static SourceModelLoader getInstance() {
        return SourceModelLoaderImpl.getInstance();
    }

    /**
     * Converts and loads the source JSON data from {@link SourceRecordContainer} to {@link SourceModelContainer}
     *
     * @param dataHolder   Which holds the data to be converted to {@link com.tlc.validator.TlcModel} and load into the {@link SourceModelContainer}
     * @param errorTracker Tracks the errors that arise while loading the data into the {@link SourceModelContainer}
     */
    void load(final DataHolder dataHolder, final ErrorTracker errorTracker);
}
