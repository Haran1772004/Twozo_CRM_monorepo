package com.tlc.entity.converter;

import com.tlc.commons.util.executor.CodeBlockExecutor;
import com.tlc.commons.util.executor.NullSafeExecutor;

import com.tlc.entity.Entity;
import com.tlc.entity.annotation.Join;

import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Converter class for converting {@link Entity} to {@link SelectQuery}.
 * This class extends the {@link AbstractConverter} class.
 */
@NoArgsConstructor( staticName = "create", access = AccessLevel.PRIVATE )
class SelQueryConverter extends AbstractConverter<SelectQuery>
{
    @Getter( AccessLevel.PACKAGE )
    @Accessors( fluent = true )
    private static final SelQueryConverter INSTANCE = SelQueryConverter.create();
    private static final Logger LOGGER = LoggerFactory.getLogger( SelQueryConverter.class );
    private final ThreadLocal<Table> currentTable = new ThreadLocal<>();
    private final ThreadLocal<JoinClause.JoinType> previousJoin = new ThreadLocal<>();

    /**
     * {@inheritDoc}
     *
     * @param columnName      The name of the primary key column.
     * @param converterParams The parameters for conversion.
     */
    @Override
    protected void handlePKColumn( final String columnName, final ConverterParams<SelectQuery> converterParams )
    {
        handleCol( columnName, converterParams );
    }

    /**
     * {@inheritDoc}
     *
     * @param columnName      The name of the column.
     * @param converterParams The parameters for conversion.
     */
    @Override
    protected void handleColumn( final String columnName, final ConverterParams<SelectQuery> converterParams )
    {
        handleCol( columnName, converterParams );
    }

    /**
     * Helper method to handle a column.
     *
     * @param columnName      The name of the column.
     * @param converterParams The parameters for conversion.
     */
    private void handleCol( final String columnName, final ConverterParams<SelectQuery> converterParams )
    {
        final Column column = getTable( converterParams ).getColumn( columnName );
        LOGGER.debug( "Adding select clause for column: {}", columnName );
        converterParams.target().addSelectClause( column );
    }

    /**
     * {@inheritDoc}
     *
     * @param converterParams The parameters for conversion.
     * @param joinAnnot       The annotation defining the join.
     * @throws Exception If an error occurs during handling.
     */
    @Override
    protected void handleSingularRelation( final ConverterParams<SelectQuery> converterParams,
                                           final Join joinAnnot ) throws Exception
    {
        LOGGER.debug( "Handling singular relations..." );
        handleJoin( converterParams, joinAnnot );
        LOGGER.debug( "Handled singular relations..." );
    }

    /**
     * {@inheritDoc}
     *
     * @param converterParams The parameters for conversion.
     * @param joinAnnot       The annotation defining the join.
     * @throws Exception If an error occurs during handling.
     */
    @Override
    protected void handlePluralRelation( final ConverterParams<SelectQuery> converterParams,
                                         final Join joinAnnot ) throws Exception
    {
        LOGGER.debug( "Handling plural relations..." );
        handleJoin( converterParams, joinAnnot );
        LOGGER.debug( "Handled plural relations..." );
    }

    /**
     * Helper method to handle joining.
     *
     * @param converterParams The parameters for conversion.
     * @param joinAnnot       The annotation defining the join.
     * @throws Exception If an error occurs during handling.
     */
    private void handleJoin( final ConverterParams<SelectQuery> converterParams, final Join joinAnnot ) throws Exception
    {
        runIfSelectAllowed( joinAnnot.cascades(), () ->
        {
            LOGGER.debug( "Handling join column..." );
            final Table localTable = getTable( converterParams );

            // Determine the join type based on whether a previous join exists and its type,
            // or if the current join annotation specifies nullable, defaulting to INNER join.
            final JoinClause.JoinType joinType = ( previousJoin.get() != null && previousJoin.get() == JoinClause.JoinType.LEFT ) ?
                    previousJoin.get() : joinAnnot.nullable() ? JoinClause.JoinType.LEFT : JoinClause.JoinType.INNER;
            final JoinClause.JoinType prevJoin = previousJoin.get();

            // Set the current join type as the previous join for subsequent joins.
            previousJoin.set( joinType );

            Stream.of( joinAnnot.joinTables() ).forEach( joinTable ->
            {
                final Table currentTab = currentTable.get();
                currentTable.remove();

                final Class<? extends Entity<?>> refEntityClass = joinTable.entityClass();
                final String aliasName = joinTable.aliasName();
                final Table refTable = aliasName.isBlank() ? ConverterUtil.getTable( refEntityClass )
                                                           : ConverterUtil.getTable( refEntityClass, aliasName );

                // Map the join columns to relations between local and referenced tables.
                final List<JoinClause.Relation> relations = Arrays.stream( joinTable.joinColumns() ).map( joinColumn ->
                {
                    final Column localColumn = localTable.getColumn( joinColumn.localColumn() );
                    final Column refColumn = refTable.getColumn( joinColumn.referenceColumn() );
                    return new JoinClause.Relation( localColumn, refColumn );
                } ).toList();

                final JoinClause joinClause = new JoinClause( localTable, refTable, relations, joinType );
                converterParams.target().addJoinClause( joinClause );
                // Execute conversion for the referenced entity within the join.
                CodeBlockExecutor.runAsUnchecked( () ->
                {
                    final Entity<?> refEntity = ConverterUtil.newInstance( refEntityClass );
                    // Set the current table to the referenced table and perform conversion recursively.
                    currentTable.set( refTable );
                    convert( refEntity, converterParams.target(), converterParams.valueTransformer() );
                } );
                // Restore the current table.
                currentTable.set( currentTab );
            } );
            // Resets the previous join type after conversion.
            previousJoin.set( prevJoin );
            LOGGER.debug( "Referenced entity converted recursively." );
        } );
    }

    /**
     * Executed when the conversion process fails.
     * Removes the previous join type and the current table from the thread-local storage.
     *
     * @param entity The source entity.
     * @param target The target object.
     */
    @Override
    protected void onFail( final Entity<?> entity, final SelectQuery target )
    {
        previousJoin.remove();
        currentTable.remove();
    }

    /**
     * Executed after the conversion process is successfully completed.
     * Removes the current table from the thread-local storage.
     *
     * @param entity The source entity.
     * @param target The target object.
     */
    @Override
    protected void postConversion( final Entity<?> entity, final SelectQuery target )
    {
        currentTable.remove();
    }

    /**
     * Retrieves the table associated with the provided entity class.
     * If the table does not exist, create a new table.
     *
     * @param converterParams The converter parameters.
     * @return The corresponding table associated with the provided entity class.
     */
    private Table getTable( final ConverterParams<SelectQuery> converterParams )
    {
        // Retrieve the current table from the thread-local storage if present.
        // Otherwise, create a new table with a table name.
        return NullSafeExecutor.orElseGet( currentTable.get(), () ->
        {
            final Table table = ConverterUtil.getTable( converterParams.tableName() );
            currentTable.set( table );
            return table;
        } );
    }
}
