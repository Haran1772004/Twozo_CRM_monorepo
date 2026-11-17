package com.tlc.sql.common.api.dml.secure;

import com.tlc.sql.common.api.dml.WhereClause;

import java.util.Map;
import java.util.Objects;

/**
 * Container for permission-derived query pieces used when securing select queries.
 * Holds a {@link WhereClause} representing row-level filters and a map of {@link JoinConfigs} used to resolve any additional joins required
 * by the permission predicates.
 *
 * <p>Instances are considered empty when no where-clause is present. Implementations and builders produce this record to convey per-table
 * permission constraints to secure query builders.</p>
 *
 * @author Sathish
 * @version 1.0
 */
public record PermissionCriteria(WhereClause whereClause, Map<String, JoinConfigs> joinConfigs) {

    /**
     * Return true when this {@link PermissionCriteria} contains no where-clause and therefore does not impose any row-level restrictions.
     *
     * @return true when whereClause is null, false otherwise
     */
    public boolean isEmpty() {
        return Objects.isNull(whereClause);
    }
}
