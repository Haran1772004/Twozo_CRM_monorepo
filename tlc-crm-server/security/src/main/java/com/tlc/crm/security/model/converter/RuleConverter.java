package com.tlc.crm.security.model.converter;

import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.security.model.Constraint;
import com.tlc.crm.security.model.Rule;
import com.tlc.sql.common.api.dml.Connector;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Converter for transforming JSON rules into the appropriate Rule object
 *
 * @param <C> Represents the {@link Constraint}
 * @param <R> Represents the {@link Rule}
 * @author sundar
 * @version 1.0
 */
public abstract class RuleConverter<C extends Constraint, R extends Rule<C, ?>> {

    protected static final String CONNECTOR = "connector";
    protected static final String RULES = "rules";

    /**
     * Converts the given {@link JsonObject} to a {@link R}
     *
     * @param jsonObject the {@link JsonObject} to convert
     * @return the corresponding {@link R}
     */
    public R convert(final JsonObject jsonObject) {

        if (jsonObject.containsKey(CONNECTOR)) {
            final Connector connector = Connector.valueOf(jsonObject.getString(CONNECTOR));
            final Collection<R> rules = new ArrayList<>();
            final JsonArray rulesArray = jsonObject.getJsonArray(RULES);
            final int rulesArraySize = rulesArray.size();

            for (int index = 0; index < rulesArraySize; index++) {
                final JsonObject ruleJson = rulesArray.getJsonObject(index);

                rules.add(convert(ruleJson));
            }
            return buildRule(connector, rules);
        } else {
            return buildRule(buildConstraint(jsonObject));
        }
    }

    /**
     * Builds the constraint from the given {@link JsonObject}
     *
     * @param jsonObject the {@link JsonObject} to build {@link C}
     * @return the {@link C}
     */
    protected abstract C buildConstraint(final JsonObject jsonObject);

    /**
     * Builds a {@link R} using the given {@link C}
     *
     * @param constraint the constraint to be wrapped with the {@link R}
     * @return the {@link R}
     */
    protected abstract R buildRule(final C constraint);

    /**
     * Builds the {@link R} by connecting the given rules using the provided {@link Connector}
     *
     * @param connector the connector used to link the rules
     * @param rules     the rules to be encapsulated within {@link R}
     * @return the wrapped {@link R}
     */
    protected abstract R buildRule(final Connector connector, final Collection<R> rules);
}