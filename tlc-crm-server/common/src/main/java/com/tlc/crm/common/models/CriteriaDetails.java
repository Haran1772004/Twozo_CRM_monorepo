package com.tlc.crm.common.models;

import com.tlc.sql.common.api.dml.Operator;

/**
 * Represents a single criteria configuration used for filtering or querying data.
 * Encapsulates the field name, its expected value, the comparison operator,
 * and a flag indicating if the criteria is mandatory.
 *
 * @author Sathish
 * @version 1.0
 */
public record CriteriaDetails(String field, Object value, Operator operator, boolean isMandatory) {

    public CriteriaDetails(final String field, final Object value, final Operator operator) {
        this(field, value, operator, true);
    }
}
