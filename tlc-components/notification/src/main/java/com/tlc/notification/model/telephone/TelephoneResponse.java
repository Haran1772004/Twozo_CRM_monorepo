package com.tlc.notification.model.telephone;

/**
 * Represents a generic response from a telephone operation.
 *
 * @author kishore J
 * @version 1.0
 */
public class TelephoneResponse {

    private Status status;
    private String errorMessage;

    public Status getStatus() {
        return status;
    }

    public void setStatus(final Status status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(final String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public enum Status {
        SUCCESS,
        FAILURE;

        /**
         * <p>
         * Checks if the current enum instance represents a successful state
         * </p>
         *
         * @return {@code true} if the enum is {@link #SUCCESS}, {@code false} otherwise
         */
        public boolean isSuccessful() {
            return this == SUCCESS;
        }
    }
}
