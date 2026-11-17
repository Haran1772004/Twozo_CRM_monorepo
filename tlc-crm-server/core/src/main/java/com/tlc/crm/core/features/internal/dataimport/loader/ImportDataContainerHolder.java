package com.tlc.crm.core.features.internal.dataimport.loader;

import com.tlc.crm.core.features.internal.dataimport.loader.impl.ImportDataContainerHolderImpl;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.sql.common.api.DataContainer;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * Holds the data containers according to their source and model ids
 * </p>
 *
 * @author Muthu Kumar V
 * @version 1.0
 */
public interface ImportDataContainerHolder {

    /**
     * Gets the instance of {@link ImportDataContainerHolder}
     *
     * @return {@link ImportDataContainerHolder} instance
     */
    static ImportDataContainerHolderImpl create(final long orgId, final long importId) {
        return new ImportDataContainerHolderImpl(orgId, importId);
    }

    /**
     * <p>
     * Adds the data container related to its source and model ids
     * </p>
     *
     * @param source        Represents the {@link Source}
     * @param dataContainer Represents the {@link DataContainer}
     * @param modelIds      A collection that contains the model ids
     */
    void addSourceDataContainer(final Source source, final DataContainer dataContainer, final Collection<Long> modelIds);

    /**
     * <p>
     * Fetches and loads the data containers into the model container map
     * </p>
     *
     * @param source            Represents the {@link Source}
     * @param modelIds          Represents the ids of the models
     * @param modelContainerMap Represents the map where the data containers to be loaded
     */
    void fetchAndLoadContainers(final Source source, final Collection<Long> modelIds, final Map<Long, DataContainer> modelContainerMap);

    /**
     * <p>
     * Gets the data container related to the model id
     * </p>
     *
     * @param source   Represents the {@link Source}
     * @param modelId  Represents the model id
     * @return Represents the {@link DataContainer}
     */
    DataContainer getRelatedDataContainer(final Source source, final Long modelId);

    /**
     * Gets the organization ID
     *
     * @return the organization ID
     */
    long getOrgId();

    /**
     * Gets the import ID
     *
     * @return the import ID
     */
    long getImportId();
}