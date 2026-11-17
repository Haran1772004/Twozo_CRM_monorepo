package com.tlc.sql.common.api.dml;

import java.util.Objects;

/**
 * <p>
 * The HavingClause class represents a HAVING clause in a SQL query, which is used to specify conditions
 * on groups created by the GROUP BY clause.
 * A HAVING clause is used to filter the results of a SQL query based on the results of an aggregate function,
 * such as COUNT, SUM, or AVG, applied to groups of rows.
 * Note that the HAVING clause can only be used in conjunction with the GROUP BY clause,
 * and it must come after the GROUP BY clause in the SQL query.
 * </p>
 *
 * @author eswari
 * @version 1.0
 */
public class HavingClause {

    private final HavingClause left;
    private final HavingClause right;
    private final Connector connector;
    private final boolean isConnector;
    private final boolean isInverted;
    private final Criteria criteria;

    public HavingClause(final Criteria criteria) {
        this.isConnector = false;
        this.isInverted = false;
        this.connector = null;
        this.left = null;
        this.right = null;
        this.criteria = Objects.requireNonNull(criteria);
    }

    public HavingClause(final HavingClause criteriaBuilder, final boolean isInverted) {
        this.isConnector = criteriaBuilder.isConnector;
        this.isInverted = isInverted;
        this.connector = criteriaBuilder.connector;
        this.left = criteriaBuilder.left;
        this.right = criteriaBuilder.right;
        this.criteria = criteriaBuilder.criteria;
    }

    private HavingClause(final HavingClause left, final HavingClause right, final Connector connector) {
        this.isConnector = true;
        this.isInverted = false;
        this.left = Objects.requireNonNull(left);
        this.right = Objects.requireNonNull(right);
        this.connector = connector;
        this.criteria = null;
    }

    /**
     * Adds a new criteria to the criteria tree using the logical OR operator.
     *
     * @param criteria the criteria to add to the tree
     * @return a new HavingClause node that represents the combined criteria tree
     */
    public HavingClause or(final Criteria criteria) {
        return new HavingClause(this, new HavingClause(criteria), Connector.OR);
    }

    /**
     * Adds a new criteria tree to the existing criteria tree using the logical OR operator.
     *
     * @param tree the criteria tree to add to the existing tree
     * @return a new HavingClause node that represents the combined criteria tree
     */
    public HavingClause or(final HavingClause tree) {
        return new HavingClause(this, Objects.requireNonNull(tree), Connector.OR);
    }

    /**
     * Adds a new criteria tree to the existing criteria tree using the logical AND operator.
     *
     * @param tree the criteria tree to add to the existing tree
     * @return a new {@link HavingClause} node that represents the combined criteria tree
     */
    public HavingClause and(final HavingClause tree) {
        return new HavingClause(this, Objects.requireNonNull(tree), Connector.AND);
    }

    /**
     * Adds a new criteria to the criteria tree using the logical AND operator.
     *
     * @param criteria the criteria to add to the tree
     * @return a new {@link HavingClause} node that represents the combined criteria tree
     */
    public HavingClause and(final Criteria criteria) {
        return new HavingClause(this, new HavingClause(criteria), Connector.AND);
    }

    /**
     * Inverts the sense of the current criteria tree.
     *
     * @return a new {@link HavingClause} node that represents the inverted criteria tree
     */
    public HavingClause not() {
        return new HavingClause(this, !this.isInverted());
    }

    /**
     * Returns the left child of the current criteria tree node.
     *
     * @return the left child of the current criteria tree node
     */
    public HavingClause getLeft() {
        return left;
    }

    /**
     * Returns the right child of the current criteria tree node.
     *
     * @return the right child of the current criteria tree node
     */
    public HavingClause getRight() {
        return right;
    }

    /**
     * Returns the connector specifies whether the child nodes should be connected with an "AND" or an "OR" operator.
     *
     * @return the connector used in the current node of the having clause tree
     */
    public Connector getConnector() {
        return connector;
    }

    /**
     * Returns true if this is a connector node which represents a logical operator (AND or OR) between two child nodes.
     *
     * @return true if this is a connector node, false otherwise.
     */
    public boolean isConnector() {
        return isConnector;
    }

    /**
     * Returns a boolean value indicating whether the criteria in this having clause is inverted or not.
     *
     * @return true if this is inverted or false
     */
    public boolean isInverted() {
        return isInverted;
    }

    /**
     * This method returns the Criteria object associated with the current HavingClause instance.
     *
     * @return {@link Criteria} criteria object
     */
    public Criteria getCriteria() {
        return criteria;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();

        loadCriteriaString(builder, this);

        return builder.toString();
    }

    /**
     * Recursively generates a string representation of the current criteria tree.
     *
     * @param builder         the {@link StringBuilder} to append the string representation of the criteria tree
     * @param criteriaBuilder the current node in the criteria tree to generate the string representation
     */
    private void loadCriteriaString(final StringBuilder builder, final HavingClause criteriaBuilder) {
        if (criteriaBuilder.isInverted()) {
            builder.append("!");
        }

        if (criteriaBuilder.isConnector()) {
            builder.append("(");
            final HavingClause left = criteriaBuilder.getLeft();
            final HavingClause right = criteriaBuilder.getRight();

            loadCriteriaString(builder, left);
            builder.append(criteriaBuilder.getConnector() == Connector.AND ? " && " : " || ");
            loadCriteriaString(builder, right);
            builder.append(")");
        } else {
            builder.append(criteriaBuilder.getCriteria());
        }
    }
}