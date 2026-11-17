package com.tlc.iam.common.models.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.iam.common.ApplicationFields;
import com.tlc.iam.common.models.ClientApplication;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Converts the json data into {@link ClientApplication}
 *
 * @author Maharaja S
 * @version 1.0
 */
public class ApplicationConverter {

    private ApplicationConverter() {}

    private static final class Instance {
        private static final ApplicationConverter APPLICATION_CONVERTER = new ApplicationConverter();
    }

    /**
     * <p>
     * Gets the instance of {@link ApplicationConverter}
     * </p>
     *
     * @return {@link ApplicationConverter}
     */
    public static ApplicationConverter getInstance() {
        return Instance.APPLICATION_CONVERTER;
    }

    /**
     * <p>
     * Converts the provided json data into {@link ApplicationConverter}
     * </p>
     *
     * @param jsonObject Represents the json data to be converted
     * @return the {@link ClientApplication}
     */
    public ClientApplication convertToApplication(final JsonObject jsonObject) {
        final String applicationName = jsonObject.getString(ApplicationFields.NAME);
        final String authSuccessRedirectUrl = jsonObject.getString(ApplicationFields.AUTH_SUCCESS_REDIRECT_URL);
        final String authFailureRedirectUrl = jsonObject.getString(ApplicationFields.AUTH_FAILURE_REDIRECT_URL);
        final boolean isSecureCookieEnabled = jsonObject.optBoolean(ApplicationFields.IS_SECURE_COOKIE_ENABLED, true);
        final String cookieDomain = jsonObject.optString(ApplicationFields.COOKIE_DOMAIN, null);
        final String cookiePath = jsonObject.optString(ApplicationFields.COOKIE_PATH, null);
        final String sameSiteMode = jsonObject.optString(ApplicationFields.SAME_SITE_MODE, null);
        final String cookieValidityInSeconds = jsonObject.optString(ApplicationFields.COOKIE_VALIDITY_IN_SECONDS, null);

        return new ClientApplication(applicationName, authSuccessRedirectUrl, authFailureRedirectUrl,
                isSecureCookieEnabled, cookieDomain,
                Objects.nonNull(cookiePath) && !cookiePath.isBlank() ? cookiePath : "/",
                Objects.nonNull(cookieValidityInSeconds) && !cookieValidityInSeconds.isBlank()
                        ? Integer.parseInt(cookieValidityInSeconds) : (int) TimeUnit.HOURS.toSeconds(8), sameSiteMode);
    }

    /**
     * <p>
     * Converts {@link ClientApplication} into json object and returns it.
     * </p>
     *
     * @param clientApplication Represents the {@link ClientApplication}
     * @return the json containing clientApplication details
     */
    public JsonObject convertToJson(final ClientApplication clientApplication) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(ApplicationFields.NAME, clientApplication.name());
        jsonObject.put(ApplicationFields.AUTH_SUCCESS_REDIRECT_URL, clientApplication.authSuccessRedirectUrl());
        jsonObject.put(ApplicationFields.AUTH_FAILURE_REDIRECT_URL, clientApplication.authFailureRedirectUrl());
        jsonObject.put(ApplicationFields.IS_SECURE_COOKIE_ENABLED, clientApplication.isSecureCookieEnabled());
        jsonObject.put(ApplicationFields.COOKIE_PATH, clientApplication.cookiePath());
        jsonObject.put(ApplicationFields.COOKIE_VALIDITY_IN_SECONDS, clientApplication.cookieValidityInSeconds());
        jsonObject.put(ApplicationFields.COOKIE_DOMAIN, clientApplication.cookieDomain());
        jsonObject.put(ApplicationFields.SAME_SITE_MODE, clientApplication.sameSiteMode());

        return jsonObject;
    }
}