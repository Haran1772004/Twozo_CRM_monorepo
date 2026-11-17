package com.tlc.search.model;

/**
 * <p>
 * Model depicting the count request related fields.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class CountRequest implements IndexRequestModel {

    private FilterClause filterClause;

    public CountRequest() {
    }

    public CountRequest(final FilterClause filterClause) {
        this.filterClause = filterClause;
    }

    /**
     * <p>
     * Sets {@link FilterClause}.
     * </p>
     *
     * @param filterClause{@link FilterClause}.
     */
    public void setFilter(final FilterClause filterClause) {
        this.filterClause = filterClause;
    }

    /**
     * <p>
     * Gets {@link FilterClause}.
     * </p>
     *
     * @return Gets {@link FilterClause}.
     */
    public FilterClause getFilter() {
        return filterClause;
    }
}
