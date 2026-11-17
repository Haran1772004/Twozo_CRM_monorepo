package com.tlc.crm.hospital.api.manager;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.hospital.api.model.Doctor;
import com.tlc.crm.hospital.sql.resource.MHODOCTOR;
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
 * Saves and reads doctor records.
 */
public class DoctorManager extends AbstractConfigManager<Doctor> {

    private static class Instance {
        private static final DoctorManager INSTANCE = new DoctorManager();
    }

    private DoctorManager() {
    }

    public static DoctorManager getInstance() {
        return Instance.INSTANCE;
    }

    @Override
    public Table getTable() {
        return Table.get(MHODOCTOR.TABLE_NAME);
    }

    @Override
    public Collection<Doctor> get(final Long orgId, final Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }

        final Table table = getTable();
        final DataContainer dataContainer = orgDataStore(orgId).get(table, ids);

        return dataContainer.getRows(table).map(this::convertRowToModel).toList();
    }

    @Override
    public Collection<Doctor> partialGet(final Long orgId, final Collection<Long> ids) {
        return get(orgId, ids);
    }

    @Override
    public Doctor create(final Doctor doctor) {
        final Table table = getTable();
        final Row row = new Row(table);
        final DateTime now = new DateTime(Instant.now().toEpochMilli());
        final DataContainer dataContainer = DataContainer.create();

        row.setOrgId(doctor.orgId());
        row.set(MHODOCTOR.OWNER_ID, doctor.getOwnerId());
        row.set(MHODOCTOR.FULL_NAME, doctor.getFullName());
        row.set(MHODOCTOR.SPECIALTY, doctor.getSpecialty());
        row.set(MHODOCTOR.PHONE, doctor.getPhone());
        row.set(MHODOCTOR.CREATED_TIME, now);
        row.set(MHODOCTOR.MODIFIED_TIME, now);
        row.set(MHODOCTOR.CREATED_BY, doctor.getCreatedBy().id());
        row.set(MHODOCTOR.UPDATED_BY, doctor.getUpdatedBy().id());

        dataContainer.addNewRow(row);
        orgDataStore(doctor.orgId()).commitChanges(dataContainer);

        doctor.setId(row.getPKValue());
        doctor.setCreatedTime(now);
        doctor.setModifiedTime(now);
        return doctor;
    }

    @Override
    protected ProcessOutcome update(final Long orgId, final Collection<Doctor> doctors) {
        final ProcessOutcome processOutcome = new ProcessOutcome();
        final List<ErrorDescriptor> failedEntries = new ArrayList<>();
        final Table table = getTable();
        final Collection<Long> ids = doctors.stream()
                .map(Doctor::id)
                .filter(Objects::nonNull)
                .toList();
        final DataContainer existingRecords = orgDataStore(orgId).get(table, ids);

        int index = 0;
        for (final Doctor doctor : doctors) {
            final Row row = Objects.isNull(doctor.id()) ? null : existingRecords.getRow(table, doctor.id());

            if (Objects.isNull(row)) {
                failedEntries.add(updateFailure(index));
            } else {
                try {
                    final DateTime now = new DateTime(Instant.now().toEpochMilli());
                    final DataContainer changes = DataContainer.create();

                    row.set(MHODOCTOR.FULL_NAME, doctor.getFullName());
                    row.set(MHODOCTOR.SPECIALTY, doctor.getSpecialty());
                    row.set(MHODOCTOR.PHONE, doctor.getPhone());
                    row.set(MHODOCTOR.UPDATED_BY, doctor.getUpdatedBy().id());
                    row.set(MHODOCTOR.MODIFIED_TIME, now);
                    changes.updateRow(row);
                    orgDataStore(orgId).commitChanges(changes);
                    doctor.setModifiedTime(now);
                    processOutcome.addProcessedRecord(doctor.id());
                } catch (final ErrorCode errorCode) {
                    LOGGER.error("Update action: Doctor update failed Org ID : {} ID : {} reason : {}",
                            orgId, doctor.id(), errorCode.getMessage(), errorCode);
                    failedEntries.add(updateFailure(index));
                }
            }
            index++;
        }

        if (!failedEntries.isEmpty()) {
            processOutcome.setErrorSummary(new ErrorSummary(
                    new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED,
                            "i18n.common.update.failed")), failedEntries));
        }

        return processOutcome;
    }

    private ErrorDescriptor updateFailure(final int index) {
        return new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED,
                "i18n.common.update.failed"), ErrorKeys.UPDATE_ACTION_FAILED, String.valueOf(index));
    }

    private Doctor convertRowToModel(final Row row) {
        final Doctor doctor = new Doctor();

        doctor.setId(row.get(MHODOCTOR.ID));
        doctor.setOrgId(row.get(MHODOCTOR.ORG_ID));
        doctor.setOwnerId(row.get(MHODOCTOR.OWNER_ID));
        doctor.setFullName(row.get(MHODOCTOR.FULL_NAME));
        doctor.setSpecialty(row.get(MHODOCTOR.SPECIALTY));
        doctor.setPhone(row.get(MHODOCTOR.PHONE));
        doctor.setCreatedTime(row.get(MHODOCTOR.CREATED_TIME));
        doctor.setModifiedTime(row.get(MHODOCTOR.MODIFIED_TIME));
        doctor.setUserIds(Collections.emptyList());

        return doctor;
    }
}
