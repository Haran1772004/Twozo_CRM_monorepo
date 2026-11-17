package com.tlc.crm.security.internal.permission.model.rule.constraint;

import com.tlc.crm.security.internal.permission.model.SecureDefaultValue;

import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Operator;

/**
 * Holds the constraint data to be checked for the table and column access
 *
 * @param column   Represents the column of the criteria.
 * @param value    Represents the value of the criteria.
 * @param operator Represents the operator of the criteria.
 *
 * @author Sathish
 * @version 1.0
 */
public record CriteriaConstraintData(Column column, SecureDefaultValue value, Operator operator) {
}
