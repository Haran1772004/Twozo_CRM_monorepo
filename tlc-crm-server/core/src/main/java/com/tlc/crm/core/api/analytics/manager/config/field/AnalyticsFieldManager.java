package com.tlc.crm.core.api.analytics.manager.config.field;

import com.tlc.crm.core.api.analytics.entity.config.fields.AnalyticsField;
import com.tlc.crm.core.api.analytics.integ.managers.CRMAbstractReportsManager;
import com.tlc.crm.core.sql.resource.MCANAFIELD;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.ds.ReadableDataStore;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * Provides an abstract way to manage the analytics fields
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 * @see CRMAbstractReportsManager
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
public class AnalyticsFieldManager extends CRMAbstractReportsManager<AnalyticsField>
{

    private static class Instance
    {
        private static final AnalyticsFieldManager INSTANCE = new AnalyticsFieldManager();
    }

    public static AnalyticsFieldManager INSTANCE()
    {
        return Instance.INSTANCE;
    }

    @NonNull
    protected Class<AnalyticsField> getModelClass()
    {
        return AnalyticsField.class;
    }

    @Override
    public Table getTable()
    {
        return Table.get( MCANAFIELD.TABLE );
    }

    /**
     * Fetches all {@link AnalyticsField} records from the database.
     *
     * @return {@link Collection} of {@link AnalyticsField}, never null
     */
    public Collection<AnalyticsField> fetchAll() {
        final Table fieldTable = Table.get(MCANAFIELD.TABLE);
        final DataContainer dataContainer = getReadableDataStore().get(buildBaseQuery(null, null));
        final Collection<AnalyticsField> result = new ArrayList<>();

        if (Objects.nonNull(dataContainer)) {
            dataContainer.getRows(fieldTable).forEach(row -> result.add(buildAnalyticsField(row)));
        }

        LOGGER.debug("CRM Analytics: Fetched {} analytics fields from DB", result.size());
        return result;
    }

    /**
     * Fetches the {@link Collection} of {@link AnalyticsField} from the database for the given module ID.
     *
     * @param moduleId Represents the module identifier
     * @return {@link Collection} of {@link AnalyticsField}, never null
     */
    public Collection<AnalyticsField> fetchByModuleId(final Integer moduleId) {
        final Table fieldTable = Table.get(MCANAFIELD.TABLE);
        final DataContainer dataContainer = getReadableDataStore().get(
                buildBaseQuery(fieldTable.getColumn(MCANAFIELD.MODULE_ID), moduleId));
        final Collection<AnalyticsField> result = new ArrayList<>();

        if (Objects.nonNull(dataContainer)) {
            dataContainer.getRows(fieldTable).forEach(row -> result.add(buildAnalyticsField(row)));
        }

        LOGGER.debug("CRM Analytics: Fetched {} analytics fields from DB for moduleId: {}", result.size(), moduleId);
        return result;
    }

    /**
     * Fetches a single {@link AnalyticsField} from the database for the given field ID.
     *
     * @param fieldId Represents the field identifier
     * @return {@link AnalyticsField} or null if not found
     */
    public AnalyticsField fetchByFieldId(final Long fieldId) {
        final Table fieldTable = Table.get(MCANAFIELD.TABLE);
        final DataContainer dataContainer = getReadableDataStore().get(
                buildBaseQuery(fieldTable.getPKColumn(), fieldId));

        if (Objects.nonNull(dataContainer)) {
            final Row row = dataContainer.getRow(fieldTable);

            if (Objects.nonNull(row)) {
                final String name = row.get(MCANAFIELD.NAME);

                if (Objects.nonNull(name)) {
                    return buildAnalyticsField(row);
                }
            }
        }

        LOGGER.debug("CRM Analytics: No analytics field found in DB for fieldId: {}", fieldId);
        return null;
    }

    /**
     * <p>
     * Builds the base {@link SelectQuery} for the analytics field table.
     * Optionally applies a where clause if filterColumn and filterValue are provided.
     * </p>
     *
     * @param filterColumn The column to filter on, or null for no filter
     * @param filterValue  The value to filter by, or null for no filter
     * @return The constructed {@link SelectQuery}
     */
    private SelectQuery buildBaseQuery(final Column filterColumn, final Object filterValue) {
        final Table table = Table.get(MCANAFIELD.TABLE);
        final SelectQuery selectQuery = SelectQuery.get(table);

        if (Objects.nonNull(filterColumn) && Objects.nonNull(filterValue)) {
            selectQuery.setWhereClause(new WhereClause(Criteria.eq(filterColumn, filterValue)));
        }

        return selectQuery;
    }

    /**
     * <p>
     * Builds an {@link AnalyticsField} instance from the given row data.
     * </p>
     *
     * @param row The database row containing analytics field data
     * @return The constructed {@link AnalyticsField}
     */
    private AnalyticsField buildAnalyticsField(final Row row) {
        return AnalyticsField.builder()
                .id(row.getPKValue())
                .moduleId(row.get(MCANAFIELD.MODULE_ID))
                .name(row.get(MCANAFIELD.NAME))
                .aliasName(row.get(MCANAFIELD.ALIAS_NAME))
                .fieldTypeId(row.get(MCANAFIELD.FIELD_TYPE_ID))
                .build();
    }

    /**
     * Returns the {@link ReadableDataStore} instance for executing read queries.
     *
     * @return {@link ReadableDataStore}
     */
    private ReadableDataStore getReadableDataStore() {
        return DataStoreProvider.getReadableDataStore();
    }
}
