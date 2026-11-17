package com.tlc.crm.core.features.internal.dataimport.container;

import com.tlc.crm.core.features.internal.dataimport.container.impl.RevertContainerImpl;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.validator.TlcModel;

import java.util.Map;

/**
 * A repository that manages the {@link TlcModel} data,
 * grouped by sources to facilitate the revert process.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public interface RevertContainer {

    /**
     * Gets the {@link RevertContainer} instance.
     *
     * @return {@link RevertContainer}.
     */
    static RevertContainer create() {
        return new RevertContainerImpl();
    }

    /**
     * Adds the updated model into the container.
     *
     * @param source Represents the source.
     * @param index  Represents the row index.
     * @param model  To be loaded into the container.
     */
    void addUpdatedModel(final Source source, final int index, final TlcModel model);

    /**
     * Gets all the updated models grouped by the source and indices.
     *
     * @return {@link Map} that contains the updated models grouped by row index and source.
     */
    Map<Integer, Map<Source, TlcModel>> getUpdatedModels();

    /**
     * Adds the deleted model into the container.
     *
     * @param source Represents the source.
     * @param index  Represents the row index.
     * @param model  To be loaded into the container.
     */
    void addDeletedModel(final Source source, final int index, final TlcModel model);

    /**
     * Gets all deleted models grouped by the source and indices.
     *
     * @return {@link Map} that contains the deleted models grouped by row index and source.
     */
    Map<Integer, Map<Source, TlcModel>> getDeletedModels();

    /**
     * Adds the {@link DataContainer} with its index for revert action.
     *
     * @param index         Represents the row index.
     * @param dataContainer Which holds the data rows of given index intended for the reversion process.
     */
    void addDataContainer(final int index, final DataContainer dataContainer);

    /**
     * Gets all the data containers grouped by indices.
     *
     * @return {@link Map} that contains the data containers grouped by row index.
     */
    Map<Integer, DataContainer> getDataContainers();
}