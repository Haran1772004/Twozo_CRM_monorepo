package com.tlc.crm.core.list.dynamic;

import com.tlc.crm.entity.list.ListFieldsFetcher;
import com.tlc.crm.entity.list.dynamic.model.DynamicFieldRequest;
import com.tlc.crm.entity.list.dynamic.model.DynamicFieldsData;

/**
 * <p>
 * Provides the dynamic field data fetcher for entities.
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public abstract class DynamicFieldsFetcher implements ListFieldsFetcher<DynamicFieldsData> {

    /**
     * {@inheritDoc}
     *
     * @param dynamicFieldRequestData Represents the {@link DynamicFieldRequest}
     * @return The {@link DynamicFieldsData}
     */
    public DynamicFieldsData fetchDynamicFieldsData(final DynamicFieldRequest dynamicFieldRequestData) {
        return get(dynamicFieldRequestData);
    }

    /**
     * <p>
     * Fetches dynamic field data based on the provided {@link DynamicFieldRequest}
     * </p>
     *
     * @param dynamicFieldRequest Represents the {@link DynamicFieldRequest}
     * @return The {@link DynamicFieldsData}
     */
    public abstract DynamicFieldsData get(final DynamicFieldRequest dynamicFieldRequest);
}