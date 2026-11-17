package com.tlc.crm.core.api.analytics.manager.config.field;

import com.tlc.crm.core.api.analytics.entity.config.modules.CRMCustomModularField;
import com.tlc.crm.core.api.analytics.integ.managers.CRMAbstractReportsManager;
import com.tlc.crm.core.sql.resource.CRMCUSTOMMODULARFIELD;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.ds.ReadableDataStore;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * Manager for custom modular-field analytics configuration records.
 * Author: karthick V
 * Version: 1.0
 */
public class CRMCustomModularFieldManager extends CRMAbstractReportsManager<CRMCustomModularField> {

    private static class Instance
    {
        private static final CRMCustomModularFieldManager INSTANCE = new CRMCustomModularFieldManager();
    }

    /**
     * Returns singleton instance.
     *
     * @return {@link CRMCustomModularFieldManager} singleton
     */
    public static CRMCustomModularFieldManager INSTANCE()
    {
        return CRMCustomModularFieldManager.Instance.INSTANCE;
    }

    /**
     * Returns supported model class.
     *
     * @return custom modular-field model class
     */
    @Override
    protected @NonNull Class<CRMCustomModularField> getModelClass()
    {
        return CRMCustomModularField.class;
    }

    /**
     * Returns associated table metadata.
     *
     * @return SQL table metadata
     */
    @Override
    public Table getTable()
    {
        return Table.get(CRMCUSTOMMODULARFIELD.TABLE);
    }

    /**
     * Fetches the {@link Collection} of {@link CRMCustomModularField} from the database for the given module ID.
     *
     * @param moduleId Represents the module identifier
     * @return {@link Collection} of {@link CRMCustomModularField}, never null
     */
    public Collection<CRMCustomModularField> fetchByModuleId(final Integer moduleId) {
        final Table table = Table.get(CRMCUSTOMMODULARFIELD.TABLE);
        final SelectQuery selectQuery = SelectQuery.get(table);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(CRMCUSTOMMODULARFIELD.MODULE_ID), moduleId)));

        final DataContainer dataContainer = getReadableDataStore().get(selectQuery);
        final Collection<CRMCustomModularField> result = new ArrayList<>();

        if (Objects.nonNull(dataContainer)) {
            dataContainer.getRows(table).forEach(row -> result.add(CRMCustomModularField.builder()
                    .id(row.getPKValue())
                    .moduleId(moduleId)
                    .fieldName(row.get(CRMCUSTOMMODULARFIELD.FIELD_NAME))
                    .crmFieldId(row.get(CRMCUSTOMMODULARFIELD.CRM_FIELD_ID))
                    .fieldConfigAsString(row.get(CRMCUSTOMMODULARFIELD.FIELD_CONFIG))
                    .build()));
        }

        LOGGER.info("CRM Analytics : Fetched [{}] CRMCustomModularField from DB for moduleId [{}]", result.size(), moduleId);
        return result;
    }

    /**
     * Fetches a single {@link CRMCustomModularField} from the database for the given field ID.
     *
     * @param fieldId Represents the field identifier
     * @return {@link CRMCustomModularField} or null if not found
     */
    public CRMCustomModularField fetchByFieldId(final Long fieldId) {
        final Table table = Table.get(CRMCUSTOMMODULARFIELD.TABLE);
        final SelectQuery selectQuery = SelectQuery.get(table);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(table.getPKColumn(), fieldId)));

        final DataContainer dataContainer = getReadableDataStore().get(selectQuery);

        if (Objects.nonNull(dataContainer)) {
            final Row row = dataContainer.getRow(table);

            if (Objects.nonNull(row)) {
                return CRMCustomModularField.builder()
                        .id(fieldId)
                        .moduleId(row.get(CRMCUSTOMMODULARFIELD.MODULE_ID))
                        .fieldName(row.get(CRMCUSTOMMODULARFIELD.FIELD_NAME))
                        .crmFieldId(row.get(CRMCUSTOMMODULARFIELD.CRM_FIELD_ID))
                        .fieldConfigAsString(row.get(CRMCUSTOMMODULARFIELD.FIELD_CONFIG))
                        .build();
            }
        }

        LOGGER.info("CRM Analytics : No CRMCustomModularField found in DB for fieldId [{}]", fieldId);
        return null;
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