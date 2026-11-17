package com.tlc.crm.webform.api.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.webform.internal.status.WebFormErrorCodes;

/**
 * Holds the web-form {@link Alignment} data.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public enum Alignment {

    LEFT("Left", 1),
    CENTER("Center", 2),
    RIGHT("Right", 3),
    ;

    private final String name;
    private final int id;

    Alignment(final String name, final int id) {
        this.name = name;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    /**
     * Gets the {@link Alignment} by id.
     *
     * @param id Refers the id of the alignment.
     * @return The {@link Alignment}.
     */
    public static Alignment getAlignment(final int id) {
        return switch (id) {
            case 1 -> LEFT;
            case 2 -> CENTER;
            case 3 -> RIGHT;
            default -> throw ErrorCode.get(WebFormErrorCodes.INVALID_ALIGNMENT);
        };
    }
}
