package com.tlc.crm.ai.fields;

/**
 * AIFields is a constants utility class that centralizes all field names and configuration keys
 * used throughout the AI module of the CRM system.
 * <p>
 * This class serves as a single source of truth for:
 * - Configuration property names (AI provider settings, URLs, API keys)
 * - JSON field names used in request/response payloads
 * - HTTP header names and values
 * - JSON object field names for requester context and data structures
 * <p>
 * By centralizing these constants, the class enables:
 * - Consistent naming across the AI module and related components
 * - Easy refactoring of field names in a single location
 * - Prevention of typos in hardcoded strings throughout the codebase
 * - Clear documentation of all configuration and data structure keys
 * <p>
 * The class is final and contains only static constants, making it a pure utility class
 * that should never be instantiated.
 *
 * @author Sathish
 * @version 1.0
 * @see com.tlc.crm.ai.action.AIReportGeneratorAction for usage examples
 */
public final class AIFields {

    public static final String AI = "ai";
    public static final String PROVIDER = "provider";
    public static final String PLAN_ID = "planId";
    public static final String PLAN_UNDERSCORE_ID = "plan_id";
    public static final String CUSTOMER_ID = "customer_id";
    public static final String CUSTOMER_NAME = "customer_name";
    public static final String USER_ID = "user_id";
    public static final String USER_NAME = "user_name";
    public static final String REQUESTER = "requester";
    public static final String URL = "url";
    public static final String API_KEY = "apiKey";
    public static final String URI = "uri";
    public static final String EXECUTE = "execute";
    public static final String REQUEST_BODY = "requestBody";
    public static final String AUTHORIZATION = "Authorization";
    public static final String BEARER = "Bearer";
    public static final String APPLICATION_JSON = "application/json";
    public static final String DATA = "data";
}
