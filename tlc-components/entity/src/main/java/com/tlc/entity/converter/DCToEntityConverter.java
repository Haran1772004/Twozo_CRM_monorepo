package com.tlc.entity.converter;

import com.tlc.commons.functions.SafeConsumer;
import com.tlc.commons.util.executor.CodeBlockExecutor;
import com.tlc.commons.util.executor.NullSafeExecutor;
import com.tlc.entity.Entity;
import com.tlc.entity.annotation.Join;
import com.tlc.entity.annotation.JoinColumn;
import com.tlc.entity.annotation.JoinTable;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import lombok.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Converter class for converting {@link DataContainer} to {@link Entity}.
 * This class extends the {@link AbstractConverter} class.
 */
@NoArgsConstructor( staticName = "create", access = AccessLevel.PRIVATE )
class DCToEntityConverter extends AbstractConverter<DataContainer>
{
    private static final Logger LOGGER = LoggerFactory.getLogger( DCToEntityConverter.class );
    private static final DCToEntityConverter INSTANCE = DCToEntityConverter.create();
    private static final ThreadLocal<ColumnIdPair> CID_PAIR_TL = new ThreadLocal<>();
    private final ThreadLocal<Row> currentRow = new ThreadLocal<>();

    /**
     * Returns the singleton instance of {@link DCToEntityConverter}.
     *
     * @param pkId The primary key ID.
     * @return The singleton instance of {@link DCToEntityConverter}.
     */
    static <T> DCToEntityConverter getInstance( @NonNull final T pkId )
    {
        return getInstance( null, pkId );
    }

    /**
     * Retrieves an instance of {@link DCToEntityConverter} with specified column name and primary key ID.
     *
     * @param columnName The name of the primary key column.
     * @param pkId       The primary key ID.
     * @return An instance of {@link DCToEntityConverter}.
     */
    private static <T> DCToEntityConverter getInstance( final String columnName, final T pkId )
    {
        final ColumnIdPair columnIdPair = ColumnIdPair.create( columnName, pkId );
        CID_PAIR_TL.set( columnIdPair );
        return INSTANCE;
    }

    /**
     * Converts data from a {@link DataContainer} to the corresponding entity.
     *
     * @param entity        The {@link Entity} where the converted data will be stored.
     * @param dataContainer The {@link DataContainer} containing the data to be converted.
     * @throws Exception If an error occurs during conversion.
     */
    @Override
    protected void preConversion( final Entity<?> entity, final DataContainer dataContainer ) throws Exception
    {
        LOGGER.debug( "Starting pre-processing..." );
        // Ensure the primary key column is set if not already
        NullSafeExecutor.runIfNotPresentSafely( CID_PAIR_TL.get().getPkColumn(), () ->
        {
            final String pkColumn = ConverterUtil.getPKColumnName( entity.getClass() );
            CID_PAIR_TL.get().setPkColumn( pkColumn );
            LOGGER.debug( "Primary key column set to: " + pkColumn );
        } );
        getRow( entity, dataContainer );
        LOGGER.debug( "Pre-processing completed." );
    }

    /**
     * {@inheritDoc}
     *
     * @param columnName      The name of the primary key column.
     * @param converterParams The {@link ConverterParams} containing the necessary parameters for conversion.
     * @throws Exception If an error occurs during conversion.
     */
    @Override
    protected void handlePKColumn( final String columnName,
                                   final ConverterParams<DataContainer> converterParams ) throws Exception
    {
        LOGGER.debug( "Starting PK column handling..." );

        final Field pkField = converterParams.field();
        NullSafeExecutor.acceptIfPresentSafely( getRow( converterParams.entity(), converterParams.target() ), row ->
        {
            final Object pkValue = row.get( columnName );

            // Set the primary key value if present
            NullSafeExecutor.acceptIfPresentSafely( pkValue, nonNullValue ->
            {
                pkField.set( converterParams.entity(), nonNullValue );
                LOGGER.debug( "PK value set to: " + nonNullValue );
            } );

        } );
        LOGGER.debug( "PK column handling completed." );
    }

    /**
     * {@inheritDoc}
     *
     * @param columnName      The name of the column.
     * @param converterParams The {@link ConverterParams} containing the necessary parameters for conversion.
     * @throws Exception If an error occurs during conversion.
     */
    @Override
    protected void handleColumn( final String columnName, final ConverterParams<DataContainer> converterParams ) throws
            Exception
    {
        LOGGER.debug( "Starting column handling..." );

        final Field columnField = converterParams.field();
        NullSafeExecutor.acceptIfPresentSafely( getRow( converterParams.entity(), converterParams.target() ), row ->
        {
            final Object colValue = row.get( columnName );

            NullSafeExecutor.acceptIfPresentSafely( colValue, nonNullValue ->
            {
                // Set the column value using the value transformer if present
                columnField.set( converterParams.entity(),
                                 converterParams.valueTransformer()
                                                .apply( converterParams.tableName(), columnName, nonNullValue ) );
                LOGGER.debug( "Column value is updated." );
            } );

        } );
        LOGGER.debug( "Column handling completed." );
    }

