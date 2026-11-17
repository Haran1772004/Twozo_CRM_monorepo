package com.tlc.crm.hospital.api.manager;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.hospital.api.model.Appointment;
import com.tlc.crm.hospital.sql.resource.MHOAPPOINTMENT;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
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
 * @author hari
 *         Saves and reads appointments and checks their patient, doctor, and
 *         schedule.
 */
public class AppointmentManager extends AbstractConfigManager<Appointment> {

    private static final long MILLIS_PER_MINUTE = 60_000L;

    private final PatientManager patientManager;
    private final DoctorManager doctorManager;

    private static class Instance {
        private static final AppointmentManager INSTANCE = new AppointmentManager();
    }

    private AppointmentManager() {
        patientManager = PatientManager.getInstance();
        doctorManager = DoctorManager.getInstance();
    }

    public static AppointmentManager getInstance() {
        return Instance.INSTANCE;
    }

    @Override
    public Table getTable() {
        return Table.get(MHOAPPOINTMENT.TABLE_NAME);
    }

    @Override
    public Collection<Appointment> get(final Long orgId, final Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }

        final Table table = getTable();
        final DataContainer dataContainer = orgDataStore(orgId).get(table, ids);

        return dataContainer.getRows(table).map(this::convertRowToModel).toList();
    }

    @Override
    public Collection<Appointment> partialGet(final Long orgId, final Collection<Long> ids) {
        return get(orgId, ids);
    }

    @Override
    public Appointment create(final Appointment appointment) {
        validateReferences(appointment);
        ensureNoScheduleConflict(appointment.orgId(), appointment);

        final Table table = getTable();
        final Row row = new Row(table);
        final DateTime now = new DateTime(Instant.now().toEpochMilli());
        final DataContainer dataContainer = DataContainer.create();

        row.setOrgId(appointment.orgId());
        row.set(MHOAPPOINTMENT.OWNER_ID, appointment.getOwnerId());
        row.set(MHOAPPOINTMENT.PATIENT_ID, appointment.getPatientId());
        row.set(MHOAPPOINTMENT.DOCTOR_ID, appointment.getDoctorId());
        row.set(MHOAPPOINTMENT.START_TIME, appointment.getStartTime());
        row.set(MHOAPPOINTMENT.DURATION, appointment.getDuration());
        row.set(MHOAPPOINTMENT.STATUS, appointment.getStatus().name());
        row.set(MHOAPPOINTMENT.CREATED_TIME, now);
        row.set(MHOAPPOINTMENT.MODIFIED_TIME, now);
        row.set(MHOAPPOINTMENT.CREATED_BY, appointment.getCreatedBy().id());
        row.set(MHOAPPOINTMENT.UPDATED_BY, appointment.getUpdatedBy().id());

        dataContainer.addNewRow(row);
        orgDataStore(appointment.orgId()).commitChanges(dataContainer);

        appointment.setId(row.getPKValue());
        appointment.setCreatedTime(now);
        appointment.setModifiedTime(now);
        return appointment;
    }

    @Override
    protected ProcessOutcome update(final Long orgId, final Collection<Appointment> appointments) {
        final ProcessOutcome processOutcome = new ProcessOutcome();
        final List<ErrorDescriptor> failedEntries = new ArrayList<>();
        final Table table = getTable();
        final Collection<Long> ids = appointments.stream()
                .map(Appointment::id)
                .filter(Objects::nonNull)
                .toList();
        final DataContainer existingRecords = orgDataStore(orgId).get(table, ids);

        int index = 0;
        for (final Appointment appointment : appointments) {
            final Row row = Objects.isNull(appointment.id()) ? null : existingRecords.getRow(table, appointment.id());

            if (Objects.isNull(row)) {
                failedEntries.add(updateFailure(index));
            } else {
                try {
                    validateReferences(appointment);
                    ensureNoScheduleConflict(orgId, appointment);

                    final DateTime now = new DateTime(Instant.now().toEpochMilli());
                    final DataContainer changes = DataContainer.create();

                    row.set(MHOAPPOINTMENT.PATIENT_ID, appointment.getPatientId());
                    row.set(MHOAPPOINTMENT.DOCTOR_ID, appointment.getDoctorId());
                    row.set(MHOAPPOINTMENT.START_TIME, appointment.getStartTime());
                    row.set(MHOAPPOINTMENT.DURATION, appointment.getDuration());
                    row.set(MHOAPPOINTMENT.STATUS, appointment.getStatus().name());
                    row.set(MHOAPPOINTMENT.UPDATED_BY, appointment.getUpdatedBy().id());
                    row.set(MHOAPPOINTMENT.MODIFIED_TIME, now);
                    changes.updateRow(row);
                    orgDataStore(orgId).commitChanges(changes);
                    appointment.setModifiedTime(now);
                    processOutcome.addProcessedRecord(appointment.id());
                } catch (final ErrorCode errorCode) {
                    LOGGER.error("Update action: Appointment update failed Org ID : {} ID : {} reason : {}",
                            orgId, appointment.id(), errorCode.getMessage(), errorCode);
                    failedEntries.add(updateFailure(index));
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

    private void validateReferences(final Appointment appointment) {
        final Long orgId = appointment.orgId();

        if (Objects.isNull(appointment.getPatientId())
                || !patientManager.exists(orgId, appointment.getPatientId())) {
            throw ErrorCode.get(CommonErrorCodes.ENTITY_NOT_FOUND, "i18n.patient.not.found");
        }
        if (Objects.isNull(appointment.getDoctorId())
                || !doctorManager.exists(orgId, appointment.getDoctorId())) {
            throw ErrorCode.get(CommonErrorCodes.ENTITY_NOT_FOUND, "i18n.doctor.not.found");
        }
    }

    private void ensureNoScheduleConflict(final Long orgId, final Appointment appointment) {
        if (hasScheduleConflict(orgId, appointment)) {
            throw ErrorCode.get(CommonErrorCodes.DATA_VALIDATION_FAILED,
                    "i18n.appointment.schedule.conflict");
        }
    }

    public boolean hasScheduleConflict(final Long orgId, final Appointment appointment) {
        if (appointment.getStatus() != Appointment.Status.SCHEDULED) {
            return false;
        }

        final long start = appointment.getStartTime().getTimeStamp();
        final long end = calculateEndTime(appointment.getStartTime(), appointment.getDuration());
        final Table table = getTable();
        final WhereClause matchingRecords = new WhereClause(Criteria.eq(
                table.getColumn(MHOAPPOINTMENT.DOCTOR_ID), appointment.getDoctorId()))
                .or(Criteria.eq(table.getColumn(MHOAPPOINTMENT.PATIENT_ID), appointment.getPatientId()));
        final DataContainer dataContainer = orgDataStore(orgId).get(table, matchingRecords);

        return dataContainer.getRows(table).anyMatch(row -> isConflictingRow(row, appointment.id(), start, end));
    }

    private boolean isConflictingRow(final Row row, final Long appointmentId, final long start, final long end) {
        if (Objects.equals(row.get(MHOAPPOINTMENT.ID), appointmentId)
                || !Appointment.Status.SCHEDULED.name().equals(row.get(MHOAPPOINTMENT.STATUS))) {
            return false;
        }

        final DateTime existingStart = row.get(MHOAPPOINTMENT.START_TIME);
        final Integer existingDuration = row.get(MHOAPPOINTMENT.DURATION);
        final long existingStartTime = existingStart.getTimeStamp();
        final long existingEndTime = calculateEndTime(existingStart, existingDuration);

        return intervalsOverlap(start, end, existingStartTime, existingEndTime);
    }

    static boolean intervalsOverlap(final long firstStart, final long firstEnd,
            final long secondStart, final long secondEnd) {
        return firstStart < secondEnd && secondStart < firstEnd;
    }

    private long calculateEndTime(final DateTime startTime, final Integer duration) {
        try {
            return Math.addExact(startTime.getTimeStamp(),
                    Math.multiplyExact(duration.longValue(), MILLIS_PER_MINUTE));
        } catch (final ArithmeticException exception) {
            throw ErrorCode.get(CommonErrorCodes.DATA_VALIDATION_FAILED,
                    "i18n.appointment.time.invalid", exception);
        }
    }

    private ErrorDescriptor updateFailure(final int index) {
        return new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED,
                "i18n.common.update.failed"), ErrorKeys.UPDATE_ACTION_FAILED, String.valueOf(index));
    }

    private Appointment convertRowToModel(final Row row) {
        final Appointment appointment = new Appointment();

        appointment.setId(row.get(MHOAPPOINTMENT.ID));
        appointment.setOrgId(row.get(MHOAPPOINTMENT.ORG_ID));
        appointment.setOwnerId(row.get(MHOAPPOINTMENT.OWNER_ID));
        appointment.setPatientId(row.get(MHOAPPOINTMENT.PATIENT_ID));
        appointment.setDoctorId(row.get(MHOAPPOINTMENT.DOCTOR_ID));
        appointment.setStartTime(row.get(MHOAPPOINTMENT.START_TIME));
        appointment.setDuration(row.get(MHOAPPOINTMENT.DURATION));
        appointment.setStatus(Appointment.Status.valueOf(row.get(MHOAPPOINTMENT.STATUS)));
        appointment.setCreatedTime(row.get(MHOAPPOINTMENT.CREATED_TIME));
        appointment.setModifiedTime(row.get(MHOAPPOINTMENT.MODIFIED_TIME));
        appointment.setUserIds(Collections.emptyList());

        return appointment;
    }
}
