package com.tlc.sql.common.api.dml.secure;

import com.tlc.commons.json.JsonArray;
import com.tlc.sql.common.api.dml.Connector;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.Table;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * Utility class that represents a list of {@link JoinConfig} objects constructed from a JSON array.
 * This class provides functionality to convert join configurations into actual {@link JoinClause} instances.
 *
 * <p>Used primarily in dynamic table join operations where the join information is provided as JSON.</p>
 *
 * @author Sathish
 * @version 1.0
 */
public class JoinConfigs {

    private final List<JoinConfig> joinConfigs;

    public JoinConfigs(final JsonArray joins) {
        joinConfigs = new LinkedList<>();

        if (Objects.nonNull(joins)) {
            final int size = joins.size();

            for (int index = 0; index < size; index++) {
                joinConfigs.add(new JoinConfig(joins.getJsonObject(index)));
            }
        }
    }

    /**
     * Converts all stored join configurations to {@link JoinClause} objects using the provided table definitions.
     *
     * @param tables the tables to use while resolving column references in joins
     * @return list of {@link JoinClause} instances representing the configured joins
     */
    public List<JoinClause> getJoins(final Table... tables) {
        final List<JoinClause> joinClauses = new LinkedList<>();

        for (final JoinConfig joinConfig : joinConfigs) {
            joinClauses.add(joinConfig.getJoin(false, null, tables));
        }
        return joinClauses;
    }

    /**
     * Converts all stored join configurations to {@link JoinClause} objects using the provided table definitions.
     *
     * @param criteriaConnection the criteriaConnection to use while resolving column references in joins
     * @param tables             the tables to use while resolving column references in joins
     * @return list of {@link JoinClause} instances representing the configured joins
     */
    public List<JoinClause> getJoins(final Connector criteriaConnection, final Table... tables) {
        final List<JoinClause> joinClauses = new LinkedList<>();

        for (final JoinConfig joinConfig : joinConfigs) {
            joinClauses.add(joinConfig.getJoin(false, criteriaConnection, tables));
        }
        return joinClauses;
    }

    /**
     * Converts all stored join configurations to {@link JoinClause} objects using the provided table definitions.
     *
     * @param needJoinInversion indicates whether the join type should be inverted (e.g., LEFT to RIGHT)
     * @param tables            the tables to use while resolving column references in joins
     * @return list of {@link JoinClause} instances representing the configured joins
     */
    public List<JoinClause> getJoins(final boolean needJoinInversion, final Table... tables) {
        final List<JoinClause> joinClauses = new LinkedList<>();

        for (int index = joinConfigs.size() - 1; index >= 0; index--) {
            joinClauses.add(joinConfigs.get(index).getJoin(needJoinInversion, null, tables));
        }
        return joinClauses;
    }

    /**
     * Converts all stored join configurations to {@link JoinClause} objects using the provided table definitions.
     *
     * @param needJoinInversion  indicates whether the join type should be inverted (e.g., LEFT to RIGHT)
     * @param criteriaConnection the criteriaConnection to use while resolving column references in joins
     * @param tables             the tables to use while resolving column references in joins
     * @return list of {@link JoinClause} instances representing the configured joins
     */
    public List<JoinClause> getJoins(final boolean needJoinInversion, final Connector criteriaConnection, final Table... tables) {
        final List<JoinClause> joinClauses = new LinkedList<>();

        for (int index = joinConfigs.size() - 1; index >= 0; index--) {
            joinClauses.add(joinConfigs.get(index).getJoin(needJoinInversion, criteriaConnection, tables));
        }
        return joinClauses;
    }

    /**
     * Checks whether there are any join configurations present.
     *
     * @return {@code true} if no join configurations are stored, {@code false} otherwise
     */
    public boolean isEmpty() {
        return joinConfigs.isEmpty();
    }
}
