package com.tlc.crm.core.features.internal.dataimport.builder;

import com.tlc.crm.core.features.internal.dataimport.builder.impl.RevertContainerBuilderImpl;
import com.tlc.crm.core.features.internal.dataimport.container.RevertContainer;
import com.tlc.crm.core.features.internal.dataimport.loader.DataHolder;
import com.tlc.crm.core.features.internal.dataimport.tracker.ErrorTracker;
import com.tlc.sql.common.api.DataContainer;

/**
 * The builder which provides the abstract way to build the container required for data revert related actions.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public interface RevertContainerBuilder {

    /**
     * Gets the instance of {@link RevertContainerBuilder}.
     *
     * @return {@link RevertContainerBuilder}.
     */
    static RevertContainerBuilder getInstance() {
        return RevertContainerBuilderImpl.getInstance();
    }

    /**
     * Gets the {@link RevertContainer} which holds required data for the revert process.
     *
     * @param dataHolder      Which holds the data ie, required frequently to convert the data.
     * @param dataContainer   Refers the {@link DataContainer} which contains the rows of imported records.
     * @param errorTracker    Tracks the error that arises while reverting the data.
     * @param revertContainer Represents the {@link RevertContainer}
     * @return The {@link RevertContainer} which holds the details for the revert process to be executed.
     */
    RevertContainer build(final DataHolder dataHolder, final DataContainer dataContainer, final ErrorTracker errorTracker,
                          final RevertContainer revertContainer);
}
