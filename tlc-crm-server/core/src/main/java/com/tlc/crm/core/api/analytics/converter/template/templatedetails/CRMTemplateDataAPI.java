package com.tlc.crm.core.api.analytics.converter.template.templatedetails;

import com.tlc.crm.core.api.analytics.converter.template.CRMReportTemplate;

/**
 * Interface providing access to the CRM {@link CRMTemplateAPI}
 *
 * @author Ryan
 * @version 1.0
 */
public interface CRMTemplateDataAPI {
    default CRMReportTemplate templateDataAPI() {
        return CRMTemplateAPI.INSTANCE();
    }
}
