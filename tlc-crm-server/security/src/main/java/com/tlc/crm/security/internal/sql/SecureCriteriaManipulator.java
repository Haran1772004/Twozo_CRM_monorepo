package com.tlc.crm.security.internal.sql;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Connector;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.secure.JoinConfigs;
import com.tlc.sql.common.api.dml.secure.PermissionCriteria;
import com.tlc.sql.common.api.dml.secure.RefactoredWhereClause;
import com.tlc.sql.common.api.dml.util.WhereClauseUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Represents a manipulator for secure criteria, encapsulating a {@link WhereClause} and associated join configurations.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
final class SecureCriteriaManipulator {

    private static final List<JoinClause> EMPTY_JOIN_CLAUSES = Collections.emptyList();

    private final WhereClause baseWhereClause;
    private final WhereClause secureWhereClause;
    private final Map<String, JoinConfigs> joinConfigs;
    private final Map<String, Criteria> supportingCriteria;

    SecureCriteriaManipulator(final WhereClause baseWhereClause, final PermissionCriteria permissionCriteria) {
        this.baseWhereClause = baseWhereClause;
        this.secureWhereClause = permissionCriteria.whereClause();
        this.joinConfigs = permissionCriteria.joinConfigs();
        this.supportingCriteria = new HashMap<>();
    }

    /**
     * <p>
     * Adds the supporting criteria.
     * </p>
     *
     * @param supportingCriteria Represents the supporting criteria keyed by criteria table name.
     */
    public void addSupportingCriteria(final Map<String, Criteria> supportingCriteria) {
        this.supportingCriteria.putAll(supportingCriteria);
    }

    /**
     * <p>
     * Gets the where clause with or without inner queries
     * </p>
     *
     * @param baseTable Represents the {@link Table}
     * @return the {@link WhereClause}
     */
    public WhereClause getWhereClause(final Table baseTable) {
        if (Objects.isNull(secureWhereClause)) {
            return baseWhereClause;
        }
        return new RefactoredWhereClause(WhereClauseUtil.connect(baseWhereClause, convertToExistCriteria(baseTable, secureWhereClause), Connector.AND));
    }

    /**
     * <p>
     * Gets the missing joins based on the criteria.
     * </p>
     *
     * @param baseTable   Represents the base table
     * @param whereClause Represents the where clause
     * @return the list of {@link JoinClause} representing the missing joins
     */
    private WhereClause convertToExistCriteria(final Table baseTable, final WhereClause whereClause) {

        if (whereClause.isConnector()) {
            final WhereClause left = convertToExistCriteria(baseTable, whereClause.getLeft());
            final WhereClause right = convertToExistCriteria(baseTable, whereClause.getRight());

            return WhereClauseUtil.connect(left, right, whereClause.getConnector());
        } else {
            final Criteria criteria = whereClause.getCriteria();
            final Column column = criteria.getColumn();
            final Table criteriaTable = column.getTable();

            if (Objects.equals(baseTable, criteriaTable)) {
                return whereClause;
            } else if (Objects.equals(baseTable.getName(), criteriaTable.getName())) {
                return new WhereClause(new Criteria(baseTable.getColumn(column.getColumnName()), criteria.getValue(), criteria.getOperator()));
            }
            final SelectQuery selectQuery = SelectQuery.get(criteriaTable);

            selectQuery.addStaticSelectClause(NumericalConstant.ONE);
            selectQuery.addJoinClause(getJoins(criteriaTable));
            selectQuery.setWhereClause(appendSupportingCriteria(criteriaTable, whereClause));

            return new WhereClause(Criteria.exists(selectQuery));
        }
    }

    /**
     * <p>
     * Gets the list of {@link JoinClause} based on the criteria table.
     * </p>
     *
     * @param criteriaTable Represents the criteria {@link Table}
     * @return the list of {@link JoinClause} representing the joins for the criteria table
     */
    private List<JoinClause> getJoins(final Table criteriaTable) {
        final JoinConfigs joinConfigs = this.joinConfigs.get(criteriaTable.getName());

        if (Objects.nonNull(joinConfigs)) {
            return joinConfigs.getJoins(true);
        }
        return EMPTY_JOIN_CLAUSES;
    }

    /**
     * <p>
     * Appends the supporting criteria to the given where clause based on the base table and criteria table.
     * </p>
     *
     * @param criteriaTable Represents the criteria {@link Table}
     * @param whereClause   Represents the existing {@link WhereClause}
     * @return the updated {@link WhereClause} with supporting criteria appended if available
     */
    private WhereClause appendSupportingCriteria(final Table criteriaTable, final WhereClause whereClause) {
        final Criteria supportingCriteria = this.supportingCriteria.get(criteriaTable.getName());

        if (Objects.nonNull(supportingCriteria)) {
            return WhereClauseUtil.connect(new WhereClause(supportingCriteria), whereClause, Connector.AND);
        }
        return whereClause;
    }
}