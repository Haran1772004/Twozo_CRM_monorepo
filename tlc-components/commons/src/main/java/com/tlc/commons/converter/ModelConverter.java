package com.tlc.commons.converter;

import com.tlc.commons.converter.model.ConverterData;

/**
 * Defines the contract for bidirectional conversion between a domain model and its
 * serialized representation.
 *
 * @param <M> the domain model type
 * @param <J> the serialized representation type
 *
 * @author Sathish
 * @version 1.0
 */
public interface ModelConverter<M, J> {

    /**
     * Converts the given {@link ConverterData} into the corresponding domain model.
     *
     * @param converterData the container holding the serialized data and conversion context
     * @return the deserialized domain model instance
     */
    <D extends ConverterData<J>> M convertToModel(final D converterData);

    /**
     * Converts the given {@link ConverterData} into its serialized representation.
     *
     * @param converterData the container holding the domain model and conversion context
     * @return the serialized representation of the domain model
     */
    <D extends ConverterData<M>> J convertToJson(final D converterData);
}
