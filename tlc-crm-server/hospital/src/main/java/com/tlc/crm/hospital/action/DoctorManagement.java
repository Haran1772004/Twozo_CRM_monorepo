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
 * Handles doctor CRUD requests at {@code /hospital/doctor/mgmt}.
 *
 * @author haris
 */
@WebAction(path = "/hospital/doctor/mgmt")
public class DoctorManagement extends CrmConfigAction<Doctor> {

    private final ConverterConfigBuilder converterConfigBuilder;
    private final DoctorConverter doctorConverter;
    private final DoctorManager doctorManager;

    /** Creates the doctor action using the shared converter and manager instances. */
    public DoctorManagement() {
        converterConfigBuilder = ConverterConfigBuilder.getInstance();
        doctorConverter = DoctorConverter.getInstance();
        doctorManager = DoctorManager.getInstance();
    }

    /**
     * Rejects custom actions; CRUD operations are handled by {@link CrmConfigAction}.
     *
     * @param request the incoming CRM request
     * @param response the response associated with the request
     */
    @Override
    public void handle(final CrmRequest request, final CrmResponse response) {
        throw ErrorCode.get(CommonErrorCodes.UNSUPPORTED_ACTION);
    }

    /**
     * Returns the manager used by the base action for doctor persistence.
     *
     * @return the doctor manager
     */
    @Override
    public ConfigManager<Doctor> getConfigManager() {
        return doctorManager;
    }

    /**
     * Converts a request payload into a doctor model.
     *
     * @param request the incoming CRM request
     * @param jsonObject the doctor payload
     * @return the converted doctor
     */
    @Override
    public Doctor convert(final CrmRequest request, final JsonObject jsonObject) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, false, jsonObject);
        return doctorConverter.convert(converterConfig, jsonObject);
    }

    /**
     * Converts a doctor model into its API JSON representation.
     *
     * @param doctor the doctor to convert
     * @return the doctor payload
     */
    @Override
    public JsonObject convert(final Doctor doctor) {
        return doctorConverter.convert(doctor);
    }

    /**
     * Converts a partial request payload into a doctor model.
     *
     * @param request the incoming CRM request
     * @param jsonObject the partial doctor payload
     * @return the converted doctor
     */
    @Override
    public Doctor convertPartialData(final CrmRequest request, final JsonObject jsonObject) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, true, jsonObject);
        return doctorConverter.convert(converterConfig, jsonObject);
    }
}
