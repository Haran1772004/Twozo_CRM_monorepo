package com.tlc.sql.common.api.ds.access;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Operation;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.secure.JoinConfigs;
import com.tlc.sql.common.api.dml.secure.PermissionCriteria;
import com.tlc.sql.common.api.dml.secure.SelectQueryWrapper;

import java.util.Map;

/**
 * Manages authorization processes for read operations to be executed before and after interacting with the data store
 *
 * @author sundar
 * @version 1.0
 * @see AccessController
 */
public interface ReadAccessController extends AccessController {

    /**
     * Apply permission-driven transformations to the provided where-clause for
     * the given table and operation. Implementations should return an updated
     * WhereClause that enforces any access restrictions relevant to the
     * current context (user/org).
     *
     * @param table the table for which the where-clause applies
     * @param whereClause the original where-clause (may be null)
     * @param operation the operation for which access is being evaluated (e.g. SELECT)
     * @return a refactored WhereClause that enforces permission checks
     */
    WhereClause refactor(final Table table, final WhereClause whereClause, final Operation operation);

    /**
     * Return permission criteria for the given table and operation. The
     * returned {@link PermissionCriteria} encapsulates any row-level filters
     * or parameterized predicates that must be applied when selecting from
     * the table.
     *
     * @param table the table for which to obtain permission criteria
     * @param operation the operation being authorized
     * @return PermissionCriteria describing required filters (may be empty)
     */
    PermissionCriteria getPermissionCriteria(final Table table, final Operation operation);

    /**
     * Check whether the supplied SelectQueryWrapper is permitted for the
     * current context and, if so, refactor it in-place to include any
     * permission-driven changes (selected columns, additional predicates,
     * etc.). Implementations return true when the query is allowed and has
     * been refactored; false when the query should not be executed.
     *
     * @param selectQueryWrapper wrapper around the select query to check/refactor
     * @return true if the query is permitted (and possibly refactored), false otherwise
     */
    boolean checkAndRefactor(final SelectQueryWrapper selectQueryWrapper);

    /**
     * <p>
     * Resolves secure column values for the given tables within the provided DataContainer.
     * This method ensures that sensitive data stored in secure columns is properly
     * processed using their respective value resolvers before being used.
     * </p>
     *
     * <p><b>Process:</b>
     * <ol>
     *   <li>Retrieves the secure columns for each table from the DataPermissionHandler.</li>
     *   <li>Iterates through each secure column and applies its value resolver on
     *       the corresponding data in the DataContainer.</li>
     *   <li>Replaces the original values with the resolved (decrypted/transformed) values.</li>
     * </ol>
     *
     * @param dataContainer The DataContainer holding the rows to be processed.
     */
    void resolveSecureColumnValues(final DataContainer dataContainer);

    /**
     * Returns the join configurations associated with the given table,
     * keyed by the related table name.
     *
     * @param table The {@link Table} for which join details are retrieved.
     * @return A map of related table name → {@link JoinConfigs}.
     */
    Map<String, JoinConfigs> getJoinDetails(final Table table);
}