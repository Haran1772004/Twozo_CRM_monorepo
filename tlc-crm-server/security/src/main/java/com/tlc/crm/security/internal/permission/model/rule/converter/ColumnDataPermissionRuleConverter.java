package com.tlc.crm.security.internal.permission.model.rule.converter;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.security.internal.permission.model.rule.ColumnDataPermissionRule;
import com.tlc.crm.security.internal.permission.model.rule.PermissionRule;
import com.tlc.crm.security.internal.permission.model.rule.constraint.ColumnDataRuleConstraint;
import com.tlc.crm.security.model.converter.RuleConverter;
import com.tlc.sql.common.api.dml.Connector;

import java.util.Collection;

/**
 * Converter for transforming JSON rules into the appropriate {@link ColumnDataPermissionRule} object
 *
 * @author sundar
 * @version 1.0
 * @see RuleConverter
 */
public class ColumnDataPermissionRuleConverter extends RuleConverter<ColumnDataRuleConstraint, ColumnDataPermissionRule> {

    private static final String VALUE_KEY = "value";
    private static final String OPERATOR = "operator";
    private static final String EQUAL = "equal";
    private static final String VALUE_PERMISSION_VALIDATOR = "valuePermissionValidator";

    private ColumnDataPermissionRuleConverter() {
    }

    private static final class Instance {
        private static final ColumnDataPermissionRuleConverter COLUMN_DATA_PERMISSION_RULE_CONVERTER = new ColumnDataPermissionRuleConverter();
    }

    /**
     * Gets the {@link ColumnDataPermissionRule} instance
     *
     * @return the {@link ColumnDataPermissionRule}
     */
    public static ColumnDataPermissionRuleConverter getInstance() {
        return ColumnDataPermissionRuleConverter.Instance.COLUMN_DATA_PERMISSION_RULE_CONVERTER;
    }

    /**
     * {@inheritDoc}
     *
     * @param jsonObject the {@link JsonObject} to build {@link ColumnDataRuleConstraint}
     * @return the {@link ColumnDataRuleConstraint}
     */
    @Override
    protected ColumnDataRuleConstraint buildConstraint(final JsonObject jsonObject) {
        final PermissionRule permissionRule = PermissionRuleConverter.getInstance().convert(jsonObject);

        return new ColumnDataRuleConstraint(jsonObject.optString(OPERATOR, EQUAL), jsonObject.optString(VALUE_PERMISSION_VALIDATOR, null),
                jsonObject.optJsonObject(VALUE_KEY), permissionRule);
    }

    /**
     * Builds a {@link ColumnDataPermissionRule} using the given {@link ColumnDataRuleConstraint}
     *
     * @param constraint the constraint to be wrapped with the {@link ColumnDataPermissionRule}
     * @return the {@link ColumnDataPermissionRule}
     */
    @Override
    protected ColumnDataPermissionRule buildRule(final ColumnDataRuleConstraint constraint) {
        return new ColumnDataPermissionRule(constraint);
    }

    /**
     * Builds the {@link ColumnDataPermissionRule} by connecting the given rules using the provided {@link Connector}
     *
     * @param connector the connector used to link the rules
     * @param rules     the rules to be encapsulated within {@link ColumnDataPermissionRule}
     * @return the wrapped {@link ColumnDataPermissionRule}
     */
    @Override
    protected ColumnDataPermissionRule buildRule(final Connector connector, final Collection<ColumnDataPermissionRule> rules) {
        return new ColumnDataPermissionRule(connector, rules);
    }
}