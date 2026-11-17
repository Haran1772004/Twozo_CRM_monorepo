package com.tlc.sql.common.api.dml.util;

import com.tlc.sql.common.api.dml.Connector;
import com.tlc.sql.common.api.dml.WhereClause;

import java.util.Objects;

/**
 * <p>
 * Utility class for handling {@link WhereClause} related functionality
 * </p>
 *
 * @author Mohamed Yasar k
 * @version 1.0
 */
public final class WhereClauseUtil {

    private WhereClauseUtil() {
    }

    /**
     * Connects the source and target where clauses using the specified connector
     *
     * @param left      The source where clause to link
     * @param right     The target where clause to link
     * @param connector The connector used to link the where clauses
     * @return The {@link WhereClause} connected by the provided {@link Connector}
     */
    public static WhereClause connect(final WhereClause left, final WhereClause right, final Connector connector) {

        if (Objects.isNull(right)) {
            return left;
        }

        if (Objects.isNull(connector) || Objects.isNull(left)) {
            return right;
        }

        return switch (connector) {
            case AND -> left.and(right);
            case OR -> left.or(right);
        };
    }
}