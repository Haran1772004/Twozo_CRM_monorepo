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
import com.tlc.crm.hospital.api.manager.AppointmentManager;
import com.tlc.crm.hospital.api.model.Appointment;
import com.tlc.crm.hospital.api.model.converter.AppointmentConverter;
import com.tlc.web.WebAction;

/**
 * Handles Appointment API requests.
 */
@WebAction(path = "/hospital/appointment/mgmt")
public class AppointmentManagement extends CrmConfigAction<Appointment> {

    private final ConverterConfigBuilder converterConfigBuilder; // ConverterConfigBuilder prepares the additional
                                                                 // information, ConverterConfig carries it, and
                                                                 // AppointmentConverter uses it while converting
                                                                 // appointment data.
    private final AppointmentConverter appointmentConverter;
    private final AppointmentManager appointmentManager;

    public AppointmentManagement() {
        converterConfigBuilder = ConverterConfigBuilder.getInstance();
        appointmentConverter = AppointmentConverter.getInstance();
        appointmentManager = AppointmentManager.getInstance();
    }

    @Override
    public void handle(final CrmRequest request, final CrmResponse response) {
        throw ErrorCode.get(CommonErrorCodes.UNSUPPORTED_ACTION);
    }

    @Override
    public ConfigManager<Appointment> getConfigManager() {
        return appointmentManager;
    }

    @Override
    public Appointment convert(final CrmRequest request, final JsonObject jsonObject) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, false, jsonObject);
        return appointmentConverter.convert(converterConfig, jsonObject);
    }

    @Override
    public JsonObject convert(final Appointment appointment) {
        return appointmentConverter.convert(appointment);
    }

    @Override
    public Appointment convertPartialData(final CrmRequest request, final JsonObject jsonObject) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, true, jsonObject);
        return appointmentConverter.convert(converterConfig, jsonObject);
    }
}
