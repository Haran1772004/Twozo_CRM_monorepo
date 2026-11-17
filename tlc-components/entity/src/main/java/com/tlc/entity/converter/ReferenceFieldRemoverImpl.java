package com.tlc.entity.converter;

import com.tlc.commons.util.executor.CodeBlockExecutor;

import com.tlc.entity.Entity;

import com.tlc.sql.common.api.dml.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import lombok.experimental.Accessors;

import javax.annotation.Nullable;

import java.lang.reflect.Field;

/**
 * Implementation of the {@link ReferenceFieldRemover} interface.
 * This handler class is responsible for recursively removing reference fields
 * (such as linked entities and relationships) from entities.
 * It extends {@link AbstractReferenceFieldHandler} to provide the functionality
 * of nullifying reference fields during the conversion process.
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
class ReferenceFieldRemoverImpl extends AbstractReferenceFieldHandler<Entity<?>, Entity<?>>
    implements ReferenceFieldRemover
{
    @Getter( AccessLevel.PACKAGE )
    @Accessors( fluent = true )
    private static final ReferenceFieldRemoverImpl INSTANCE = new ReferenceFieldRemoverImpl();

    /**
     * {@inheritDoc}
     *
     * @param entity The entity from which reference fields should be removed recursively.
     * @throws Exception If an error occurs during the removal process.
     */
    @Override
    public final void removeReferenceFieldsRecursively( @NonNull final Entity<?> entity ) throws Exception
    {
        convert( entity, entity );
    }

    /**
     * Handles the removal of the primary key column value by setting it to {@code null}.
     *
     * @param columnName      The name of the primary key column.
     * @param converterParams The parameters required for conversion, including the entity and field.
     * @throws Exception If an error occurs during processing.
     */
    @Override
    protected void handlePKColumn( final String columnName, final ConverterParams<Entity<?>> converterParams ) throws Exception
    {
        final Entity<?> entity = converterParams.entity();
        final Field primaryKeyField = converterParams.field();
        primaryKeyField.set( entity, null );
    }

    /**
     * Handles regular columns in the entity. No specific operation is performed for regular columns in this handler.
     *
     * @param columnName      The name of the column.
     * @param converterParams The parameters required for conversion.
     * @throws Exception If an error occurs during processing.
     */
    @Override
    protected void handleColumn( final String columnName, final ConverterParams<Entity<?>> converterParams ) throws Exception
    {
        // No operation is performed for regular columns.
    }

    /**
     * Creates a new {@link ReferenceContext} for the given entity, associated table, and target container.
     *
     * @param entity The entity being processed.
     * @param table  The associated database table.
     * @param target The target data container.
     * @return A newly created {@link ReferenceContext} instance for the current processing context.
     */
    @Override
    protected @NonNull ReferenceContext<Entity<?>> createReferenceContext( final Entity<?> entity,
                                                                           final Table table,
                                                                           final Entity<?> target )
    {
        return ReferenceContext.create( target, entity );
    }

    /**
     * Retrieves the value of a column from the given element.
     * This implementation returns {@code null} as no specific retrieval logic is defined.
     *
     * @param element    The entity containing the column.
     * @param columnName The name of the column to retrieve the value from.
     * @return Always returns {@code null} in this implementation.
     */
    @Override
    protected @Nullable Object getColumnValue( final Entity<?> element, final String columnName )
    {
        return null;
    }

    /**
     * Sets a column value in the given element. This implementation nullifies the value for the specified column.
     *
     * @param element    The entity to update.
     * @param columnName The name of the column to nullify.
     * @param value      The value to set (ignored in this implementation, as null is set).
     */
    @Override
    protected void setColumnValue( final Entity<?> element, final String columnName, final Object value )
    {
        final Field field = ConverterUtil.getFieldByColumnName( element.getClass(), columnName );
        CodeBlockExecutor.runAsUnchecked( () -> field.set( element, null ) );
    }

    /**
     * Updates the target data container with the given element.
     * This implementation does not perform any operation on the target.
     *
     * @param target  The target data container to update.
     * @param element The entity used to update the target.
     */
    @Override
    protected void updateTarget( final Entity<?> target, final Entity<?> element )
    {
        // No operation is performed to update the target in this implementation.
    }
}
