package com.tlc.crm.entity.list.filter.appender;

import com.tlc.crm.entity.list.filter.appender.model.CriteriaAppenderRequisites;

import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.WhereClause;

/**
 * <p>
 * Provides the appender to appends the {@link Criteria}.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface CriteriaAppender<R extends CriteriaAppenderRequisites> {

    /**
     * <p>
     * Checks whether the column is present or not.
     * </p>
     *
     * @return A flag that represents whether the column is present or not.
     */
    boolean hasColumn();

    /**
     * <p>
     * Sets the {@link R} to build criteria.
     * </p>
     *
     * @param requisites Represents the {@link R}.
     */
    void setCriteriaRequisites(final R requisites);

    /**
     * <p>
     * Provide the type of the appender.
     * </p>
     *
     * @return Appender {@link Type}.
     */
    Type getType();

    /**
     * <p>
     * Appends the criteria into the given existing {@link WhereClause}.
     * </p>
     *
     * @param whereClause {@link WhereClause} that holds the retrieval criteria.
     * @return {@link WhereClause} that holds the retrieval criteria.
     */
    WhereClause append(final WhereClause whereClause);

    /**
     * <p>
     * Represents the type of {@link CriteriaAppender}.
     * </p>
     */
    interface Type {

        /**
         * <p>
         * Provides the name of the {@link CriteriaAppender} type.
         * </p>
         *
         * @return {@link CriteriaAppender} type name.
         */
        String getName();
    }
}
