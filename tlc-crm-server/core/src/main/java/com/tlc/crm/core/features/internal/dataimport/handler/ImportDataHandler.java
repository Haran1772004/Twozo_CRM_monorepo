package com.tlc.crm.core.features.internal.dataimport.handler;

import com.tlc.attachment.io.File;
import com.tlc.attachment.model.RecordHolder;

import com.tlc.commons.json.JsonArray;

import com.tlc.crm.core.features.internal.dataimport.SourceFieldHeaders;
import com.tlc.crm.core.features.internal.dataimport.loader.DataHolder;
import com.tlc.crm.entity.fields.source.Source;

import com.tlc.crm.core.features.api.dataimport.model.ImportInfo;
import com.tlc.crm.core.features.internal.dataimport.MergeConfig;
import com.tlc.crm.core.features.internal.dataimport.container.SourceRecordContainer;
import com.tlc.crm.core.features.internal.dataimport.container.SourceModelContainer;
import com.tlc.crm.core.features.internal.dataimport.handler.impl.ImportDataHandlerImpl;
import com.tlc.crm.core.features.internal.dataimport.tracker.ErrorTracker;

import java.util.Collection;
import java.util.Map;

/**
 * Handles the required services for the import related actions.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public interface ImportDataHandler {

    /**
     * Gets the instance of {@link ImportDataHandler}.
     *
     * @return {@link ImportDataHandler}.
     */
    static ImportDataHandler get() {
        return ImportDataHandlerImpl.getInstance();
    }

    /**
     * Gets the {@link RecordHolder} that contains the values obtained from the uploaded file
     *
     * @param importInfo Which contains the uploaded attachment and import information
     * @param file       Refers the {@link File}.
     * @param start      Refers to where the reading of the file should be started
     * @param limit      Refers to where the reading of the file should be stopped
     */
    void parse(final ImportInfo importInfo, final File file, final Integer start, final Integer limit, final RecordHolder recordHolder);

    /**
     * Fetches the {@link SourceFieldHeaders} based on their respective groups using the provided array
     *
     * @param orgId       Represents the organization ID
     * @param fieldsArray An array containing information about the mapping between fields and header indices
     * @return A {@link Map} of {@link SourceFieldHeaders} grouped by the source
     */
    Map<Source, SourceFieldHeaders> fetchSourceFieldHeadersMap(final Long orgId, final JsonArray fieldsArray);

    /**
     * Gets the default merge configuration
     *
     * @param orgId                Represents the organization ID
     * @param mergeRequired        A flag that represents the additional merge configurations are needed or not
     * @param sourceFieldHeaderMap Which holds information about the mapping between fields and headers, organized by the source
     * @return {@link MergeConfig}
     */
    MergeConfig getDefaultMergeConfig(final Long orgId, final boolean mergeRequired, final Map<Source, SourceFieldHeaders> sourceFieldHeaderMap);

    /**
     * Loads the related source fields into the {@link SourceRecordContainer}
     *
     * @param orgId                 Represents the organization ID
     * @param mappedSources         Which holds the sources mapped in the import action
     * @param sourceRecordContainer Which is to be loaded with the related source fields
     */
    void loadRelatedFields(final long orgId, final Collection<Source> mappedSources, final SourceRecordContainer sourceRecordContainer);

    /**
     * Loads the distinct fields info into the {@link SourceRecordContainer}
     *
     * @param orgId                 Represents the organization ID
     * @param sourceRecordContainer Which is to be loaded with the associated source fields
     */
    void injectDistinctFields(final Long orgId, final SourceRecordContainer sourceRecordContainer);

    /**
     * Validates the source models and records the corresponding validation messages within the {@link ErrorTracker}
     *
     * @param sourceModelContainer is a repository that encompasses all the model values extracted from the {@link SourceRecordContainer} Which contains the {@link Collection} of source models grouped by each row index
     * @param errorTracker         Tracks the errors that arise while validating the source models
     */
    void validateModels(final SourceModelContainer sourceModelContainer, final ErrorTracker errorTracker);

    /**
     * Generates the import request id by the given details
     *
     * @param orgId    Represents the organisation ID
     * @param userId   Represents the user ID
     * @param importId Represents the import ID
     * @return the generated import request ID
     */
    String generateImportRequestId(final long orgId, final long userId, final long importId);

    /**
     * <p>
     * Loads the indexable records into the container
     * </p>
     *
     * @param orgId                  Represents the organization ID
     * @param sources                The collection of {@link Source}
     * @param importableRecordHolder Represents the {@link RecordHolder}
     * @param sourceRecordContainer  Represents the {@link SourceRecordContainer}
     */
    void loadRecordsIntoContainer(final Long orgId, final Collection<Source> sources, final RecordHolder importableRecordHolder, final SourceRecordContainer sourceRecordContainer);

    /**
     * <p>
     * Fetches and Loads the data containers of the sources that has the ids in the indexed data of the source record container
     * </p>
     *
     * @param sources    The collection of {@link Source}
     * @param dataHolder Represents the {@link DataHolder}
     */
    void fetchAndStoreSourceDataContainers(final Collection<Source> sources, final DataHolder dataHolder);
}