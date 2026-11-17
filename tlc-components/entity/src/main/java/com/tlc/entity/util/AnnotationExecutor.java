package com.tlc.entity.util;

import com.tlc.commons.functions.SafeRunnable;
import com.tlc.entity.converter.ConverterUtil;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.lang.annotation.Annotation;
import java.util.Optional;

/**
 * Utility class for annotations.
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
public class AnnotationExecutor
{
    /**
     * Executes a code block if a specified annotation is present on the given element.
     *
     * @param entityCLass The entity class to check for the presence of the annotation.
     * @param annotation  The class object representing the annotation to check for.
     * @param runnable    The code block to execute if the annotation is present.
     * @throws Exception if an error occurs during the execution, or if the annotation is not present.
     */
    public static void executeIfAnnotationPresent( @NonNull final Class<?> entityCLass,
                                                   @NonNull final Class<? extends Annotation> annotation,
                                                   @NonNull final SafeRunnable runnable ) throws Exception
    {
        final boolean isPresent = ConverterUtil.findAndGet( entityCLass, eClass ->
        {
            if( eClass.isAnnotationPresent( annotation ) )
            {
                return Optional.of( Boolean.TRUE );
            }
            return Optional.empty();
        } );

        if( isPresent )
        {
            runnable.run();
        }
        else
        {
            throw new RuntimeException( "Annotation is not present in the entity object" );
        }
    }
}