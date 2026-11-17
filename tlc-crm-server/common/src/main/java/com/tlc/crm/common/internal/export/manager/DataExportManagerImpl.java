package com.tlc.crm.common.internal.export.manager;

import com.tlc.crm.common.export.manager.DataExportManager;
import com.tlc.crm.common.export.req.ExportTracker;
import com.tlc.crm.common.export.req.ExportTracker.ExportStatus;
import com.tlc.crm.common.sql.resource.OMEXEXPORT;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;

import java.time.Instant;
import java.util.Optional;

/**
 * <p>
 * Provides the implementation of {@link DataExportManager} related services.
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 * @see DataExportManager
 */
public final class DataExportManagerImpl implements DataExportManager {

    private DataExportManagerImpl() {
    }

    private static class Instance {
        private static final DataExportManagerImpl INSTANCE = new DataExportManagerImpl();
    }

    /**
     * <p>
     * Gets the instance of {@link DataExportManagerImpl}.
     * </p>
     *
     * @return {@link DataExportManagerImpl}
     */
    public static DataExportManagerImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Gets the {@link Table} of export.
     * </p>
     *
     * @return export table
     */
    private Table getTable() {
        return Table.get(OMEXEXPORT.TABLE);
    }

    /**
     * <p>
     * Gets the {@link OrgDataStore} of given orgId.
     * </p>
     *
     * @param orgId Represents an organization id
     * @return {@link OrgDataStore}
     */
    private OrgDataStore getOrgDataStore(final long orgId) {
        return DataStoreProvider.getSecureOrgDataStore(orgId);
    }

    /**
     * {@inheritDoc}
     *
     * @param exportTracker Represents the {@link ExportTracker}
     */
    @Override
    public void create(final ExportTracker exportTracker) {
        final Table exportTable = getTable();
        final DataContainer dataContainer = DataContainer.create();
        final Long orgId = exportTracker.orgId();
        final Row exportRow = new Row(exportTable);
        final long startedTime = Instant.now().toEpochMilli();

        exportRow.setOrgId(orgId);
        exportRow.set(OMEXEXPORT.USER_ID, exportTracker.getUserId());
        exportRow.set(OMEXEXPORT.STATUS, exportTracker.getExportStatus().getId());
        exportRow.set(OMEXEXPORT.CREATED_TIME, startedTime);
        exportRow.set(OMEXEXPORT.UPDATED_TIME, startedTime);
        exportTracker.setId(exportRow.getPKValue());

        dataContainer.addNewRow(exportRow);
        getOrgDataStore(orgId).commitChanges(dataContainer);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param exportId Represents the export id
     * @return An {@code Optional} containing the {@code ExportStatusInfo} if found, or an empty {@code Optional} if no information is available
     */
    @Override
    public Optional<ExportTracker> getDataExport(final long orgId, final Long exportId) {
        final Row row = getExportRow(orgId, exportId);

        return Optional.ofNullable(row).map(exportRow -> {
            final ExportTracker exportTracker = new ExportTracker();

            exportTracker.setId(exportRow.getPKValue());
            exportTracker.setExportStatus(ExportStatus.get(exportRow.get(OMEXEXPORT.STATUS)));
            exportTracker.setOrgId(exportRow.getOrgId());
            exportTracker.setUserId(exportRow.get(OMEXEXPORT.USER_ID));
            exportTracker.setExportFileId(exportRow.get(OMEXEXPORT.FILE_ID));

            return exportTracker;
        });
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId        Represents on organization id
     * @param exportId     Represents the export id
     * @param exportStatus Represents the {@link ExportStatus}
     */
    @Override
    public void updateStatus(final long orgId, final long exportId, final ExportStatus exportStatus) {
        final Table exportTable = getTable();
        final UpdateQuery updateQuery = UpdateQuery.get(exportTable);

        updateQuery.addUpdateColumn(exportTable.getColumn(OMEXEXPORT.STATUS), exportStatus.getId());
        updateQuery.addUpdateColumn(exportTable.getColumn(OMEXEXPORT.UPDATED_TIME), Instant.now().toEpochMilli());
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(exportTable.getPKColumn(), exportId)));

        getOrgDataStore(orgId).update(updateQuery);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Refers the ID of the organization for which the export is associated.
     * @param exportId Refers the ID of the export whose file ID needs to be updated.
     * @param fileId   Refers the new file ID to be associated with the export.
     */
    @Override
    public void updateExportFileId(final long orgId, final long exportId, final long fileId) {
        final Table exportTable = getTable();
        final UpdateQuery updateQuery = UpdateQuery.get(exportTable);

        updateQuery.addUpdateColumn(exportTable.getColumn(OMEXEXPORT.FILE_ID), fileId);
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(exportTable.getPKColumn(), exportId)));

        getOrgDataStore(orgId).update(updateQuery);
    }

    /**
     * <p>
     * Fetches the export row for the given export id
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param exportId Represents an export id
     * @return The export {@link Row}
     */
    private Row getExportRow(final long orgId, final long exportId) {
        final Table exportTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(exportTable);

        selectQuery.addSelectClause(exportTable);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(exportTable.getPKColumn(), exportId)));

        return getOrgDataStore(orgId).get(selectQuery).getRow(exportTable);
    }
}
