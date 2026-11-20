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
 * Handles appointment CRUD requests at {@code /hospital/appointment/mgmt}.
 *
 * @author haris
 */
@WebAction(path = "/hospital/appointment/mgmt")
public class AppointmentManagement extends CrmConfigAction<Appointment> {

    private final ConverterConfigBuilder converterConfigBuilder; // ConverterConfigBuilder prepares the additional
                                                                 // information, ConverterConfig carries it, and
                                                                 // AppointmentConverter uses it while converting
                                                                 // appointment data.
    private final AppointmentConverter appointmentConverter;
    private final AppointmentManager appointmentManager;

    /** Creates the appointment action using the shared converter and manager instances. */
    public AppointmentManagement() {
        converterConfigBuilder = ConverterConfigBuilder.getInstance();
        appointmentConverter = AppointmentConverter.getInstance();
        appointmentManager = AppointmentManager.getInstance();
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
     * Returns the manager used by the base action for appointment persistence.
     *
     * @return the appointment manager
     */
    @Override
    public ConfigManager<Appointment> getConfigManager() {
        return appointmentManager;
    }

    /**
     * Converts a request payload into an appointment model.
     *
     * @param request the incoming CRM request
     * @param jsonObject the appointment payload
     * @return the converted appointment
     */
    @Override
    public Appointment convert(final CrmRequest request, final JsonObject jsonObject) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, false, jsonObject);
        return appointmentConverter.convert(converterConfig, jsonObject);
    }

    /**
     * Converts an appointment model into its API JSON representation.
     *
     * @param appointment the appointment to convert
     * @return the appointment payload
     */
    @Override
    public JsonObject convert(final Appointment appointment) {
        return appointmentConverter.convert(appointment);
    }

    /**
     * Converts a partial request payload into an appointment model.
     *
     * @param request the incoming CRM request
     * @param jsonObject the partial appointment payload
     * @return the converted appointment
     */
    @Override
    public Appointment convertPartialData(final CrmRequest request, final JsonObject jsonObject) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, true, jsonObject);
        return appointmentConverter.convert(converterConfig, jsonObject);
    }
}
