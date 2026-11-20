package com.tlc.crm.hospital.api.model.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.hospital.api.manager.PatientManager;
import com.tlc.crm.hospital.api.model.Patient;
import com.tlc.crm.entity.builder.EntityConverter;
import com.tlc.l10n.temporal.model.Date;

import java.util.Collections;
import java.util.Objects;

/**
 * Converts patient request data to and from API JSON.
 *
 * @author haris
 */
public class PatientConverter extends EntityConverter<Patient> {

    private final PatientManager patientManager;
    private final UserService userService;

    private static class Instance {
        private static final PatientConverter INSTANCE = new PatientConverter();
    }

    private PatientConverter() {
        patientManager = PatientManager.getInstance();
        userService = UserService.getInstance();
    }

    /**
     * Returns the shared patient converter instance.
     *
     * @return the patient converter
     */
    public static PatientConverter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Converts patient JSON into a new or existing patient model.
     *
     * @param converterConfig the organization and user context for conversion
     * @param jsonObject the patient payload
     * @return the converted patient
     */
    @Override
    public Patient convert(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final Long orgId = converterConfig.orgId();
        final Long id = jsonObject.optNullableValue("id", null);
        final Patient patient = Objects.nonNull(id) ? patientManager.get(orgId, id) : new Patient();
        final CrmUser currentUser = userService.getUser(orgId, converterConfig.ownerId());

        patient.setOrgId(orgId);
        patient.setUserIds(Collections.emptyList());
        patient.setUpdatedBy(currentUser);

        if (Objects.isNull(id)) {
            patient.setOwnerId(currentUser.id());
            patient.setCreatedBy(currentUser);
            patient.setSalesOwner(currentUser);
        } else {
            patient.setSalesOwner(userService.getUser(orgId, patient.getOwnerId()));
        }

        if (jsonObject.containsKey("fullName")) {
            patient.setFullName(jsonObject.optNullableValue("fullName", patient.getFullName()));
        }
        if (jsonObject.containsKey("dateOfBirth")) {
            patient.setDateOfBirth(jsonObject.isKeyPresentWithNullValue("dateOfBirth")
                    ? null
                    : new Date(jsonObject.getLong("dateOfBirth")));
        }
        if (jsonObject.containsKey("phone")) {
            patient.setPhone(jsonObject.optNullableValue("phone", patient.getPhone()));
        }

        return patient;
    }

    /**
     * Converts a patient model into its API JSON representation.
     *
     * @param patient the patient to convert
     * @return the patient payload
     */
    @Override
    public JsonObject convert(final Patient patient) {
        final JsonObject jsonObject = Json.object();
        jsonObject.put("id", patient.id());
        jsonObject.put("orgId", patient.orgId());
        jsonObject.put("fullName", patient.getFullName());
        jsonObject.put("dateOfBirth", Objects.isNull(patient.getDateOfBirth())
                ? null
                : patient.getDateOfBirth().getTimeStamp());
        jsonObject.put("phone", patient.getPhone());
        return jsonObject;
    }
}
