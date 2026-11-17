package com.tlc.crm.security.internal.permission.model.rule;

import com.tlc.crm.security.context.SecurityContext;
import com.tlc.sql.common.api.dml.secure.JoinConfigs;
import com.tlc.crm.security.internal.permission.model.rule.constraint.CriteriaConstraint;
import com.tlc.crm.security.internal.permission.model.rule.constraint.CriteriaConstraintData;
import com.tlc.crm.security.model.Rule;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Connector;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Operator;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.util.WhereClauseUtil;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

/**
 * Rules for establishing criteria required for operations.
 *
 * @author Sathish
 * @version 1.0
 * @see Rule
 */
public class CriteriaRule extends Rule<CriteriaConstraint, SecurityContext> {

    public CriteriaRule(final Connector connector, final Collection<? extends CriteriaRule> rules) {
        super(connector, rules);
    }

    public CriteriaRule(final CriteriaConstraint constraint) {
        super(constraint);
    }

    /**
     * {@inheritDoc}
     *
     * @param securityContext the data to evaluate
     * @return true if the constraints are met, false otherwise
     */
    @Override
    protected boolean testConstraint(final SecurityContext securityContext) {
        final Optional<Object> optionalOrgId = constraint.orgId().getValue();

        if (optionalOrgId.isPresent() && optionalOrgId.get() instanceof final Long orgId) {
            final CountQuery countQuery = CountQuery.get(constraint.baseTable());
            final JoinConfigs joinConfigs = constraint.joinConfigs();
            final Connector connector = constraint.connector();
            final Collection<CriteriaConstraintData> criteriaConstraintDataList = constraint.criteriaConstraintData();
            WhereClause whereClause = null;

            for (final CriteriaConstraintData criteriaConstraintData : criteriaConstraintDataList) {
                final Operator operator = criteriaConstraintData.operator();
                final Object value = criteriaConstraintData.value().getValue().orElseGet(() -> null);

                if (operator.isMultiValueOperator() && Objects.isNull(value)) {
                    return false;
                }
                final Column column = criteriaConstraintData.column();
                final WhereClause criteria = new WhereClause(new Criteria(column, value, operator));

                whereClause = Objects.isNull(whereClause) ? criteria : WhereClauseUtil.connect(whereClause, criteria, connector);

                countQuery.addJoinClause(joinConfigs.getJoins(column.getTable()));
            }
            countQuery.setWhereClause(whereClause);

            return 0 < DataStoreProvider.getOrgDataStore(orgId).get(countQuery);
        }
        return false;
    }
}
