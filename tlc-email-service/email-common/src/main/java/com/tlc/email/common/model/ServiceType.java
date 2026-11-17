package com.tlc.email.common.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.email.common.internal.status.EmailErrorCodes;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * Enum representing the supported email service types.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public enum ServiceType {

    SMART_BCC("smartbcc"),
    TEAM_INBOX("teaminbox"),
    ;

    private final String name;

    ServiceType(final String name) {
        this.name = name;
    }

    /**
     * <p>
     * Returns the service name string.
     * </p>
     *
     * @return the {@link String} name of the service type
     */
    public String getName() {
        return name;
    }

    /**
     * <p>
     * Gets the supported service types.
     * </p>
     *
     * @return a {@link Collection} containing supported {@link ServiceType}s
     */
    public static Collection<ServiceType> get() {
        return List.of(SMART_BCC, TEAM_INBOX);
    }

    /**
     * <p>
     * Gets the {@link ServiceType} for the given string value.
     * </p>
     *
     * @param value the string representation of the service type
     * @return the matching {@link ServiceType}
     */
    public static ServiceType get(final String value) {
        return switch (value.toLowerCase()) {
            case "smartbcc" -> SMART_BCC;
            case "teaminbox" -> TEAM_INBOX;
            default -> throw ErrorCode.get(EmailErrorCodes.UNSUPPORTED_SERVICE_TYPE);
        };
    }

    /**
     * <p>
     * Check whether it is smart bcc type or not.
     * </p>
     *
     * @return whether it is smart bcc type or not
     */
    public boolean isSmartBcc() {
        return Objects.equals(this, SMART_BCC);
    }
}