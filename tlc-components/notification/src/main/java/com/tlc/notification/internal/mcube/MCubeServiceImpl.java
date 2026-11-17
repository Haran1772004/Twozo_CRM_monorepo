package com.tlc.notification.internal.mcube;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.Configurations;
import com.tlc.commons.util.StringUtil;
import com.tlc.notification.internal.fields.TelephoneFields;
import com.tlc.notification.internal.status.NotificationErrorCodes;
import com.tlc.notification.model.telephone.TelephoneResponse;
import com.tlc.notification.model.telephone.mcube.MCubeAuthData;
import com.tlc.notification.model.telephone.mcube.MCubeUser;
import com.tlc.notification.service.telephony.mcube.MCubeService;
import com.tlc.web.HttpConnectionHelper;
import com.tlc.web.HttpMethod;
import com.tlc.web.HttpRequest;
import com.tlc.web.HttpUtil;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Implementation of the {@link MCubeService} for the MCube telephony provider.
 *
 * @author Sathish
 * @version 1.0
 */
public class MCubeServiceImpl implements MCubeService {

    private String apiBaseUrl;
    private String configBaseUrl;
    private String webhookUrl;
    private String recordUrl;

    private MCubeServiceImpl() {
    }

    /**
     * Singleton holder pattern for lazy initialization.
     */
    private static class InstanceHolder {
        private static final MCubeServiceImpl INSTANCE = new MCubeServiceImpl();
    }

