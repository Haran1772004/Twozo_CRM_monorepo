package com.tlc.crm.core.features.internal.dataimport.container;

import com.tlc.commons.pair.Pair;
import com.tlc.crm.core.features.internal.dataimport.container.impl.TriggerModelContainerImpl;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.validator.TlcModel;

import java.util.Collection;
import java.util.Map;

/**
 * A repository that manages the {@link TlcModel} data,
 * grouped by sources to facilitate the trigger process.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public interface TriggerModelContainer {

    /**
     * Gets the {@link TriggerModelContainer} instance.
     *
     * @return {@link TriggerModelContainer}.
     */
    static TriggerModelContainer create() {
        return new TriggerModelContainerImpl();
    }

    /**
     * Adds the deleted model into the container.
     *
     * @param source Represents the source.
     * @param index  Represents the row index.
     * @param model  To be loaded into the container.
     */
    void addCreatedModel(final Source source, final int index, final TlcModel model);

    /**
     * Adds the updated model into the container.
     *
     * @param source   Represents the source.
     * @param index    Represents the row index.
     * @param model    To be loaded into the container.
     * @param oldModel Refers the old model to be loaded into the container.
     */
    void addUpdatedModel(final Source source, final int index, final TlcModel model, final TlcModel oldModel);

    /**
     * Adds the deleted model into the container.
     *
     * @param source Represents the source.
     * @param index  Represents the row index.
     * @param model  To be loaded into the container.
     */
    void addDeletedModel(final Source source, final int index, final TlcModel model);

    /**
     * Gets all created models grouped by the source and indices.
     *
     * @return {@link Map} that contains the created models grouped by row index and source.
     */
    Map<Integer, Map<Source, TlcModel>> getCreatedModels();

    /**
     * Gets all the updated models grouped by the source and indices.
     *
     * @return {@link Map} that contains the updated models grouped by row index and source.
     */
    Map<Integer, Map<Source, Pair<TlcModel, TlcModel>>> getUpdatedModels();

    /**
     * Gets all deleted models grouped by the source and indices.
     *
     * @return {@link Map} that contains the deleted models grouped by row index and source.
     */
    Map<Integer, Map<Source, TlcModel>> getDeletedModels();

    /**
     * Includes the invalid indices in the container, aiding in the prevention of unnecessary model triggers.
     *
     * @param invalidIndices Which contains the indices of invalid models.
     */
    void addInvalidIndices(final Collection<Integer> invalidIndices);

    /**
     * Gets the invalid indices of the models.
     *
     * @return The invalid indices.
     */
    Collection<Integer> getInvalidIndices();

}
