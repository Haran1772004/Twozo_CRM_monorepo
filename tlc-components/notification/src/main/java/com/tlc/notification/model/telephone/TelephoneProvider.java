package com.tlc.notification.model.telephone;

import com.tlc.commons.code.ErrorCode;
import com.tlc.notification.internal.status.NotificationErrorCodes;

/**
 * Represents the supported telephony providers
 *
 * @author kishore J
 * @version 1.0
 */
public enum TelephoneProvider {

    TWILIO((short) 1, "i18n.notification.telephone.twilio.display.name"),
    TELE_CMI((short) 2, "i18n.notification.telephone.telecmi.display.name"),
    M_CUBE((short) 3, "i18n.notification.telephone.mcube.display.name"),
    ;

    private final short id;
    private final String displayName;

    TelephoneProvider(final short id, final String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    /**
     * <p>
     * Gets the {@link TelephoneProvider}
     * </p>
     *
     * @param id Represents the telephone id
     * @return The {@link TelephoneProvider}
     */
    public static TelephoneProvider get(final short id) {
        return switch (id) {
            case 1 -> TWILIO;
            case 2 -> TELE_CMI;
            case 3 -> M_CUBE;
            default -> throw ErrorCode.get(NotificationErrorCodes.INVALID_TELEPHONE_PROVIDER);
        };
    }

    /**
     * <p>
     * Gets the {@link TelephoneProvider}
     * </p>
     *
     * @param name Represents the telephone name
     * @return The {@link TelephoneProvider}
     */
    public static TelephoneProvider get(final String name) {
        return switch (name) {
            case "twilio" -> TWILIO;
            case "telecmi" -> TELE_CMI;
            case "mcube" -> M_CUBE;
            default -> throw ErrorCode.get(NotificationErrorCodes.INVALID_TELEPHONE_PROVIDER);
        };
    }

    public short getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static TelephoneProvider getDefaultProvider() {
        return TWILIO;
    }
}
