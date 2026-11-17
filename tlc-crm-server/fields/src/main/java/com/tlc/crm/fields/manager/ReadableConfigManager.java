package com.tlc.crm.fields.manager;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import java.util.Collection;

/**
 * <p>
 * Provides an abstract way to manage the readable DB actions of entities
 * </p>
 *
 * @param <T>
 * @author Selvakumar G
 * @version 1.0
 */
public interface ReadableConfigManager<T> {

    /**
     * <p>
     * Checks whether any rows are having the given id
     * </p>
     *
     * @param id Entity id
     * @return If no rows are having the given id, it will return false
     */
    boolean exists(final Long id);

    /**
     * <p>
     * Returns the count of the rows that matches the given query
     * </p>
     *
     * @param countQuery Count query
     * @return Count of records that matches the query
     */
    Long count(final CountQuery countQuery);

    /**
     * <p>
     * Fetches a row that has the given id
     * </p>
     *
     * @param id ID of an entity
     * @return If no rows are having the given id, it will throw an error otherwise returns the matched entity
     */
    T get(final Long id);

    /**
     * <p>
     * Fetches a row that has the given id
     * </p>
     *
     * @param ids Collection of ids of multiple entities
     * @return Collection of model entities
     */
    Collection<T> get(final Collection<Long> ids);

    /**
     * <p>
     * Fetches the rows that matches the given query
     * </p>
     *
     * @param selectQuery Filters the unnecessary rows and columns
     * @return Collection of model entities
     */
    Collection<T> get(final SelectQuery selectQuery);

    /**
     * <p>
     * Fetches the rows that matches the given query
     * </p>
     *
     * @param whereClause Filters the unnecessary rows
     * @return Collection of model entities
     */
    Collection<T> get(final WhereClause whereClause);

    /**
     * <p>
     * Fetches the rows from the table that matches the given query and loads it into the container
     * </p>
     *
     * @param table       A table from which rows will be fetched
     * @param whereClause Filters the unnecessary rows
     * @return Container to hold the multiple rows of the given table
     */
    DataContainer getDataContainer(final Table table, final WhereClause whereClause);

    /**
     * <p>
     * Fetches the rows that matches the given query
     * </p>
     *
     * @param selectQuery Filters the unnecessary rows and columns
     * @return Container to hold the multiple rows of the given table
     */
    DataContainer getDataContainer(final SelectQuery selectQuery);

    /**
     * <p>
     * Converts the row data into model entity
     * </p>
     *
     * @param row Row that holds the information of model
     * @return Converted entity
     */
    T convertRowToModel(final Row row);

    /**
     * <p>
     * Fetches the table of the given
     * </p>
     *
     * @return Converted entity
     */
    Table getTable();

}
