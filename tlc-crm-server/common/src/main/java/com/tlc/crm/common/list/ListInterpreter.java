package com.tlc.crm.common.list;

import com.tlc.crm.common.list.model.ConfiguredField;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.WhereClause;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * Processing lists of data, providing methods to configure queries for selecting, ordering, filtering, and limiting data retrieval.
 * </p>
 *
 * @author Abishek
 * @version 1.0
 */
public interface ListInterpreter {

    /**
     * Applies the default ordering to the query if no specific sorting criteria are provided.
     */
    void applyDefaultOrderBy();

    /**
     * Sets the default selection of fields if no specific fields are specified.
     */
    void applyDefaultSelect();

    /**
     * Applies the default limits to the query if no specific limits are provided.
     */
    void applyDefaultLimit();

    /**
     * Adds a select clause for the specified field to the query.
     *
     * @param field The field to be included in the select clause.
     */
    void selectClause(final String field);

    /**
     * Adds an order by clause for the specified field to the query.
     *
     * @param field The field to be used for ordering.
     * @param isAsc A boolean indicating whether the ordering is ascending (true) or descending (false).
     */
    void orderByClause(final String field, final boolean isAsc);

    /**
     * Sets the limits for pagination in the query results.
     *
     * @param start The index of the first result to retrieve.
     * @param limit The maximum number of results to retrieve.
     */
    void setLimits(final int start, final int limit);

    /**
     * Retrieves the total number of hits matching the query criteria.
     *
     * @return The total number of hits.
     */
    long fetchTotalHits();

    /**
     * Retrieves the result set containing mapped data.
     *
     * @return The collection of maps containing the mapped data.
     */
    Collection<Map<String, Object>> getResult();

    /**
     * Retrieves the primary field used for mapping data.
     *
     * @return The name of the primary field.
     */
    String primaryField();

    /**
     * Retrieves the structure defining the fields and their configurations.
     *
     * @return The {@link ListStructure} details
     */
    ListStructure structure();

    /**
     * Adds a criteria for filtering data based on the specified key, value, and condition.
     *
     * @param key       The key or field name for the criteria.
     * @param value     The value to match against the specified key.
     * @param comparator The condition specifying how to apply the criteria (e.g., "equals", "greater than").
     */
    void criteria(final String key, final Object value, final String comparator);

    /**
     * <p>
     * Gets the {@link List} of default configured fields.
     * </p>
     *
     * @return {@link Collection} of {@link ConfiguredField}
     */
    Collection<ConfiguredField> getDefaultConfiguredFields();

    /**
     * <p>
     * Adds a select clause to the query based on the provided configured field
     * </p>
     *
     * @param field Represents the {@link ConfiguredField} details
     */
    void selectClause(final ConfiguredField field);

    /**
     * <p>
     * Sets the {@link WhereClause} for the query
     * </p>
     *
     * @param whereClause The {@link WhereClause} to be set
     */
    void setWhereClause(final WhereClause whereClause);

    /**
     * <p>
     * Gets a collection of dynamic fields name
     * </p>
     *
     * @return The collection of dynamic fields name
     */
    Collection<String> getDynamicFields();
}