    /**
     * {@inheritDoc}
     *
     * @param converterParams The {@link ConverterParams} containing the necessary parameters for conversion.
     * @param joinAnnot       The {@link Join} annotation instance defining the relationship between the entities.
     * @throws Exception If an error occurs during conversion.
     */
    @Override
    protected void handleSingularRelation( final ConverterParams<DataContainer> converterParams,
                                           final Join joinAnnot ) throws Exception
    {
        runIfSelectAllowed( joinAnnot.cascades(), () ->
        {
            LOGGER.debug( "Starting singular relation handling..." );

            findPresentJoinAndProcess( joinAnnot.joinTables(), converterParams, tableHolder ->
            {
                final Class<? extends Entity<?>> refEntityClass = tableHolder.joinTable.entityClass();
                final String pkColumn = ConverterUtil.getPKColumnName( refEntityClass );
                final Table refTable = tableHolder.refTable;
                final Row currRow = getRow( converterParams.entity(), converterParams.target() );
                final WhereClause whereClause = getWhereClause( tableHolder, currRow );

                NullSafeExecutor.acceptIfPresentSafely( converterParams.target().getRow( refTable, whereClause ), row ->
                {
                    final Entity<?> refEntity = invoke( row, refEntityClass, pkColumn, converterParams );
                    converterParams.field().set( converterParams.entity(), refEntity );
                    LOGGER.debug( "Singular relation handling completed." );
                } );
            } );
        } );
    }

    /**
     * Constructs a {@link WhereClause} based on the join columns between the main table and its referenced table,
     * using the values from the current row.
     *
     * @param tableHolder The holder object containing information about the main table and its referenced table.
     * @param currRow     The current row from the main table.
     * @return The constructed {@link WhereClause}.
     */
    private WhereClause getWhereClause( final TableHolder tableHolder, final Row currRow )
    {
        WhereClause whereClause = null;
        for( JoinColumn joinColumn : tableHolder.joinTable().joinColumns() )
        {
            final Object localValue = currRow.get( joinColumn.localColumn() );
            final Criteria criteria = Criteria.eq( tableHolder.refTable().getColumn( joinColumn.referenceColumn() ),
                                                   localValue );
            if( Objects.isNull( whereClause ) )
            {
                whereClause = new WhereClause( criteria );
            }
            else
            {
                whereClause.and( criteria );
            }
        }
        return whereClause;
    }

    /**
     * Handles the conversion process for a field annotated as a plural relation (e.g., OneToMany or ManyToMany).
     *
     * @param converterParams The ConverterParams containing the necessary parameters for conversion.
     * @param joinAnnot       The Join annotation instance defining the relationship between the entities.
     * @throws Exception If an error occurs during conversion.
     */
    @Override
    protected void handlePluralRelation( final ConverterParams<DataContainer> converterParams,
                                         final Join joinAnnot ) throws Exception
    {
        runIfSelectAllowed( joinAnnot.cascades(), () ->
        {
            LOGGER.debug( "Starting plural relation handling..." );

            findPresentJoinAndProcess( joinAnnot.joinTables(), converterParams, tableHolder ->
            {
                final List<? extends Entity<?>> refEntities = processesAndGetEntities( converterParams, tableHolder );
                // Set the converted entities list in the converter parameters field
                converterParams.field().set( converterParams.entity(), refEntities.isEmpty() ? null : refEntities );
                LOGGER.debug( "Plural relation handling completed." );
            } );
        } );
    }

