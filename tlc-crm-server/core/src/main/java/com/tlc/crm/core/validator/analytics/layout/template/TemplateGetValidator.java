package com.tlc.crm.core.validator.analytics.layout.template;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.core.api.analytics.general.keys.common.CRMReportKeys;
import com.tlc.crm.core.api.analytics.integ.managers.CRMTemplateManagerAPI;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.CRMTemplateDetails;
import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;
import com.tlc.crm.core.validator.analytics.RequestValidator;

import lombok.NonNull;

import java.util.Objects;

/**
 * <p>
 * Validates GET request for retrieving CRM template details.
 * </p>
 *
 * @author Ryan
 * @version 1.0
 */
public class TemplateGetValidator implements RequestValidator<JsonObject> {
    private final CRMTemplateManagerAPI crmTemplateManagerAPI;

    private TemplateGetValidator(){
        this.crmTemplateManagerAPI = CRMTemplateManagerAPI.INSTANCE();
    }

    private static class Instance {
        private static final TemplateGetValidator INSTANCE = new TemplateGetValidator();
    }

    public static TemplateGetValidator INSTANCE() {
        return TemplateGetValidator.Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId      Represents an organization id
     * @param userId     Represents the user id
     * @param jsonObject Represents the {@link JsonObject} containing the configuration
     */
    @Override
    public void validateRequest(final @NonNull Long orgId,
                                final @NonNull Long userId,
                                final @NonNull JsonObject jsonObject) throws Exception {
        if(Objects.isNull(crmTemplateManagerAPI.getCRMTemplatedManager().get(orgId, jsonObject.getLong(CRMReportKeys.ID)))) {
            throw ErrorCode.get(AnalyticsErrorCodes.TEMPLATE_NOT_FOUND);
        }
    }
}
