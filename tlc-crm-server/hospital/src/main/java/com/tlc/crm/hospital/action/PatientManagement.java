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
 * Handles Patient API requests.
 * 
 * @author Haris
 * @version 1.0
 */
@WebAction(path = "/hospital/patient/mgmt")
public class PatientManagement extends CrmConfigAction<Patient> { // for web related methods access

    private final ConverterConfigBuilder converterConfigBuilder; // ConverterConfigBuilder prepares the additional
                                                                 // information, ConverterConfig carries it, and
                                                                 // PatientConverter uses it while converting patient
                                                                 // data.
    private final PatientConverter patientConverter;
    private final PatientManager patientManager;

    public PatientManagement() {
        converterConfigBuilder = ConverterConfigBuilder.getInstance();
        patientConverter = PatientConverter.getInstance();
        patientManager = PatientManager.getInstance();
    }

    @Override
    public void handle(final CrmRequest request, final CrmResponse response) { // for unsupported type except
                                                                               // create,read,delete,update so this is
                                                                               // abstarct method define in
                                                                               // crmconfigaction
        throw ErrorCode.get(CommonErrorCodes.UNSUPPORTED_ACTION);
    }

    @Override
    public ConfigManager<Patient> getConfigManager() { // This tells the parent class:"When you need to perform database
                                                       // operations for this action, use PatientManager."
        return patientManager;
    }

    @Override
    public Patient convert(final CrmRequest request, final JsonObject jsonObject) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, false, jsonObject);
        return patientConverter.convert(converterConfig, jsonObject);
    }

    @Override
    public JsonObject convert(final Patient patient) {
        return patientConverter.convert(patient);
    }

    @Override
    public Patient convertPartialData(final CrmRequest request, final JsonObject jsonObject) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, true, jsonObject);
        return patientConverter.convert(converterConfig, jsonObject);
    }
}
