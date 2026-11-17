package com.tlc.entity.converter;

import com.tlc.entity.Entity;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.entity.annotation.Column;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Utility class for various conversion operations.
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
public class ConverterUtil
{
    /**
     * Retrieves the table name associated with the provided entity class.
     *
     * @param entityClass The entity class.
     * @return The table name.
     */
    public static String getTableName( @NonNull final Class<?> entityClass )
    {
        final AnnotationHelper aHelper = AnnotationHelper.INSTANCE();
        return findAndGet( entityClass,
                           eClass -> Optional.of( eClass )
                                             .filter( clazz -> clazz.isAnnotationPresent( aHelper.getTableClass() ) )
                                             .map( clazz -> aHelper.getTable( clazz ).name() ) );
    }


    /**
     * Retrieves the primary key column name associated with the provided entity class.
     *
     * @param entityClass The entity class.
     * @return The primary key column name.
     */
    public static String getPKColumnName( @NonNull final Class<?> entityClass )
    {
        final AnnotationHelper aHelper = AnnotationHelper.INSTANCE();
        return findAndGet( entityClass,
                           eClass -> Arrays.stream( eClass.getDeclaredFields() )
                                           .filter( field -> field.isAnnotationPresent( aHelper.getColumnClass() ) )
                                           .map( aHelper::getColumn )
                                           .filter( Column::isPrimaryKey )
                                           .map( Column::name )
                                           .findFirst() );
    }

    /**
     * Retrieves the primary key field associated with the provided entity class.
     *
     * @param entityClass The entity class.
     * @return The primary key column {@link Field}.
     */
    public static Field getPKField( @NonNull final Class<?> entityClass )
    {
        final AnnotationHelper aHelper = AnnotationHelper.INSTANCE();
        return findAndGet( entityClass,
                           eClass -> Arrays.stream( eClass.getDeclaredFields() )
                                           .filter( field -> field.isAnnotationPresent( aHelper.getColumnClass() ) )
                                           .filter( field -> aHelper.getColumn( field ).isPrimaryKey() )
                                           .findFirst() );
    }

    /**
     * Retrieves the field from the specified entity class based on the provided column name.
     * This method searches for the field that is annotated with the {@link Column} annotation
     * and matches the given column name.
     *
     * @param entityClass The class of the entity in which the field is located.
     * @param columnName  The name of the column to search for in the entity's fields.
     * @return The {@link Field} that corresponds to the specified column name, or {@code null} if no matching field is found.
     */
    public static Field getFieldByColumnName( @NonNull final Class<?> entityClass, @NonNull final String columnName )
    {
        final AnnotationHelper aHelper = AnnotationHelper.INSTANCE();
        return findAndGet( entityClass,
                           eClass -> Arrays.stream( eClass.getDeclaredFields() )
                                           .filter( field -> field.isAnnotationPresent( aHelper.getColumnClass() ) )
                                           .filter( field -> aHelper.getColumn( field )
                                                                    .name()
                                                                    .equals( columnName ) )
                                           .findFirst() );
    }

    /**
     * Parses the provided entity class and applies the specified function to retrieve a value.
     *
     * @param entityClass The entity class to parse.
     * @param function    The function to apply.
     * @param <T>         The type of the value to retrieve.
     * @return The retrieved value.
     * @throws RuntimeException if the value cannot be retrieved.
     */
    public static <T> T findAndGet( @NonNull final Class<?> entityClass,
                                    @NonNull final Function<Class<?>, Optional<T>> function )
    {
        final boolean isEntityClass = Entity.class.isAssignableFrom( entityClass );
        if( isEntityClass )
        {
            Class<?> currentClass = entityClass;
            while( Objects.nonNull( currentClass ) )
            {
                final Optional<T> optional = function.apply( currentClass );
                if( optional.isPresent() )
                {
                    return optional.get();
                }
                currentClass = currentClass.getSuperclass();
            }
            throw new RuntimeException( "Value not found for the given entity class. EntityCLass -> " + entityClass.getName() );
        }
        else
        {
            throw new RuntimeException( "Invalid entity class. It must be a child of the EntityClass. Provided Class -> " + entityClass.getName() );
        }
    }

    /**
     * Retrieves the column name associated with the provided field.
     *
     * @param columnField The field representing the column.
     * @return The column name.
     */
    public static String getColumnName( @NonNull final Field columnField )
    {
        return columnField.getAnnotation( AnnotationHelper.INSTANCE().getColumnClass() ).name();
    }

