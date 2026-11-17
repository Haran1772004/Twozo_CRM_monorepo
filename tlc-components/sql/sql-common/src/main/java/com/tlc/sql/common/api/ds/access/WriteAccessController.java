package com.tlc.sql.common.api.ds.access;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Operation;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.DeleteQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.UpdateQuery;

import java.util.Collection;

/**
 * Manages access for read and write operations to be executed before and after interacting with the data store
 *
 * @author sundar
 * @version 1.0
 * @see AccessController
 */
public interface WriteAccessController extends ReadAccessController {

    /**
     * <p>
     * Checks whether the given {@link Table} is accessible and if all the modified columns are accessible.
     * </p>
     *
     * @param table     Represents the table to be checked.
     * @param operation Represents the operation to be performed.
     * @param modifiedColumns Represents the modified column to be checked.
     * @return true if the table is accessible.
     */
    boolean isAccessible(final Table table, final Operation operation, final Collection<Column> modifiedColumns);

    /**
     * Validates and refactors the provided {@link UpdateQuery} according to specified rules
     * <b>Example Scenario</b>
     * <pre>
     *
     * If an UpdateQuery involves a table that is not accessible,
     * an Access Denied exception should be thrown. If a column is restricted from being updated,
     * the update action should be blocked. Additionally, if the current user's role is mapped
     * to a specific territory or restricted scopes,
     * the appropriate where clause should be appended to the UpdateQuery.
     * If a column has a special exemption when processed through origins
     * such as workflow or web form, the column value will be replaced by the configured default value.
     *
     * <b>Example 1</b>
     * {@code
     *      final UpdateQuery updateQuery = UpdateQuery.get(accessibleTable);
     *
     *      updateQuery.addUpdateColumn(nonAccessibleColumn, "EG Value");
     *
     *      //The access check will throw an error due to the lack of permission for the nonAccessibleColumn
     *      if (writeAccessController.checkAndRefactor(updateQuery)) {
     * 	    //update process
     *      }
     * }
     *
     * <b>Example 2</b>
     * {@code
     *      final UpdateQuery updateQuery = UpdateQuery.get(nonAccessibleTable);
     *
     *      updateQuery.addUpdateColumn(accessibleColumn, "EG Value");
     *
     *      //The access check will return false due to the lack of permission for the nonAccessibleTable
     *      if (writeAccessController.checkAndRefactor(updateQuery)) {
     * 	    //update process
     *      }
     * }
     * </pre>
     *
     * @param query The {@link UpdateQuery} to be validated and refactored
     * @return true if the table referenced in the {@link UpdateQuery} is accessible
     */
    boolean checkAndRefactor(final UpdateQuery query);

    /**
     * Validates and refactors the provided {@link DeleteQuery} according to specified rules
     *
     * <pre>
     * <b>Example Scenario:</b>
     * If the table involved in the DeleteQuery is not accessible, the method will return false.
     * Additionally, if the current user's role is mapped to a specific territory or restricted scopes,
     * the relevant where clause should be appended to the DeleteQuery.
     *
     *     {@code
     *          final DeleteQuery deleteQuery = DeleteQuery.get(scopeRestrictedTable);
     *
     *          // If the table in the DeleteQuery is accessible, a relevant scope WhereClause will be appended to the query
     *          if (writeAccessController.checkAndRefactor(deleteQuery)) {
     *       	    //delete process
     *          }
     *     }
     * </pre>
     *
     * @param query The {@link DeleteQuery} to be validated and refactored
     * @return true if the table referenced in the {@link DeleteQuery} is accessible
     */
    boolean checkAndRefactor(final DeleteQuery query);

    /**
     * Modifies the column values of the provided rows.
     * If a column lacks access and has a configured default value, the default value is used. Otherwise, an error is thrown.
     *
     * <pre>
     * <b>Example Scenario:</b>
     * The table rows will be modified if the columns in the provided table are configured
     * to allow value alterations based on the configuration; otherwise, an error will be thrown.
     * {@code
     * 	    final Table table = Table.get("SampleTable");
     * 	    final Collection<Row> sampleRows = dataContainer.getRows(table);
     *
     * 	    if (writeAccessController.hasAccess(table)) {
     * 		    alterRows(table, sampleRows, operation // Create or Update);
     *        }
     * }
     * </pre>
     *
     * @param table     Represents the table to which the rows will be altered
     * @param rows      A collection of rows to be modified
     * @param operation Represents the operation to be performed
     */
    void alterRows(final Table table, final Collection<Row> rows, final Operation operation);

    /**
     * Modifies the column values of the provided rows through the {@link DataContainer}.
     * If a column lacks access and has a configured default value, the default value is used. Otherwise, an error is thrown.
     *
     * <pre>
     * <b>Example Scenario:</b>
     * The rows in the provided container will be modified if the columns lack access but are configured to allow value alterations;
     * otherwise, an error will be thrown
     * {@code
     *        final DataContainer dataContainer; // Container built to commit the data
     *
     *        writeAccessController.alterRows(dataContainer);
     *        super.commitChanges(dataContainer);
     * }
     * </pre>
     *
     * @param dataContainer The data container to validate
     */
    void alterRows(final DataContainer dataContainer);
}
