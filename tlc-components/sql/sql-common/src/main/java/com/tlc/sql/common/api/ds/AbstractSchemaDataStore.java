package com.tlc.sql.common.api.ds;

import com.tlc.commons.code.ErrorCode;
import com.tlc.sql.common.api.handler.DDLHandler;
import com.tlc.sql.common.api.meta.ColumnDefinition;
import com.tlc.sql.common.api.meta.DatabaseType;
import com.tlc.sql.common.api.meta.TableDefinition;
import com.tlc.sql.common.ddl.AdvDDLAction;
import com.tlc.sql.common.ddl.DDLAction;
import com.tlc.sql.common.internal.ds.DsProvider;
import com.tlc.sql.common.internal.status.SQLErrorCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Abstract base implementation of {@link SchemaDataStore} that provides schema-level
 * database operations such as table existence checks, table creation, and DDL action execution.
 */
public abstract class AbstractSchemaDataStore<C extends ColumnDefinition, T extends TableDefinition<C>, D extends DDLHandler<C, T>> implements SchemaDataStore<C, T, D> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSchemaDataStore.class);

    protected final DsProvider dsProvider;
    protected final D ddlHandler;

    protected AbstractSchemaDataStore(final DsProvider dsProvider, final D ddlHandler) {
        this.dsProvider = dsProvider;
        this.ddlHandler = ddlHandler;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isTableExists(String tableName) throws SQLException
    {
        try (Connection conn = dsProvider.getConnection())
        {
            return isTableExists(conn, tableName);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createTables(Collection<T> tableDefinitions)
    {
        try (Connection conn = dsProvider.getConnection())
        {
            if (Objects.equals(DatabaseType.OLAP, dsProvider.databaseType())){
                createTable(ddlHandler, conn, tableDefinitions);
            } else {
                conn.setAutoCommit(false);
                try
                {
                    createTable(ddlHandler, conn, tableDefinitions);
                    conn.commit();
                }
                catch (Throwable exp)
                {
                    conn.rollback();
                    throw exp;
                }
            }
        }
        catch (ErrorCode errorCode)
        {
            throw errorCode;
        }
        catch (Exception exp)
        {
            throw ErrorCode.get(SQLErrorCodes.DB_CREATE_TABLE_FAILED, exp);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void executeDDLActions(List<? extends DDLAction<C, T, D>> actions)
    {
        final long time = System.currentTimeMillis();
        try (Connection connection = dsProvider.getConnection())
        {
            if (Objects.equals(DatabaseType.OLAP, dsProvider.databaseType())){
                executeDDLActions(ddlHandler, connection, actions);
            } else {
                connection.setAutoCommit(false);
                try
                {
                    executeDDLActions(ddlHandler, connection, actions);
                    connection.commit();
                }
                catch (Throwable exp)
                {
                    connection.rollback();
                    throw exp;
                }
            }
        }
        catch (ErrorCode errorCode)
        {
            throw errorCode;
        }
        catch (Exception exp)
        {
            throw ErrorCode.get(SQLErrorCodes.DB_DDL_ACTION_FAILED, exp);
        }
        LOGGER.debug("Time taken to process DDL Actions {}", System.currentTimeMillis() - time);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void revertDDLActions(List<? extends AdvDDLAction<C, T, D>> actions)
    {
        final long time = System.currentTimeMillis();
        try (Connection connection = dsProvider.getConnection())
        {
            connection.setAutoCommit(false);
            try
            {
                revertDDLActions(ddlHandler, connection, actions);
                connection.commit();
            }
            catch (Throwable exp)
            {
                connection.rollback();
                throw exp;
            }
        }
        catch (ErrorCode errorCode)
        {
            throw errorCode;
        }
        catch (Exception exp)
        {
            throw ErrorCode.get(SQLErrorCodes.DB_DDL_ACTION_FAILED, exp);
        }
        LOGGER.debug("Time taken to Revert DDL Actions {}", System.currentTimeMillis() - time);
    }

    /**
     * Checks whether a table with the given name exists using the provided connection.
     * Attempts case-sensitive, lowercase, and uppercase matches against the database metadata.
     *
     * @param connection the database connection to use
     * @param tableName  the name of the table to check
     * @return {@code true} if the table exists, {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    private boolean isTableExists(Connection connection, String tableName) throws SQLException
    {
        try(ResultSet rs = connection.getMetaData().getTables(null, null, tableName, null))
        {
            if(rs.next())
            {
                return true;
            }
        }
        try(ResultSet rs = connection.getMetaData().getTables(null, null, tableName.toLowerCase(), null))
        {
            if(rs.next())
            {
                return true;
            }
        }
        try(ResultSet rs = connection.getMetaData().getTables(null, null, tableName.toUpperCase(), null))
        {
            return rs.next();
        }
    }

    /**
     * Batches and executes CREATE TABLE SQL statements for the given table definitions.
     *
     * @param ddlHandler       the DDL handler used to generate CREATE TABLE SQL
     * @param connection       the database connection to use
     * @param tableDefinitions the collection of table definitions to create
     */
    private void createTable(D ddlHandler, Connection connection, Collection<T> tableDefinitions)
    {
        try(Statement statement = connection.createStatement())
        {
            for (T tableDefinition : tableDefinitions)
            {
                final String[] createSql = ddlHandler.getCreateTableSQL(tableDefinition);
                for (String sql : createSql)
                {
                    statement.addBatch(sql);
                }
            }
            statement.executeBatch();
        }
        catch(Exception exp)
        {
            throw ErrorCode.get(SQLErrorCodes.DB_CREATE_TABLE_FAILED, exp);
        }
    }

    /**
     * Executes the SQL statements generated by each DDL action sequentially.
     *
     * @param ddlHandler the DDL handler used to generate SQL from each action
     * @param connection the database connection to use
     * @param ddlActions the list of DDL actions to execute
     */
    private void executeDDLActions(D ddlHandler, Connection connection, List<? extends DDLAction<C, T, D>> ddlActions)
    {
        try
        {
            try(Statement statement = connection.createStatement())
            {
                for (DDLAction<C, T, D> ddlAction : ddlActions)
                {
                    final String[] sqlList = ddlAction.getSQL(ddlHandler);
                    for (String sql : sqlList)
                    {
                        LOGGER.info("Going to execute - {}", sql);
                        statement.execute(sql);
                        LOGGER.info("Successfully executed - {}", sql);
                    }
                }
            }
        }
        catch(Exception exp)
        {
            throw ErrorCode.get(SQLErrorCodes.DB_DDL_ACTION_FAILED, exp);
        }
    }

    /**
     * Executes the revert SQL statements generated by each advanced DDL action sequentially.
     *
     * @param ddlHandler the DDL handler used to generate revert SQL from each action
     * @param connection the database connection to use
     * @param actions    the list of advanced DDL actions to revert
     */
    private void revertDDLActions(D ddlHandler, Connection connection, List<? extends AdvDDLAction<C, T, D>> actions)
    {
        try
        {
            try(Statement statement = connection.createStatement())
            {
                for (AdvDDLAction<C, T, D> ddlAction : actions)
                {
                    final String[] sqlList = ddlAction.getRevertSQL(ddlHandler);
                    for (String sql : sqlList)
                    {
                        LOGGER.info("Going to execute - {}", sql);
                        statement.execute(sql);
                        LOGGER.info("Successfully executed - {}", sql);
                    }
                }
            }
        }
        catch(Exception exp)
        {
            throw ErrorCode.get(SQLErrorCodes.DB_DDL_ACTION_FAILED, exp);
        }
    }
}