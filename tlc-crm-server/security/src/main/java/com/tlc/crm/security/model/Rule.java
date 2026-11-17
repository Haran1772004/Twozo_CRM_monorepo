package com.tlc.crm.security.model;

import com.tlc.sql.common.api.dml.Connector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Rules for setting criteria necessary for operations
 *
 * @param <C> the type of {@link Constraint}
 * @param <D> the type of data to be tested
 * @author sundar
 * @version 1.0
 */
public abstract class Rule<C extends Constraint, D> implements Predicate<D> {

    protected final Collection<? extends Rule<C, D>> rules;
    private final boolean hasConstraint;

    protected Connector connector;
    protected C constraint;

    protected Rule(final Connector connector, final Collection<? extends Rule<C, D>> ruleCriteria) {
        this.connector = Objects.requireNonNull(connector);
        this.rules = new ArrayList<>(Objects.requireNonNull(ruleCriteria));
        this.hasConstraint = false;
    }

    protected Rule(final C constraint) {
        this.rules = Collections.emptyList();
        this.constraint = Objects.requireNonNull(constraint);
        this.hasConstraint = true;
    }

    /**
     * Gets the constraint
     *
     * @return {@link Constraint}
     */
    public C getConstraint() {
        return constraint;
    }

    /**
     * Gets the connector
     *
     * @return {@link Connector}
     */
    public Connector getConnector() {
        return connector;
    }

    /**
     * Evaluates the rule using the provided test data
     *
     * @param testData the data to evaluate against the rule
     * @return true if the rule is met, false otherwise
     */
    @Override
    public final boolean test(final D testData) {
        return hasConstraint ? testConstraint(testData) : test(connector, rules, testData);
    }

    /**
     * Evaluates the constraints using the provided data
     *
     * @param data the data to evaluate
     * @return true if the constraints are met, false otherwise
     */
    protected abstract boolean testConstraint(final D data);

    /**
     * Evaluates the provided rules using the given data by connecting them
     *
     * @param connector the connector used to connect the rules
     * @param rules     the rules to be evaluated
     * @param testData  the data to be evaluated
     * @return true if the rules are met, false otherwise
     */
    private boolean test(final Connector connector, final Collection<? extends Rule<C, D>> rules, final D testData) {
        Predicate<D> basePredicate = null;

        for (final Rule<C, D> rule : rules) {

            if (Objects.isNull(basePredicate)) {
                basePredicate = rule;
            } else {
                basePredicate = switch (connector) {
                    case AND -> basePredicate.and(rule);
                    case OR -> basePredicate.or(rule);
                };
            }
        }
        return Objects.nonNull(basePredicate) && basePredicate.test(testData);
    }

    /**
     * Represents the type of the rules.
     */
    public enum Type {
        PERMISSION, CRITERIA
    }
}