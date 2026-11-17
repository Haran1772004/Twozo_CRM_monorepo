package com.tlc.notification.internal.telecmi;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.Configurations;
import com.tlc.commons.util.StringUtil;
import com.tlc.notification.internal.fields.TelephoneFields;
import com.tlc.notification.internal.status.NotificationErrorCodes;
import com.tlc.notification.model.telephone.TelephoneResponse;
import com.tlc.notification.model.telephone.telecmi.TeleCMIAuthData;
import com.tlc.notification.model.telephone.telecmi.TeleCMIUser;
import com.tlc.notification.service.telephony.telecmi.TeleCMIService;
import com.tlc.web.HttpConnectionHelper;
import com.tlc.web.HttpMethod;
import com.tlc.web.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.regex.Pattern;

/**
 * Implementation of the {@link TeleCMIService} for the TeleCMI provider
 *
 * @author kishore J
 * @version 1.0
 */
public class TeleCMIServiceImpl implements TeleCMIService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TeleCMIServiceImpl.class);
    private static final Pattern NON_DIGIT = Pattern.compile("\\D");

    private String baseV2Url;
    private String baseV3Url;
    private String recordUrl;

    private TeleCMIServiceImpl() {
    }

    private static class InstanceHolder {
        private static final TeleCMIServiceImpl INSTANCE = new TeleCMIServiceImpl();
    }

    /**
     * <p>
     * Gets the {@link TeleCMIServiceImpl}
     * </p>
     *
     * @return the singleton instance of {@link TeleCMIServiceImpl}.
     */
    public static TeleCMIServiceImpl getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Represents the {@link Configurations}
     */
    @Override
    public void start(final Configurations configurations) {
        final Configurations teleCMIConfig = configurations.getNestedConfigs("telecmi");

        if (teleCMIConfig.getFirstKey().isPresent()) {
            this.baseV2Url = teleCMIConfig.getConfig("baseV2Url");
            this.baseV3Url = teleCMIConfig.getConfig("baseV3Url");
            this.recordUrl = StringUtil.join(StringUtil.Delimiter.EMPTY_STRING, baseV2Url, teleCMIConfig.getConfig("recordPath"));
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
     *
     * @param telephoneAuthData the authentication details required to connect with the TeleCMI provider
     * @return the authentication response from the TeleCMI provider
     */
    @Override
    public TelephoneResponse authenticate(final TeleCMIAuthData telephoneAuthData) {
        final JsonObject data = Json.object().put(TelephoneFields.APPID, telephoneAuthData.id())
                .put(TelephoneFields.SECRET, telephoneAuthData.getSecret());
        final JsonObject response = processRequest(new HttpRequest(buildURL(baseV2Url, TelephoneFields.TOKEN), getRequestBody(data), HttpMethod.POST, getDefaultHeaders()));

        return buildResponse(response);
    }

    /**
     * {@inheritDoc}
     *
     * @param teleCMIUser the user details to be created in the TeleCMI system
     * @return the creation response from the TeleCMI provider
     */
    @Override
    public TelephoneResponse createUser(final TeleCMIUser teleCMIUser) {
        final JsonObject data = buildUserJson(teleCMIUser);

        data.put(TelephoneFields.FOLLOW_ME, true);
        data.put(TelephoneFields.EXTENSION, teleCMIUser.getExtension());
        LOGGER.info("TeleCMI Create User Request : {}", data);
        final JsonObject response = processRequest(new HttpRequest(buildURL(baseV3Url, TelephoneFields.USER, TelephoneFields.ADD), getRequestBody(data),
                HttpMethod.POST, getDefaultHeaders()));
        LOGGER.info("TeleCMI Create User Response : {}", response);
        final TelephoneResponse telephoneResponse = buildResponse(response);

        if (telephoneResponse.getStatus().isSuccessful()) {
            teleCMIUser.setId(response.getJsonObject(TelephoneFields.AGENT).getString(TelephoneFields.AGENT_ID));
        }
        return telephoneResponse;
    }

    /**
     * {@inheritDoc}
     *
     * @param teleCMIUser the updated user details to be synchronized with the TeleCMI system
     * @return the update response from the TeleCMI provider
     */
    @Override
    public TelephoneResponse updateUser(final TeleCMIUser teleCMIUser) {
        final JsonObject data = buildUserJson(teleCMIUser);

        data.put(TelephoneFields.AGENT_ID, teleCMIUser.getId());
        final JsonObject response = processRequest(new HttpRequest(buildURL(baseV3Url, TelephoneFields.USER, TelephoneFields.UPDATE), getRequestBody(data),
                HttpMethod.POST, getDefaultHeaders()));

        return buildResponse(response);
    }

    /**
     * {@inheritDoc}
     *
     * @param teleCMIUser the user details identifying the account to be removed
     * @return the deletion response from the TeleCMI provider
     */
    @Override
    public TelephoneResponse removeUser(final TeleCMIUser teleCMIUser) {
        final JsonObject dataMap = Json.object();
        final TeleCMIAuthData teleCMIAuthentication = teleCMIUser.getTeleCMIAuthData();

        dataMap.put(TelephoneFields.APPID, teleCMIAuthentication.id());
        dataMap.put(TelephoneFields.SECRET, teleCMIAuthentication.getSecret());
        dataMap.put(TelephoneFields.AGENT_ID, teleCMIUser.getId());
        final JsonObject response = processRequest(new HttpRequest(buildURL(baseV3Url, TelephoneFields.USER, TelephoneFields.REMOVE), getRequestBody(dataMap),
                HttpMethod.POST, getDefaultHeaders()));

        return buildResponse(response);
    }

    /**
     * {@inheritDoc}
     *
     * @param userId          the agent ID of the user to be retrieved
     * @param teleCMIAuthData the authentication details used to authorize the request
     * @return the {@link TeleCMIUser} populated from the TeleCMI provider response,
     * or {@code null} if the retrieval fails
     */
    @Override
    public TeleCMIUser getUser(final String userId, final TeleCMIAuthData teleCMIAuthData) {
        final JsonObject dataMap = Json.object();

        dataMap.put(TelephoneFields.APPID, teleCMIAuthData.id());
        dataMap.put(TelephoneFields.SECRET, teleCMIAuthData.getSecret());
        dataMap.put(TelephoneFields.AGENT_ID, userId);
        final JsonObject response = processRequest(new HttpRequest(buildURL(baseV3Url, TelephoneFields.USER, TelephoneFields.GET), getRequestBody(dataMap),
                HttpMethod.POST, getDefaultHeaders()));

        if (Objects.equals(response.getInt(TelephoneFields.CODE), 200)) {
            final JsonObject agentJson = response.getJsonObject(TelephoneFields.AGENT);
            final TeleCMIUser teleCMIUser = new TeleCMIUser();

            teleCMIUser.setId(agentJson.getString(TelephoneFields.AGENT_ID));
            teleCMIUser.setFirstName(agentJson.getString(TelephoneFields.FIRST_NAME));
            teleCMIUser.setLastName(agentJson.getString(TelephoneFields.LAST_NAME));
            teleCMIUser.setPhoneNumber(agentJson.getString(TelephoneFields.PHONE));
            teleCMIUser.setExtension(agentJson.getInt(TelephoneFields.EXTENSION));
            teleCMIUser.setPassword(agentJson.getString(TelephoneFields.PASSWORD));

            return teleCMIUser;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @param teleCMIAuthenticationData Represents the {@link TeleCMIAuthData}
     * @param fileName                  The name of the recorded file for which the URL is required
     * @return The accessible URL of the recorded file as a {@link String}
     */
    @Override
    public String buildVoiceRecordUrl(final TeleCMIAuthData teleCMIAuthenticationData, final String fileName) {
        return String.format(recordUrl, teleCMIAuthenticationData.getAppId(), teleCMIAuthenticationData.getSecret(), fileName);
    }

    /**
     * <p>
     * Builds a JSON representation of a {@link TeleCMIUser}, including its associated {@link TeleCMIAuthData} details
     * </p>
     *
     * @param teleCMIUser the TeleCMI user containing authentication and user details
     * @return a {@link JsonObject} representing the user and authentication data
     */
    private JsonObject buildUserJson(final TeleCMIUser teleCMIUser) {
        final JsonObject data = Json.object();
        final TeleCMIAuthData teleCMIAuthentication = teleCMIUser.getTeleCMIAuthData();

        data.put(TelephoneFields.APPID, teleCMIAuthentication.id());
        data.put(TelephoneFields.SECRET, teleCMIAuthentication.getSecret());
        data.put(TelephoneFields.FIRST_NAME, teleCMIUser.getFirstName());
        data.put(TelephoneFields.LAST_NAME, teleCMIUser.getLastName());
        data.put(TelephoneFields.EMAIL_ID, teleCMIUser.getEmail());
        data.put(TelephoneFields.PASSWORD, teleCMIUser.getPassword());
        data.put(TelephoneFields.PHONE_NUMBER, formatPhoneNumber(teleCMIUser.getPhoneNumber()));

        return data;
    }

    /**
     * Formats the phone number with country code and digits only, as required by TeleCMI.
     *
     * @param phoneNumber the user's phone number
     * @return the formatted phone number, or {@code null} if unavailable
     */
    private String formatPhoneNumber(final String phoneNumber) {
        return Objects.isNull(phoneNumber) ? null : NON_DIGIT.matcher(phoneNumber).replaceAll("");
    }

    /**
     * <p>
     * Builds a {@link TelephoneResponse} object from the provided JSON response
     * </p>
     * .
     *
     * @param responseJson the JSON response containing result details
     * @return a {@link TelephoneResponse} populated with response data
     */
    private TelephoneResponse buildResponse(final JsonObject responseJson) {
        final TelephoneResponse telephoneResponse = new TelephoneResponse();

        telephoneResponse.setStatus(Objects.equals(responseJson.getInt(TelephoneFields.CODE), 200) ?
                TelephoneResponse.Status.SUCCESS : TelephoneResponse.Status.FAILURE);
        telephoneResponse.setErrorMessage(responseJson.toString());

        return telephoneResponse;
    }

    /**
     * <p>
     * Executes the HTTP request and returns the parsed JSON response object.
     * </p>
     *
     * @param httpRequest ready-to-execute request object
     * @return parsed response JSON object
     */
    private JsonObject processRequest(final HttpRequest httpRequest) {
        try (final HttpConnectionHelper connectionHelper = new HttpConnectionHelper()) {
            return connectionHelper.getResponse(httpRequest);
        }
    }

    /**
     * <p>
     * Prepares the request body map containing the serialized JSON payload.
     * </p>
     *
     * @param data the request data as a {@link JsonObject}
     * @return a map with the request body under the key {@link TelephoneFields}
     */
    private Map<String, String> getRequestBody(final JsonObject data) {
        final Map<String, String> requestBodyMap = new HashMap<>();

        requestBodyMap.put(TelephoneFields.REQUEST_BODY, data.toString());

        return requestBodyMap;
    }

    /**
     * Utility to build URLs for teleCMI API requests.
     *
     * @param pathElements parts of the API path
     * @return constructed URL object
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
     * <p>
     * Gets the default HTTP headers for TeleCMI requests.
     * </p>
     *
     * @return a map of default request headers
     */
    private Map<String, String> getDefaultHeaders() {
        final Map<String, String> headers = new HashMap<>();

        headers.put(TelephoneFields.CONTENT_TYPE, TelephoneFields.APPLICATION_JSON);

        return headers;
    }
}