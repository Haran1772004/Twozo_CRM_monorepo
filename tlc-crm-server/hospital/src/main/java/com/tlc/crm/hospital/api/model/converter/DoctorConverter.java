package com.tlc.crm.hospital.api.model.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.entity.builder.EntityConverter;
import com.tlc.crm.hospital.api.manager.DoctorManager;
import com.tlc.crm.hospital.api.model.Doctor;

import java.util.Collections;
import java.util.Objects;

/**
 * Converts Doctor request data to and from API JSON.
 */
public class DoctorConverter extends EntityConverter<Doctor> {

    private final DoctorManager doctorManager;
    private final UserService userService;

    private static class Instance {
        private static final DoctorConverter INSTANCE = new DoctorConverter();
    }

    private DoctorConverter() {
        doctorManager = DoctorManager.getInstance();
        userService = UserService.getInstance();
    }

    public static DoctorConverter getInstance() {
        return Instance.INSTANCE;
    }

    @Override
    public Doctor convert(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final Long orgId = converterConfig.orgId();
        final Long id = jsonObject.optNullableValue("id", null);
        final Doctor doctor = Objects.nonNull(id) ? doctorManager.get(orgId, id) : new Doctor();
        final CrmUser currentUser = userService.getUser(orgId, converterConfig.ownerId());

        doctor.setOrgId(orgId);
        doctor.setUserIds(Collections.emptyList());
        doctor.setUpdatedBy(currentUser);

        if (Objects.isNull(id)) {
            doctor.setOwnerId(currentUser.id());
            doctor.setCreatedBy(currentUser);
            doctor.setSalesOwner(currentUser);
        } else {
            doctor.setSalesOwner(userService.getUser(orgId, doctor.getOwnerId()));
        }

        if (jsonObject.containsKey("fullName")) {
            doctor.setFullName(jsonObject.optNullableValue("fullName", doctor.getFullName()));
        }
        if (jsonObject.containsKey("specialty")) {
            doctor.setSpecialty(jsonObject.optNullableValue("specialty", doctor.getSpecialty()));
        }
        if (jsonObject.containsKey("phone")) {
            doctor.setPhone(jsonObject.optNullableValue("phone", doctor.getPhone()));
        }

        return doctor;
    }

    @Override
    public JsonObject convert(final Doctor doctor) {
        final JsonObject jsonObject = Json.object();
        jsonObject.put("id", doctor.id());
        jsonObject.put("orgId", doctor.orgId());
        jsonObject.put("fullName", doctor.getFullName());
        jsonObject.put("specialty", doctor.getSpecialty());
        jsonObject.put("phone", doctor.getPhone());
        return jsonObject;
    }
}
