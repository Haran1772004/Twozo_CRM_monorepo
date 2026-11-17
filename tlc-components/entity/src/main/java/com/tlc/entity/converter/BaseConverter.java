package com.tlc.entity.converter;

import com.tlc.commons.functions.TriFunction;
import com.tlc.entity.Entity;
import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.Join;
import com.tlc.entity.util.AnnotationExecutor;

import lombok.NonNull;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Abstract base class for converters providing common functionality for converting data from a {@link Entity} to a target type.
 * Subclasses of BasicConverter are responsible for implementing specific conversion logic for different target types.
 *
 * @param <T> The type of target for conversion.
 */
public abstract class BaseConverter<T> implements Converter<T>
{
    private final ThreadLocal<AtomicInteger> currentPos = ThreadLocal.withInitial( AtomicInteger::new );

    /**
     * Converts data from a {@link Entity} to the target representation.
     *
     * @param entity         The source entity to convert from.
     * @param target         The target object to convert into.
     * @param transformValue A function to transform column values during conversion.
     * @throws Exception If an error occurs during the conversion process.
     */
    @Override
    public void convert( @NonNull final Entity<?> entity,
                         @NonNull final T target,
                         @NonNull final TriFunction<String, String, Object, Object> transformValue ) throws Exception
    {
        currentPos.get().incrementAndGet();
        try
        {
            final Class<?> entityClass = entity.getClass();
            final AnnotationHelper aHelper = annotationHelper();

            AnnotationExecutor.executeIfAnnotationPresent( entityClass, aHelper.getTableClass(), () ->
            {
                preProcess( entity, target );
                final Set<Field> fields = getFields( entityClass );
                final String tableName = ConverterUtil.getTableName( entityClass );

                for( final Field field : fields )
                {
                    field.setAccessible( Boolean.TRUE );
                    try
                    {
                        final ConverterParams<T> converterParams = ConverterParams.create( field,
                                                                                           tableName,
                                                                                           entity,
                                                                                           target,
                                                                                           transformValue );
                        if( field.isAnnotationPresent( aHelper.getColumnClass() ) )
                        {
                            final Column columnAnnot = field.getAnnotation( aHelper.getColumnClass() );
                            processColumnField( columnAnnot, converterParams );
                        }
                        else if( field.isAnnotationPresent( aHelper.getJoinClass() ) )
                        {
                            final Join joinColumnAnnot = field.getAnnotation( aHelper.getJoinClass() );
                            processJoinColumnField( joinColumnAnnot, converterParams );
                        }
                    }
                    finally
                    {
                        field.setAccessible( Boolean.FALSE );
                    }
                }
                postProcess( entity, target );
            } );
        }
        catch( Exception e )
        {
            try
            {
                onFail( entity, target );
                throw e;
            }
            finally
            {
                currentPos.get().set( 0 );
            }
        }
        currentPos.get().decrementAndGet();
    }

    /**
     * Method to be overridden for handling actions when conversion fails.
     *
     * @param entity The source entity.
     * @param target The target object.
     */
    protected void onFail( final Entity<?> entity, final T target )
    {}

    /**
     * Retrieve the current position in the entity hierarchy during conversion.
     *
     * <p>
     * The "current position" refers to the position of the entity within the hierarchy
     * being processed during conversion. This position can indicate the entity's
     * position relative to its parent and other entities in the hierarchy.
     * </p>
     *
     * @return The current position in the entity hierarchy.
     */
    protected int getCurrentPos()
    {
        return currentPos.get().get();
    }

    /**
     * Retrieves the annotation helper instance.
     *
     * @return The {@link AnnotationHelper} instance.
     */
    protected final AnnotationHelper annotationHelper()
    {
        return AnnotationHelper.INSTANCE();
    }

    /**
     * Performs preprocessing before the conversion.
     *
     * @param entity The source entity.
     * @param target The target object.
     * @throws Exception If an error occurs during preprocessing.
     */
    abstract protected void preProcess( final Entity<?> entity, final T target ) throws Exception;

    /**
     * Retrieves the fields to be processed for conversion.
     *
     * @param entityClass The class of the source entity.
     * @return The set of fields to be processed.
     */
    @NonNull
    abstract protected Set<Field> getFields( final Class<?> entityClass );

    /**
     * Processes a field annotated with @Column.
     *
     * @param columnAnnot     The Column annotation instance.
     * @param converterParams The parameters required for conversion.
     * @throws Exception If an error occurs during field processing.
     */
    abstract protected void processColumnField( final Column columnAnnot,
                                                final ConverterParams<T> converterParams ) throws Exception;

    /**
     * Processes a field annotated with @JoinColumn.
     *
     * @param joinAnnot       The Join annotation instance.
     * @param converterParams The parameters required for conversion.
     * @throws Exception If an error occurs during field processing.
     */
    abstract protected void processJoinColumnField( final Join joinAnnot,
                                                    final ConverterParams<T> converterParams ) throws Exception;

    /**
     * Performs postprocessing after the conversion.
     *
     * @param entity The source entity.
     * @param target The target object.
     * @throws Exception If an error occurs during postprocessing.
     */
    abstract protected void postProcess( final Entity<?> entity, final T target ) throws Exception;
}