    public static MCubeServiceImpl getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * Initializes the MCube service with configuration properties.
     *
     * @param configurations application configurations containing MCube API URLs
     */
    @Override
    public void start(final Configurations configurations) {
        final Configurations mCubeConfig = configurations.getNestedConfigs("mcube");

        if (mCubeConfig.getFirstKey().isPresent()) {
            final Configurations urlConfigs = mCubeConfig.getNestedConfigs("url");

            this.apiBaseUrl = urlConfigs.getConfig("common");
            this.configBaseUrl = urlConfigs.getConfig("config");
            this.webhookUrl = urlConfigs.getConfig("webhook");
            this.recordUrl = urlConfigs.getConfig("recordPath");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
    }

    /**
     * {@inheritDoc}
     * Validates the API key by returning a success response.
     * MCube authentication is verified implicitly through API calls.
     */
    @Override
    public TelephoneResponse authenticate(final MCubeAuthData mCubeAuthData) {
        final TelephoneResponse telephoneResponse = new TelephoneResponse();

        telephoneResponse.setStatus(TelephoneResponse.Status.SUCCESS);
        return telephoneResponse;
    }

    /**
     * {@inheritDoc}
     * Sends agent creation request to MCube's add-emp-new endpoint.
     * <p>
     * API: POST config.mcube.com/Restmcube-api/add-emp-new
     * <br>
     * Required fields: Authorization, username, agentname, email, phonenumber, role,
     * inbound, outbound, autodialer, requestedby
     * <br>
     * Success response: {@code {"status": "succ", "id": "12345", "msg": "Agent Added Successfully"}}
     */
    @Override
    public TelephoneResponse createUser(final MCubeUser mCubeUser) {
        final MCubeAuthData authData = mCubeUser.getMCubeAuthData();
        final JsonObject data = Json.object()
                .put(MCubeFields.AUTHORIZATION, authData.getAuthToken())
                .put(MCubeFields.USERNAME, mCubeUser.getEmail())
                .put(MCubeFields.AGENT_NAME, StringUtil.join(StringUtil.Delimiter.EMPTY_SPACE, mCubeUser.getFirstName(), mCubeUser.getLastName()))
                .put(MCubeFields.EMAIL, mCubeUser.getEmail())
                .put(MCubeFields.PHONE_NUMBER, mCubeUser.getPhoneNumber())
                .put(MCubeFields.ROLE, mCubeUser.getRoleId())
                .put(MCubeFields.INBOUND, MCubeFields.TRUE)
                .put(MCubeFields.OUTBOUND, MCubeFields.TRUE)
                .put(MCubeFields.AUTO_DIALER, MCubeFields.FALSE)
                .put(MCubeFields.REQUESTED_BY, mCubeUser.getEmail());

        try {
            final JsonObject response = processRequest(new HttpRequest(
                    buildURL(configBaseUrl, MCubeFields.REST_M_CUBE_API, MCubeFields.ADD_EMP_NEW),
                    getRequestBody(data), HttpMethod.POST, buildHeaders()));
            final TelephoneResponse telephoneResponse = buildResponse(response);

            if (telephoneResponse.getStatus().isSuccessful() && response.containsKey(MCubeFields.ID)) {
                mCubeUser.setId(response.getString(MCubeFields.ID));
            }
            return telephoneResponse;
        } catch (final Exception exception) {
            return buildErrorResponse(exception);
        }
    }

    /**
     * {@inheritDoc}
     * Sends agent update request to MCube's edit-emp endpoint.
     * <p>
     * API: POST config.mcube.com/Restmcube-api/edit-emp
     * <br>
     * Required fields: Authorization, agentname, email, phonenumber, newphonenumber, role, requestedby
     * <br>
     * Success response: {@code {"status": "succ", "msg": "Agent updated Successfully"}}
     */
    @Override
    public TelephoneResponse updateUser(final MCubeUser mCubeUser) {
        final MCubeAuthData authData = mCubeUser.getMCubeAuthData();
        final JsonObject data = Json.object()
                .put(MCubeFields.AUTHORIZATION, authData.getAuthToken())
                .put(MCubeFields.AGENT_NAME, mCubeUser.getFirstName() + " " + mCubeUser.getLastName())
                .put(MCubeFields.EMAIL, mCubeUser.getEmail())
                .put(MCubeFields.PHONE_NUMBER, mCubeUser.getPreviousPhoneNumber())
                .put(MCubeFields.NEW_PHONE_NUMBER, mCubeUser.getPhoneNumber())
                .put(MCubeFields.ROLE, mCubeUser.getRoleId())
                .put(MCubeFields.REQUESTED_BY, mCubeUser.getEmail());

        try {
            final JsonObject response = processRequest(new HttpRequest(
                    buildURL(configBaseUrl, MCubeFields.REST_M_CUBE_API, MCubeFields.EDIT_EMP),
                    getRequestBody(data), HttpMethod.POST, buildHeaders()));

            return buildResponse(response);
        } catch (final Exception e) {
            return buildErrorResponse(e);
        }
    }

    /**
     * {@inheritDoc}
     * Sends agent removal request to MCube's remove-agent endpoint.
     * <p>
     * API: POST config.mcube.com/mcube-api/remove-agent
     * <br>
     * Required fields: Authorization, email
     * <br>
     * Success response: {@code {"status": "success", "message": "Agent deleted successfully"}}
     */
    @Override
    public TelephoneResponse removeUser(final MCubeUser mCubeUser) {
        final MCubeAuthData authData = mCubeUser.getMCubeAuthData();
        final JsonObject data = Json.object()
                .put(MCubeFields.AUTHORIZATION, authData.getAuthToken())
                .put(MCubeFields.EMAIL, mCubeUser.getEmail());

        try {
            final JsonObject response = processRequest(new HttpRequest(
                    buildURL(configBaseUrl, MCubeFields.M_CUBE_API, MCubeFields.REMOVE_AGENT),
                    getRequestBody(data), HttpMethod.POST, buildHeaders()));

            return buildResponse(response);
        } catch (final Exception e) {
            return buildErrorResponse(e);
        }
    }

    /**
     * {@inheritDoc}
     * Initiates an outbound click-to-call via MCube REST API.
     * <p>
     * API: POST api.mcube.com/Restmcube-api/outbound-calls
     * <br>
     * Required fields: HTTP_AUTHORIZATION, exenumber, custnumber, refurl
     * <br>
     * Authentication is via Bearer token in request body as HTTP_AUTHORIZATION field.
     */
    @Override
    public TelephoneResponse initiateCall(final MCubeAuthData mCubeAuthData, final String agentNumber, final String customerNumber) {
        final JsonObject data = Json.object()
                .put(MCubeFields.HTTP_AUTHORIZATION, mCubeAuthData.getAuthToken())
                .put(MCubeFields.EXE_NUMBER, agentNumber)
                .put(MCubeFields.CUST_NUMBER, customerNumber)
                .put(MCubeFields.REF_URL, HttpUtil.buildAbsoluteUrl(webhookUrl));

        try {
            final JsonObject response = processRequest(new HttpRequest(
                    buildURL(apiBaseUrl, MCubeFields.REST_M_CUBE_API, MCubeFields.OUTBOUND_CALLS),
                    getRequestBody(data), HttpMethod.POST, buildHeaders()));

            return buildResponse(response);
        } catch (final Exception e) {
            return buildErrorResponse(e);
        }
    }

    /**
     * Gets the configured webhook URL for MCube callbacks.
     *
     * <p>This URL is used by MCube to send event notifications such as call status updates,
     * call recordings, and other telephony events back to the CRM system.</p>
     *
     * @return the webhook {@link URL} configured for MCube event callbacks
     */
    @Override
    public URL getWebhookURL() {
        try {
            return new URL(HttpUtil.buildAbsoluteUrl(webhookUrl));
        } catch (final MalformedURLException e) {
            throw ErrorCode.get(NotificationErrorCodes.INVALID_URL, e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param mCubeAuthData Represents the {@link MCubeAuthData}
     * @param callId        The MCube call id for which the recording URL is required
     * @return The accessible URL of the recorded file as a {@link String}
     */
    @Override
    public String buildVoiceRecordUrl(final MCubeAuthData mCubeAuthData, final String callId) {
        return String.format(recordUrl, mCubeAuthData.getAuthToken(), callId);
    }

    /**
     * Builds a response for APIs that return {@code "status": "succ"}.
     * Used for edit-emp and outbound-calls endpoints.
     *
     * @param responseJson the raw JSON response from MCube
     * @return the parsed telephone response with success/failure status
     */
    private TelephoneResponse buildResponse(final JsonObject responseJson) {
        final TelephoneResponse telephoneResponse = new TelephoneResponse();
        final String status = responseJson.getNullableString(MCubeFields.STATUS);

        telephoneResponse.setStatus(Objects.equals(status, MCubeFields.SUCC) || Objects.equals(status, MCubeFields.SUCCESS)
                ? TelephoneResponse.Status.SUCCESS : TelephoneResponse.Status.FAILURE);

        return telephoneResponse;
    }

    /**
     * Executes the HTTP request and returns the JSON response.
     *
     * @param httpRequest the HTTP request to execute
     * @return the JSON response body
     */
    private JsonObject processRequest(final HttpRequest httpRequest) {
        try (final HttpConnectionHelper connectionHelper = new HttpConnectionHelper()) {
            return connectionHelper.getResponse(httpRequest);
        }
    }

    /**
     * Builds a failure {@link TelephoneResponse} populated with the exception's error message.
     *
     * @param exception the exception thrown during request processing
     * @return a {@link TelephoneResponse} with {@link TelephoneResponse.Status#FAILURE} and the error message
     */
    private TelephoneResponse buildErrorResponse(final Exception exception) {
        final TelephoneResponse telephoneResponse = new TelephoneResponse();

        telephoneResponse.setStatus(TelephoneResponse.Status.FAILURE);
        telephoneResponse.setErrorMessage(exception.getMessage());
        return telephoneResponse;
    }

    /**
     * Wraps the JSON data into a request body map.
     *
     * @param data the JSON payload
     * @return a map containing the serialized request body
     */
    private Map<String, String> getRequestBody(final JsonObject data) {
        final Map<String, String> requestBodyMap = new HashMap<>();

        requestBodyMap.put(TelephoneFields.REQUEST_BODY, data.toString());
        return requestBodyMap;
    }

    /**
     * Builds the standard HTTP headers with content type for MCube API calls.
     *
     * @return a map of HTTP headers
     */
    private Map<String, String> buildHeaders() {
        final Map<String, String> headers = new HashMap<>();

        headers.put(TelephoneFields.CONTENT_TYPE, TelephoneFields.APPLICATION_JSON);
        return headers;
    }

    /**
     * Constructs a URL from the given path elements joined by forward slashes.
     *
     * @param pathElements the URL path segments
     * @return the constructed {@link URL}
     * @throws RuntimeException if the URL is malformed
     */
    private URL buildURL(final String... pathElements) {
        final StringJoiner stringJoiner = new StringJoiner(StringUtil.Delimiter.FORWARD_SLASH.getDelimiter());

        for (final String pathElement : pathElements) {
            stringJoiner.add(pathElement);
        }
        try {
            return new URL(stringJoiner.toString());
        } catch (final MalformedURLException exception) {
            throw ErrorCode.get(NotificationErrorCodes.INVALID_URL, exception);
        }
    }

    /**
     * MCube-specific API field names and endpoint paths.
     */
    private static final class MCubeFields {

        // Authentication & common fields
        static final String AUTHORIZATION = "Authorization";
        static final String HTTP_AUTHORIZATION = "HTTP_AUTHORIZATION";
        static final String STATUS = "status";
        static final String SUCC = "succ";
        static final String SUCCESS = "success";
        static final String ID = "id";

        // API endpoint paths
        static final String REST_M_CUBE_API = "Restmcube-api";
        static final String M_CUBE_API = "mcube-api";
        static final String ADD_EMP_NEW = "add-emp-new";
        static final String EDIT_EMP = "edit-emp";
        static final String REMOVE_AGENT = "remove-agent";
        static final String OUTBOUND_CALLS = "outbound-calls";

        // Add agent fields
        static final String USERNAME = "username";
        static final String AGENT_NAME = "agentname";
        static final String EMAIL = "email";
        static final String PHONE_NUMBER = "phonenumber";
        static final String ROLE = "role";
        static final String INBOUND = "inbound";
        static final String OUTBOUND = "outbound";
        static final String AUTO_DIALER = "autodialer";
        static final String REQUESTED_BY = "requestedby";

        // Edit agent fields
        static final String NEW_PHONE_NUMBER = "newphonenumber";

        // Outbound call fields
        static final String EXE_NUMBER = "exenumber";
        static final String CUST_NUMBER = "custnumber";
        static final String REF_URL = "refurl";

        // Default values
        static final String TRUE = "true";
        public static final String FALSE = "false";
    }
}
