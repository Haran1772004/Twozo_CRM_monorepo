package com.tlc.pk.export.csvexporter.strategy;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * Factory class for creating instances of {@link CSVStrategy}.
 *
 * <p>This class provides utility methods to obtain predefined CSV strategies, such as the default strategy.
 * It ensures that strategy creation is centralized, making it easy to modify or extend CSV formatting rules.</p>
 *
 * <p>This class is not meant to be instantiated.</p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CSVStrategyFactory
{

    /**
     * Returns the default {@link CSVStrategy} instance.
     *
     * <p>The default strategy uses a comma (',') as the delimiter, double quotes ('"') for enclosing values,
     * and a newline ('\n') for line termination.</p>
     *
     * @return the default {@code CSVStrategy} instance
     */
    public static @NonNull CSVStrategy getDefaultStrategy()
    {
        return DefaultCSVStrategy.getInstance();
    }

}
