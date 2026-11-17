package com.tlc.email.common.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.email.common.internal.status.EmailErrorCodes;

/**
 * <p>
 * Each provider defines a different delivery method or integration (e.g., cloud email services, or API-driven delivery).
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public enum ServiceProvider {

    /*
     * <p>
     * A cloud-based transactional email service designed for reliable,
     * scalable, and cost-effective delivery of system or application-generated emails.
     * </p>
     */
    SES("ses"),

    /**
     * <p>
     * Uses the SendGrid REST API for sending high-volume or transactional emails.
     * </p>
     */
    SEND_GRID("sendGrid"),

    /**
     * <p>
     * Uses the ACS for send email.
     * </p>
     */
    ACS("acs"),
    ;

    private final String name;

    ServiceProvider(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /**
     * <p>
     * Returns the {@link ServiceProvider} enum corresponding to the given string value.
     * The lookup is case-insensitive.
     * </p>
     *
     * @param name The string representation of the service provider
     * @return The matching {@link ServiceProvider} enum
     */
    public static ServiceProvider get(final String name) {
        return switch (name) {
            case "sendGrid" -> SEND_GRID;
            case "ses" -> SES;
            case "acs" -> ACS;
            default -> throw ErrorCode.get(EmailErrorCodes.UNSUPPORTED_SERVICE_PROVIDER);
        };
    }
}