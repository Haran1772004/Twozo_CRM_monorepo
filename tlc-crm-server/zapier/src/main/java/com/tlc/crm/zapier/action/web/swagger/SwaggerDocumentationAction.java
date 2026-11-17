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
 * Handles the serving of Swagger API documentation and UI resources.
 * <p>
 * This action provides endpoints to serve the Swagger UI (index.html and related static assets)
 * and the Swagger JSON documentation for the API. It ensures that the documentation is accessible
 * from within the application and can be used to explore and test the available APIs.
 * </p>
 * <p>
 * Example endpoints:
 * - /api/api-doc: Serves the Swagger UI
 * - /api/swagger.json: Serves the Swagger JSON definition
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@WebAction(path = "/api-doc", method = {HttpMethod.GET})
public class SwaggerDocumentationAction extends SwaggerConfigAction {

    /**
     * {@inheritDoc}
     *
     * @param request  Represents thee {@link CrmRequest}
     * @param response Represents the {@link CrmResponse}
     */
    @Override
    public void sProcess(final CrmRequest request, final CrmResponse response) {
        final InputStream inputStream = getClass().getResourceAsStream("/resources/swagger/index.html");

        if (Objects.isNull(inputStream)) {
            throw ErrorCode.get(ErrorCodes.INVALID_DATA);
        }
        final CrmStreamResponse crmStreamResponse = (CrmStreamResponse) response;

        crmStreamResponse.setFileInputStream(inputStream);
        crmStreamResponse.setContentType("text/html");
        crmStreamResponse.setInlineOutputFileName("index.html");
        crmStreamResponse.send();
    }
}
