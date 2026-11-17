package com.tlc.crm.core.api.analytics.manager.config.field;

import com.tlc.crm.core.api.analytics.entity.config.modules.CRMSystemModularField;
import com.tlc.crm.core.api.analytics.integ.managers.CRMAbstractReportsManager;
import com.tlc.crm.core.sql.resource.CRMSYSTEMMODULARFIELD;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.ds.ReadableDataStore;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * Manager for system modular-field analytics configuration records.
 * Responsible for managing the configuration and persistence of {@link CRMSystemModularField} entities.
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 * @see CRMAbstractReportsManager
 */
public class SystemModularFieldManager extends CRMAbstractReportsManager<CRMSystemModularField> {

    private static class Instance {
        private static final SystemModularFieldManager INSTANCE = new SystemModularFieldManager();
    }

    /**
     * Returns singleton instance.
     *
     * @return {@link SystemModularFieldManager} singleton
     */
    public static SystemModularFieldManager INSTANCE() {
        return SystemModularFieldManager.Instance.INSTANCE;
    }

    /**
     * Returns supported model class.
     *
     * @return system modular-field model class
     */
    @Override
    protected @NonNull Class<CRMSystemModularField> getModelClass() {
        return CRMSystemModularField.class;
    }

    /**
     * Returns associated table metadata.
     *
     * @return SQL table metadata
     */
    @Override
    public Table getTable() {
        return Table.get(CRMSYSTEMMODULARFIELD.TABLE);
    }

    /**
     * Fetches all system modular field IDs from the database.
     *
     * @return {@link Collection} of field IDs, never null
     */
    public Collection<Long> fetchAllFieldIds() {
        final Table table = Table.get(CRMSYSTEMMODULARFIELD.TABLE);
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(table.getColumn(CRMSYSTEMMODULARFIELD.MODULE_ID));

        final Collection<Long> fieldIds = getReadableDataStore().get(selectQuery).getRows(table)
                .map(Row::getPKValue).toList();

        LOGGER.info("CRM Analytics : Fetched [{}] system modular field IDs from DB", fieldIds.size());
        return fieldIds;
    }

    /**
     * Fetches the {@link Collection} of {@link CRMSystemModularField} from the database for the given module ID.
     *
     * @param moduleId Represents the module identifier
     * @return {@link Collection} of {@link CRMSystemModularField}, never null
     */
    public Collection<CRMSystemModularField> fetchByModuleId(final Integer moduleId) {
        final Table table = Table.get(CRMSYSTEMMODULARFIELD.TABLE);
        final DataContainer dataContainer = getReadableDataStore().get(
                buildBaseQuery(table.getColumn(CRMSYSTEMMODULARFIELD.MODULE_ID), moduleId));
        final Collection<CRMSystemModularField> result = new ArrayList<>();

        if (Objects.nonNull(dataContainer)) {
            dataContainer.getRows(table).forEach(row -> result.add(buildSystemModularField(row)));
        }

        LOGGER.info("CRM Analytics : Fetched [{}] CRMSystemModularField from DB for moduleId [{}]", result.size(), moduleId);
        return result;
    }

    /**
     * Fetches a single {@link CRMSystemModularField} from the database for the given field ID.
     *
     * @param fieldId Represents the field identifier
     * @return {@link CRMSystemModularField} or null if not found
     */
    public CRMSystemModularField fetchByFieldId(final Long fieldId) {
        final Table table = Table.get(CRMSYSTEMMODULARFIELD.TABLE);
        final DataContainer dataContainer = getReadableDataStore().get(
                buildBaseQuery(table.getPKColumn(), fieldId));

        if (Objects.nonNull(dataContainer)) {
            final Row row = dataContainer.getRow(table);

            if (Objects.nonNull(row)) {
                return buildSystemModularField(row);
            }
        }

        LOGGER.info("CRM Analytics : No CRMSystemModularField found in DB for fieldId [{}]", fieldId);
        return null;
    }

    /**
     * Fetches all {@link CRMSystemModularField} from the database.
     *
     * @return {@link Collection} of {@link CRMSystemModularField}, never null
     */
    public Collection<CRMSystemModularField> fetchAll() {
        final Table table = Table.get(CRMSYSTEMMODULARFIELD.TABLE);
        final DataContainer dataContainer = getReadableDataStore().get(buildBaseQuery(null, null));
        final Collection<CRMSystemModularField> result = new ArrayList<>();

        if (Objects.nonNull(dataContainer)) {
            dataContainer.getRows(table).forEach(row -> result.add(buildSystemModularField(row)));
        }

        LOGGER.info("CRM Analytics : Fetched [{}] CRMSystemModularField from DB", result.size());
        return result;
    }

    /**
     * <p>
     * Builds the base {@link SelectQuery} for the system modular field table.
     * Optionally applies a where clause if filterColumn and filterValue are provided.
     * </p>
     *
     * @param filterColumn The column to filter on, or null for no filter
     * @param filterValue  The value to filter by, or null for no filter
     * @return The constructed {@link SelectQuery}
     */
    private SelectQuery buildBaseQuery(final Column filterColumn, final Object filterValue) {
        final Table table = Table.get(CRMSYSTEMMODULARFIELD.TABLE);
        final SelectQuery selectQuery = SelectQuery.get(table);

        if (Objects.nonNull(filterColumn) && Objects.nonNull(filterValue)) {
            selectQuery.setWhereClause(new WhereClause(Criteria.eq(filterColumn, filterValue)));
        }

        return selectQuery;
    }

    /**
     * <p>
     * Builds a {@link CRMSystemModularField} instance from the given row data.
     * </p>
     *
     * @param row The database row containing system modular field data
     * @return The constructed {@link CRMSystemModularField}
     */
    private CRMSystemModularField buildSystemModularField(final Row row) {
        return CRMSystemModularField.builder()
                .id(row.getPKValue())
                .moduleId(row.get(CRMSYSTEMMODULARFIELD.MODULE_ID))
                .fieldName(row.get(CRMSYSTEMMODULARFIELD.FIELD_NAME))
                .crmFieldId(row.get(CRMSYSTEMMODULARFIELD.CRM_FIELD_ID))
                .fieldConfigAsString(row.get(CRMSYSTEMMODULARFIELD.FIELD_CONFIG))
                .anaFieldId(row.get(CRMSYSTEMMODULARFIELD.ANALYTICS_FIELD_ID))
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