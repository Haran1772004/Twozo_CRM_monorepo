package com.tlc.crm.entity.list.filter.source;

import com.tlc.crm.entity.list.filter.models.JoinPreference;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * <p>
 * Provides the source mapping groups that identify which mapper table joins the two sources.
 * This enum defines the join priority details between different filter source types.
 * </p>
 *
 * @author Mohamed Yasar k
 * @version 1.0
 */
public enum SourceJoinDetails {

    CONTACT_DEAL(JoinPreference.MAPPER_TABLE, FilterSourceType.CONTACT, FilterSourceType.DEAL),
    DEAL_CONTACT(JoinPreference.MAPPER_TABLE, FilterSourceType.DEAL, FilterSourceType.CONTACT);

    private static final Map<FilterSourceType, Map<FilterSourceType, JoinPreference>> SOURCE_TABLE_MAPPINGS;

    private final JoinPreference joinPreference;
    private final FilterSourceType primarySource;
    private final FilterSourceType secondarySource;

    static {
        SOURCE_TABLE_MAPPINGS = new HashMap<>();

        for (final SourceJoinDetails sourceJoinDetails : values()) {
            SOURCE_TABLE_MAPPINGS.computeIfAbsent(sourceJoinDetails.primarySource, mappings -> new HashMap<>())
                    .put(sourceJoinDetails.secondarySource, sourceJoinDetails.joinPreference);
        }
    }

    SourceJoinDetails(final JoinPreference joinPreference, final FilterSourceType primarySource,
                      final FilterSourceType secondarySource) {
        this.joinPreference = joinPreference;
        this.primarySource = primarySource;
        this.secondarySource = secondarySource;
    }

    /**
     * <p>
     * Provides the {@link JoinPreference} for the given primary and secondary sources.
     * </p>
     *
     * @param primarySource   Represents the primary source.
     * @param secondarySource Represents the secondary source.
     * @return The {@link JoinPreference} that defines the join priority between the two sources, or {@code null} if no mapping exists.
     */
    public static Optional<JoinPreference> getJoinPriorityType(final FilterSourceType primarySource, final FilterSourceType secondarySource) {
        if (SOURCE_TABLE_MAPPINGS.containsKey(primarySource)) {
            final Map<FilterSourceType, JoinPreference> sourceMappings = SOURCE_TABLE_MAPPINGS.get(primarySource);

            return Optional.ofNullable(sourceMappings.get(secondarySource));
        }

        return Optional.empty();
    }

    /**
     * <p>
     * Checks whether a mapping exists between the given primary and secondary sources.
     * </p>
     *
     * @param primarySource   Represents the primary source.
     * @param secondarySource Represents the secondary source.
     * @return true if a mapping exists, otherwise false.
     */
    public static boolean contains(final FilterSourceType primarySource, final FilterSourceType secondarySource) {
        if (SOURCE_TABLE_MAPPINGS.containsKey(primarySource)) {
            final Map<FilterSourceType, JoinPreference> sourceMappings = SOURCE_TABLE_MAPPINGS.get(primarySource);

            return sourceMappings.containsKey(secondarySource);
        } else if (SOURCE_TABLE_MAPPINGS.containsKey(secondarySource)) {
            final Map<FilterSourceType, JoinPreference> sourceMappings = SOURCE_TABLE_MAPPINGS.get(secondarySource);

            return sourceMappings.containsKey(primarySource);
        }

        return false;
    }
}