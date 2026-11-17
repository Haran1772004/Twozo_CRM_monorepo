package com.tlc.crm.entity.list.filter.loader;

import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.JoinClause;

/**
 * <p>
 * Loads the joins into the select query.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface JoinsLoader {

    /**
     * <p>
     * Builds and adds the {@link JoinClause} in the select query.
     * </p>
     *
     * @param localColumn  Represents the local column of the {@link JoinClause}.
     * @param remoteColumn Represents the remote column of the {@link JoinClause}.
     */
    void load(final Column localColumn, final Column remoteColumn);
}
