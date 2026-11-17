package com.tlc.commons.util;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.util.constants.NumericalConstant;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import java.util.Collection;
import java.util.Optional;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * @author Abishek
 * @version 1.0
 */
public final class JavaUtil {
    public static void sleep(long time) {

        try {
            Thread.sleep(time);
        } catch (InterruptedException ignore) {
        }
    }

    /**
     * Splits the given {@link Collection} of values by the batch size
     *
     * @param values Which contains the collection of values
     * @return the {@link Collection} of chunked values
     */
    public static <T> Collection<Collection<T>> splitCollection(final Collection<T> values, final int chunkSize) {
        int totalSize = values.size();
        final Collection<Collection<T>> chunks = new HashSet<>((totalSize + chunkSize - 1) / chunkSize);
        final List<T> valueList = values.stream().toList();

        for (int i = 0; i < totalSize; i += chunkSize) {
            chunks.add(valueList.subList(i, Math.min(totalSize, i + chunkSize)));
        }

        return chunks;
    }

    /**
     * <p>
     * Gets unchecked cast.
     * </p>
     *
     * @param clazz {@link Class}.
     * @return unchecked cast.
     */
    @SuppressWarnings("unchecked")
    public static <T> Class<T> uncheckedCast(final Class<?> clazz) {
        return (Class<T>) clazz;
    }

    /***
     * <p>
     *     Gets instance if passed class has an getInstance method.
     * </p>
     * @param clazz {@link Class}.
     * @return instance.
     * @param <T> type of class.
     */
    public static <T> Optional<T> getInstance(final Class<T> clazz) {
        try {
            final Optional<Method> optionalInstanceMethod = Arrays.stream(clazz.getDeclaredMethods()).filter(method ->
                    Modifier.isStatic(method.getModifiers()) && NumericalConstant.ZERO == method.getParameterCount() &&
                            method.getReturnType().isAssignableFrom(clazz) && method.getName().equals("getInstance")).findFirst();

            if (optionalInstanceMethod.isPresent()) {
                final Method instanceMethod = optionalInstanceMethod.get();

                instanceMethod.setAccessible(true);

                return Optional.of((T) instanceMethod.invoke(null));
            }
        } catch (Exception e) {
            throw ErrorCode.get(ErrorCodes.INSTANCE_METHOD_RETRIEVAL_FAILED);
        }
        return Optional.empty();
    }
}
