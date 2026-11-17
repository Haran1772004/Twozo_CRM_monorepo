package com.tlc.crm.entity.resolver.filter;

import com.tlc.crm.entity.filter.api.models.CriteriaOption;
import com.tlc.crm.entity.filter.api.models.DropdownDetails;
import com.tlc.crm.entity.resolver.internal.FilterFieldResolverImpl;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * A Resolver that resolves the filter field related details
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public interface FilterFieldResolver {

    /**
     * <p>
     * Gets the instance of {@link FilterFieldResolver}.
     * </p>
     *
     * @return {@link FilterFieldResolver}.
     */
    static FilterFieldResolver getInstance() {
        return FilterFieldResolverImpl.getInstance();
    }

    /**
     * <p>
     * Checks if the option dropdown details exist or not for the provided criteria field ID.
     * If not cached, retrieves data from the database, caches it, and returns whether
     * the option dropdown details exist or not from the cache.
     * </p>
     *
     * @param criteriaFieldId Represents the criteria field ID.
     * @return If option dropdown details exists it will return true, otherwise it will return false
     */
    boolean checkOptionDropdownDetailExists(final Long criteriaFieldId);

    /**
     * <p>
     * Fetches a collection of option dropdown details for the given criteria field ID.
     * </p>
     *
     * @param criteriaFieldId Represents the criteria field ID
     * @return The {@link Map} containing key as boolean and values as {@link DropdownDetails}.
     */
    Map<Boolean, Collection<DropdownDetails>> getOptionDropdownDetails(final Long criteriaFieldId);

    /**
     * <p>
     * Checks whether the type dropdown details exists or not for the given field type ID.
     * If not cached, retrieves data from the database, caches it, and returns whether
     * the type dropdown details exist or not from the cache.
     * </p>
     *
     * @param fieldTypeId Represents the field type ID.
     * @return If type dropdown details exists it will return true, otherwise it will return false
     */
    boolean checkTypeDropdownDetailExists(final Long fieldTypeId);

    /**
     * <p>
     * Fetches a collection of type dropdown details for the given field type ID
     * </p>
     *
     * @param fieldTypeId Represents the field type ID.
     * @return The {@link Map} containing key as boolean and value as {@link DropdownDetails}.
     */
    Map<Boolean, Collection<DropdownDetails>> getTypeDropdownDetails(final Long fieldTypeId);

    /**
     * <p>
     * Fetches all criteria options grouped by their IDs. If not cached, retrieves data from the database,
     * caches it, and returns criteria options from the cache.
     * </p>
     *
     * @return The {@link Map} containing key as criteria option ID value as {@link CriteriaOption}.
     */
    Map<Long, CriteriaOption> getAllFilterOptions();

    /**
     * <p>
     * Fetches a collection of criteria options grouped by source IDs for the provided source IDs.
     * If not cached, retrieves data from the database, caches it, and returns criteria options from the cache.
     * </p>
     *
     * @param sourceIds Represents the source IDs.
     * @return Outer {@link Map} key as the source ID, inner {@link Map} key as criteria option ID value as {@link CriteriaOption}
     */
    Map<Long, Map<Long, CriteriaOption>> getCriteriaOptions(final Set<Long> sourceIds);
}