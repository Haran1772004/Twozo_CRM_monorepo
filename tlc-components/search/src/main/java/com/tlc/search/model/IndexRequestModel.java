package com.tlc.search.model;

/**
 * <p>
 * Basic index request requirement depicting model.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public interface IndexRequestModel {

    /**
     * <p>
     * Sets {@link FilterClause}.
     * </p>
     *
     * @param filterClause{@link FilterClause}.
     */
    void setFilter(final FilterClause filterClause);

    /**
     * <p>
     * Gets {@link FilterClause}.
     * </p>
     *
     * @return Gets {@link FilterClause}.
     */
    public FilterClause getFilter();
}