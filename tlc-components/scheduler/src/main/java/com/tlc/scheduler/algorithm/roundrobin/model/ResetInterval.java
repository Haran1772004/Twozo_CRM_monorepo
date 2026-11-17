package com.tlc.scheduler.algorithm.roundrobin.model;

/**
 * Represents the various reset intervals that can be applied in a round-robin scheduling algorithm.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public enum ResetInterval {

    /**
     * Indicates that the limit resets every day.
     */
    DAILY((short) 1),

    /**
     * Indicates that the limit resets every week.
     */
    WEEKLY((short) 2),

    /**
     * Indicates that the limit resets every month.
     */
    MONTHLY((short) 3),
    ;

    private final short id;

    ResetInterval(final short id) {
        this.id = id;
    }

    public short getId() {
        return id;
    }

    /**
     * Retrieves the corresponding {@link ResetInterval} for the given numeric identifier.
     *
     * @param id The numeric ID representing a {@link ResetInterval}.
     * @return The matching {@link ResetInterval}, or {@code null} if no match is found.
     */
    public static ResetInterval get(final short id) {
        return switch (id) {
            case 1 -> DAILY;
            case 2 -> WEEKLY;
            case 3 -> MONTHLY;
            default -> null;
        };
    }
}
