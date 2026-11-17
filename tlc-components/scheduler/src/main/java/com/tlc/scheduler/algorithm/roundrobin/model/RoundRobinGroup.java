package com.tlc.scheduler.algorithm.roundrobin.model;

/**
 * Represents a logical group used in the round-robin scheduling algorithm.
 * <p>
 * Each group defines a distinct scheduling scope that may have its own configuration,
 * such as name, unique identifier, and associated {@link LimitType}.
 * Groups help segregate or organize scheduling logic, for example by organization,
 * department, or rule definition.
 * </p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * RoundRobinGroup group = new RoundRobinGroup(1L, "Support Team", LimitType.ORG_BASED);
 * System.out.println(group.name()); // Output: Support Team
 * }</pre>
 *
 * @param id        Unique identifier of the round-robin group.
 * @param name      Descriptive name for the group.
 * @param limitType Type of limit applied to this group, defined by {@link LimitType}.
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public record RoundRobinGroup(Long id, String name, LimitType limitType) {
}
