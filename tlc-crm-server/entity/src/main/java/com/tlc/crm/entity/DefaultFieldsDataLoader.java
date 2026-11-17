package com.tlc.crm.entity;

import com.tlc.crm.entity.internal.DefaultFieldsDataLoaderImpl;
import com.tlc.crm.entity.models.Entity;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.internal.DataLoaderValues;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * An interface which provides abstract way to deal with default fields
 * </p>
 *
 * @author Ajith venkadesh.
 * @version 1.0
 */
public interface DefaultFieldsDataLoader {

    /**
     * <p>
     * Fetches the instance of {@link DefaultFieldsDataLoader}
     * </p>
     *
     * @return {@link DefaultFieldsDataLoader}
     */
    static DefaultFieldsDataLoader getInstance() {
        return DefaultFieldsDataLoaderImpl.getInstance();
    }

    /**
     * <p>
     * Loads the default field data into container.
     * </p>
     *
     * @param dataLoaderValues Entity of {@link DataLoaderValues}
     */
    <T extends Entity> void loadData(final DataLoaderValues<T> dataLoaderValues);

    /**
     * <p>
     * Provides the common way to fetch the data container for all the sources generically.
     * </p>
     *
     * @param orgId           Represents the organization id.
     * @param selectQuery     Entity of {@link SelectQuery}
     * @param table           Represents the main source table.
     * @param fields          Represents the collection of fields of the source.
     * @param requiredColumns Represents the collection of required columns.
     * @param joins           Represents the collection of joins.
     * @return Entity of {@link DataContainer}
     */
    DataContainer fetchDataContainer(final Long orgId, final SelectQuery selectQuery, final Table table,
                                     final Collection<FieldInfo> fields, final Map<Table, Collection<Column>> requiredColumns,
                                     final Map<Table, JoinClause.JoinType> joins);

    /**
     * Fetch the data container for default fields of all sources generically when the required.
     *
     * @param orgId       Represents the organization id.
     * @param selectQuery Refers the {@link SelectQuery}.
     * @param table       Represents the main table.
     * @param fields      Represents the collection of fields of the source.
     * @param entityIds   Represents the Collection of entity ids
     * @return The {@link DataContainer} of default fields.
     */
    DataContainer fetchDefaultFieldsDataContainer(final Long orgId, final SelectQuery selectQuery, final Table table,
                                                  final Collection<FieldInfo> fields, final Collection<Long> entityIds);

    /**
     * <p>
     * Provides the common way to build the select query for all the sources generically.
     * </p>
     *
     * @param orgId           Represents the organization id.
     * @param entityIds       Represents the Collection of entity ids
     * @param selectQuery     Entity of {@link SelectQuery}
     * @param table           Represents the main source table.
     * @param fields          Represents the collection of fields of the source.
     * @param requiredColumns Represents the collection of required columns.
     * @param joins           Represents the collection of joins.
     */
    Collection<SelectQuery> getSelectQueries(final Long orgId, final Collection<Long> entityIds, final SelectQuery selectQuery, final Table table,
                                             final Collection<FieldInfo> fields, final Map<Table, Collection<Column>> requiredColumns,
                                             final Map<Table, JoinClause.JoinType> joins);

    /**
     * <p>
     * Provides the common way to fetch the data container for all the sources generically when the required
     * columns and joins are not specified.
     * </p>
     *
     * @param orgId       Represents the organization id.
     * @param selectQuery Entity of {@link SelectQuery}
     * @param table       Represents the main source table.
     * @param fields      Represents the collection of fields of the source.
     * @return Entity of {@link DataContainer}
     */
    DataContainer fetchDataContainer(final Long orgId, final SelectQuery selectQuery, final Table table,
                                     final Collection<FieldInfo> fields);
}