    /**
     * Retrieves the table associated with the provided entity instance.
     *
     * @param entity The entity instance.
     * @return The table.
     */
    public static <T extends Entity<?>> Table getTable( @NonNull final T entity )
    {
        final Class<?> entityClass = entity.getClass();
        return getTable( entityClass.asSubclass( Entity.class ) );
    }

    /**
     * Retrieves the table associated with the provided entity instance and alias name.
     *
     * @param entity    The entity instance.
     * @param aliasName The alias name for the table.
     * @param <T>       The type of the entity must extend {@link Entity}.
     * @return The corresponding table.
     */
    public static <T extends Entity<?>> Table getTable( @NonNull final T entity, @NonNull final String aliasName )
    {
        final Class<?> entityClass = entity.getClass();
        return getTable( entityClass.asSubclass( Entity.class ), aliasName );
    }

    /**
     * Retrieves the table associated with the provided entity class.
     *
     * @param entityClass The class of the entity.
     * @param <T>         The type of the entity must extend {@link Entity}.
     * @return The corresponding table.
     */
    public static <T extends Entity<?>> Table getTable( @NonNull final Class<T> entityClass )
    {
        return findAndGetTable( entityClass, ConverterUtil::getTable );
    }

    /**
     * Retrieves the table associated with the provided entity class and alias name.
     *
     * @param entityClass The class of the entity.
     * @param aliasName   The alias name for the table.
     * @param <T>         The type of the entity must extend {@link Entity}.
     * @return The corresponding table.
     */
    public static <T extends Entity<?>> Table getTable( @NonNull final Class<T> entityClass,
                                                        @NonNull final String aliasName )
    {
        return findAndGetTable( entityClass, tableName -> Table.get( tableName, aliasName ) );
    }

    /**
     * Creates and returns a new Table object with the specified table name and alias name.
     *
     * @param tableName The name of the table.
     * @param aliasName The alias name for the table.
     * @return A new Table object with the specified table name and alias name.
     */
    public static Table getTable( @NonNull final String tableName,
                                  @NonNull final String aliasName )
    {
        return Table.get( tableName, aliasName );
    }

    /**
     * Retrieves and returns a Table object with the specified table name.
     * This method delegates to the static get method of the Table class.
     *
     * @param tableName The name of the table to retrieve.
     * @return The Table object with the specified table name.
     */
    public static Table getTable( @NonNull final String tableName )
    {
        return Table.get( tableName );
    }

    /**
     * Finds and retrieves a table associated with the given entity class using the provided function.
     *
     * @param entityClass The entity class for which to find the associated table.
     * @param function    A function that takes a table name as input and returns the corresponding Table object.
     * @return The Table object associated with the entity class.
     */
    private static Table findAndGetTable( final Class<?> entityClass, final Function<String, Table> function )
    {
        final AnnotationHelper aHelper = AnnotationHelper.INSTANCE();
        return findAndGet( entityClass, eClass ->
        {
            if( eClass.isAnnotationPresent( aHelper.getTableClass() ) )
            {
                final String tableName = eClass.getAnnotation( aHelper.getTableClass() ).name();
                final Table table = function.apply( tableName );
                return Optional.of( table );
            }
            return Optional.empty();
        } );
    }

    /**
     * Creates a new instance of the provided class using its builder if available or its default constructor.
     *
     * @param clazz The class of the object to instantiate.
     * @return A new instance of the class.
     * @throws Exception if an error occurs during instantiation.
     */
    @SuppressWarnings( "unchecked" )
    public static <T> T newInstance( @NonNull final Class<T> clazz ) throws Exception
    {
        if( Entity.class.isAssignableFrom( clazz ) )
        {
            final Object builderInstance = clazz.getMethod( "builder" ).invoke( null );
            final Class<?> builderClass = builderInstance.getClass();
            final Method buildMethod = builderClass.getMethod( "build" );
            buildMethod.setAccessible( Boolean.TRUE );
            return ( T ) buildMethod.invoke( builderInstance );
        }
        return clazz.getDeclaredConstructor().newInstance();
    }

    /**
     * Safely typecasts and retrieves an object of the provided class.
     *
     * @param clazz The class of the object to retrieve.
     * @param obj   The object to typecast.
     * @return The typecast object.
     */
    public static <T> T typecastAndGet( @NonNull final Class<T> clazz, @NonNull final Object obj )
    {
        if( clazz.isAssignableFrom( obj.getClass() ) )
        {
            // Safely cast the object with unchecked cast suppression
            @SuppressWarnings( "unchecked" ) final T result = ( T ) obj;
            return result;
        }
        else
        {
            throw new RuntimeException( "Unknown object." );
        }
    }
}
