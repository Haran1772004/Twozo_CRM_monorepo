package com.tlc.sql.common.api.dml.secure;

import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.With;

import java.util.Map;

/**
 * Builder abstraction for constructing permission-aware select query representations. Implementations will transform an incoming
 * {@link SelectQuery} together with a map of per-table {@link PermissionCriteria} into a secured query form appropriate for execution.
 * The generic type parameter T represents the resulting secured query type (for example a transformed
 * {@link SelectQuery} or a {@link With} wrapper that embeds the security filters).
 *
 * @param <T> the secured query representation produced by the builder
 * @author Sathish
 * @version 1.0
 */
public interface SecureSelectQueryBuilder<T> {

    /**
     * Return the default SecureSelectQueryBuilder instance. Currently this delegates to the CTE-based secure builder implementation.
     *
     * @return default SecureSelectQueryBuilder implementation
     */
    static SecureSelectQueryBuilder<?> getInstance() {
        return SecureCTESelectQueryBuilder.getInstance();
    }

    /**
     * Build a secured query representation from the provided select query and per-table permission criteria.
     * Implementations should produce a query that enforces the supplied {@code permissionCriteriaMap} (row-level
     * filters, additional joins, selected columns, etc.) and is safe to execute for the current access context.
     *
     * @param selectQuery           the original select query to secure
     * @param permissionCriteriaMap mapping of Table -> PermissionCriteria to apply
     * @return secured query representation of type T
     */
    T build(final SelectQuery selectQuery, final Map<Table, PermissionCriteria> permissionCriteriaMap);
}
