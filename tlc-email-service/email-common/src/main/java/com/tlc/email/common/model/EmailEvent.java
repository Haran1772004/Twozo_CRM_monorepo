package com.tlc.email.common.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.email.common.internal.status.EmailErrorCodes;

/**
 * <p>
 * Represents a generic email webhook event. Implementations of this interface define specific types.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public interface EmailEvent {

    /**
     * <p>
     * Returns the type of event associated with this webhook.
     * </p>
     *
     * @return The {@link Event} type of the webhook.
     */
    Event getEvent();

    /**
     * <p>
     * Enumerates the supported actions that can be performed via email webhook events.
     * </p>
     *
     * @author Mohamed Yasar K
     * @version 1.0
     */
    enum Event {

        CREATE("create"),
        UPDATE("update"),
        DELETE("delete");

        private final String name;

        /**
         * <p>
         * Constructs an {@link Event} with the given name.
         * </p>
         *
         * @param name The string representation of the event.
         */
        Event(final String name) {
            this.name = name;
        }

        /**
         * <p>
         * Returns the name of the event as a lowercase string.
         * </p>
         *
         * @return The event name.
         */
        public String getName() {
            return name;
        }

        /**
         * <p>
         * Converts a string into a corresponding {@link Event} value.
         * </p>
         *
         * @param name The name of the event to match (case-insensitive).
         * @return The corresponding {@link Event}.
         */
        public static Event get(final String name) {
            return switch (name.toLowerCase()) {
                case "create" -> CREATE;
                case "update" -> UPDATE;
                case "delete" -> DELETE;
                default -> throw ErrorCode.get(EmailErrorCodes.UNSUPPORTED_WEBHOOK_EVENT);
            };
        }
    }
}