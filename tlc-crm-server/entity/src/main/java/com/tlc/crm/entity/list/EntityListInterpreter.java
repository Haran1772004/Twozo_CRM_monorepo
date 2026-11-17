package com.tlc.crm.entity.list;

import com.tlc.crm.common.list.ListStructure;
import com.tlc.crm.common.list.model.ConfiguredField;
import com.tlc.crm.common.list.model.FieldConfig;
import com.tlc.crm.common.list.ListInterpreter;

import com.tlc.crm.entity.list.summary.model.ListSummary;
import com.tlc.crm.entity.filter.api.models.FilterCriteria;
import com.tlc.crm.entity.list.model.Provider;

import com.tlc.sql.common.api.dml.JoinClause;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * Processing lists of data, providing methods to configure queries for selecting, ordering, filtering, and limiting data retrieval
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 */
public interface EntityListInterpreter extends ListInterpreter {

    /**
     * Holds the configured fields
     *
     * @return {@link ConfiguredField}
     */
    List<ConfiguredField> configuredFields();

    /**
     * Gets the {@link Collection} of {@link FieldConfig}.
     *
     * @return {@link Collection} of {@link FieldConfig}
     */
    Collection<FieldConfig> getFieldConfigs();

    /**
     * Updates the given configured fields
     *
     * @param fields Which contains the list of configured fields
     */
    void updateConfiguredFields(final Collection<ConfiguredField> fields);

    /**
     * Updates configured sub-fields
     *
     * @param subFieldName represents sub-Field name
     * @param fieldName    represents field name
     */
    void updateConfiguredSubField(final String subFieldName, final String fieldName, final boolean isConfigured);

    /**
     * <p>
     * Gets the multi select fields
     * </p>
     *
     * @return Map of multi select values
     */
    Map<String, String> getMultiSelectFields();

    /**
     * <p>
     * Loads the criteria of where clause into selectQuery.
     * </p>
     *
     * @param orgId              Represents the organisation id.
     * @param provider           name of the provider.
     * @param filterCriteriaList holds the values of {@link FilterCriteria}.
     */
    void applyCriteria(final Long orgId, final Provider provider, final Collection<FilterCriteria> filterCriteriaList);

    /**
     * <p>
     * Adds the selected and unselected field name.
     * </p>
     *
     * @param fieldNames Represents the selected field names
     */
    void setFieldNames(final Collection<String> fieldNames);

    /**
     * <p>
     * Gets the configured field names.
     * </p>
     *
     * @return {@link Collection} of configured field names
     */
    Collection<String> getConfiguredFieldNames();

    /**
     * <p>
     * Loads fields that are set as mandatory.
     * </p>
     */
    void applyMandatorySelect();

    /**
     * <p>
     * Fetches the {@link ListSummary} details.
     * </p>
     *
     * @return Collection of {@link ListSummary}.
     */
    Collection<ListSummary> fetchSummaryDetails();

    /**
     * <p>
     * Fetches the dynamic fields rows
     * </p>
     *
     * @return A {@link Map} contains key as id string value as collection dynamic fields rows
     */
    Map<String, Map<String, Object>> fetchDynamicFields();

    /**
     * <p>
     * Fetches the preview details into the entity list rows.
     * </p>
     *
     * @return The {@link Map} containing entity IDs to their associated data rows
     */
    Map<String, Map<String, Object>> fetchPreviewData();

    /**
     * <p>
     * Fetches the {@link ListStructure}
     * </p>
     *
     * @return The {@link ListStructure}
     */
    ListStructure getListStructure();

    /**
     * Adds the {@link JoinClause} into the query.
     *
     * @param joinClauses {@link JoinClause}.
     */
    void addJoinClause(final Collection<JoinClause> joinClauses);
}
