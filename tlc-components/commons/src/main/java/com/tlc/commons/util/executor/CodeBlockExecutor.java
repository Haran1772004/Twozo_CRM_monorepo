package com.tlc.commons.util.executor;

import com.tlc.commons.functions.SafeConsumer;
import com.tlc.commons.functions.SafeRunnable;
import com.tlc.commons.functions.SafeSupplier;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Optional;

/**
 * Utility class for executing code blocks safely and handling exceptions.
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
public class CodeBlockExecutor
{
    private static final Logger LOGGER = LoggerFactory.getLogger( CodeBlockExecutor.class );

    /**
     * Executes a code block silently, logging any exceptions.
     *
     * @param runnable The code block to execute.
     */
    public static void runSilently( final @NonNull SafeRunnable runnable )
    {
        try
        {
            runnable.run();
        }
        catch( Exception e )
        {
            printTrace( e );
        }
    }

    /**
     * Retrieves a value from a code block safely, logging any exceptions.
     *
     * @param supplier The code block supplying the value.
     * @param <T>      The type of the value to retrieve.
     * @return An Optional containing the retrieved value, or empty if an exception occurs.
     */
    public static <T> Optional<T> getSilently( final @NonNull SafeSupplier<T> supplier )
    {
        try
        {
            return Optional.ofNullable( supplier.get() );
        }
        catch( Exception e )
        {
            printTrace( e );
        }
        return Optional.empty();
    }

    /**
     * Executes a code block without checked exceptions.
     *
     * @param runnable The code block to execute.
     */
    public static void runAsUnchecked( final @NonNull SafeRunnable runnable )
    {
        try
        {
            runnable.run();
        }
        catch ( final Exception exception )
        {
            sneakyThrow( exception );
        }
    }

    /**
     * Retrieves a value from a code block without checked exceptions.
     *
     * @param supplier The code block supplying the value.
     * @param <T>      The type of the value to retrieve.
     * @return The retrieved value.
     */
    public static <T> T getWithUnchecked( final @NonNull SafeSupplier<T> supplier )
    {
        try
        {
            return supplier.get();
        }
        catch ( final Exception exception )
        {
            return sneakyThrow( exception );
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable, R> R sneakyThrow( final Throwable throwable ) throws T
    {
        throw (T) throwable;
    }

    /**
     * Iterates over a collection of elements and safely applies a consumer to each element.
     *
     * @param elements The collection of elements to iterate over.
     * @param consumer The consumer function to apply to each element.
     * @param <T>      The type of elements in the collection.
     * @throws Exception If an exception occurs while applying the consumer.
     */
    public static <T> void forEachSafely( final @NonNull Collection<T> elements,
                                          final @NonNull SafeConsumer<T> consumer ) throws Exception
    {
        for( T element : elements )
        {
            consumer.accept( element );
        }
    }

    /**
     * Prints the stack trace of an exception.
     *
     * @param e The exception whose stack trace to print.
     */
    private static void printTrace( final Exception e )
    {
        LOGGER.error( "Exception occurred during the execution of a code block", e );
    }

    @SuppressWarnings( "unchecked" )
    private static <T extends Throwable> T rethrow( final Throwable throwable ) throws T
    {
        throw (T) throwable;
    }
}
