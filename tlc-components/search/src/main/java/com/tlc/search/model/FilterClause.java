package com.tlc.search.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * <p>
 * A model that holds the details of the filter.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class FilterClause implements Serializable {

    public enum Connector {
        AND, OR
    }

    private final FilterClause left;
    private final FilterClause right;
    private final Connector connector;
    private final boolean hasConnector;
    private final Criteria criteria;

    public FilterClause(final Criteria criteria) {
        this.hasConnector = false;
        this.connector = null;
        this.left = null;
        this.right = null;
        this.criteria = Objects.requireNonNull(criteria);
    }

    private FilterClause(final FilterClause left, final FilterClause right, final Connector connector) {
        this.hasConnector = true;
        this.left = Objects.requireNonNull(left);
        this.right = Objects.requireNonNull(right);
        this.connector = connector;
        this.criteria = null;
    }

    /**
     * <p>
     * Creates the {@link FilterClause} with "or" criteria. Appends the new criteria after the old one.
     * </p>
     *
     * @param criteria Old {@link Criteria}.
     * @return Created {@link FilterClause}.
     */
    public FilterClause or(final Criteria criteria) {
        return new FilterClause(this, new FilterClause(criteria), Connector.OR);
    }

    /**
     * <p>
     * Creates the {@link FilterClause} with "or" criteria. Appends the new criteria after the old one.
     * </p>
     *
     * @param filterClause Old {@link FilterClause}.
     * @return Created {@link FilterClause}.
     */
    public FilterClause or(final FilterClause filterClause) {
        return new FilterClause(this, Objects.requireNonNull(filterClause), Connector.OR);
    }

    /**
     * <p>
     * Creates the {@link FilterClause} with "and" criteria. Appends the new criteria after the old one.
     * </p>
     *
     * @param filterClause Old {@link FilterClause}.
     * @return Created {@link FilterClause}.
     */
    public FilterClause and(final FilterClause filterClause) {
        return new FilterClause(this, Objects.requireNonNull(filterClause), Connector.AND);
    }

    /**
     * <p>
     * Creates the {@link FilterClause} with "and" criteria. Appends the new criteria after the old one.
     * </p>
     *
     * @param criteria Old {@link Criteria}.
     * @return Created {@link FilterClause}.
     */
    public FilterClause and(final Criteria criteria) {
        return new FilterClause(this, new FilterClause(criteria), Connector.AND);
    }

    /**
     * <p>
     * Provides the {@link FilterClause} of the left.
     * </p>
     *
     * @return Left {@link FilterClause}
     */
    public FilterClause getLeft() {
        return left;
    }

    /**
     * <p>
     * Provides the {@link FilterClause} of the right.
     * </p>
     *
     * @return Right {@link FilterClause}
     */
    public FilterClause getRight() {
        return right;
    }

    /**
     * <p>
     * Provides the {@link Connector} of the {@link FilterClause}.
     * </p>
     *
     * @return {@link Connector} of the {@link FilterClause}.
     */
    public Connector getConnector() {
        return connector;
    }

    /**
     * <p>
     * Returns the boolean flag that represents this {@link FilterClause} has a {@link Connector} or not.
     * </p>
     *
     * @return A flag that indicates the {@link Connector} is available or not.
     */
    public boolean hasConnector() {
        return hasConnector;
    }

    /**
     * <p>
     * Provides the {@link Criteria} of the {@link FilterClause}.
     * </p>
     *
     * @return {@link Criteria} that holds the retrieval criteria.
     */
    public Criteria getCriteria() {
        return criteria;
    }

    /**
     * <p>
     * Copies the current {@link FilterClause}.
     * </p>
     *
     * @return Copied {@link FilterClause}.
     */
    public FilterClause copy() {
        return copy(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();

        loadCriteriaString(builder, this);

        return builder.toString();
    }

    /**
     * <p>
     * Copies the given {@link FilterClause}.
     * </p>
     *
     * @param filterClause {@link FilterClause} for take copy.
     * @return Copied {@link FilterClause}.
     */
    private FilterClause copy(final FilterClause filterClause) {
        final FilterClause newBuilder;

        if (filterClause.hasConnector) {
            final FilterClause left = copy(Objects.requireNonNull(filterClause.left));
            final FilterClause right = copy(Objects.requireNonNull(filterClause.right));
            newBuilder = new FilterClause(left, right, filterClause.connector);
        } else {
            newBuilder = new FilterClause(filterClause.criteria);
        }

        return newBuilder;
    }

    /**
     * <p>
     * Loads the criteria string into {@link StringBuilder}.
     * </p>
     *
     * @param builder      Holds the criteria string value.
     * @param filterClause {@link FilterClause}.
     */
    private void loadCriteriaString(final StringBuilder builder, final FilterClause filterClause) {
        if (filterClause.hasConnector()) {
            builder.append("(");

            final FilterClause left = filterClause.getLeft();
            final FilterClause right = filterClause.getRight();

            loadCriteriaString(builder, left);

            if (filterClause.getConnector() == Connector.AND) {
                builder.append(" && ");
            } else {
                builder.append(" || ");
            }

            loadCriteriaString(builder, right);
            builder.append(")");
        } else {
            builder.append(filterClause.getCriteria());
        }
    }
}
