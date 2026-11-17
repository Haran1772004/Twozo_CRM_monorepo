package com.tlc.entity.converter;

import com.tlc.commons.util.executor.CodeBlockExecutor;
import com.tlc.commons.util.executor.NullSafeExecutor;

import com.tlc.entity.Entity;
import com.tlc.entity.annotation.Join;
import com.tlc.entity.annotation.JoinColumn;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Operator;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import lombok.*;
import lombok.experimental.Accessors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Converter class for converting {@link Entity} to {@link DataContainer}.
 * This class extends the {@link AbstractConverter} class.
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
class EntityToDCConvertor extends AbstractReferenceFieldHandler<DataContainer, Row>
{
    @Getter( AccessLevel.PACKAGE )
    @Accessors( fluent = true )
    private static final EntityToDCConvertor INSTANCE = new EntityToDCConvertor();
    private static final Logger LOGGER = LoggerFactory.getLogger( EntityToDCConvertor.class );


    /**
     * {@inheritDoc}
     *
     * @param columnName      The name of the primary key column.
     * @param converterParams The parameters required for conversion.
     * @throws Exception If an error occurs during handling.
     */
    @Override
    protected void handlePKColumn( final String columnName, final ConverterParams<DataContainer> converterParams ) throws Exception
    {
        final Entity<?> entity = converterParams.entity();
        final ReferenceContext<Row> referenceContext = getReferenceContext( entity, converterParams.target() );
        final Field pkField = converterParams.field();

        // Set PK value in the entity if it's null using NullSafeExecutor
        NullSafeExecutor.runIfNotPresentSafely( pkField.get( entity ), () ->
        {
            final Object pkValue = referenceContext.getElement().getPKValue();
            LOGGER.debug( "Primary key value set in the entity. ID: {}", pkValue );
            pkField.set( entity, pkValue );
        } );
    }

    /**
     * {@inheritDoc}
     *
     * @param columnName      The name of the column.
     * @param converterParams The parameters required for conversion.
     * @throws Exception If an error occurs during handling.
     */
    @Override
    protected void handleColumn( final String columnName, final ConverterParams<DataContainer> converterParams ) throws Exception
    {
        final Field columnField = converterParams.field();
        final Entity<?> entity = converterParams.entity();

        NullSafeExecutor.acceptIfPresentSafely( columnField.get( entity ), colValue ->
        {
            final Row row = getReferenceContext( entity, converterParams.target() ).getElement();
            // Set the column value in the row using the value transformer
            final Object finalVal = converterParams.valueTransformer()
                                                   .apply( row.getTable().getAlias(), columnName, colValue );
            row.set( columnName, finalVal );
            LOGGER.debug( "Column '{}' value set in the row.", columnName );
        } );
    }

    /**
     * {@inheritDoc}
     *
     * @param entity        The entity being processed.
     * @param table         The associated database table.
     * @param dataContainer The target data container.
     * @return A newly created {@link ReferenceContext}.
     * @throws Exception If an error occurs during context creation.
     */
    @Override
    protected @NonNull ReferenceContext<Row> createReferenceContext( final Entity<?> entity,
                                                                     final Table table,
                                                                     final DataContainer dataContainer ) throws Exception
    {
        final String tableAliasName = table.getAlias();
        final Object id = entity.id();

        if( Objects.isNull( id ) )
        {
            // If the entity ID is null, create a new Row
            final Row row = new Row( table );
            final ReferenceContext<Row> referenceContext = ReferenceContext.create( row, entity );
            LOGGER.debug( "Created new Row and RowJoinColumn for the entity. TableName: {}", tableAliasName );
            return referenceContext;
        }
        else
        {
            // If the entity ID exists, find the corresponding row in the data container
            final Criteria criteria = Criteria.eq( table.getPKColumn(), id );
            final WhereClause whereClause = new WhereClause( criteria );
            final Row row = dataContainer.getRow( table, whereClause );
            if( Objects.nonNull( row ) )
            {
                // If the row is found, create a ReferenceContext for it
                final ReferenceContext<Row> referenceContext = ReferenceContext.create( row, entity );
                LOGGER.debug( "Created RowJoinColumn for the entity with ID: {} in table: {}", id, tableAliasName );
                return referenceContext;
            }
            else
            {
                // If the row is not found, throw an exception
                throw new Exception( "Failed to find row for entity with ID: " + id + " in table: " + tableAliasName );
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param joinAnnot     The Join annotation instance defining the join.
     * @param refEntityList The list of reference entity objects.
     * @param dataContainer The target data container where records may be deleted.
     * @throws Exception If an error occurs during record deletion.
     */
    @Override
    protected void handleDeletedRecords( final Entity<?> currentEntity,
                                         final Join joinAnnot,
                                         final Collection<?> refEntityList,
                                         final DataContainer dataContainer ) throws Exception
    {
        CodeBlockExecutor.forEachSafely( Arrays.asList( joinAnnot.joinTables() ), joinTable ->
        {
            final Class<? extends Entity<?>> refEntityClass = joinTable.entityClass();
            final Object refEntityObj = refEntityList.stream()
                                                     .findFirst()
                                                     .orElse( null );

            final boolean isUnknownEntity = Objects.nonNull( refEntityObj )
                                            && !refEntityClass.equals( refEntityObj.getClass() );

            if( !isUnknownEntity )
            {
                final Table refTable = getTableNameOrAliasName( joinTable );
                final String pkColumn = ConverterUtil.getPKColumnName( refEntityClass );

                WhereClause whereClause = null;
                for( JoinColumn joinColumn : joinTable.joinColumns() )
                {
                    final Object localVal = getReferenceContext( currentEntity, dataContainer ).getElement()
                                                                                               .get( joinColumn.localColumn() );

                    final Column refColumn = refTable.getColumn( joinColumn.referenceColumn() );
                    final Criteria refCriteria = new Criteria( refColumn, localVal, Operator.EQUAL );
                    if( Objects.isNull( whereClause ) )
                    {
                        whereClause = new WhereClause( refCriteria );
                    }
                    else
                    {
                        whereClause.and( refCriteria );
                    }
                }

                dataContainer.getRows( refTable, whereClause )
                             .filter( refRow -> !refRow.isNewRow() )
                             .filter( refRow -> refEntityList.stream()
                                                             .filter( Objects::nonNull )
                                                             .map( refEntityObject -> ConverterUtil.typecastAndGet( Entity.class,
                                                                                                                    refEntityObject ) )
                                                             .noneMatch( refEntity -> Objects.equals( refEntity.id(),
                                                                                                      refRow.get( pkColumn ) ) ) )
                             .toList()
                             .forEach( dataContainer::deleteRow );
            }
        } );
    }

    /**
     * {@inheritDoc}
     *
     * @param element    The element containing the value.
     * @param columnName The column name for which the value is retrieved.
     * @return The value corresponding to the column.
     */
    @Override
    protected @Nullable Object getColumnValue( final Row element, final String columnName )
    {
        return element.get( columnName );
    }

    /**
     * {@inheritDoc}
     *
     * @param element    The element to update.
     * @param columnName The column name to set the value for.
     * @param value      The value to set.
     */
    @Override
    protected void setColumnValue( final Row element, final String columnName, final Object value )
    {
        element.set( columnName, value );
    }

    /**
     * {@inheritDoc}
     *
     * @param target  The target data container to update.
     * @param element The element used to update the target.
     */
    @Override
    protected void updateTarget( final DataContainer target, final Row element )
    {
        target.updateRow( element );
    }
}

