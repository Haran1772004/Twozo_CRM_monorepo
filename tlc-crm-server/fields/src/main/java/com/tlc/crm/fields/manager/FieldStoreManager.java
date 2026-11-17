package com.tlc.crm.fields.manager;

import com.tlc.crm.fields.models.FieldStore;
import com.tlc.crm.fields.models.FieldType;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.fields.service.FieldTypeService;
import com.tlc.crm.fields.sql.resource.METFIELDSTORE;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * Provides the {@link FieldStore} related DB actions
 * </p>
 *
 * @author Selvakumar
 * @version 1.0
 */
public class FieldStoreManager extends AbstractReadableConfigManager<FieldStore> {

    private static class Instance {
        private static final FieldStoreManager INSTANCE = new FieldStoreManager();
    }

    private FieldStoreManager() {
    }

    public static FieldStoreManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Table getTable() {
        return Table.get(METFIELDSTORE.TABLE);
    }

    /**
     * <p>
     * Fetches all available field stores
     * </p>
     *
     * @return Collection of {@link FieldStore} entities
     */
    public Collection<FieldStore> getAllFieldStore() {
        final Table crmFieldStore = getTable();
        final WhereClause whereClause = new WhereClause(Criteria.notEq(crmFieldStore.getPKColumn(), null));

        return get(whereClause);
    }

    /**
     * <p>
     * Converts the row into {@link FieldStore}
     * </p>
     *
     * @param row A row that holds the attributes information of {@link FieldStore}
     */
    @Override
    public FieldStore convertRowToModel(final Row row) {
        final FieldType fieldType = FieldTypeService.getInstance().get(row.get(METFIELDSTORE.FIELD_TYPE_ID));
        final StoreType storeType = fieldType.storeType();
        final Boolean isPersistent = row.get(METFIELDSTORE.IS_PERSISTENT);

        return new FieldStore(row.getPKValue(), storeType, StoreType.isModelStore(storeType),
                row.get(METFIELDSTORE.COLUMN), row.get(METFIELDSTORE.TABLE_NAME), row.get(METFIELDSTORE.COLUMN_RANGE),
                row.get(METFIELDSTORE.TABLE_RANGE), row.get(METFIELDSTORE.SOURCE_ID),
                Objects.nonNull(isPersistent) && isPersistent);
    }

    /**
     * <p>
     * Gets the collection of field store details for the given source id.
     * </p>
     *
     * @param sourceId Represents the source id
     * @return The Collection of {@link FieldStore} details.
     */
    public Collection<FieldStore> getFieldStoresBySourceId(final Long sourceId) {
        return get(new WhereClause(Criteria.eq(getTable().getColumn(METFIELDSTORE.SOURCE_ID), sourceId)));
    }
}
