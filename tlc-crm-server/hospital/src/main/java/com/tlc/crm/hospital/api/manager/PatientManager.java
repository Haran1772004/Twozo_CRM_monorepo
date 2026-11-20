package com.tlc.crm.hospital.api.manager;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.hospital.api.model.Patient;
import com.tlc.crm.hospital.sql.resource.MHOPATIENT;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.validator.ErrorKeys;
import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Persists and retrieves patient records.
 *
 * @author haris
 */
public class PatientManager extends AbstractConfigManager<Patient> {

    private static class Instance {
        private static final PatientManager INSTANCE = new PatientManager();
    }

    private PatientManager() {
    }

    /**
     * Returns the shared patient manager instance.
     *
     * @return the patient manager
     */
    public static PatientManager getInstance() {
        return Instance.INSTANCE;
    }

    /** {@inheritDoc} */
    @Override
    public Table getTable() {
        return Table.get(MHOPATIENT.TABLE_NAME);
    }

    /** {@inheritDoc} */
    @Override
    public Collection<Patient> get(final Long orgId, final Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }

        final Table table = getTable();
        final DataContainer dataContainer = orgDataStore(orgId).get(table, ids);

        return dataContainer.getRows(table).map(this::convertRowToModel).toList();
    }

    /** {@inheritDoc} */
    @Override
    public Collection<Patient> partialGet(final Long orgId, final Collection<Long> ids) {
        return get(orgId, ids);
    }

    /** {@inheritDoc} */
    @Override
    public Patient create(final Patient patient) {
        final Table table = getTable();
        final Row row = new Row(table);
        final DateTime now = new DateTime(Instant.now().toEpochMilli());
        final DataContainer dataContainer = DataContainer.create();

        row.setOrgId(patient.orgId());
        row.set(MHOPATIENT.OWNER_ID, patient.getOwnerId());
        row.set(MHOPATIENT.FULL_NAME, patient.getFullName());
        row.set(MHOPATIENT.DATE_OF_BIRTH, patient.getDateOfBirth());
        row.set(MHOPATIENT.PHONE, patient.getPhone());
        row.set(MHOPATIENT.CREATED_TIME, now);
        row.set(MHOPATIENT.MODIFIED_TIME, now);
        row.set(MHOPATIENT.CREATED_BY, patient.getCreatedBy().id());
        row.set(MHOPATIENT.UPDATED_BY, patient.getUpdatedBy().id());

        dataContainer.addNewRow(row);
        orgDataStore(patient.orgId()).commitChanges(dataContainer);

        patient.setId(row.getPKValue());
        patient.setCreatedTime(now);
        patient.setModifiedTime(now);
        return patient;
    }

    /** {@inheritDoc} */
    @Override
    protected ProcessOutcome update(final Long orgId, final Collection<Patient> patients) {
        final ProcessOutcome processOutcome = new ProcessOutcome();
        final List<ErrorDescriptor> failedEntries = new ArrayList<>();
        final Table table = getTable();
        final Collection<Long> ids = patients.stream()
                .map(Patient::id)
                .filter(Objects::nonNull)
                .toList();
        final DataContainer existingRecords = orgDataStore(orgId).get(table, ids);

        int index = 0;
        for (final Patient patient : patients) {
            final Row row = Objects.isNull(patient.id()) ? null : existingRecords.getRow(table, patient.id());

            if (Objects.isNull(row)) {
                failedEntries.add(new ErrorDescriptor(
                        ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED, "i18n.common.update.failed"),
                        ErrorKeys.UPDATE_ACTION_FAILED, String.valueOf(index)));
            } else {
                try {
                    final DateTime now = new DateTime(Instant.now().toEpochMilli());
                    final DataContainer changes = DataContainer.create();

                    row.set(MHOPATIENT.FULL_NAME, patient.getFullName());
                    row.set(MHOPATIENT.DATE_OF_BIRTH, patient.getDateOfBirth());
                    row.set(MHOPATIENT.PHONE, patient.getPhone());
                    row.set(MHOPATIENT.UPDATED_BY, patient.getUpdatedBy().id());
                    row.set(MHOPATIENT.MODIFIED_TIME, now);
                    changes.updateRow(row);
                    orgDataStore(orgId).commitChanges(changes);
                    patient.setModifiedTime(now);
                    processOutcome.addProcessedRecord(patient.id());
                } catch (final ErrorCode errorCode) {
                    LOGGER.error("Update action: Patient update failed Org ID : {} ID : {} reason : {}",
                            orgId, patient.id(), errorCode.getMessage(), errorCode);
                    failedEntries.add(new ErrorDescriptor(
                            ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED, "i18n.common.update.failed"),
                            ErrorKeys.UPDATE_ACTION_FAILED, String.valueOf(index)));
                }
            }
            index++;
        }

        if (!failedEntries.isEmpty()) {
            processOutcome.setErrorSummary(new ErrorSummary(
                    new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED,
                            "i18n.common.update.failed")),
                    failedEntries));
        }

        return processOutcome;
    }

    private Patient convertRowToModel(final Row row) {
        final Patient patient = new Patient();

        patient.setId(row.get(MHOPATIENT.ID));
        patient.setOrgId(row.get(MHOPATIENT.ORG_ID));
        patient.setOwnerId(row.get(MHOPATIENT.OWNER_ID));
        patient.setFullName(row.get(MHOPATIENT.FULL_NAME));
        patient.setDateOfBirth(row.get(MHOPATIENT.DATE_OF_BIRTH));
        patient.setPhone(row.get(MHOPATIENT.PHONE));
        patient.setCreatedTime(row.get(MHOPATIENT.CREATED_TIME));
        patient.setModifiedTime(row.get(MHOPATIENT.MODIFIED_TIME));
        patient.setUserIds(Collections.emptyList());

        return patient;
    }
}
