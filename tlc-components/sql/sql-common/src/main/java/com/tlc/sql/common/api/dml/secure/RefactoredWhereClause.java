package com.tlc.sql.common.api.dml.secure;

import com.tlc.sql.common.api.dml.WhereClause;

/**
 * <p>
 * Wraps the {@link WhereClause} after it has been refactored.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class RefactoredWhereClause extends WhereClause {

    public RefactoredWhereClause(final WhereClause whereClause) {
        super(whereClause, false);
    }
}
