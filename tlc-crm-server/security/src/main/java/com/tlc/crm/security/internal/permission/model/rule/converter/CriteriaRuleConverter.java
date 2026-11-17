package com.tlc.crm.security.internal.permission.model.rule.converter;

import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.sql.common.api.dml.secure.JoinConfigs;
import com.tlc.crm.security.internal.permission.model.SecureDefaultValue;
import com.tlc.crm.security.internal.permission.model.rule.CriteriaRule;
import com.tlc.crm.security.internal.permission.model.rule.constraint.CriteriaConstraint;
import com.tlc.crm.security.internal.permission.model.rule.constraint.CriteriaConstraintData;
import com.tlc.crm.security.model.converter.RuleConverter;

import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Connector;
import com.tlc.sql.common.api.dml.Operator;
import com.tlc.sql.common.api.dml.Table;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Objects;

/**
 * Converter for transforming JSON rules into the appropriate {@link CriteriaRule} object
 *
 * @author Sathish
 * @version 1.0
 * @see RuleConverter
 */
public class CriteriaRuleConverter extends RuleConverter<CriteriaConstraint, CriteriaRule> {

    private static final String TABLE = "table";
    private static final String COLUMN = "column";
    private static final String OPERATOR = "operator";
    private static final String VALUE = "value";
    private static final String ORG_ID_PROVIDER = "orgIdProvider";
    private static final String CRITERIA = "criteria";
    private static final String BASE_TABLE = "baseTable";
    private static final String JOIN_CONFIG = "joinConfig";

    private CriteriaRuleConverter() {
    }

    private static final class Instance {
        private static final CriteriaRuleConverter PERMISSION_RULE_CONVERTER = new CriteriaRuleConverter();
    }

    /**
     * Gets the {@link CriteriaRuleConverter} instance
     *
     * @return the {@link CriteriaRuleConverter}
     */
    public static CriteriaRuleConverter getInstance() {
        return Instance.PERMISSION_RULE_CONVERTER;
    }

    /**
     * {@inheritDoc}
     *
     * @param jsonObject the {@link JsonObject} to convert
     * @return the corresponding {@link CriteriaRule}
     */
    @Override
    public CriteriaRule convert(final JsonObject jsonObject) {
        return jsonObject.containsKey(RULES) ? super.convert(jsonObject) : buildRule(buildConstraint(jsonObject));
    }

    /**
     * {@inheritDoc}
     *
     * @param jsonObject the {@link JsonObject} to build {@link CriteriaConstraint}
     * @return the {@link CriteriaConstraint}
     */
    @Override
    protected CriteriaConstraint buildConstraint(final JsonObject jsonObject) {
        final Collection<CriteriaConstraintData> criteriaConstraintData = new LinkedList<>();
        final JsonArray criteriaArray = jsonObject.getJsonArray(CRITERIA);
        final int criteriaSize = criteriaArray.size();

        for (int index = 0; index < criteriaSize; index++) {
            final JsonObject criteriaJson = criteriaArray.getJsonObject(index);

            final String tableName = criteriaJson.getString(TABLE);
            final String columnName = criteriaJson.getString(COLUMN);
            final Column column = Table.get(tableName).getColumn(columnName);
            final Operator operator = Operator.getOperator(criteriaJson.getString(OPERATOR));
            final JsonObject valueJson = criteriaJson.optJsonObject(VALUE);
            final SecureDefaultValue value = new SecureDefaultValue(Objects.isNull(valueJson) ? criteriaJson : valueJson);

            criteriaConstraintData.add(new CriteriaConstraintData(column, value, operator));
        }
        final JoinConfigs joinConfigs = new JoinConfigs(jsonObject.optJsonArray(JOIN_CONFIG));
        final Table baseTable = Table.get(jsonObject.getString(BASE_TABLE));
        final Connector connector = Connector.valueOf(jsonObject.getString(CONNECTOR));
        final SecureDefaultValue orgId = new SecureDefaultValue(jsonObject.getString(ORG_ID_PROVIDER));

        return new CriteriaConstraint(orgId, baseTable, joinConfigs, connector, criteriaConstraintData);
    }

    /**
     * Builds a {@link CriteriaRule} using the given {@link CriteriaConstraint}
     *
     * @param constraint the constraint to be wrapped with the {@link CriteriaRule}
     * @return the {@link CriteriaRule}
     */
    @Override
    protected CriteriaRule buildRule(final CriteriaConstraint constraint) {
        return new CriteriaRule(constraint);
    }

    /**
     * Builds the {@link CriteriaRule} by connecting the given rules using the provided {@link Connector}
     *
     * @param connector the connector used to link the rules
     * @param rules     the rules to be encapsulated within {@link CriteriaRule}
     * @return the {@link CriteriaRule}
     */
    @Override
    protected CriteriaRule buildRule(final Connector connector, final Collection<CriteriaRule> rules) {
        return new CriteriaRule(connector, rules);
    }
}