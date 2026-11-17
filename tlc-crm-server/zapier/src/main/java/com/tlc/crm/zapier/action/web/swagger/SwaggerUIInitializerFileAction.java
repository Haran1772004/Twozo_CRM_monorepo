package com.tlc.crm.zapier.action.web.swagger;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.res.CrmStreamResponse;

import com.tlc.web.HttpMethod;
import com.tlc.web.WebAction;

import java.io.InputStream;
import java.util.Objects;

/**
 * Serves the Swagger UI initializer file required for bootstrapping the Swagger documentation UI.
 * This action delivers the {@code swagger-initializer.js}.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@WebAction(path = "/swagger-initializer.js", method = {HttpMethod.GET})
public class SwaggerUIInitializerFileAction extends SwaggerConfigAction {

    /**
     * {@inheritDoc}
     *
     * @param request  Represents thee {@link CrmRequest}
     * @param response Represents the {@link CrmResponse}
     */
    @Override
    public void sProcess(final CrmRequest request, final CrmResponse response) {
        final InputStream inputStream = getClass().getResourceAsStream("/resources/swagger/swagger-initializer.js");

        if (Objects.isNull(inputStream)) {
            throw ErrorCode.get(ErrorCodes.INVALID_DATA);
        }
        response.convertIntoAsync();
        final CrmStreamResponse crmStreamResponse = response.convertTo(CrmStreamResponse.class);

        crmStreamResponse.setFileInputStream(inputStream);
        crmStreamResponse.setContentType("application/javascript");
        crmStreamResponse.setInlineOutputFileName("swagger-initializer.js");
        crmStreamResponse.send();
    }
}
