package com.tlc.crm.hospital.api.model.converter;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.entity.builder.EntityConverter;
import com.tlc.crm.hospital.api.manager.AppointmentManager;
import com.tlc.crm.hospital.api.model.Appointment;
import com.tlc.l10n.temporal.model.DateTime;

import java.util.Collections;
import java.util.Locale;
import java.util.Objects;

/**
 * Converts appointment request data to and from API JSON.
 *
 * @author haris
 */
public class AppointmentConverter extends EntityConverter<Appointment> {

    private final AppointmentManager appointmentManager;
    private final UserService userService;

    private static class Instance {
        private static final AppointmentConverter INSTANCE = new AppointmentConverter();
    }

    private AppointmentConverter() {
        appointmentManager = AppointmentManager.getInstance();
        userService = UserService.getInstance();
    }

    /**
     * Returns the shared appointment converter instance.
     *
     * @return the appointment converter
     */
    public static AppointmentConverter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Converts appointment JSON into a new or existing appointment model.
     *
     * @param converterConfig the organization and user context for conversion
     * @param jsonObject the appointment payload
     * @return the converted appointment
     */
    @Override
    public Appointment convert(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final Long orgId = converterConfig.orgId();
        final Long id = jsonObject.optNullableValue("id", null);
        final Appointment appointment = Objects.nonNull(id)
                ? appointmentManager.get(orgId, id)
                : new Appointment();
        final CrmUser currentUser = userService.getUser(orgId, converterConfig.ownerId());

        appointment.setOrgId(orgId);
        appointment.setUserIds(Collections.emptyList());
        appointment.setUpdatedBy(currentUser);

        if (Objects.isNull(id)) {
            appointment.setOwnerId(currentUser.id());
            appointment.setCreatedBy(currentUser);
            appointment.setSalesOwner(currentUser);
        } else {
            appointment.setSalesOwner(userService.getUser(orgId, appointment.getOwnerId()));
        }

        if (jsonObject.containsKey("patientId")) {
            appointment.setPatientId(jsonObject.optNullableValue("patientId", appointment.getPatientId()));
        }
        if (jsonObject.containsKey("doctorId")) {
            appointment.setDoctorId(jsonObject.optNullableValue("doctorId", appointment.getDoctorId()));
        }
        if (jsonObject.containsKey("startTime")) {
            appointment.setStartTime(jsonObject.isKeyPresentWithNullValue("startTime")
                    ? null
                    : new DateTime(jsonObject.getLong("startTime")));
        }
        if (jsonObject.containsKey("duration")) {
            appointment.setDuration(jsonObject.isKeyPresentWithNullValue("duration")
                    ? null
                    : jsonObject.getInt("duration"));
        }
        if (jsonObject.containsKey("status")) {
            appointment.setStatus(parseStatus(jsonObject));
        }

        return appointment;
    }

    /**
     * Converts an appointment model into its API JSON representation.
     *
     * @param appointment the appointment to convert
     * @return the appointment payload
     */
    @Override
    public JsonObject convert(final Appointment appointment) {
        final JsonObject jsonObject = Json.object();
        jsonObject.put("id", appointment.id());
        jsonObject.put("orgId", appointment.orgId());
        jsonObject.put("patientId", appointment.getPatientId());
        jsonObject.put("doctorId", appointment.getDoctorId());
        jsonObject.put("startTime", Objects.isNull(appointment.getStartTime())
                ? null
                : appointment.getStartTime().getTimeStamp());
        jsonObject.put("duration", appointment.getDuration());
        jsonObject.put("status", Objects.isNull(appointment.getStatus())
                ? null
                : appointment.getStatus().name());
        return jsonObject;
    }

    private Appointment.Status parseStatus(final JsonObject jsonObject) {
        if (jsonObject.isKeyPresentWithNullValue("status")) {
            return null;
        }

        try {
            return Appointment.Status.valueOf(jsonObject.getString("status").toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException exception) {
            throw ErrorCode.get(CommonErrorCodes.DATA_VALIDATION_FAILED,
                    "i18n.appointment.status.invalid", exception);
        }
    }
}
