package com.tlc.commons.code;

import java.util.Optional;

/**
 * Utility class to perform operations related to error codes, provides a set of methods to handle various operations associated with error codes
 *
 * @author sundar
 * @version 1.0
 */
public final class ErrorCodeUtil {

    private static final String COLON = ":";
    private static final ErrorCode UNKNOWN_ERROR = ErrorCode.get(ErrorCodes.UNKNOWN_ERROR, "i18n.web.internal.server.error");

    private ErrorCodeUtil() {
    }

    /**
     * Extracts the error message associated with the given {@link ErrorCode}
     *
     * @param errorCode the error code from which to extract the message
     * @return the corresponding error message
     */
    public static String extractErrorMessage(final ErrorCode errorCode) {
        final String errorCodeMessage = errorCode.getMessage();

        return errorCodeMessage.contains(COLON) ? errorCodeMessage.split(COLON, 2)[1].trim() : errorCodeMessage;
    }

    /**
     * Extracts the error message associated with the given {@link ErrorCode}
     *
     * @param errorCode the error code from which to extract the message
     * @return the corresponding error message
     */
    public static String extractErrorCodeProviderName(final ErrorCode errorCode) {
        final String errorCodeMessage = errorCode.getMessage();

        return errorCodeMessage.contains(COLON) ? errorCodeMessage.split(COLON, 2)[0].trim() : errorCodeMessage;
    }

    /**
     * Determines if the given {@link Throwable} is an instance of {@link ErrorCode}
     *
     * @param throwable the {@link Throwable} to evaluate
     * @return true if the {@link Throwable} is an instance of {@link ErrorCode}, false otherwise
     */
    public static boolean isErrorCode(final Throwable throwable) {
        return throwable instanceof ErrorCode;
    }

    /**
     * Gets the {@link ErrorCode} from the given {@link Throwable} if it is an instance of {@link ErrorCode};
     * otherwise, returns a default unknown {@link ErrorCode}
     *
     * @param throwable the {@link Throwable} to evaluate and extract the error code from
     * @return the extracted {@link ErrorCode}, or a default unknown {@link ErrorCode} if the throwable is not an instance of {@link ErrorCode}
     */
    public static ErrorCode getOrDefaultErrorCode(final Throwable throwable) {
        return throwable instanceof ErrorCode errorCode ? errorCode : UNKNOWN_ERROR;
    }

    /**
     * Returns an {@link Optional} containing the {@link ErrorCode} if the given {@link Throwable} is an instance of {@link ErrorCode}
     *
     * @param throwable the {@link Throwable} to check
     * @return an {@link Optional} containing the {@link ErrorCode} if present, otherwise an empty {@link Optional}
     */
    public static Optional<ErrorCode> optErrorCode(final Throwable throwable) {
        return Optional.ofNullable(throwable instanceof ErrorCode errorCode ? errorCode : null);
    }

    /**
     * Gets the unknown {@link ErrorCode}
     *
     * @return the unknown {@link ErrorCode}
     */
    public static ErrorCode getUnknownError() {
        return UNKNOWN_ERROR;
    }
}