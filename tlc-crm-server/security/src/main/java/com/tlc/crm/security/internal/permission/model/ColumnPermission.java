package com.tlc.crm.security.internal.permission.model;

import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.crm.security.internal.permission.model.rule.ColumnDataPermissionRule;
import com.tlc.crm.security.internal.permission.model.rule.converter.ColumnDataPermissionRuleConverter;
import com.tlc.crm.security.internal.permission.model.rule.PermissionRule;
import com.tlc.crm.security.internal.permission.model.rule.converter.PermissionRuleConverter;
import com.tlc.crm.security.internal.permission.registry.ColumnValueResolverRegistry;
import com.tlc.crm.security.permission.model.Permission;
import com.tlc.crm.security.permission.model.ScopeParticipant;
import com.tlc.sql.common.api.Operation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

/**
 * Holds the column level permission information
 *
 * @author sundar
 * @version 1.0
 */
public final class ColumnPermission implements Permission {

    private static final String SCOPE_PARTICIPANT_KEY = "scopeParticipant";
    private static final String WRITE_RESTRICTION_RULES_KEY = "writeRestrictionRules";
    private static final String DEFAULT_KEY = "default";
    private static final String APPLY_WHEN_KEY = "applyWhen";
    private static final String RESTRICTION_CONSTRAINTS_KEY = "restrictionConstraints";
    private static final String SECURE_COLUMN_VALUE_RESOLVER = "secureColumnValueResolver";

    private final boolean isSecure;
    private final String columnName;
    private final ScopeParticipant scopeParticipant;
    private final WriteRestrictionRules writeRestrictionRules;
    private final ColumnDefaultValue defaultValue;
    private final Collection<RestrictionConstraint> restrictionConstraints;
    private final SecureColumnValueResolver valueResolver;

    public ColumnPermission(final String columnName, final JsonObject configJson, final boolean isSecure) {
        this.columnName = columnName;
        this.scopeParticipant = configJson.containsKey(SCOPE_PARTICIPANT_KEY) ? ScopeParticipant.valueOf(configJson.getString(SCOPE_PARTICIPANT_KEY)) : null;
        this.isSecure = isSecure;
        this.defaultValue = configJson.containsKey(DEFAULT_KEY) ? new ColumnDefaultValue(configJson.getJsonObject(DEFAULT_KEY)) : null;
        this.writeRestrictionRules = buildWriteRestrictionRules(configJson);
        this.restrictionConstraints = buildRestrictionConstraints(configJson);
        this.valueResolver = configJson.containsKey(SECURE_COLUMN_VALUE_RESOLVER)
                ? ColumnValueResolverRegistry.getInstance().get(configJson.getString(SECURE_COLUMN_VALUE_RESOLVER)) : null;
    }

    /**
     * Gets the column name
     *
     * @return the column name
     */
    public String getColumnName() {
        return columnName;
    }

    /**
     * Gets the {@link ScopeParticipant}
     *
     * @return the {@link ScopeParticipant}
     */
    public ScopeParticipant getScopeParticipant() {
        return scopeParticipant;
    }

    /**
     * Indicates whether the column is secure
     *
     * @return true if the column is secure; otherwise false
     */
    public boolean isSecure() {
        return isSecure;
    }

    /**
     * Gets the default value for this column from a specific origin
     *
     * @return The default value
     */
    public Optional<Object> getDefaultValue() {
        return Objects.isNull(defaultValue) ? Optional.empty() : defaultValue.getValue();
    }

    /**
     * Checks whether the write operation is done on the associated column with this permission
     *
     * @return true if the write operation is possible on the associated column; false otherwise
     */
    public boolean canWrite() {

        if (Objects.isNull(writeRestrictionRules)) {
            return true;
        }
        final Optional<PermissionRule> optionalPermissionRule = writeRestrictionRules.getColumnPermissionRule();

        return optionalPermissionRule.isEmpty() || !optionalPermissionRule.get().test(LoggedInUserInfo.getSecurityContext());
    }

    /**
     * Checks whether the write operation is done on the associated column with this permission
     *
     * @return true if the write operation is possible on the associated column; false otherwise
     */
    public boolean canWrite(final Object data) {

        if (Objects.isNull(writeRestrictionRules)) {
            return true;
        }
        final Optional<ColumnDataPermissionRule> optionalColumnDataPermissionRule = writeRestrictionRules.getDataPermissionRule();

        return optionalColumnDataPermissionRule.isEmpty() || optionalColumnDataPermissionRule.get().test(data);
    }

    /**
     * Checks whether the default value associated with this permission is accessible for the given permission or not
     *
     * @return true if the default value is accessible for the given permission; false otherwise
     */
    public boolean isDefaultValueAccessible() {
        return Objects.nonNull(defaultValue) && defaultValue.isAccessible();
    }

