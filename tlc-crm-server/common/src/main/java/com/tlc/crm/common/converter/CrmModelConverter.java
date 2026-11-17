package com.tlc.crm.common.converter;

import com.tlc.commons.converter.ModelConverter;
import com.tlc.commons.converter.model.ConverterData;

/**
 * CRM-specific abstract implementation of {@link ModelConverter} that delegates
 * conversion to methods accepting {@link CrmConverterData}.
 *
 * <p>Subclasses implement the abstract {@code convertToModel} and {@code convertToJson}
 * methods that receive a {@link CrmConverterData} with CRM user context.</p>
 *
 * @param <M> the domain model type
 * @param <J> the serialized representation type
 *
 * @author Sathish
 * @version 1.0
 */
public abstract class CrmModelConverter<M, J> implements ModelConverter<M, J> {

    /**
     * {@inheritDoc}
     */
    @Override
    public final <D extends ConverterData<J>> M convertToModel(final D converterData) {
        //noinspection unchecked
        return converterData instanceof CrmConverterData<?> ? convertToModel((CrmConverterData<J>) converterData) : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final <D extends ConverterData<M>> J convertToJson(final D converterData) {
        //noinspection unchecked
        return converterData instanceof CrmConverterData<?> ? convertToJson((CrmConverterData<M>) converterData) : null;
    }

    /**
     * Converts the serialized data held in the given {@link CrmConverterData} into the
     * corresponding domain model.
     *
     * @param converterData the CRM-specific container holding the serialized data and user context
     * @return the deserialized domain model instance
     */
    public abstract M convertToModel(final CrmConverterData<J> converterData);

    /**
     * Converts the domain model held in the given {@link CrmConverterData} into its
     * serialized representation.
     *
     * @param converterData the CRM-specific container holding the domain model and user context
     * @return the serialized representation of the domain model
     */
    public abstract J convertToJson(final CrmConverterData<M> converterData);
}
