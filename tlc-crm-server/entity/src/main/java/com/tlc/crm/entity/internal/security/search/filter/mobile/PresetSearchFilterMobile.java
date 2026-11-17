package com.tlc.crm.entity.internal.security.search.filter.mobile;

import com.tlc.crm.entity.internal.security.search.filter.PresetSearchFilter;
import com.tlc.search.model.Comparator;
import com.tlc.search.model.Criteria;
import com.tlc.search.model.FilterClause;

/**
 * <p>
 * Preset fields that are to be queried for "search" API for mobile client.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class PresetSearchFilterMobile implements PresetSearchFilter {

    private final String searchText;

    private PresetSearchFilterMobile(final String searchText) {
        this.searchText = searchText;
    }

    /**
     * Gets the {@link PresetSearchFilterMobile} instance.
     *
     * @return {@link PresetSearchFilterMobile}.
     */
    public static PresetSearchFilterMobile getInstance(final String searchText) {
        return new PresetSearchFilterMobile(searchText);
    }

    /**
     * <p>
     * Gets the deal filter.
     * </p>
     *
     * @return {@link FilterClause}.
     */
    public FilterClause getDealFilter() {
        return new FilterClause(new Criteria(searchText, Comparator.CONTAINS, PresetSearchFilterFieldsMobile.DEAL_FIELD));
    }

    /**
     * <p>
     * Gets the contact filter.
     * </p>
     *
     * @return {@link FilterClause}.
     */
    public FilterClause getContactFilter() {
        return new FilterClause(new Criteria(searchText, Comparator.CONTAINS, PresetSearchFilterFieldsMobile.CONTACT_FIELDS));
    }

    /**
     * <p>
     * Gets the company filter.
     * </p>
     *
     * @return {@link FilterClause}.
     */
    public FilterClause getCompanyFilter() {
        return new FilterClause(new Criteria(searchText, Comparator.CONTAINS, PresetSearchFilterFieldsMobile.COMPANY_FIELD));
    }

    /**
     * <p>
     * Gets the product filter.
     * </p>
     *
     * @return {@link FilterClause}.
     */
    public FilterClause getProductFilter() {
        return new FilterClause(new Criteria(searchText, Comparator.CONTAINS, PresetSearchFilterFieldsMobile.PRODUCT_FIELD));
    }

    /**
     * <p>
     * Gets the activity filter.
     * </p>
     *
     * @return {@link FilterClause}.
     */
    public FilterClause getActivityFilter() {
        return new FilterClause(new Criteria(searchText, Comparator.CONTAINS, PresetSearchFilterFieldsMobile.ACTIVITY_FIELD));
    }

    /**
     * <p>
     * Gets the note/file filter.
     * </p>
     *
     * @return {@link FilterClause}.
     */
    public FilterClause getNoteAndFileFilter() {
        return new FilterClause(new Criteria(searchText, Comparator.CONTAINS, PresetSearchFilterFieldsMobile.NOTE_AND_FILE_FIELDS));
    }
}