package com.tlc.crm.security.internal.permission.model.rule.converter;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.security.internal.permission.model.rule.PermissionRule;
import com.tlc.crm.security.internal.permission.model.rule.constraint.PermissionConstraint;
import com.tlc.crm.security.model.converter.RuleConverter;
import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.Scope;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.sql.common.api.dml.Connector;

import java.util.Collection;

/**
 * Converter for transforming JSON rules into the appropriate {@link PermissionRule} object
 *
 * @author sundar
 * @version 1.0
 * @see RuleConverter
 */
public class PermissionRuleConverter extends RuleConverter<PermissionConstraint, PermissionRule> {

    private static final String CATEGORY = "category";
    private static final String SUB_CATEGORY = "subCategory";
    private static final String ACTION = "action";
    private static final String SCOPE = "scope";
    private static final String PERMISSION_STATUS_FLAG = "permissionStatusFlag";
    private static final String CHECK_CURRENT_USER_PERMISSION = "checkOnlyCurrentUserPermission";

    private PermissionRuleConverter() {
    }

    private static final class Instance {
        private static final PermissionRuleConverter PERMISSION_RULE_CONVERTER = new PermissionRuleConverter();
    }

    /**
     * Gets the {@link PermissionRuleConverter} instance
     *
     * @return the {@link PermissionRuleConverter}
     */
    public static PermissionRuleConverter getInstance() {
        return Instance.PERMISSION_RULE_CONVERTER;
    }

    /**
     * {@inheritDoc}
     *
     * @param jsonObject the {@link JsonObject} to build {@link PermissionConstraint}
     * @return the {@link PermissionConstraint}
     */
    @Override
    protected PermissionConstraint buildConstraint(final JsonObject jsonObject) {
        final Category.Type category = Category.Type.get(jsonObject.getString(CATEGORY));
        final SubCategory.Type subCategory = SubCategory.Type.get(jsonObject.getString(SUB_CATEGORY));
        final Action.Type action = Action.Type.get(jsonObject.getString(ACTION));
        final Scope scope = jsonObject.containsKey(SCOPE) ? Scope.get(jsonObject.getString(SCOPE)) : null;
        final boolean permissionStatusFlag = jsonObject.getBoolean(PERMISSION_STATUS_FLAG);
        final boolean checkCurrentUserPermission = jsonObject.optBoolean(CHECK_CURRENT_USER_PERMISSION);

        return new PermissionConstraint(category, subCategory, action, scope, permissionStatusFlag, checkCurrentUserPermission);
    }

    /**
     * Builds a {@link PermissionRule} using the given {@link PermissionConstraint}
     *
     * @param constraint the constraint to be wrapped with the {@link PermissionRule}
     * @return the {@link PermissionRule}
     */
    @Override
    protected PermissionRule buildRule(final PermissionConstraint constraint) {
        return new PermissionRule(constraint);
    }

    /**
     * Builds the {@link PermissionRule} by connecting the given rules using the provided {@link Connector}
     *
     * @param connector the connector used to link the rules
     * @param rules     the rules to be encapsulated within {@link PermissionRule}
     * @return the {@link PermissionRule}
     */
    @Override
    protected PermissionRule buildRule(final Connector connector, final Collection<PermissionRule> rules) {
        return new PermissionRule(connector, rules);
    }
}