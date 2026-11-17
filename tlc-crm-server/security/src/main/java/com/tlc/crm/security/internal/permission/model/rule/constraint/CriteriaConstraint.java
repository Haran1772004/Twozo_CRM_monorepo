package com.tlc.crm.security.internal.permission.model.rule.constraint;

import com.tlc.sql.common.api.dml.secure.JoinConfigs;
import com.tlc.crm.security.internal.permission.model.SecureDefaultValue;
import com.tlc.crm.security.model.Constraint;

import com.tlc.sql.common.api.dml.Connector;
import com.tlc.sql.common.api.dml.Table;

import java.util.Collection;

/**
 * Holds the constraint to be checked for the table and column access
 *
 * @param orgId                  Represents the organization ID of the current user.
 * @param baseTable              Represents the base table of the criteria.
 * @param joinConfigs            Holds the configurations that define the join clauses to be applied.
 * @param connector              Represent the connector of the criteria.
 * @param criteriaConstraintData Represents the data of the criteria constraint.
 * @author Sathish
 * @version 1.0
 */
public record CriteriaConstraint(SecureDefaultValue orgId, Table baseTable, JoinConfigs joinConfigs, Connector connector,
                                 Collection<CriteriaConstraintData> criteriaConstraintData) implements Constraint {
}