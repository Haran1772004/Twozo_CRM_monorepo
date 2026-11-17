package com.tlc.crm.core.features.action.quota.list;

import com.tlc.crm.common.list.ListInterpreter;
import com.tlc.crm.common.list.model.ConfiguredField;
import com.tlc.crm.common.list.model.FieldConfig;
import com.tlc.crm.entity.filter.api.models.FilterCriteria;

import java.util.Collection;

/**
 * <p>
 * Extends {@link ListInterpreter} with quota-specific operations for managing
 * configured fields, applying filter criteria, and persisting user filter preferences.
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 * @see QuotaSQLListInterpreter
 */
public interface QuotaListInterpreter extends ListInterpreter {

    /**
     * <p>
     * Fetches and returns all configured fields for the current organization and user.
     * </p>
     *
     * @return A collection of configured {@link FieldConfig}
     */
    Collection<FieldConfig> fetchConfiguredFields();

    /**
     * <p>
     * Updates the configured fields for the current organization and user.
     * </p>
     *
     * @param configuredFields the collection of fields to be persisted as configuration
     */
    void updateConfiguredFields(final Collection<ConfiguredField> configuredFields);

    /**
     * <p>
     * Applies the criteria of where clause into selectQuery.
     * </p>
     *
     * @param filterCriteriaList holds the values of {@link com.tlc.crm.entity.filter.api.models.FilterCriteria}.
     */
    void applyCriteria(final Collection<FilterCriteria> filterCriteriaList);
}
