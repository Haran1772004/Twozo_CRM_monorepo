package com.tlc.crm.common.list;

import com.tlc.sql.common.api.dml.JoinClause;

import java.util.List;

/**
 * An interface to obtain the {@link JoinClause} for the dependent
 *
 * @author karthickV
 * @version 1.0
 */
public interface JoinLoader {

    /**
     * Checks whether the source has joins are not.
     *
     * @return true if dependent source exist
     */
    boolean isJoinExist();

    /**
     * Gets the {@link JoinClause}
     *
     * @return list of {@link List<JoinClause>}
     */
    List<JoinClause> getJoins();

    /**
     * Adds the {@link JoinClause}
     *
     * @param joinClauses list of joinClauses.
     */
    void addJoinClause(final List<JoinClause> joinClauses);

}
