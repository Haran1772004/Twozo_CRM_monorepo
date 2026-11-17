package com.tlc.crm.core.features.internal.dataimport.builder.model;

import com.tlc.crm.core.features.internal.dataimport.loader.DataHolder;
import com.tlc.validator.TlcModel;

import java.util.Optional;

/**
 * Builder that can construct the source models for importing.
 *
 * @param <T> Represents the respective model of the source
 * @author sundar
 * @version 1.0
 */
public interface ModelBuilder<T extends TlcModel> {

    /**
     * Constructs the model using the provided JSON source data
     *
     * @param dataHolder The holder for the properties used to store the model in the {@link com.tlc.crm.core.features.internal.dataimport.container.SourceModelContainer}
     * @param index      Represents the index.
     * @return The constructed object of type {@link T} using the data from the DataHolder for importing.
     */
    Optional<T> build(final DataHolder dataHolder, final int index);

    /**
     * Retrieves the {@link T} loaded with the most recent values of corresponding source JSON from the {@link DataHolder}
     *
     * @param dataHolder The holder for the properties used to store the model in the {@link com.tlc.crm.core.features.internal.dataimport.container.SourceModelContainer}
     * @param index      Represents the index.
     * @param model      Represents the existing model that will be updated with the latest data.
     * @return {@link T} loaded using the data from the {@link DataHolder} for import
     */
    default Optional<T> build(final DataHolder dataHolder, final int index, final T model) {
        return build(dataHolder, index);
    }
}
