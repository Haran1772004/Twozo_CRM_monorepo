package com.tlc.entity.converter;

import com.tlc.commons.functions.SafeRunnable;
import com.tlc.commons.functions.SafeTriConsumer;
import com.tlc.entity.Entity;
import com.tlc.entity.annotation.*;

import lombok.NonNull;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Abstract base class for converters
 * providing common functionality for converting data from a {@link Entity} to a target type.
 * Subclasses of {@link AbstractConverter} are responsible
 * for implementing specific conversion logic for different target types.
 *
 * @param <T> The type of target for conversion.
 */
public abstract class AbstractConverter<T> extends BaseConverter<T>
{
    private final ThreadLocal<Map<Relation, Set<Join>>> processedJoins = ThreadLocal.withInitial( HashMap::new );

    /**
     * {@inheritDoc}
     *
     * @param entity The source entity.
     * @param target The target object.
     * @throws Exception If an error occurs during preprocessing.
     */
    @Override
    protected final void preProcess( final Entity<?> entity, final T target ) throws Exception
    {
        // Clear joins if it's the top-level entity being processed.
        if( getCurrentPos() == 1 )
        {
            processedJoins.get().clear();
        }
        preConversion( entity, target );
    }

    /**
     * Performs pre-conversion operations before converting data.
     *
     * <p>
     * This method is called before converting data from the source entity to the target object.
     * Subclasses can override this method to implement custom pre-conversion logic.
     * </P>
     *
     * @param entity The source entity.
     * @param target The target object.
     * @throws Exception If an error occurs during pre-conversion operations.
     */
    protected void preConversion( final Entity<?> entity, final T target ) throws Exception
    {}

    /**
     * {@inheritDoc}
     *
     * @param entityClass The class of the source entity.
     * @return The set of fields to be processed.
     */
    @Override
    protected @NonNull Set<Field> getFields( final Class<?> entityClass )
    {
        final Set<Field> allFields = new TreeSet<>( ( field1, field2 ) ->
        {
            if( field2.isAnnotationPresent( annotationHelper().getColumnClass() ) )
            {
                return 1;
            }
            else if( field1.isAnnotationPresent( annotationHelper().getColumnClass() ) )
            {
                return -1;
            }
            return field1.getName().compareTo( field2.getName() );
        } );

        Class<?> currentClass = entityClass;

        while( currentClass != null )
        {
            Collections.addAll( allFields, currentClass.getDeclaredFields() );
            currentClass = currentClass.getSuperclass();
        }
        return allFields;
    }

    /**
     * {@inheritDoc}
     *
     * @param columnAnnot     The Column annotation instance.
     * @param converterParams The parameters required for conversion.
     * @throws Exception If an error occurs during field processing.
     */
    @Override
    protected void processColumnField( final Column columnAnnot, final ConverterParams<T> converterParams ) throws
            Exception
    {
        final String columnName = columnAnnot.name();
        if( columnAnnot.isPrimaryKey() )
        {
            handlePKColumn( columnName, converterParams );
        }
        else
        {
            handleColumn( columnName, converterParams );
        }
    }

    /**
     * Processes a field annotated with JoinColumn.
     *
     * @param joinAnnot       The JoinColumn annotation instance.
     * @param converterParams The parameters required for conversion.
     * @throws Exception If an error occurs during field processing.
     */
    @Override
    protected void processJoinColumnField( final Join joinAnnot, final ConverterParams<T> converterParams ) throws
            Exception
    {
        final Relation relation = joinAnnot.relation();
        // Check if the join has already been processed to avoid duplicate operations.
        final boolean isAlreadyProcessed = isAlreadyProcessed( joinAnnot );

        if( !isAlreadyProcessed )
        {
            if( relation == Relation.ONE_TO_ONE || relation == Relation.MANY_TO_ONE )
            {
                handleSingularRelation( converterParams, joinAnnot );
            }
            else
            {
                handlePluralRelation( converterParams, joinAnnot );
            }
            processedJoins.get().get( relation ).remove( joinAnnot );
        }
    }

    /**
     * Checks if the given join annotation have already been processed.
     *
     * @param newJoin The new Join annotation to check.
     * @return True if the join is already processed, false otherwise.
     */
    private boolean isAlreadyProcessed( final Join newJoin )
    {
        final Relation relation = newJoin.relation();
        final Set<Join> joins = processedJoins.get().get( relation );

        if( Objects.isNull( joins ) )
        {
            // If no joins of this relation have been processed yet, add the new join.
            final Set<Join> newSet = new HashSet<>();
            newSet.add( newJoin );
            processedJoins.get().put( relation, newSet );
            return false;
        }
        else
        {
            final boolean isExists = joins.stream()
                                    .anyMatch( join -> Stream.of( join.joinTables() )
                                                             .allMatch( joinTable -> Stream.of( newJoin.joinTables() )
                                                                                           .anyMatch( newJoinTable -> joinTableEquals.apply( joinTable, newJoinTable )
                                                                                                                      && joinTable.entityClass() == newJoinTable.entityClass() ) ) );

            if( !isExists )
            {
                // If the new join is not present, add it to the processed joins.
                joins.add( newJoin );
                return false;
            }
            return true;
        }
    }

    /**
     * Checks if two JoinColumns are equal.
     */
    private final BiFunction<JoinColumn, JoinColumn, Boolean> joinColumnEquals = ( existingJColumn, newJoinColumn ) ->
            Objects.equals( existingJColumn.localColumn(), newJoinColumn.localColumn() )
            && Objects.equals( existingJColumn.referenceColumn(), newJoinColumn.referenceColumn() );

