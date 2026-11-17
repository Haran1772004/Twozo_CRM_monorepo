package com.tlc.entity.converter;

import com.tlc.commons.functions.SafeConsumer;
import com.tlc.commons.util.executor.NullSafeExecutor;

import com.tlc.entity.Entity;
import com.tlc.entity.annotation.Join;
import com.tlc.entity.annotation.JoinTable;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.dml.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Abstract class for handling reference fields during entity-to-data-container conversion.
 * This class manages relationships and ensures proper referencing between parent and child elements.
 *
 * @param <T> The type of the target data container.
 * @param <EL> The type of the entity being converted.
 */
abstract class AbstractReferenceFieldHandler<T, EL> extends AbstractConverter<T>
{
    private static final Logger LOGGER = LoggerFactory.getLogger( AbstractReferenceFieldHandler.class );

    // ThreadLocals to store the current and previous reference contexts
    private final ThreadLocal<ReferenceContext<EL>> currentReferenceContext = new ThreadLocal<>();
    private final ThreadLocal<ReferenceContext<EL>> previousReferenceContext = new ThreadLocal<>();

    /**
     * {@inheritDoc}
     * <p>
     * Prepares reference contexts before conversion begins.
     * </P>
     *
     * @param entity The {@link Entity} object being converted.
     * @param target The target {@link DataContainer} object.
     * @throws Exception If an error occurs during pre-processing.
     */
    @Override
    protected final void preConversion( final Entity<?> entity, final T target ) throws Exception
    {
        currentReferenceContext.remove();
        if( getCurrentPos() == 1 )
        {
            previousReferenceContext.remove();
        }
        else
        {
            final EL element = getReferenceContext( entity, target ).getElement();
            updateReferenceColumns( previousReferenceContext.get(), element );
        }
    }

