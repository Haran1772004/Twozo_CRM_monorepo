package com.tlc.crm.common.list.transformer;

import com.tlc.crm.common.list.transformer.model.TransformerRequest;
import com.tlc.crm.common.list.transformer.model.TransformerResponse;

/**
 * <p>
 * It transforms the entities into required forms.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface ModelTransformer {

    /**
     * <p>
     * Groups the array of fields and transforms the entities into the required forms.
     * </p>
     *
     * @param transformerRequest Represents the {@link TransformerRequest}
     * @return Transformed entities.
     */
    TransformerResponse transform(final TransformerRequest transformerRequest);

    /**
     * <p>
     * Groups the array of fields and transforms the entities into the required forms for mobile request
     * </p>
     *
     * @param transformerRequest Represents the {@link TransformerRequest}
     * @return Transformed entities.
     */
    default TransformerResponse transformDataForMobile(final TransformerRequest transformerRequest) {
        return transform(transformerRequest);
    }
}
