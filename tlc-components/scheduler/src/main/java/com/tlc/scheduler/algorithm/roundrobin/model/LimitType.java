package com.tlc.scheduler.algorithm.roundrobin.model;

/**
 * Represents the various limit types that can be applied in a round-robin scheduling algorithm.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public enum LimitType {

    /**
     * Represents a global limit applied across all organizations and users.
     * <p>
     * For example, a total cap on tasks or resources system-wide.
     * </p>
     */
    GLOBAL((short) 1),

    /**
     * Represents a limit applied at the organization level.
     * <p>
     * For example, restricting the number of tasks assigned per organization.
     * </p>
     */
    ORG_BASED((short) 2),

    /**
     * Represents a time-based limit, typically resetting after a specific duration.
     * <p>
     * For example, a daily or hourly limit on task assignment.
     * </p>
     */
    TIME_BOUND((short) 3),

    /**
     * Represents a limit that is specific to a particular rule configuration.
     * <p>
     * For example, applying limits per round-robin rule or workflow definition.
     * </p>
     */
    RULE_BASED((short) 4),
    ;

    private final short id;

    LimitType(final short id) {
        this.id = id;
    }

    /**
     * Retrieves the corresponding {@link LimitType} for the given numeric identifier.
     *
     * @param id The numeric ID representing a {@link LimitType}.
     * @return The matching {@link LimitType}, or {@code null} if no match is found.
     */
    public static LimitType get(final short id) {
        return switch (id) {
            case 1 -> GLOBAL;
            case 2 -> ORG_BASED;
            case 3 -> TIME_BOUND;
            case 4 -> RULE_BASED;
            default -> null;
        };
    }
}
