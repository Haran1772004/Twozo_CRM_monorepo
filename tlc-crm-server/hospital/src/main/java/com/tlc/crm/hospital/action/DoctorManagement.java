package com.tlc.crm.hospital.action;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.CrmConfigAction;
import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.entity.builder.ConverterConfigBuilder;
import com.tlc.crm.hospital.api.manager.DoctorManager;
import com.tlc.crm.hospital.api.model.Doctor;
import com.tlc.crm.hospital.api.model.converter.DoctorConverter;
import com.tlc.web.WebAction;

/**
 * Handles Doctor API requests.
 */
@WebAction(path = "/hospital/doctor/mgmt")
public class DoctorManagement extends CrmConfigAction<Doctor> {

    private final ConverterConfigBuilder converterConfigBuilder;
    private final DoctorConverter doctorConverter;
    private final DoctorManager doctorManager;

    public DoctorManagement() {
        converterConfigBuilder = ConverterConfigBuilder.getInstance();
        doctorConverter = DoctorConverter.getInstance();
        doctorManager = DoctorManager.getInstance();
    }

    @Override
    public void handle(final CrmRequest request, final CrmResponse response) {
        throw ErrorCode.get(CommonErrorCodes.UNSUPPORTED_ACTION);
    }

    @Override
    public ConfigManager<Doctor> getConfigManager() {
        return doctorManager;
    }

    @Override
    public Doctor convert(final CrmRequest request, final JsonObject jsonObject) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, false, jsonObject);
        return doctorConverter.convert(converterConfig, jsonObject);
    }

    @Override
    public JsonObject convert(final Doctor doctor) {
        return doctorConverter.convert(doctor);
    }

    @Override
    public Doctor convertPartialData(final CrmRequest request, final JsonObject jsonObject) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, true, jsonObject);
        return doctorConverter.convert(converterConfig, jsonObject);
    }
}
