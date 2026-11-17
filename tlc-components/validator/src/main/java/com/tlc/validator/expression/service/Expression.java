package com.tlc.validator.expression.service;

import com.tlc.validator.expression.Criteria;

import java.util.Objects;

/**
 * An expression composer responsible for formulating the evaluation
 * expression using the supplied {@link Criteria}.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class Expression {

    /**
     * Refers the connector of a criteria.
     */
    public enum Connector {

        /**
         * Indicates the OR connector between the criteria.
         */
        OR,

        /**
         * Indicates the AND connector between the criteria.
         */
        AND;
    }

    private final Expression left;
    private final Expression right;
    private final Connector connector;
    private final Criteria criteria;
    private final boolean hasConnector;

    public Expression(final Criteria criteria) {
        this.hasConnector = false;
        this.connector = null;
        this.left = null;
        this.right = null;
        this.criteria = Objects.requireNonNull(criteria);
    }

    private Expression(final Expression left, final Expression right, final Connector connector) {
        this.hasConnector = true;
        this.left = Objects.requireNonNull(left);
        this.right = Objects.requireNonNull(right);
        this.connector = connector;
        this.criteria = null;
    }

    /**
     * Adds a new {@link Criteria} to the criteria tree using the OR operator.
     * <br/>
     * <span style="color:orange"><b>For Reference: ((A || B) || C)</b></span>
     *
     * @param criteria Refers the criteria to be added.
     * @return The new {@link Criteria} node with the combined criteria one.
     */
    public Expression or(final Criteria criteria) {
        return new Expression(this, new Expression(criteria), Connector.OR);
    }

    /**
     * Adds a new {@link Criteria} to the criteria tree using the OR operator.
     * <br/>
     * <span style="color:orange"><b>For Reference: ((A || B) || (C || D))</b></span>
     *
     * @param tree Refers the new {@link Expression} to be added.
     * @return The new {@link Criteria} node with the combined criteria one.
     */
    public Expression or(final Expression tree) {
        return new Expression(this, Objects.requireNonNull(tree), Connector.OR);
    }

    /**
     * Adds a new {@link Criteria} to the criteria tree using the AND operator.
     * <br/>
     * <span style="color:orange"><b>For Reference: ((A && B) && (C && D))</b></span>
     *
     * @param tree Refers the new {@link Expression} to be added.
     * @return The new {@link Criteria} node with the combined criteria one.
     */
    public Expression and(final Expression tree) {
        return new Expression(this, Objects.requireNonNull(tree), Connector.AND);
    }

    /**
     * Adds a new {@link Criteria} to the criteria tree using the AND operator.
     * <br/>
     * <span style="color:orange"><b>For Reference: ((A && B) && C)</b></span>
     *
     * @param criteria Refers the criteria to be added.
     * @return The new {@link Criteria} node with the combined criteria one.
     */
    public Expression and(final Criteria criteria) {
        return new Expression(this, new Expression(criteria), Connector.AND);
    }

    /**
     * Gets the left tree of a node.
     *
     * @return The left tree.
     */
    public Expression getLeft() {
        return left;
    }

    /**
     * Gets the left tree of a node.
     *
     * @return The left tree.
     */
    public Expression getRight() {
        return right;
    }

    /**
     * Gets the connector.
     *
     * @return The connector.
     */
    public Connector getConnector() {
        return connector;
    }

    /**
     * A flag that indicates whether the criteria has connector or not.
     *
     * @return true, if criteria has connector otherwise false.
     */
    public boolean hasConnector() {
        return hasConnector;
    }

    /**
     * Gets the criteria.
     *
     * @return The criteria of an expression.
     */
    public Criteria getCriteria() {
        return criteria;
    }

    /**
     * Composes the whole criteria and returns the string expression.
     *
     * @return The expression to be evaluated.
     */
    @Override
    public String toString() {
        final StringBuilder expressionBuilder = new StringBuilder();

        loadCriteriaString(expressionBuilder, this);
        return expressionBuilder.toString();
    }

    /**
     * Loads the criteria string into {@link StringBuilder}.
     *
     * @param expressionBuilder Refers the {@link StringBuilder} which comprise the criteria string.
     * @param expression        Refers the {@link Expression}.
     */
    private void loadCriteriaString(final StringBuilder expressionBuilder, final Expression expression) {

        if (expression.hasConnector()) {
            expressionBuilder.append("(");

            final Expression left = expression.getLeft();
            final Expression right = expression.getRight();

            loadCriteriaString(expressionBuilder, left);

            if (expression.getConnector() == Connector.AND) {
                expressionBuilder.append(" && ");
            } else {
                expressionBuilder.append(" || ");
            }

            loadCriteriaString(expressionBuilder, right);

            expressionBuilder.append(")");
        } else {
            expressionBuilder.append(expression.getCriteria());
        }
    }

}
