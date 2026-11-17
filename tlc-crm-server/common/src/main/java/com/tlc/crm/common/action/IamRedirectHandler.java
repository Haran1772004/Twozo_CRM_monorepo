package com.tlc.crm.common.action;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.Env;
import com.tlc.crm.common.fields.QueryParamAttributes;
import com.tlc.iam.common.action.req.IamHttpRequest;
import com.tlc.iam.common.action.res.IamHttpResponse;
import com.tlc.iam.common.fields.UserFields;

import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.Base64.Decoder;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Handles the IAM related redirections
 * </p>
 *
 * @author Mohammed Asif J.
 * @version 1.0
 */
public final class IamRedirectHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(IamRedirectHandler.class);

    private static final String PROVIDER_PARAM = "provider=";
    private static final String STATE_PARAM = "&state=";

    private final Encoder encoder;
    private final Decoder decoder;

    private IamRedirectHandler() {
        this.encoder = Base64.getEncoder();
        this.decoder = Base64.getDecoder();
    }

    private static class InstanceHolder {
        private static final IamRedirectHandler INSTANCE = new IamRedirectHandler();
    }

    /**
     * Gets the instance of {@link IamRedirectHandler}.
     *
     * @return The {@link IamRedirectHandler} instance.
     */
    public static IamRedirectHandler getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * <p>
     * Sets the redirect url based on the request and redirection url provided.
     * </p>
     *
     * @param request     Refers the {@link IamHttpRequest}
     * @param response    Refers the {@link IamHttpResponse}
     * @param redirectUrl Refers the url to which the page to be redirected
     */
    public void buildRedirectUrlWithState(final IamHttpRequest request,
                                          final IamHttpResponse response,
                                          final String redirectUrl) {
        final String localRedirectParam = request.getParameter(QueryParamAttributes.LOCAL_REDIRECTION_REQUIRED);

        if (Objects.isNull(localRedirectParam)) {
            response.setTemporaryRedirectUrl(redirectUrl);
        } else {
            response.setTemporaryRedirectUrl(
                    new StringBuilder()
                            .append(redirectUrl)
                            .append(STATE_PARAM)
                            .append(encoder.encodeToString(Json.object()
                                    .put(QueryParamAttributes.LOCAL_REDIRECTION_REQUIRED,
                                            Boolean.valueOf(localRedirectParam))
                                    .getBytes()))
                            .toString()
            );
        }
    }

    /**
     * <p>
     * Handles the redirection by placing the required redirection url in response
     * </p>
     *
     * @param request   Refers to the {@link IamHttpRequest}
     * @param response  Refers to the {@link IamHttpResponse}
     * @param serverUrl Refers to the server redirect url
     * @param localUrl  Refers to the local redirect url
     */
    public void handleClientRedirect(final IamHttpRequest request,
                                     final IamHttpResponse response,
                                     final String serverUrl,
                                     final String localUrl) {
        final String stateParam = request.getParameter(QueryParamAttributes.STATE);

        if (Objects.isNull(stateParam)) {
            response.setTemporaryRedirectUrl(serverUrl);
        } else {
            response.setTemporaryRedirectUrl(
                    Json.object(decoder.decode(stateParam))
                            .optBoolean(QueryParamAttributes.LOCAL_REDIRECTION_REQUIRED)
                            && Env.isDevMode() ? localUrl : serverUrl
            );
        }
    }
    /**
     * @param request   Refers to the {@link IamHttpRequest}, used to read the incoming
     *                  {@code localRedirectionRequired} flag from its state param
     * @param firstName Refers to the user's first name from the OIDC provider
     * @param idpUserId Refers to the OIDC provider's user id
     * @param email     Refers to the verified email address
     * @return Base64 encoded JSON state
     */
    public String buildPhoneNumberRedirectState(final IamHttpRequest request, final String firstName,
                                    final String idpUserId, final String email) {
        final String incomingState = request.getParameter(QueryParamAttributes.STATE);
        final boolean localRedirectionRequired = Objects.nonNull(incomingState)
                && Json.object(decoder.decode(incomingState)).optBoolean(QueryParamAttributes.LOCAL_REDIRECTION_REQUIRED);

        return encoder.encodeToString(Json.object()
                .put(UserFields.FIRST_NAME, firstName)
                .put(UserFields.IDP_USER_ID, idpUserId)
                .put(UserFields.EMAIL, email)
                .put(QueryParamAttributes.LOCAL_REDIRECTION_REQUIRED, localRedirectionRequired)
                .getBytes());
    }

    /**
     * <p>
     * Decodes the {@code state} query param previously created for the
     * phone number redirect during signup.
     * </p>
     *
     * @param encodedState Refers to the Base64 encoded {@code state} query param
     * @return The decoded {@link JsonObject} containing firstName, idpUserId,
     *         email, and localRedirectionRequired
     */
    public JsonObject decodePhoneNumberRedirectState(final String encodedState) {
        return Json.object(decoder.decode(encodedState));
    }

    /**
     * <p>
     * Redirects to the frontend page that collects the mandatory phone number,
     * carrying the encoded signup data as the {@code state} query param.
     * Picks the server or local url the same way {@link #handleClientRedirect} does.
     * </p>
     *
     * @param response    Refers to the {@link IamHttpResponse}
     * @param serverUrl   Refers to the server frontend url that collects the phone number
     * @param localUrl    Refers to the local frontend url that collects the phone number
     * @param encodedData Refers to the encoded signup state
     */
    public void redirectToPhoneNumberPage(final IamHttpResponse response, final String serverUrl, final String localUrl, final String provider, final String encodedData){
        final String redirectUrl = Env.isDevMode() ? localUrl : serverUrl;

        response.setTemporaryRedirectUrl(
                new StringBuilder()
                        .append(redirectUrl)
                        .append(PROVIDER_PARAM)
                        .append(provider)
                        .append(STATE_PARAM)
                        .append(encodedData)
                        .toString()
        );
    }
}