    /**
     * Updates reference column values based on the parent-child relationship.
     *
     * @param parentContext The context containing the parent element and join metadata.
     * @param childElement  The child element whose reference columns need to be updated.
     */
    private void updateReferenceColumns( final ReferenceContext<EL> parentContext, final EL childElement )
    {
        if( Objects.nonNull( parentContext ) && Objects.nonNull( childElement ) )
        {
            NullSafeExecutor.acceptIfPresent( parentContext.getJoinTableAnnotation(), joinTableAnnotation ->
            {
                LOGGER.debug( "Updating reference column values..." );
                Arrays.stream( joinTableAnnotation.joinColumns() )
                      .forEach( joinColumn ->
                       {
                          final String referenceColumnName = joinColumn.referenceColumn();
                          final String localColumnName = joinColumn.localColumn();
                          final EL parentElement = parentContext.getElement();

                          Optional.ofNullable( getColumnValue( childElement, referenceColumnName ) )
                                  .ifPresentOrElse( referenceValue ->
                                   {
                                       if( Objects.isNull( getColumnValue( parentElement, localColumnName ) ) )
                                       {
                                           setColumnValue( parentElement, localColumnName, referenceValue );
                                           LOGGER.debug( "Reference column updated in parent. Column: {}, Value: {}", localColumnName, referenceValue );
                                       }
                                       else if( !referenceValue.equals( getColumnValue( parentElement, localColumnName ) ) )
                                       {
                                           final String errorMsg = String.format( "ID mismatch between join columns. Local Column: %s, Reference Column: %s, Expected: %s, Found: %s",
                                                                                  localColumnName,
                                                                                  referenceColumnName,
                                                                                  getColumnValue( parentElement, localColumnName ),
                                                                                  referenceValue );
                                           throw new RuntimeException( errorMsg );
                                      }
                                  }, () ->
                                  {
                                      final Object localValue = getColumnValue( parentElement, localColumnName );
                                      setColumnValue( childElement, referenceColumnName, localValue );
                                      LOGGER.debug( "Reference column updated in child. Column: {}, Value: {}", referenceColumnName, localValue );
                                  } );
                    } );
                LOGGER.info( "Reference column values updated successfully." );
            } );
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Handles the conversion process for singular relationships (e.g., OneToOne, ManyToOne).
     * </p>
     *
     * @param converterParams The parameters required for conversion.
     * @param joinAnnot       The annotation defining the relationship.
     * @throws Exception If an error occurs during handling.
     */
    @Override
    protected final void handleSingularRelation( final ConverterParams<T> converterParams, final Join joinAnnot ) throws Exception
    {
        final Field referencedField = converterParams.field();
        final Entity<?> parentEntity = converterParams.entity();
        final Object referencedEntityObj = referencedField.get( parentEntity );
        final List<Object> refEntityList = NullSafeExecutor.getIfPresent( referencedEntityObj, List::of )
                                                           .orElseGet( List::of );

        // Handle deleted records if cascade allows it
        handleDeletedRecordsIfAllowed( parentEntity, joinAnnot, refEntityList, converterParams.target() );

        runIfPersistAllowed( joinAnnot.cascades(),
                             () -> processJoinTables( joinAnnot.joinTables(),
                                                      joinTable -> NullSafeExecutor.acceptIfPresentSafely( referencedEntityObj, nonNullReferencedEntity ->
                                                      {
                                                          if( joinTable.entityClass() == nonNullReferencedEntity.getClass() )
                                                          {
                                                              final ReferenceContext<EL> referenceContext = getReferenceContext( parentEntity, converterParams.target() );
                                                              referenceContext.setJoinTableAnnotation( joinTable );

                                                              final Entity<?> referencedEntity = ConverterUtil.typecastAndGet( Entity.class, nonNullReferencedEntity );
                                                              invokeConversion( referencedEntity, converterParams );
                                                              LOGGER.debug( "Converted singular relation for the entity. ID: {}", referencedEntity.id() );
                                                          }
                                                      } ) ) );
    }

    /**
     * {@inheritDoc}
     * <p>
     * Handles the conversion process for plural relationships (e.g., OneToMany, ManyToMany).
     * </P>
     *
     * @param converterParams The parameters required for conversion.
     * @param joinAnnot       The annotation defining the relationship.
     * @throws Exception If an error occurs during handling.
     */
    @Override
    protected final void handlePluralRelation( final ConverterParams<T> converterParams, final Join joinAnnot ) throws Exception
    {
        final Field referencedField = converterParams.field();
        final Entity<?> parentEntity = converterParams.entity();
        final Collection<?> referencedEntities = NullSafeExecutor.getIfPresent( referencedField.get( parentEntity ),
                                                                                fieldValue -> ConverterUtil.typecastAndGet( Collection.class, fieldValue ) )
                                                                 .orElse( null );

        final Collection<?> refList = Optional.ofNullable( referencedEntities )
                                              .orElseGet( List::of );

        // Handle deleted records if cascade allows it
        handleDeletedRecordsIfAllowed( parentEntity, joinAnnot, refList, converterParams.target() );

        runIfPersistAllowed( joinAnnot.cascades(), () ->
        {
            final Optional<Object> firstEntity = NullSafeExecutor.getIfPresent( referencedEntities,
                                                                                entities -> entities.stream()
                                                                                                    .findFirst()
                                                                                                    .orElse( null ) );

            processJoinTables( joinAnnot.joinTables(),
                               joinTable -> NullSafeExecutor.acceptIfPresentSafely( referencedEntities, nonNullEntities ->
                               {
                                   if( joinTable.entityClass() == firstEntity.<Class<?>>map( Object::getClass )
                                                                             .orElse( null ) )
                                   {
                                       final ReferenceContext<EL> referenceContext = getReferenceContext( parentEntity, converterParams.target() );
                                       referenceContext.setJoinTableAnnotation( joinTable );

                                       for( Object referencedEntityObj : nonNullEntities )
                                       {
                                           final Entity<?> referencedEntity = ConverterUtil.typecastAndGet( Entity.class, referencedEntityObj );
                                           invokeConversion( referencedEntity, converterParams );
                                       }
                                       LOGGER.debug( "Converted plural relation for the entity." );
                                   }
                               } ) );
        } );
    }

    /**
     * Processes join tables and execute a consumer function for each join table.
     *
     * @param joinTables The array of JoinTable objects to process.
     * @param consumer   The consumer function to execute.
     * @throws Exception if an error occurs during the process.
     */
    private void processJoinTables( final JoinTable[] joinTables, final SafeConsumer<JoinTable> consumer ) throws Exception
    {
        for( JoinTable joinTable : joinTables )
        {
            consumer.accept( joinTable );
        }
    }

    /**
     * Invokes the conversion process for a referenced entity.
     *
     * @param referencedEntity The referenced entity to convert.
     * @param converterParams  The conversion parameters.
     * @throws Exception If an error occurs during conversion.
     */
    private void invokeConversion( final Entity<?> referencedEntity, final ConverterParams<T> converterParams ) throws Exception
    {
        final ReferenceContext<EL> currentContext = currentReferenceContext.get();
        final ReferenceContext<EL> previousContext = previousReferenceContext.get();

        previousReferenceContext.set( currentContext );
        convert( referencedEntity, converterParams.target(), converterParams.valueTransformer() );
        currentReferenceContext.set( currentContext );
        previousReferenceContext.set( previousContext );

        LOGGER.debug( "Completed conversion for referenced entity. Restored previous reference context." );
    }

    /**
     * Handles the deletion of records from the target if deletion is allowed by the specified cascade settings.
     * This method delegates the actual handling of deleted records to {@code handleDeletedRecords}.
     *
     * @param currentEntity The current entity being processed.
     * @param joinAnnot     The {@link Join} annotation specifying relationship and cascade details.
     * @param refEntityList The collection of referenced entities in the relationship.
     * @param target        The target data container where records may be deleted.
     * @throws Exception If an error occurs during record deletion or processing.
     */
    private void handleDeletedRecordsIfAllowed( final Entity<?> currentEntity,
                                                final Join joinAnnot,
                                                final Collection<?> refEntityList,
                                                final T target ) throws Exception
    {
        runIfDeleteAllowed( joinAnnot.cascades(),
                            () -> handleDeletedRecords( currentEntity, joinAnnot, refEntityList, target ) );
    }

    /**
     * {@inheritDoc}
     * <p>
     * Handles post-conversion processing by finalizing references and updating the target.
     * </p>
     *
     * @param entity The {@link Entity} object being converted.
     * @param target The target {@link DataContainer} object.
     */
    @Override
    protected final void postConversion( final Entity<?> entity, final T target )
    {
        NullSafeExecutor.acceptIfPresent( currentReferenceContext.get(), currentContext ->
        {
            final EL currentElement = currentContext.getElement();
            updateReferenceColumns( previousReferenceContext.get(), currentElement );
            updateTarget( target, currentElement );
            LOGGER.debug( "Post-processing completed for the entity. ID: {}", entity.id() );
        } );
        previousReferenceContext.remove();
        LOGGER.debug( "Reference contexts cleared." );
    }

    /**
     * Retrieves the reference context for the specified entity and target container.
     * If a reference context does not already exist in the current thread, a new one is created
     * and stored in the thread-local context.
     *
     * @param entity The entity being processed.
     * @param target The target data container.
     * @return The {@link ReferenceContext} containing reference details.
     * @throws Exception If an error occurs during context retrieval or creation.
     */
    protected final ReferenceContext<EL> getReferenceContext( final Entity<?> entity, final T target ) throws Exception
    {
        return NullSafeExecutor.orElseGetSafely( currentReferenceContext.get(), () ->
        {
            final Table entityTable = resolveTable( entity );
            final ReferenceContext<EL> referenceContext = createReferenceContext( entity, entityTable, target );
            currentReferenceContext.set( referenceContext );
            return referenceContext;
        } );
    }

    /**
     * Resolves the database table associated with the given entity.
     * If a previous reference context exists, attempts to retrieve the table name or alias
     * from its join table annotation. Falls back to the default table if not found.
     *
     * @param entity The entity for which the table is resolved.
     * @return The resolved {@link Table} object.
     */
    private Table resolveTable( final Entity<?> entity )
    {
        final Supplier<Table> defaultTableResolver = () -> ConverterUtil.getTable( entity );

        return NullSafeExecutor.getIfPresent( previousReferenceContext.get(),
                                              previousContext -> NullSafeExecutor.getIfPresent( previousContext.getJoinTableAnnotation(),
                                                                                                this::getTableNameOrAliasName )
                                                                                 .orElseGet( defaultTableResolver ) )
                               .orElseGet( defaultTableResolver );
    }

    /**
     * Deletes records from the target if they are modified based on the join column pairs and the reference entity object.
     *
     * @param joinAnnot     The Join annotation instance defining the join.
     * @param refEntityList The list of reference entity objects.
     * @param target        The target data container where records may be deleted.
     * @throws Exception If an error occurs during record deletion.
     */
    protected void handleDeletedRecords( final Entity<?> currentEntity,
                                         final Join joinAnnot,
                                         final Collection<?> refEntityList,
                                         final T target ) throws Exception
    {}

    /**
     * Creates a new reference context for the given entity, table, and target container.
     *
     * @param entity The entity being processed.
     * @param table  The associated database table.
     * @param target The target data container.
     * @return A newly created {@link ReferenceContext}.
     * @throws Exception If an error occurs during context creation.
     */
    @NonNull
    protected abstract ReferenceContext<EL> createReferenceContext( final Entity<?> entity,
                                                                    final Table table,
                                                                    final T target ) throws Exception;


    /**
     * Abstract method to retrieve a column value from an element.
     *
     * @param element    The element containing the value.
     * @param columnName The column name for which the value is retrieved.
     * @return The value corresponding to the column.
     */
    @Nullable
    protected abstract Object getColumnValue( final EL element, final String columnName );

    /**
     * Abstract method to set a column value in an element.
     *
     * @param element    The element to update.
     * @param columnName The column name to set the value for.
     * @param value      The value to set.
     */
    protected abstract void setColumnValue( final EL element, final String columnName, final Object value );

    /**
     * Abstract method to update the target data container with the given element.
     *
     * @param target  The target data container to update.
     * @param element The element used to update the target.
     */
    protected abstract void updateTarget( final T target, final EL element );

    /**
     * Represents the context containing an element and its associated join metadata.
     *
     * @param <EL> The type of the element in the context.
     */
    @RequiredArgsConstructor( staticName = "create", access = AccessLevel.PROTECTED )
    @Getter
    protected static class ReferenceContext<EL>
    {
        @NonNull
        private final EL element;
        @Setter
        private JoinTable joinTableAnnotation;
        @NonNull
        private final Entity<?> entity;
    }
}