    /**
     * Builds the collection of write restriction rules using the config JSON
     *
     * @param configJson the configuration used to build the write restriction rules
     * @return the {@link Collection} of write restriction rules
     */
    private WriteRestrictionRules buildWriteRestrictionRules(final JsonObject configJson) {

        if (configJson.containsKey(WRITE_RESTRICTION_RULES_KEY)) {
            return new WriteRestrictionRules(configJson.getJsonObject(WRITE_RESTRICTION_RULES_KEY));
        } else {
            return null;
        }
    }

    /**
     * <p>
     * Builds the {@link RestrictionConstraint} by using config json.
     * </p>
     *
     * @param configJson Represents the config as JSON.
     * @return Collection of {@link RestrictionConstraint}
     */
    private Collection<RestrictionConstraint> buildRestrictionConstraints(final JsonObject configJson) {
        final Collection<RestrictionConstraint> restrictionConstraints = new ArrayList<>();

        if (configJson.containsKey(RESTRICTION_CONSTRAINTS_KEY)) {
            final JsonArray restrictionConstraintsArray = configJson.getJsonArray(RESTRICTION_CONSTRAINTS_KEY);
            final int size = restrictionConstraintsArray.size();

            for (int index = 0; index < size; index++) {
                restrictionConstraints.add(new RestrictionConstraint(restrictionConstraintsArray.getJsonObject(index)));
            }
        } else {
            for (final Operation operation : Operation.values()) {
                restrictionConstraints.add(new RestrictionConstraint(RestrictionConstraint.Type.INCLUDE, operation));
            }
        }
        return restrictionConstraints;
    }

    /**
     * <p>
     * Provides the {@link RestrictionConstraint} of the column.
     * </p>
     *
     * @return {@link RestrictionConstraint} of the column.
     */
    public Collection<RestrictionConstraint> getRestrictionConstraints() {
        return restrictionConstraints;
    }

    /**
     * <p>
     * Checks whether the {@link SecureColumnValueResolver} is available for the column or not.
     * </p>
     *
     * @return A flag that indicates whether the {@link SecureColumnValueResolver} is available or not.
     */
    public boolean hasSecureValueResolver() {
        return Objects.nonNull(valueResolver);
    }

    /**
     * <p>
     * Provides the {@link SecureColumnValueResolver} of the column.
     * </p>
     *
     * @return {@link SecureColumnValueResolver} of the column.
     */
    public SecureColumnValueResolver getSecureValueResolver() {
        return valueResolver;
    }

    /**
     * Represents the default value defined for the column across specified origins.
     *
     * @author sundar
     * @version 1.0
     * @see SecureDefaultValue
     */
    private static class ColumnDefaultValue extends SecureDefaultValue {

        private final PermissionRule permissionRule;

        private ColumnDefaultValue(final JsonObject defaultValueJson) {
            super(defaultValueJson);
            this.permissionRule = PermissionRuleConverter.getInstance().convert(defaultValueJson.getJsonObject(APPLY_WHEN_KEY));
        }

        /**
         * {@inheritDoc}
         *
         * @return the value
         */
        @Override
        public Optional<Object> getValue() {
            return isAccessible() ? super.getValue() : Optional.empty();
        }

        /**
         * Checks whether the default value is accessible for the given permission
         *
         * @return true if the default value is accessible; false otherwise
         */
        private boolean isAccessible() {
            return Objects.nonNull(permissionRule) && permissionRule.test(LoggedInUserInfo.getSecurityContext());
        }
    }

    /**
     * Holds the permission rules that control access restrictions to columns
     *
     * @author sundar
     * @version 1.0
     */
    private static class WriteRestrictionRules {

        private static final String COLUMN_PERMISSION_RULE = "columnPermissionRule";
        private static final String DATA_PERMISSION_RULE = "dataPermissionRule";

        private PermissionRule columnPermissionRule;
        private ColumnDataPermissionRule dataPermissionRule;

        private WriteRestrictionRules(final JsonObject jsonObject) {

            if (jsonObject.containsKey(COLUMN_PERMISSION_RULE)) {
                this.columnPermissionRule = PermissionRuleConverter.getInstance().convert(jsonObject.getJsonObject(COLUMN_PERMISSION_RULE).getJsonObject(APPLY_WHEN_KEY));
            }

            if (jsonObject.containsKey(DATA_PERMISSION_RULE)) {
                this.dataPermissionRule = ColumnDataPermissionRuleConverter.getInstance().convert(jsonObject.getJsonObject(DATA_PERMISSION_RULE).getJsonObject(APPLY_WHEN_KEY));
            }
        }

        /**
         * Gets the {@link PermissionRule}
         *
         * @return the {@link PermissionRule} optionally
         */
        public Optional<PermissionRule> getColumnPermissionRule() {
            return Optional.ofNullable(columnPermissionRule);
        }

        /**
         * Gets the {@link ColumnDataPermissionRule}
         *
         * @return the {@link ColumnDataPermissionRule}
         */
        public Optional<ColumnDataPermissionRule> getDataPermissionRule() {
            return Optional.ofNullable(dataPermissionRule);
        }
    }
}