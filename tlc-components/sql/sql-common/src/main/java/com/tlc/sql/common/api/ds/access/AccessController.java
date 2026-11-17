package com.tlc.sql.common.api.ds.access;

import com.tlc.sql.common.api.Operation;
import com.tlc.sql.common.api.dml.Table;

/**
 * Manages access operations to be executed before interacting with the data store
 *
 * @author sundar
 * @version 1.0
 */
public interface AccessController {

    /**
     * Checks whether the given {@link Table} is accessible or not
     *
     * <pre>
     *     Example:
     *     {@code
     *          final Table table = Table.get("sample");
     *          final Operation operation = Operation.CREATE;
     *
     *          if (accessController.isAccessible(table, operation)) {
     *              // Create action
     *          }
     *     }
     * </pre>
     * @param table     Represents the table to be checked
     * @param operation Represents the operation to be performed
     * @return true if the table is accessible
     */
    boolean isAccessible(final Table table, final Operation operation);
}