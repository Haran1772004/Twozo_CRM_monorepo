package com.tlc.crm.core.features.internal.dataimport.loader.impl;

import com.tlc.crm.core.features.internal.dataimport.loader.ImportDataContainerHolder;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.sql.common.api.DataContainer;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * The implementation of {@link ImportDataContainerHolder}
 * </p>
 *
 * @author Muthu Kumar V
 * @version 1.0
 */
public final class ImportDataContainerHolderImpl implements ImportDataContainerHolder {

    private final long orgId;
    private final long importId;
    private final Map<Source, Map<DataContainer, Collection<Long>>> sourceDataContainers;

    public ImportDataContainerHolderImpl(final long orgId, final long importId) {
        this.orgId = orgId;
        this.importId = importId;
        sourceDataContainers = new EnumMap<>(Source.class);
    }

    /**
     * {@inheritDoc}
     *
     * @param source        Represents the {@link Source}
     * @param dataContainer Represents the {@link DataContainer}
     * @param modelIds      A collection that contains the model ids
     */
    @Override
    public void addSourceDataContainer(final Source source, final DataContainer dataContainer, final Collection<Long> modelIds) {
        sourceDataContainers.computeIfAbsent(source, data -> new HashMap<>()).put(dataContainer, modelIds);
    }

    /**
     * {@inheritDoc}
     *
     * @param source            Represents the {@link Source}
     * @param modelIds          Represents the ids of the models
     * @param modelContainerMap Represents the map where the data containers to be loaded
     */
    @Override
    public void fetchAndLoadContainers(final Source source, final Collection<Long> modelIds, final Map<Long, DataContainer> modelContainerMap) {
        final Map<DataContainer, Collection<Long>> dataContainers = sourceDataContainers.get(source);

        for (final Long modelId : modelIds) {

            for (final Map.Entry<DataContainer, Collection<Long>> dataContainerEntry : dataContainers.entrySet()) {

                if (dataContainerEntry.getValue().contains(modelId)) {
                    modelContainerMap.put(modelId, dataContainerEntry.getKey());
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param source   Represents the {@link Source}
     * @param modelId  Represents the model id
     * @return Represents the {@link DataContainer}
     */
    @Override
    public DataContainer getRelatedDataContainer(final Source source, final Long modelId) {
        final Map<DataContainer, Collection<Long>> dataContainers = sourceDataContainers.get(source);
        DataContainer dataContainer = DataContainer.create();

        for (final Map.Entry<DataContainer, Collection<Long>> dataContainerEntry : dataContainers.entrySet()) {

            if (dataContainerEntry.getValue().contains(modelId)) {
                dataContainer = dataContainerEntry.getKey();
                break;
            }
        }
        return dataContainer;
    }

    /**
     * {@inheritDoc}
     *
     * @return the organization ID
     */
    @Override
    public long getOrgId() {
        return orgId;
    }

    /**
     * {@inheritDoc}
     *
     * @return the import ID
     */
    @Override
    public long getImportId() {
        return importId;
    }
}