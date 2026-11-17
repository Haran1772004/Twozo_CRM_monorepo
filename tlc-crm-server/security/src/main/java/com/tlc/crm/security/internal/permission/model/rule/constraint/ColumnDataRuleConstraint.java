package com.tlc.crm.security.internal.permission.model.rule.constraint;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.security.internal.permission.model.ColumnDataPermissionValidator;
import com.tlc.crm.security.internal.permission.model.SecureDefaultValue;
import com.tlc.crm.security.internal.permission.model.rule.PermissionRule;
import com.tlc.crm.security.model.Constraint;
import com.tlc.crm.security.permission.ColumnDataPermissionValidatorRegistry;
import com.tlc.sql.common.api.dml.Operator;

import java.util.Objects;
import java.util.Optional;

/**
 * Constraint for validating the column data rule
 *
 * @author sundar
 * @version 1.0
 */
public class ColumnDataRuleConstraint implements Constraint {

    private final PermissionRule permissionRule;
    private final Operator operator;

    private SecureDefaultValue dataPermissionValue;
    private ColumnDataPermissionValidator<Object> columnDataPermissionValidator;

    public ColumnDataRuleConstraint(final String operator, final String columnDataPermissionValidator, final JsonObject valueJson,
                                    final PermissionRule permissionRule) {
        this.permissionRule = permissionRule;
        this.operator = Operator.getOperator(operator);

        if (Objects.nonNull(valueJson)) {
            this.dataPermissionValue = new SecureDefaultValue(valueJson);
        }

        if (Objects.nonNull(columnDataPermissionValidator)) {
            this.columnDataPermissionValidator = ColumnDataPermissionValidatorRegistry.getInstance().get(columnDataPermissionValidator);
        }
    }

    /**
     * Gets the data permission {@link SecureDefaultValue}
     *
     * @return the data permission {@link SecureDefaultValue}
     */
    public Optional<SecureDefaultValue> getValue() {
        return Optional.ofNullable(dataPermissionValue);
    }

    /**
     * Gets the {@link ColumnDataPermissionValidator}
     *
     * @return the {@link ColumnDataPermissionValidator}
     */
    public Optional<ColumnDataPermissionValidator<Object>> getDataPermissionValidator() {
        return Optional.ofNullable(columnDataPermissionValidator);
    }

    /**
     * Gets the {@link PermissionRule}
     *
     * @return the {@link PermissionRule}
     */
    public PermissionRule getPermissionRule() {
        return permissionRule;
    }

    /**
     * Gets the {@link Operator}
     *
     * @return the {@link Operator}
     */
    public Operator getOperator() {
        return operator;
    }
}