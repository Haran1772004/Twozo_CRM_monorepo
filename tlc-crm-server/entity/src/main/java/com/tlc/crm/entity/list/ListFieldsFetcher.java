package com.tlc.crm.entity.list;

import com.tlc.crm.entity.list.dynamic.model.DynamicFieldRequest;
import com.tlc.crm.entity.list.dynamic.model.DynamicFieldsData;
import com.tlc.crm.entity.list.model.ListFieldsData;

/**
 * <p>
 * Provides services for retrieving and managing list field data.
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public interface ListFieldsFetcher<T extends ListFieldsData> {

    /**
     * <p>
     * Fetches dynamic field data based on the provided {@link DynamicFieldRequest}
     * </p>
     *
     * @param dynamicFieldRequestData Represents the {@link DynamicFieldRequest}
     * @return The {@link DynamicFieldsData}
     */
    T fetchDynamicFieldsData(final DynamicFieldRequest dynamicFieldRequestData);
}