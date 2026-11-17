package com.tlc.crm.entity.handler;

import com.tlc.crm.entity.internal.EntityHandlerImpl;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.entity.models.ValueHolder;
import com.tlc.sql.common.api.dml.DeleteQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * Handler which provides abstract way to deal with entity events
 * </p>
 *
 * @author Sundar
 * @version 1.0
 */
public interface EntityHandler {

    /**
     * <p>
     * Fetches the instance of the {@link EntityHandler}
     * </p>
     *
     * @return {@link EntityHandler}
     */
    static EntityHandler getInstance() {
        return EntityHandlerImpl.getInstance();
    }

    /**
     * Builds {@link DeleteQuery} to delete the records
     *
     * @param table       represents the table that contains the rows
     * @param whereClause represents the criteria to fetch the rows from the given table
     * @return {@link DeleteQuery}
     */
    DeleteQuery buildEntityDeleteQuery(final Table table, final WhereClause whereClause);

    /**
     * Builds delete queries to delete the entity record along with the model store type records
     *
     * @param table         represents the entity table
     * @param groupedFields which contains the collection of {@link FieldInfo} grouped by the {@link StoreType}
     * @param whereClause   which contains the criteria to delete the entity record
     * @param valueHolders  which contains {@link Collection} of value holders
     * @return {@link List} of {@link DeleteQuery} to delete entity model store values
     */
    List<DeleteQuery> buildEntityDeleteQueries(final Table table, final Map<Integer, Collection<FieldInfo>> groupedFields,
                                               final WhereClause whereClause, final Collection<ValueHolder> valueHolders);

}