    /**
     * Checks if two JoinTables are equal.
     */
    private final BiFunction<JoinTable, JoinTable, Boolean> joinTableEquals = ( existingJTable, newJoinTable ) ->
            existingJTable.joinColumns().length == newJoinTable.joinColumns().length
            && Stream.of( existingJTable.joinColumns() )
                     .allMatch( joinColumn -> Stream.of( newJoinTable.joinColumns() )
                                                    .allMatch( newJoinColumn -> joinColumnEquals.apply( joinColumn, newJoinColumn ) ) );

    /**
     * Executes a runnable if the "persist" cascade operation is allowed.
     *
     * @param cascades The cascade types.
     * @param runnable The runnable to execute.
     * @throws Exception If an error occurs during execution.
     */
    protected final void runIfPersistAllowed( @NonNull final Cascade[] cascades,
                                              @NonNull final SafeRunnable runnable ) throws Exception
    {
        handlerExecutor.accept( cascades, cascade -> cascade == Cascade.ALL || cascade == Cascade.PERSIST, runnable );
    }

    /**
     * Executes a runnable if the "select" cascade operation is allowed.
     *
     * @param cascades The cascade types.
     * @param runnable The runnable to execute.
     * @throws Exception If an error occurs during execution.
     */
    protected final void runIfSelectAllowed( @NonNull final Cascade[] cascades,
                                             @NonNull final SafeRunnable runnable ) throws Exception
    {
        handlerExecutor.accept( cascades, cascade -> cascade == Cascade.ALL || cascade == Cascade.SELECT, runnable );
    }

    /**
     * Executes a runnable if the "delete" cascade operation is allowed.
     *
     * @param cascades The cascade types.
     * @param runnable The runnable to execute.
     * @throws Exception If an error occurs during execution.
     */
    protected final void runIfDeleteAllowed( @NonNull final Cascade[] cascades,
                                             @NonNull final SafeRunnable runnable ) throws Exception
    {
        handlerExecutor.accept( cascades, cascade -> cascade == Cascade.ALL || cascade == Cascade.DELETE, runnable );
    }

    // Define a consumer to execute the runnable based on the provided predicate and cascades.
    private final SafeTriConsumer<Cascade[], Predicate<Cascade>, SafeRunnable> handlerExecutor = ( cascades, predicate, safeRunnable ) ->
    {
        if( Arrays.stream( cascades ).anyMatch( predicate ) )
        {
            safeRunnable.run();
        }
    };

    /**
     * Retrieves the key for the specified field.
     * If the field is annotated with {@link JsonKey}, the key from the annotation is returned.
     * Otherwise, the field's name is returned.
     *
     * @param field The field for which to get the key.
     * @return The key associated with the field.
     */
    protected final @NonNull String getKey( @NonNull Field field )
    {
        if( field.isAnnotationPresent( JsonKey.class ) )
        {
            return field.getAnnotation( JsonKey.class ).key();
        }
        else
        {
            return field.getName();
        }
    }

    /**
     * Retrieves the table name or alias name from the provided JoinTable annotation.
     * If the alias name in the annotation is blank, it returns the table associated with the entity class.
     * Otherwise, it returns the table with the specified alias name.
     *
     * @param joinTableAnnot The {@link JoinTable} annotation to retrieve the table information from.
     * @return The Table object representing the table name or alias name.
     */
    protected final com.tlc.sql.common.api.dml.Table getTableNameOrAliasName(final JoinTable joinTableAnnot )
    {
        final String tableAliasName = joinTableAnnot.aliasName();
        return tableAliasName.isBlank() ? ConverterUtil.getTable( joinTableAnnot.entityClass() ) :
                                          ConverterUtil.getTable( joinTableAnnot.entityClass(), tableAliasName );
    }

    /**
     * {@inheritDoc}
     *
     * @param entity The source entity.
     * @param target The target object.
     */
    @Override
    protected void postProcess( final Entity<?> entity, final T target )
    {
        postConversion( entity, target );
    }

    /**
     * Performs additional post-conversion operations.
     * Subclasses can override this method to implement custom post-conversion logic.
     *
     * @param entity The source entity.
     * @param target The target object.
     */
    protected void postConversion( final Entity<?> entity, final T target )
    {}

    /**
     * Handles the conversion process for a field annotated as the primary key column.
     *
     * @param columnName      The name of the primary key column.
     * @param converterParams The parameters required for conversion.
     * @throws Exception If an error occurs during handling.
     */
    abstract protected void handlePKColumn( final String columnName, final ConverterParams<T> converterParams ) throws
            Exception;

    /**
     * Handles the conversion process for a regular column.
     *
     * @param columnName      The name of the column.
     * @param converterParams The parameters required for conversion.
     * @throws Exception If an error occurs during handling.
     */
    abstract protected void handleColumn( final String columnName, final ConverterParams<T> converterParams ) throws
            Exception;

    /**
     * Handles the conversion process for a field annotated as a singular relation (e.g., OneToOne or ManyToOne).
     *
     * @param converterParams The parameters required for conversion.
     * @param join            The annotation defining the join.
     * @throws Exception If an error occurs during handling.
     */
    abstract protected void handleSingularRelation( final ConverterParams<T> converterParams, final Join join ) throws
            Exception;

    /**
     * Handles the conversion process for a field annotated as a plural relation (e.g., OneToMany or ManyToMany).
     *
     * @param converterParams The parameters required for conversion.
     * @param join            The annotation defining the join.
     * @throws Exception If an error occurs during handling.
     */
    abstract protected void handlePluralRelation( final ConverterParams<T> converterParams, final Join join ) throws
            Exception;
}
