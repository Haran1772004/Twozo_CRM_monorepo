package com.tlc.marketplace.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.marketplace.status.MarketplaceErrorCodes;

import java.util.Collection;
import java.util.List;

/**
 * Refers the enum of {@code AppState}.
 *
 * @author ThalaimalaiPandiyanT, Muthu kumar V
 * @version 1.0
 */
public enum AppState {

    /**
     * App is installed but not yet connected.
     */
    INSTALLED((short) 1),

    /**
     * App is properly authenticated and active.
     */
    CONNECTED((short) 2),

    /**
     * App was installed but now inactive.
     */
    DISCONNECTED((short) 3),

    /**
     * App is uninstalled.
     */
    UNINSTALLED((short) 4),
    ;

    private final short id;

    AppState(short id) {
        this.id = id;
    }

    public short getId() {
        return id;
    }

    /**
     * Gets the {@link AppState} by its id.
     *
     * @param id Refers the id.
     * @return The {@link AppState}.
     */
    public static AppState get(final short id) {
        return switch (id) {
            case 1 -> INSTALLED;
            case 2 -> CONNECTED;
            case 3 -> DISCONNECTED;
            case 4 -> UNINSTALLED;
            default -> throw ErrorCode.get(MarketplaceErrorCodes.INVALID_APP_STATE, "i18n.marketplace.app.state.invalid");
        };
    }

    /**
     * Gets the collection of active {@link AppState} ids
     *
     * @return The collection of active app state ids.
     */
    public static Collection<Short> getActiveConnectionIds() {
        return List.of(INSTALLED.id, CONNECTED.id);
    }
}