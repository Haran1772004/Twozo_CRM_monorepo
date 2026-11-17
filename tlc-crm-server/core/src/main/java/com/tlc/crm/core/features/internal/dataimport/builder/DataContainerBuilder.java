package com.tlc.crm.core.features.internal.dataimport.builder;

import com.tlc.crm.core.features.internal.dataimport.builder.impl.DataContainerBuilderImpl;
import com.tlc.crm.core.features.internal.dataimport.container.SourceModelContainer;
import com.tlc.crm.core.features.internal.dataimport.tracker.ErrorTracker;
import com.tlc.crm.core.iam.role.permission.controller.ImportLimitController;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.validator.TlcModel;

import java.util.Collection;
import java.util.Map;

/**
 * A builder to build the {@link DataContainer} with valid model rows that contains the data of uploaded Excel file
 *
 * @author sundar
 * @version 1.0
 */
public interface DataContainerBuilder {

    /**
     * Creates a new {@link DataContainerBuilder} to build the {@link DataContainer} with valid model rows that contains the data of uploaded Excel file
     *
     * @param orgId                Represents the organization ID
     * @param importId             Represents the import ID
     * @param sourceModelContainer is a repository that encompasses all the model values extracted from the {@link com.tlc.crm.core.features.internal.dataimport.container.SourceRecordContainer} Which contains the {@link Collection} of source models grouped by each row index
     * @param errorTracker         Tracks the errors that arise while importing the data into the database.
     * @return the {@link DataContainerBuilder}
     */
    static DataContainerBuilder create(final Long orgId, final Long importId, final SourceModelContainer sourceModelContainer,
                                       final ImportLimitController limitController, final ErrorTracker errorTracker) {
        return new DataContainerBuilderImpl(orgId, importId, sourceModelContainer, limitController, errorTracker);
    }

    /**
     * Builds the {@link DataContainer} with collection of {@link TlcModel}
     *
     * @param index        Represents the row index
     * @param sourceModels Which contains the collection of {@link TlcModel} grouped by source
     * @return {@link DataContainer} that contains the data to import
     */
    DataContainer build(final int index, final Map<Source, TlcModel> sourceModels);

    /**
     * Loads the data containers of given models by source
     *
     * @param sourceModelMap Which contains the models grouped by the sources
     */
    void loadSourceDataContainers(final Map<Source, Collection<TlcModel>> sourceModelMap);
}
