package com.tlc.crm.entity.cache.filter;

import com.tlc.crm.common.cache.LocalCache;
import com.tlc.crm.entity.cache.internal.FilterFieldCacheImpl;
import com.tlc.crm.entity.filter.api.models.CriteriaField;
import com.tlc.crm.entity.filter.api.models.CriteriaOption;
import com.tlc.crm.entity.filter.api.models.DropdownDetails;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Provides an abstract way to deal with {@link CriteriaField} objects.
 *
 * @author Sathish
 * @version 1.0
 */
public interface FilterFieldCache extends LocalCache {

    static FilterFieldCache getInstance() {
        return FilterFieldCacheImpl.getInstance();
    }

    /**
     * <p>
     * Sets the option dropdown details into cache.
     * </p>
     *
     * @param criteriaOptionId             Represents the criteria option id.
     * @param groupedOptionDropdownDetails Which contains {@link Map} key as boolean value as collections of {@link DropdownDetails}.
     */
    void setOptionDropdownDetails(final Long criteriaOptionId, Map<Boolean, Collection<DropdownDetails>> groupedOptionDropdownDetails);

    /**
     * <p>
     * Sets the type dropdown details into cache
     * </p>
     *
     * @param fieldTypeId                Represents the field type id
     * @param groupedTypeDropdownDetails Which contains {@link Map} key as boolean value as collection of {@link DropdownDetails}
     */
    void setTypeDropdownDetails(final Long fieldTypeId, final Map<Boolean, Collection<DropdownDetails>> groupedTypeDropdownDetails);

    /**
     * <p>
     * Sets the criteria option details into filter option cache
     * </p>
     *
     * @param criteriaOption Represents the {@link CriteriaOption}
     */
    void setFilterOption(final CriteriaOption criteriaOption);

    /**
     * <p>
     * Fetches a collection of option dropdown details from the option dropdown cache for the given criteria option ID
     * </p>
     *
     * @param criteriaOptionId Represents the criteria option id
     * @return The {@link Map} containing key as boolean value as collection of {@link DropdownDetails}.
     */
    Map<Boolean, Collection<DropdownDetails>> fetchOptionDropdownDetails(final Long criteriaOptionId);

    /**
     * <p>
     * Fetches a collection of type dropdown details from the type dropdown details cache for the given field type ID.
     * </p>
     *
     * @param fieldTypeId Represents field type id
     * @return The {@link Map} containing key as boolean value as collection of {@link DropdownDetails}.
     */
    Map<Boolean, Collection<DropdownDetails>> fetchFieldTypeDropdownDetails(final Long fieldTypeId);

    /**
     * <p>
     * Fetches a collection of criteria options organized by source IDs from the cache for the given source IDs.
     * </p>
     *
     * @param ids Which holds the collection of source IDs
     * @return The {@link Map} key as the source ID, inner {@link Map} key as criteria option ID value as {@link CriteriaOption}
     */
    Map<Long, Map<Long, CriteriaOption>> getFilterOptions(final Set<Long> ids);

    /**
     * <p>
     * Fetches all the criteria options organized by their IDs from the cache.
     * </p>
     *
     * @return The {@link Map} containing key as criteria option ID value as {@link CriteriaOption}
     */
    Map<Long, CriteriaOption> getAllFilterOptions();

    /**
     * <p>
     * Checks whether the option dropdown details exist or not in the cache for the given criteria field ID
     * </p>
     *
     * @param criteriaOptionId Represents the criteria option ID.
     * @return If option dropdown details exists it will return true, otherwise it will return false
     */
    boolean checkOptionDropdownDetailExists(final Long criteriaOptionId);

    /**
     * <p>
     * Checks whether the type dropdown details exists or not in the cache for the given field type ID.
     * </p>
     *
     * @param fieldTypeId Represents the field type ID.
     * @return If type dropdown details exists it will return true, otherwise it will return false
     */
    boolean checkTypeDropdownDetailExists(final Long fieldTypeId);
}