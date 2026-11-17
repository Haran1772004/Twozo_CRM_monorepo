package com.tlc.sql.common.api.dml;

import com.tlc.sql.common.api.meta.DatabaseType;
import com.tlc.sql.common.registrar.TableResolverRegistry;

/**
 * Represents a derived table in the database, which is a table created from a subquery.
 * Provides methods to retrieve and manage derived tables.
 *
 * @author Abishek
 * @version 1.0
 */
public interface DerivedTable extends Table {

    /**
     * Retrieves a derived table for the given name, alias, and select query using the default database type (OLTP).
     *
     * @param name the name of the derived table
     * @param alias the alias for the derived table
     * @param selectQuery the select query used to create the derived table
     * @return the derived table instance
     */
    static DerivedTable get(final String name, final String alias, final SelectQuery selectQuery) {
        return TableResolverRegistry.getInstance().get(DatabaseType.OLTP).get(name, alias, selectQuery);
    }

    /**
     * Retrieves a derived table for the given database type, name, alias, and select query.
     *
     * @param databaseType the type of the database (e.g., OLTP, OLAP)
     * @param name the name of the derived table
     * @param alias the alias for the derived table
     * @param selectQuery the select query used to create the derived table
     * @return the derived table instance
     */
    static DerivedTable get(final DatabaseType databaseType, final String name, final String alias, final SelectQuery selectQuery) {
        return TableResolverRegistry.getInstance().get(databaseType).get(name, alias, selectQuery);
    }

    /**
     * Retrieves the select query associated with the derived table.
     *
     * @return the select query
     */
    SelectQuery getSelectQuery();

    /**
     * Checks if the derived table is a Common Table Expression (CTE) with a WITH clause.
     *
     * @return true if the derived table is a WITH clause CTE, false otherwise
     */
    default boolean isWithClauseCTE() {
        return false;
    }

    /**
     * Sets whether the derived table is a Common Table Expression (CTE) with a WITH clause.
     *
     * @param withClauseCTE true to set the derived table as a WITH clause CTE, false otherwise
     */
    void setWithClauseCTE(boolean withClauseCTE);
}