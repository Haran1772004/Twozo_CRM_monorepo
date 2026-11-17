package com.tlc.crm.ai.action;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;
import com.tlc.commons.util.StringUtil;
import com.tlc.crm.ai.fields.AIFields;
import com.tlc.crm.ai.status.AIErrorCodes;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.CrmSecureAction;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.web.HttpMethod;
import com.tlc.web.HttpRequest;
import com.tlc.web.HttpUtil;
import com.tlc.web.WebAction;
import com.tlc.web.WebExchange;

import java.net.URL;
import java.util.Map;

/**
 * AIReportGeneratorAction is a secured web action that handles the execution of AI-powered report generation requests.
 *
 * This action:
 * - Receives execution data from the client (CRM request)
 * - Retrieves AI provider configuration (API endpoint, authentication details)
 * - Builds a requester context containing user and organization information
 * - Makes an HTTP POST request to the external AI service
 * - Returns the AI service response to the client
 *
 * The class extends CrmSecureAction to ensure that only authenticated users can access this endpoint.
 * It is exposed as a REST endpoint at path "/crm/ai/execute" supporting both POST and GET HTTP methods.
 *
 * Configuration is loaded from a centralized configuration holder, supporting multiple AI providers.
 * Each provider requires: URL, URI paths, and API key authentication.
 *
 * @see CrmSecureAction for authentication and authorization details
 * @see AIFields for all AI-related configuration field constants
 */
@WebAction(path = "/crm/ai/execute", method = {HttpMethod.POST, HttpMethod.GET})
public class AIReportGeneratorAction extends CrmSecureAction {

    /** Instance of ConfHolder singleton used to retrieve centralized configuration properties */
    private final ConfHolder confHolder;

    /**
     * Constructor initializes the ConfHolder singleton instance.
     * This is used to access centralized configuration properties throughout the action lifecycle.
     */
    public AIReportGeneratorAction() {
        confHolder = ConfHolder.getInstance();
    }

    /**
     * Processes an AI report generation request by:
     * 1. Extracting execution data from the incoming request
     * 2. Loading AI provider configuration (provider type, endpoint, API key)
     * 3. Building requester context with user and organization details
     * 4. Constructing an HTTP request to the external AI service with proper headers and authentication
     * 5. Executing the HTTP request and returning the AI service response
     *
     * @param request The incoming CRM request containing execution data in JSON format
     * @param crmResponse The response object where the AI service result will be placed
     * @throws ErrorCode if any error occurs during processing
     *                  (network error, configuration missing, JSON parsing failure, etc.)
     */
    @Override
    public void sProcess(final CrmRequest request, final CrmResponse crmResponse) {
        final JsonObject executionData = request.getRequestJson();
        final Configurations aiConfigs = confHolder.getProperties(AIFields.AI);
        final String aiProvider = aiConfigs.getConfig(AIFields.PROVIDER);
        final Configurations aiProviderConfigs = aiConfigs.getNestedConfigs(aiProvider);
        final JsonObject requesterJson = buildRequesterJson(request, aiProviderConfigs);
        final Configurations uriConfigs = aiProviderConfigs.getNestedConfigs(AIFields.URI);
        final String baseUrl = aiProviderConfigs.getConfig(AIFields.URL);
        final String uri = uriConfigs.getConfig(AIFields.EXECUTE);
        final String apiKey = aiProviderConfigs.getConfig(AIFields.API_KEY);

        executionData.put(AIFields.REQUESTER, requesterJson);

        try {
            final HttpRequest httpRequest = new HttpRequest(
                    new URL(StringUtil.join(StringUtil.Delimiter.EMPTY_STRING, baseUrl, uri)),
                    Map.of(AIFields.REQUEST_BODY, executionData.toString()),
                    HttpMethod.POST,
                    Map.of(
                            AIFields.AUTHORIZATION, StringUtil.join(StringUtil.Delimiter.EMPTY_SPACE, AIFields.BEARER, apiKey),
                            WebExchange.Header.CONTENT_TYPE.getName(), AIFields.APPLICATION_JSON,
                            WebExchange.Header.ACCEPT.getName(), AIFields.APPLICATION_JSON
                    )
            );

            final JsonObject jsonObject = HttpUtil.makeRequest(httpRequest);

            crmResponse.put(AIFields.DATA, jsonObject);
        } catch (final Exception exception) {
            throw ErrorCode.get(AIErrorCodes.REQUEST_PROCESSING_FAILED, exception);
        }
    }

    /**
     * Builds a JSON object containing requester context information.
     * This context identifies the user and organization making the AI request, used by the AI service
     * for logging, auditing, rate limiting, and personalization purposes.
     *
     * The requester JSON includes:
     * - Plan ID: The billing/subscription plan associated with the AI service usage
     * - Customer ID: The organization ID making the request
     * - Customer Name: The organization name for reference
     * - User ID: The individual user making the request
     * - User Name: The full name of the user for audit trails
     *
     * @param request The CRM request containing authenticated user information
     * @param aiConfigs The AI provider configuration containing the plan ID
     * @return A JsonObject containing all requester context information
     */
    private JsonObject buildRequesterJson(final CrmRequest request, final Configurations aiConfigs) {
        final JsonObject requesterJson = Json.object();
        final String planId = aiConfigs.getConfig(AIFields.PLAN_ID);
        final CrmUser user = request.getUser();

        requesterJson.put(AIFields.PLAN_UNDERSCORE_ID, planId);
        requesterJson.put(AIFields.CUSTOMER_ID, user.orgId().toString());
        requesterJson.put(AIFields.CUSTOMER_NAME, user.getOrganization().getName());
        requesterJson.put(AIFields.USER_ID, user.id().toString());
        requesterJson.put(AIFields.USER_NAME, user.getFullName());

        return requesterJson;
    }
}
