package com.tlc.commons.util.executor;

import com.tlc.commons.functions.SafeConsumer;
import com.tlc.commons.functions.SafeFunction;
import com.tlc.commons.functions.SafeRunnable;
import com.tlc.commons.functions.SafeSupplier;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Utility class for executing operations safely with null checks.
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
public class NullSafeExecutor
{
    /**
     * Accepts a value and executes a consumer if the value is not null.
     *
     * @param value    The value to be accepted by the consumer.
     * @param consumer The consumer function to execute.
     * @param <T>      The type of the value.
     */
    public static <T> void acceptIfPresent( final T value, final @NonNull Consumer<T> consumer )
    {
        if( Objects.nonNull( value ) )
        {
            consumer.accept( value );
        }
    }

    /**
     * Accepts a value and executes a consumer safely if the value is not null.
     *
     * @param value    The value to be accepted by the consumer.
     * @param consumer The safe consumer function to execute.
     * @param <T>      The type of the value.
     * @throws Exception if an error occurs during execution.
     */
    public static <T> void acceptIfPresentSafely( final T value, final @NonNull SafeConsumer<T> consumer ) throws
            Exception
    {
        if( Objects.nonNull( value ) )
        {
            consumer.accept( value );
        }
    }

    /**
     * Executes a runnable if the value is null.
     *
     * @param value    The value to check for null.
     * @param runnable The runnable to execute if the value is null.
     * @param <T>      The type of the value.
     */
    public static <T> void runIfNotPresent( final T value, final @NonNull Runnable runnable )
    {
        if( Objects.isNull( value ) )
        {
            runnable.run();
        }
    }

    /**
     * Executes a runnable safely if the value is null.
     *
     * @param value    The value to check for null.
     * @param runnable The safe runnable to execute if the value is null.
     * @param <T>      The type of the value.
     * @throws Exception if an error occurs during execution.
     */
    public static <T> void runIfNotPresentSafely( final T value, final @NonNull SafeRunnable runnable ) throws Exception
    {
        if( Objects.isNull( value ) )
        {
            runnable.run();
        }
    }

    /**
     * Retrieves a value using a function if the input value is not null.
     *
     * @param object   The input value to apply the function to.
     * @param function The function to apply.
     * @param <T>      The type of the input value.
     * @param <R>      The type of the result value.
     * @return An Optional containing the result value if the input value is not null, otherwise empty.
     */
    public static <T, R> Optional<R> getIfPresent( final T object, final @NonNull Function<T, R> function )
    {
        if( Objects.isNull( object ) )
        {
            return Optional.empty();
        }
        return Optional.ofNullable( function.apply( object ) );
    }

    /**
     * Retrieves a value safely using a function if the input value is not null.
     *
     * @param object   The input value to apply the function to.
     * @param function The safe function to apply.
     * @param <T>      The type of the input value.
     * @param <R>      The type of the result value.
     * @return An Optional containing the result value if the input value is not null, otherwise empty.
     * @throws Exception if an error occurs during execution.
     */
    public static <T, R> Optional<R> getIfPresentSafely( final T object, final @NonNull SafeFunction<T, R> function ) throws Exception
    {
        if( Objects.isNull( object ) )
        {
            return Optional.empty();
        }
        return Optional.ofNullable( function.apply( object ) );
    }

    /**
     * Retrieves a default value if the input value is null.
     *
     * @param value        The input value to check for null.
     * @param defaultValue The default value to return if the input value is null.
     * @param <T>          The type of the value.
     * @return The input value if not null, otherwise the default value.
     */
    public static <T> T getOrDefault( final T value, final T defaultValue )
    {
        if( Objects.nonNull( value ) )
        {
            return value;
        }
        return defaultValue;
    }

    /**
     * Retrieves a value or generates one using a supplier if the input value is null.
     *
     * @param value    The input value to check for null.
     * @param supplier The supplier to generate a value if the input value is null.
     * @param <T>      The type of the value.
     * @return The input value if not null, otherwise the value generated by the supplier.
     */
    public static <T> T orElseGet( final T value, final @NonNull Supplier<T> supplier )
    {
        if( Objects.nonNull( value ) )
        {
            return value;
        }
        return supplier.get();
    }

    /**
     * Retrieves a value safely or generates one using a safe supplier if the input value is null.
     *
     * @param value    The input value to check for null.
     * @param supplier The safe supplier to generate a value if the input value is null.
     * @param <T>      The type of the value.
     * @return The input value if not null, otherwise the value generated by the supplier.
     * @throws Exception if an error occurs during execution.
     */
    public static <T> T orElseGetSafely( final T value, final @NonNull SafeSupplier<T> supplier ) throws Exception
    {
        if( Objects.nonNull( value ) )
        {
            return value;
        }
        return supplier.get();
    }
}
