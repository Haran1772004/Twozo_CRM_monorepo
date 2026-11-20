package com.tlc.crm.hospital.action;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.CrmConfigAction;
import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.hospital.api.manager.PatientManager;
import com.tlc.crm.hospital.api.model.Patient;
import com.tlc.crm.hospital.api.model.converter.PatientConverter;
import com.tlc.crm.entity.builder.ConverterConfigBuilder;
import com.tlc.web.WebAction;

/**
 * Handles patient CRUD requests at {@code /hospital/patient/mgmt}.
 * 
 * @author Haris
 * @version 1.0
 */
@WebAction(path = "/hospital/patient/mgmt")
public class PatientManagement extends CrmConfigAction<Patient> {

    private final ConverterConfigBuilder converterConfigBuilder; // ConverterConfigBuilder prepares the additional
                                                                 // information, ConverterConfig carries it, and
                                                                 // PatientConverter uses it while converting patient
                                                                 // data.
    private final PatientConverter patientConverter;
    private final PatientManager patientManager;

    /** Creates the patient action using the shared converter and manager instances. */
    public PatientManagement() {
        converterConfigBuilder = ConverterConfigBuilder.getInstance();
        patientConverter = PatientConverter.getInstance();
        patientManager = PatientManager.getInstance();
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
     * Returns the manager used by the base action for patient persistence.
     *
     * @return the patient manager
     */
    @Override
    public ConfigManager<Patient> getConfigManager() {
        return patientManager;
    }

    /**
     * Converts a request payload into a patient model.
     *
     * @param request the incoming CRM request
     * @param jsonObject the patient payload
     * @return the converted patient
     */
    @Override
    public Patient convert(final CrmRequest request, final JsonObject jsonObject) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, false, jsonObject);
        return patientConverter.convert(converterConfig, jsonObject);
    }

    /**
     * Converts a patient model into its API JSON representation.
     *
     * @param patient the patient to convert
     * @return the patient payload
     */
    @Override
    public JsonObject convert(final Patient patient) {
        return patientConverter.convert(patient);
    }

    /**
     * Converts a partial request payload into a patient model.
     *
     * @param request the incoming CRM request
     * @param jsonObject the partial patient payload
     * @return the converted patient
     */
    @Override
    public Patient convertPartialData(final CrmRequest request, final JsonObject jsonObject) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, true, jsonObject);
        return patientConverter.convert(converterConfig, jsonObject);
    }
}
