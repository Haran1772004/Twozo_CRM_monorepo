package com.tlc.entity.converter;

import com.tlc.commons.util.executor.CodeBlockExecutor;

import com.tlc.entity.Entity;

import com.tlc.sql.common.api.dml.Table;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import javax.annotation.Nullable;

import java.lang.reflect.Field;

/**
 * A cloner implementation that copies data from a source entity to a target entity
 * by handling primary key and regular columns using reflection.
 *
 * @param <ID> The type of the entity's identifier.
 * @param <E>  The type of the entity being cloned.
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
class EntityClonerImpl<ID, E extends Entity<ID>> extends AbstractReferenceFieldHandler<E, E>
    implements EntityCloner<ID, E>
{
    /**
     * A private static class that holds the singleton instance of {@link EntityClonerImpl}.
     * This ensures lazy initialization and thread-safety for accessing the instance.
     */
    private static class Instance
    {
        private static final EntityCloner<?, ?> INSTANCE = new EntityClonerImpl<>();
    }

    /**
     * A static factory method to access the singleton instance of {@link EntityClonerImpl}.
     * This method allows type-safe casting to the desired generic types.
     *
     * @param <ID> Type representing the identifier of the entity.
     * @param <E>  Type representing the entity, constrained to extend Entity<ID>.
     * @return The singleton instance of {@link EntityClonerImpl} typed with the provided generics.
     */
    @SuppressWarnings( "unchecked" )
    public static <ID, E extends Entity<ID>> EntityClonerImpl<ID, E> INSTANCE()
    {
        return (EntityClonerImpl<ID, E>) Instance.INSTANCE;
    }

    /**
     * Creates a clone of the given source entity by instantiating a new entity of the same type
     * and copying its properties.
     *
     * @param sourceEntity The entity to be cloned.
     * @return A new instance of the entity that is a copy of the source entity.
     */
    public E clone( final @NonNull E sourceEntity )
    {
        return CodeBlockExecutor.getWithUnchecked( () ->
        {
            @SuppressWarnings( "unchecked" )
            final E newEntity = ( E ) ConverterUtil.newInstance( sourceEntity.getClass() );
            return newEntity;
        } );
    }

    /**
     * Handles the conversion process for a field annotated as the primary key (PK) column.
     * This method delegates the handling of the primary key column to the general column handler.
     *
     * @param columnName      The name of the primary key column.
     * @param converterParams The parameters required for the conversion, containing source and target entities.
     * @throws Exception If an error occurs during the handling of the primary key column.
     */
    @Override
    protected void handlePKColumn( final String columnName, final ConverterParams<E> converterParams ) throws Exception
    {
        handleColumn( columnName, converterParams );
    }

    /**
     * Handles the conversion process for a regular column by copying the value from the source entity
     * to the target entity. This is done by reflecting on the field associated with the column name.
     *
     * @param columnName      The name of the column to be handled.
     * @param converterParams The parameters required for the conversion, containing source and target entities.
     * @throws Exception If an error occurs during the handling of the column.
     */
    @Override
    protected void handleColumn( final String columnName, final ConverterParams<E> converterParams ) throws Exception
    {
        final Entity<?> sourceEntity = converterParams.entity();
        final Field field = converterParams.field();
        final Object value = field.get( sourceEntity );
        field.set( converterParams.target(), value );
    }

    /**
     * Creates a new reference context for the given entity and table, with the specified target data container.
     * The reference context is used to manage relationships and dependencies during the cloning process.
     *
     * @param entity The source entity being cloned.
     * @param table  The database table associated with the entity.
     * @param target The target entity where the data will be cloned.
     * @return A newly created {@link ReferenceContext} for managing references during the cloning process.
     * @throws Exception If an error occurs during the creation of the reference context.
     */
    @Override
    protected @NonNull ReferenceContext<E> createReferenceContext( final Entity<?> entity,
                                                                   final Table table,
                                                                   final E target ) throws Exception
    {
        return ReferenceContext.create( target, entity );
    }

    /**
     * Retrieves the value of a column from the specified element (entity) by using reflection to find the field
     * associated with the column name.
     *
     * @param element    The entity containing the value.
     * @param columnName The name of the column for which the value is retrieved.
     * @return The value corresponding to the specified column, or {@code null} if the value cannot be retrieved.
     */
    @Override
    protected @Nullable Object getColumnValue( final E element, final String columnName )
    {
        final Field field = ConverterUtil.getFieldByColumnName( element.getClass(), columnName );
        return CodeBlockExecutor.getWithUnchecked( () -> field.get( element ) );
    }

    /**
     * Sets the value of a column in the specified element (entity) by using reflection to find the field
     * associated with the column name.
     *
     * @param element    The entity to update with the new value.
     * @param columnName The name of the column for which the value is being set.
     * @param value      The value to set in the specified column.
     */
    @Override
    protected void setColumnValue( final E element, final String columnName, final Object value )
    {
        final Field field = ConverterUtil.getFieldByColumnName( element.getClass(), columnName );
        CodeBlockExecutor.runAsUnchecked( () -> field.set( element, value ) );
    }

    /**
     * {@inheritDoc}
     *
     * @param target  The target data container to update.
     * @param element The element containing the updated data.
     */
    @Override
    protected void updateTarget( final E target, final E element )
    {}
}

