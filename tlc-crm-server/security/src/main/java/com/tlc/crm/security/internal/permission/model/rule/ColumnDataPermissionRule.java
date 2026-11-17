package com.tlc.crm.security.internal.permission.model.rule;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.crm.security.internal.permission.model.ColumnDataPermissionValidator;
import com.tlc.crm.security.internal.permission.model.SecureDefaultValue;
import com.tlc.crm.security.internal.permission.model.rule.constraint.ColumnDataRuleConstraint;
import com.tlc.crm.security.internal.status.PermissionErrorCodes;
import com.tlc.crm.security.model.Rule;

import com.tlc.sql.common.api.dml.Connector;
import com.tlc.sql.common.api.dml.Operator;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Rules for establishing column data permission criteria required for operations
 *
 * @author Sundar
 * @version 1.0
 * @see Rule
 */
public class ColumnDataPermissionRule extends Rule<ColumnDataRuleConstraint, Object> {

    public ColumnDataPermissionRule(final Connector connector, final Collection<ColumnDataPermissionRule> rules) {
        super(connector, rules);
    }

    public ColumnDataPermissionRule(final ColumnDataRuleConstraint constraint) {
        super(constraint);
    }

    /**
     * {@inheritDoc}
     *
     * @param data the data to evaluate
     * @return true if the constraints are met, false otherwise
     */
    @Override
    protected boolean testConstraint(final Object data) {
        final ColumnDataRuleConstraint columnDataRuleConstraint = getConstraint();

        if (columnDataRuleConstraint.getPermissionRule().test(LoggedInUserInfo.getSecurityContext())) {
            final Optional<SecureDefaultValue> secureDefaultValue = columnDataRuleConstraint.getValue();

            if (secureDefaultValue.isPresent()) {
                final Optional<Object> optionalValue = secureDefaultValue.get().getValue();

                return optionalValue.map(value -> validateValues(data, value, columnDataRuleConstraint.getOperator())).orElseGet(() -> true);
            }
            final Optional<ColumnDataPermissionValidator<Object>> dataPermissionValidator = columnDataRuleConstraint.getDataPermissionValidator();

            if (dataPermissionValidator.isPresent()) {
                return dataPermissionValidator.get().validate(data);
            }
        }
        return true;
    }

    /**
     * <p>
     * Validates the given values based on {@link Operator}.
     * </p>
     *
     * @param providedValue Represents the provided value.
     * @param defaultValue  Represents the default value.
     * @param operator      Represents the {@link Operator}.
     * @return A flag that indicates whether the provided value matches the default value.
     */
    private boolean validateValues(final Object providedValue, final Object defaultValue, final Operator operator) {
        if (Objects.isNull(providedValue)) {
            return true;
        }
        final Supplier<Boolean> stringEqualsChecker = () -> Objects.equals(providedValue, defaultValue);
        final Supplier<Boolean> stringContainsChecker = () ->
                defaultValue instanceof final String defaultStringValue && providedValue instanceof final String providedValueAsString
                        ? defaultStringValue.contains(providedValueAsString) : stringEqualsChecker.get();
        final Supplier<Boolean> collectionContainsChecker = () ->
                defaultValue instanceof final Collection<?> defaultValues ? defaultValues.contains(providedValue) : stringEqualsChecker.get();

        return switch (operator) {
            case EQUAL -> stringEqualsChecker.get();
            case NOT_EQUAL -> !stringEqualsChecker.get();
            case IN -> collectionContainsChecker.get();
            case NOT_IN -> !collectionContainsChecker.get();
            case CONTAINS -> stringContainsChecker.get();
            case NOT_CONTAINS -> !stringContainsChecker.get();

            default -> throw ErrorCode.get(PermissionErrorCodes.UNSUPPORTED_OPERATION);
        };
    }
}