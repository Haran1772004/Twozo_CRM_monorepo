package com.tlc.crm.fields.manager;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.fields.internal.status.FieldErrorCodes;
import com.tlc.crm.fields.models.settings.SourceSettings;
import com.tlc.crm.fields.sql.resource.OMETSOURCESETTINGS;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;

import com.tlc.validator.ErrorKeys;
import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * Implements the {@link SourceSettings} related DB actions
 * </p>
 *
 * @author PandiarajkumarG
 */
public class SourceSettingsManager extends AbstractReadableConfigManager<SourceSettings> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SourceSettingsManager.class);

    private static class Instance {
        private static final SourceSettingsManager INSTANCE = new SourceSettingsManager();
    }

    private SourceSettingsManager() {
    }

    public static SourceSettingsManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Gets the table of {@link SourceSettings}.
     *
     * @return The source settings table.
     */
    public Table getTable() {
        return Table.get(OMETSOURCESETTINGS.TABLE);
    }

    /**
     * <p>
     * Loads the rename details into the target container
     * </p>
     *
     * @param sourceSettings  An entity holds the settings rename attributes
     * @param sourceContainer Holds the source information
     * @param targetContainer Holds the updated information of user defined and system field values
     */
    private void loadSettingsChangesIntoContainer(final SourceSettings sourceSettings, final DataContainer sourceContainer,
                                                  final DataContainer targetContainer) {
        final Table settingsTable = getTable();
        final Row existingRow = sourceContainer.getRow(settingsTable);
        final Row row = Objects.isNull(existingRow) ? new Row(settingsTable) : existingRow;
        final String alternateName = sourceSettings.getAlternateName();

        if (Objects.nonNull(existingRow) && Objects.isNull(alternateName)) {
            sourceSettings.setId(row.getPKValue());
            row.set(OMETSOURCESETTINGS.ALTERNATE_NAME, sourceSettings.getAlternateName());

            targetContainer.updateRow(existingRow);
        } else if (!Objects.equals(row.get(OMETSOURCESETTINGS.ALTERNATE_NAME), alternateName)) {
            row.set(OMETSOURCESETTINGS.ALTERNATE_NAME, alternateName);
            row.set(OMETSOURCESETTINGS.SOURCE_ID, sourceSettings.getSourceId());
            row.set(OMETSOURCESETTINGS.IS_ENABLED, sourceSettings.isEnabled());
            sourceSettings.setId(row.getPKValue());

            if (row.isNewRow()) {
                row.setOrgId(sourceSettings.orgId());

                targetContainer.addNewRow(row);
            } else {
                targetContainer.updateRow(row);
            }
        }
    }

    /**
     * <p>
     * Updates the given fields
     * </p>
     *
     * @param orgId  Organisation id
     * @param models module rename object
     * @return Updated status
     */
    public ProcessOutcome update(final Long orgId, final Collection<SourceSettings> models) {
        final OrgDataStore orgDataStore = DataStoreProvider.getSecureOrgDataStore(orgId);
        final Table settingsTable = getTable();
        final ProcessOutcome processOutcome = new ProcessOutcome();
        final List<ErrorDescriptor> failedEntities = new ArrayList<>();

        models.forEach(rename -> {
            try {
                final DataContainer targetContainer = DataContainer.create();
                final WhereClause whereClause = new WhereClause(Criteria.eq(settingsTable.getColumn(OMETSOURCESETTINGS.SOURCE_ID),
                        rename.getSourceId()));
                final DataContainer entityContainer = orgDataStore.get(settingsTable, whereClause);

                loadSettingsChangesIntoContainer(rename, entityContainer, targetContainer);
                orgDataStore.commitChanges(targetContainer);
                final Long settingsId = rename.id();

                if (Objects.nonNull(settingsId)) {
                    processOutcome.addProcessedRecord(settingsId);
                }
            } catch (ErrorCode errorCode) {
                LOGGER.error("Update action: Source settings update failed Org ID : {} ID : {} reason : {}", orgId, rename.id(), errorCode.getMessage(), errorCode);
                failedEntities.add(new ErrorDescriptor(ErrorCode.get(FieldErrorCodes.UPDATE_ACTION_FAILED,
                        "i18n.core.update.failed"), ErrorKeys.UPDATE_ACTION_FAILED, rename.id().toString()));
            }
        });


        if (!failedEntities.isEmpty()) {
            processOutcome.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(FieldErrorCodes.UPDATE_ACTION_FAILED,
                    "i18n.common.update.failed")), failedEntities));
        }

        return processOutcome;
    }

    /**
     * <p>
     * Converts row into {@link SourceSettings}
     * </p>
     *
     * @param row Table row
     * @return {@link SourceSettings}
     */
    public SourceSettings convertRowToModel(final Row row) {
        final SourceSettings sourceSettings = new SourceSettings();

        sourceSettings.setId(row.getPKValue());
        sourceSettings.setAlternateName(row.get(OMETSOURCESETTINGS.ALTERNATE_NAME));
        sourceSettings.setSourceId(row.get(OMETSOURCESETTINGS.SOURCE_ID));
        sourceSettings.setOrgId(row.getOrgId());
        sourceSettings.setEnabled(row.get(OMETSOURCESETTINGS.IS_ENABLED));
        sourceSettings.setConfig(row.get(OMETSOURCESETTINGS.CONFIG));

        return sourceSettings;
    }

    /**
     * Updates the configuration of source.
     *
     * @param orgId          Refers the organization id.
     * @param sourceSettings Refers the {@link SourceSettings} to be updated.
     * @return true if successfully updated, otherwise false.
     */
    public boolean updateConfig(final Long orgId, final SourceSettings sourceSettings) {
        final Table table = getTable();
        final OrgDataStore orgDataStore = orgDataStore(orgId);
        final DataContainer targetContainer = DataContainer.create();
        final DataContainer sourceContainer = orgDataStore.get(table,
                new WhereClause(Criteria.eq(table.getColumn(OMETSOURCESETTINGS.SOURCE_ID), sourceSettings.getSourceId())));
        final Row existingRow = sourceContainer.getRow(table);
        final Row row = Objects.isNull(existingRow) ? new Row(table) : existingRow;

        if (row.isNewRow()) {
            row.setOrgId(sourceSettings.orgId());
        }

        row.set(OMETSOURCESETTINGS.SOURCE_ID, sourceSettings.getSourceId());
        row.set(OMETSOURCESETTINGS.IS_ENABLED, sourceSettings.isEnabled());
        row.set(OMETSOURCESETTINGS.CONFIG, sourceSettings.getConfig());

        targetContainer.updateRow(row);

        try {
            orgDataStore.commitChanges(targetContainer);
            return true;
        } catch (ErrorCode errorCode) {
            return false;
        }
    }

    /**
     * Gets all the source settings details.
     *
     * @return The collection of {@link SourceSettings}.
     */
    public Collection<SourceSettings> getAllSourceSettings() {
        final Table settingsTable = getTable();
        final DataContainer dataContainer = readableDataStore().get(settingsTable,
                new WhereClause(Criteria.eq(settingsTable.getColumn(OMETSOURCESETTINGS.ORG_ID), null)));

        return dataContainer.getRows(settingsTable).filter(Objects::nonNull).map(this::convertRowToModel).toList();
    }

    /**
     * <p>
     * Fetches default source settings for the given source id
     * </p>
     *
     * @param sourceId Represents the source id.
     * @return The {@link SourceSettings}
     */
    public SourceSettings getDefaultSourceSettings(final Long sourceId) {
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(getTable().getColumn(OMETSOURCESETTINGS.SOURCE_ID), sourceId)).
                and(Criteria.eq(getTable().getColumn(OMETSOURCESETTINGS.ORG_ID), null)));

        return convertRowToModel(readableDataStore().get(selectQuery).getRow(table));
    }

    /**
     * <p>
     * Fetches a collection of org source settings for the given organization id
     * </p>
     *
     * @param orgId Represents an organization id
     * @return The collection of {@link SourceSettings}
     */
    public Collection<SourceSettings> getOrgSourceSettings(final Long orgId) {
        return get(new WhereClause(Criteria.eq(getTable().getColumn(OMETSOURCESETTINGS.ORG_ID), orgId)));
    }
}