    private List<? extends Entity<?>> processesAndGetEntities( final ConverterParams<DataContainer> converterParams,
                                                               final TableHolder tableHolder )
    {
        final Class<? extends Entity<?>> refEntityClass = tableHolder.joinTable.entityClass();
        final String pkColumn = ConverterUtil.getPKColumnName( refEntityClass );
        final Table refTable = tableHolder.refTable();
        final Row currRow = getRow( converterParams.entity(), converterParams.target() );
        final WhereClause whereClause = getWhereClause( tableHolder, currRow );
        return converterParams.target()
                              .getRows( refTable, whereClause )
                              .map( row ->
                                    CodeBlockExecutor.getWithUnchecked( () ->
                                    {
                                        final Entity<?> refEntity = invoke( row, refEntityClass, pkColumn, converterParams );
                                        LOGGER.debug( "Singular relation handling completed." );
                                        return refEntity;
                                    } ) )
                              .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     *
     * @param entity        The {@link Entity} where the converted data will be stored.
     * @param dataContainer The {@link DataContainer} containing the data to be converted.
     * @throws Exception If an error occurs during conversion.
     */
    @Override
    protected void postConversion( final Entity<?> entity, final DataContainer dataContainer )
    {
        currentRow.remove();
    }

    /**
     * Executed when the conversion process from {@link DataContainer} to entity fails.
     * Removes the current row from the thread-local storage.
     *
     * @param entity        The {@link Entity} where the data conversion was attempted.
     * @param dataContainer The {@link DataContainer} containing the data that failed to convert.
     */
    @Override
    protected void onFail( final Entity<?> entity, final DataContainer dataContainer )
    {
        currentRow.remove();
    }

    /**
     * Invokes the conversion process for the given reference row to an entity of the specified class
     * using a specific primary key column and provided converter parameters.
     *
     * @param refRow           The reference row to convert.
     * @param refEntityClass   The class of the reference entity to convert to.
     * @param pkColumn         The primary key column name.
     * @param converterParams  The converter parameters to be used during the conversion process.
     * @throws Exception       Throws an exception if the conversion fails.
     */
    private Entity<?> invoke( final Row refRow,
                              final Class<? extends Entity<?>> refEntityClass,
                              final String pkColumn,
                              final ConverterParams<DataContainer> converterParams ) throws Exception
    {
        return NullSafeExecutor.getIfPresentSafely( refRow.getPKValue(), pkValue ->
        {
            // Create a new instance of the referenced entity and convert it
            final Entity<?> refEntity = ConverterUtil.newInstance( refEntityClass );
            // Retrieve the current row
            final Row currRow = currentRow.get();
            currentRow.set( refRow );
            // Convert the reference entity using the specified primary key column and value
            getInstance( pkColumn, pkValue ).convert( refEntity,
                                                      converterParams.target(),
                                                      converterParams.valueTransformer() );
            // Restore the current and previous rows
            currentRow.set( currRow );
            return refEntity;
        } ).orElseThrow();
    }

    /**
     * Helper method to find the present {@link JoinTable} based on the {@link DataContainer}.
     *
     * @param joinTables      The array of {@link JoinTable} instances to search from.
     * @param converterParams The ConverterParams containing the necessary parameters for conversion.
     * @param consumer        A callback function to process the found {@link TableHolder}.
     * @throws Exception if an error occurs during processing.
     */
    private void findPresentJoinAndProcess( final JoinTable[] joinTables,
                                            final ConverterParams<DataContainer> converterParams,
                                            final SafeConsumer<TableHolder> consumer ) throws Exception
    {
        for( JoinTable joinTable : joinTables )
        {
            // Retrieve the corresponding table for the JoinTable.
            final Table refTable = getTableNameOrAliasName( joinTable );
            final int size = converterParams.target().size( refTable );
            if( 0 < size )
            {
                final Row localRow = getRow( converterParams.entity(), converterParams.target() );
                final boolean isRefExists = Objects.nonNull( localRow )
                                      && converterParams.target()
                                                        .getRows( refTable )
                                                        .anyMatch( refRow -> Stream.of( joinTable.joinColumns() )
                                                                                   .allMatch( joinCol ->
                                                                                   {
                                                                                       String localColumn = joinCol.localColumn();
                                                                                       String refColumn = joinCol.referenceColumn();
                                                                                       return Objects.equals( localRow.get( localColumn ),
                                                                                                              refRow.get( refColumn ) );
                                                                                   } ) );
                if( isRefExists )
                {
                    final TableHolder tableHolder = new TableHolder( joinTable, refTable );
                    consumer.accept( tableHolder );
                }
            }
        }
    }

    /**
     * A record representing a pair of {@link JoinTable} and its corresponding {@link Table}.
     */
    private record TableHolder( JoinTable joinTable, Table refTable )
    {}

    /**
     * Helper method to get the row for a {@link Entity} from a DataContainer.
     *
     * @param entity        The {@link Entity} for which the row is retrieved.
     * @param dataContainer The DataContainer from which the row is retrieved.
     * @return The Row corresponding to the {@link Entity} in the DataContainer.
     */
    private Row getRow( final Entity<?> entity, final DataContainer dataContainer )
    {
        return NullSafeExecutor.orElseGet( currentRow.get(), () ->
        {
            final Table table = ConverterUtil.getTable( entity );
            final ColumnIdPair criteriaFieldIdPair = CID_PAIR_TL.get();
            final Criteria criteria = Criteria.eq( table.getColumn( criteriaFieldIdPair.getPkColumn() ),
                                                   criteriaFieldIdPair.getPkId() );
            final WhereClause whereClause = new WhereClause( criteria );
            final Row row = dataContainer.getRow( table, whereClause );
            currentRow.set( row );
            return row;
        } );
    }

    /**
     * Inner class representing a pair of column and ID
     */
    @Getter
    @Setter
    @AllArgsConstructor( staticName = "create", access = AccessLevel.PRIVATE )
    private static class ColumnIdPair
    {
        private String pkColumn;
        private Object pkId;
    }
}

