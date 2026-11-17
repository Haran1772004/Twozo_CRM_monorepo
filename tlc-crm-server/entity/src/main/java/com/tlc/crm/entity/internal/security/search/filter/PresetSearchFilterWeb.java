package com.tlc.crm.entity.internal.security.search.filter;

import com.tlc.search.model.Comparator;
import com.tlc.search.model.Criteria;
import com.tlc.search.model.FilterClause;

/**
 * <p>
 * Preset fields that are to be queried for "search" API for web client.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public final class PresetSearchFilterWeb implements PresetSearchFilter {

    private final String searchText;

    private PresetSearchFilterWeb(final String searchText) {
        this.searchText = searchText;
    }

    /**
     * Gets the {@link PresetSearchFilterWeb} instance.
     *
     * @return {@link PresetSearchFilterWeb}.
     */
    public static PresetSearchFilterWeb getInstance(final String searchText) {
        return new PresetSearchFilterWeb(searchText);
    }

    /**
     * <p>
     * Gets the deal filter.
     * </p>
     *
     * @return {@link FilterClause}.
     */
    public FilterClause getDealFilter() {
        return new FilterClause(new Criteria(searchText, Comparator.CONTAINS, PresetSearchFilterFieldsWeb.DEAL_FIELDS));
    }

    /**
     * <p>
     * Gets the contact filter.
     * </p>
     *
     * @return {@link FilterClause}.
     */
    public FilterClause getContactFilter() {
        return new FilterClause(new Criteria(searchText, Comparator.CONTAINS, PresetSearchFilterFieldsWeb.CONTACT_FIELDS));
    }

    /**
     * <p>
     * Gets the company filter.
     * </p>
     *
     * @return {@link FilterClause}.
     */
    public FilterClause getCompanyFilter() {
        return new FilterClause(new Criteria(searchText, Comparator.CONTAINS, PresetSearchFilterFieldsWeb.COMPANY_FIELDS));
    }

    /**
     * <p>
     * Gets the product filter.
     * </p>
     *
     * @return {@link FilterClause}.
     */
    public FilterClause getProductFilter() {
        return new FilterClause(new Criteria(searchText, Comparator.CONTAINS, PresetSearchFilterFieldsWeb.PRODUCT_FIELDS));
    }

    /**
     * <p>
     * Gets the activity filter.
     * </p>
     *
     * @return {@link FilterClause}.
     */
    public FilterClause getActivityFilter() {
        return new FilterClause(new Criteria(searchText, Comparator.CONTAINS, PresetSearchFilterFieldsWeb.ACTIVITY_FIELDS));
    }

    /**
     * <p>
     * Gets the note/file filter.
     * </p>
     *
     * @return {@link FilterClause}.
     */
    public FilterClause getNoteAndFileFilter() {
        return new FilterClause(new Criteria(searchText, Comparator.CONTAINS, PresetSearchFilterFieldsWeb.NOTE_AND_FILE_FIELDS));
    }
}
