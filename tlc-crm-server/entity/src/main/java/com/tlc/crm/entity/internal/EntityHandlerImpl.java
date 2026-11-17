package com.tlc.crm.entity.internal;

import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.entity.handler.EntityHandler;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.entity.models.ValueHolder;
import com.tlc.crm.entity.models.EntityManagerHolder;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.DeleteQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.validator.TlcModel;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>
 * Implements the {@link EntityHandler} and provides multiple way to handle the entity events
 * </p>
 *
 * @author Sundar
 * @version 1.0
 * @see EntityHandler
 */
public class EntityHandlerImpl implements EntityHandler {

    private final EntityManagerHolder entityManagerHolder;

    private static class Instance {
        private static final EntityHandlerImpl INSTANCE = new EntityHandlerImpl();
    }

    private EntityHandlerImpl() {
        entityManagerHolder = EntityManagerHolder.getInstance();
    }

    /**
     * Fetches the {@link EntityHandler}
     *
     * @return {@link EntityHandler}
     */
    public static EntityHandler getInstance() {
        return EntityHandlerImpl.Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param table       represents the table that contains the rows
     * @param whereClause represents the criteria to fetch the rows from the given table
     * @return {@link DeleteQuery}
     */
    @Override
    public DeleteQuery buildEntityDeleteQuery(final Table table, final WhereClause whereClause) {
        final DeleteQuery deleteQuery = DeleteQuery.get(table);

        deleteQuery.setWhereClause(whereClause);

        return deleteQuery;
    }

    /**
     * Builds delete queries to delete the entity record along with the model store type records
     *
     * @param table         represents the entity table
     * @param groupedFields which contains the collection of {@link FieldInfo} grouped by the {@link StoreType}
     * @param whereClause   which contains the {@link Criteria} to delete the entity record
     * @param valueHolders  which contains {@link Collection} of value holders
     * @return {@link List} of {@link DeleteQuery} to delete entity model store values
     */
    @Override
    public List<DeleteQuery> buildEntityDeleteQueries(final Table table,
                                                      final Map<Integer, Collection<FieldInfo>> groupedFields,
                                                      final WhereClause whereClause,
                                                      final Collection<ValueHolder> valueHolders) {
        final List<DeleteQuery> deleteQueries = new ArrayList<>();

        deleteQueries.add(buildEntityDeleteQuery(table, whereClause));

        groupedFields.forEach((type, fields) -> {
            final StoreType storeType = StoreType.valueOf(type);

            if (StoreType.DROPDOWN != storeType && StoreType.isModelStore(storeType) && !storeType.isEntityType()
                    && !fields.isEmpty()) {
                final Table relatedTable = getRelatedTable(storeType);

                if (Objects.nonNull(relatedTable)) {
                    final Set<Long> fieldIds = fields.stream().map(FieldInfo::id).collect(Collectors.toSet());
                    final Set<Long> relatedIds = new HashSet<>();

                    for (final Long fieldId : fieldIds) {
                        relatedIds.addAll(valueHolders.stream().map(valueHolder -> valueHolder.get(fieldId))
                                .map(value -> value instanceof final TlcModel model ? model.id() : null)
                                .filter(Objects::nonNull).collect(Collectors.toSet()));
                    }

                    if (!relatedIds.isEmpty()) {
                        deleteQueries.add(buildEntityDeleteQuery(relatedTable,
                                new WhereClause(Criteria.in(relatedTable.getPKColumn(), relatedIds))));
                    }
                }
            }
        });
        return deleteQueries;
    }

    /**
     * Gets the table regarding {@link StoreType}
     *
     * @param storeType Represents the store type to fetch the respective config manager
     * @return {@link Table}
     */
    private Table getRelatedTable(final StoreType storeType) {
        return !storeType.isEntityType() && entityManagerHolder.getManager(storeType) instanceof
                final AbstractConfigManager<? extends TlcModel> abstractConfigManager
                ? abstractConfigManager.getTable() : null;

    }
}