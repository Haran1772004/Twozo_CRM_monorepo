package com.tlc.crm.entity.internal.security.search.filter;

import com.tlc.search.model.FilterClause;

/**
 * <p>
 * Preset search filters for all indexes in CRM.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public interface PresetSearchFilter {

    /**
     * <p>
     * Gets the deal filter.
     * </p>
     *
     * @return {@link FilterClause}.
     */
    FilterClause getDealFilter();

    /**
     * <p>
     * Gets the contact filter.
     * </p>
     *
     * @return {@link FilterClause}.
     */
    FilterClause getContactFilter();

    /**
     * <p>
     * Gets the company filter.
     * </p>
     *
     * @return {@link FilterClause}.
     */
    FilterClause getCompanyFilter();

    /**
     * <p>
     * Gets the product filter.
     * </p>
     *
     * @return {@link FilterClause}.
     */
    FilterClause getProductFilter();

    /**
     * <p>
     * Gets the activity filter.
     * </p>
     *
     * @return {@link FilterClause}.
     */
    FilterClause getActivityFilter();

    /**
     * <p>
     * Gets the note/file filter.
     * </p>
     *
     * @return {@link FilterClause}.
     */
    FilterClause getNoteAndFileFilter();
}