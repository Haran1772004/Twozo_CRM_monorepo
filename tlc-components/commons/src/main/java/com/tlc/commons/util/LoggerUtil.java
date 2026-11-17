package com.tlc.commons.util;

import org.slf4j.Logger;

/**
 * Utility class that provides functionality for logging management
 *
 * @author sundar
 * @version 1.0
 */
public final class LoggerUtil {

    private LoggerUtil() {
    }

    /**
     * Logs the provided error message and cause if debug mode is enabled in the logger; otherwise, only the error message is logged
     *
     * @param logger  The logger instance
     * @param cause   The exception that was raised
     * @param message The error message to log
     * @param args    The arguments to be included in the log alongside the error message
     */
    public static void logError(final Logger logger, final Throwable cause, final String message, final Object... args) {

        if (logger.isDebugEnabled()) {
            logger.error(message, args, cause);
        } else {
            logger.error(message, args);
        }
    }
}