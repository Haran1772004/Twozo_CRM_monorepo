package com.tlc.crm.entity.list.filter;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.entity.filter.api.models.FilterCriteria;
import com.tlc.crm.entity.list.model.Provider;
import com.tlc.crm.entity.list.filter.internal.FilterCriteriaBuilderImpl;

import java.util.Collection;

/**
 * <p>
 * An interface to obtain the {@link FilterCriteria} for the dependent
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
public interface FilterCriteriaBuilder {

    /**
     * <p>
     * Gets the {@link FilterCriteriaBuilder} instance
     * </p>
     *
     * @return The {@link FilterCriteriaBuilder}
     */
    static FilterCriteriaBuilder getInstance() {
        return FilterCriteriaBuilderImpl.getInstance();
    }

    /**
     * <p>
     * Constructs the {@link FilterCriteria} based on the criteria json object
     * </p>
     *
     * @param orgId        Represents an organization id
     * @param criteriaJson holds the values of criteria
     * @return The {@link FilterCriteria}
     */
    FilterCriteria build(final long orgId, final JsonObject criteriaJson);

    /**
     * <p>
     * Constructs the {@link Collection} of {@link FilterCriteria} based on the criteria json object
     * </p>
     *
     * @param orgId        Represents an organization id
     * @param userId       Represents the user id
     * @param provider     Represents the {@link Provider}
     * @param criteriaJson Which holds the values of criteria
     * @return The {@link Collection} of {@link FilterCriteria}
     */
    Collection<FilterCriteria> constructFilterCriteria(final long orgId, final long userId, final Provider provider,
                                                       final JsonObject criteriaJson);
}
