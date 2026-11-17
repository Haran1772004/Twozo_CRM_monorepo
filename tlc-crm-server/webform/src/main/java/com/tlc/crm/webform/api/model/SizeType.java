package com.tlc.crm.webform.api.model;

/**
 * Holds the web-form {@link SizeType} data.
 *
 * @author Breethika Devi
 * @version 1.0
 */
public enum SizeType {

    AUTOMATICALLY_ADJUST("Automatically Adjust", 1),
    EXACT_WIDTH_AND_HEIGHT("Exact Width and Height", 2),
    ;

    private final String name;
    private final int id;

    SizeType(final String name, final int id) {
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
     * Gets the {@link SizeType} by id.
     *
     * @param id Refers the id of the size type.
     * @return The {@link SizeType}.
     */
    public static SizeType getSizeType(final int id) {
        return switch (id) {
            case 1 -> AUTOMATICALLY_ADJUST;
            case 2 -> EXACT_WIDTH_AND_HEIGHT;
            default -> throw new IllegalArgumentException("Invalid size type");
        };
    }
}