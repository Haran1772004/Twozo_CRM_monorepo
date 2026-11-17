package com.tlc.crm.common.util.oauth;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.commons.util.Env;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;

import java.util.Base64;
import java.util.Objects;

import static com.tlc.crm.common.fields.QueryParamAttributes.LOCAL_REDIRECTION_REQUIRED;
import static com.tlc.crm.common.fields.QueryParamAttributes.STATE;

/**
 * <p>
 * Provides assistance in handling the redirection in the response
 * </p>
 *
 * @author AswiniN
 * @version 1.0
 */
public final class RedirectHandler {

    private RedirectHandler() {
    }

    private static class Instance {
        private static final RedirectHandler INSTANCE = new RedirectHandler();
    }

    /**
     * Gets the instance of {@link RedirectHandler}.
     *
     * @return The {@link RedirectHandler} instance.
     */
    public static RedirectHandler getInstance() {
        return RedirectHandler.Instance.INSTANCE;
    }

    /**
     * <p>
     * Sets the redirect url based on the request and consent url provided.
     * </p>
     *
     * @param request     Refers the {@link CrmRequest}
     * @param url         Refers the url to which the page to be redirected
     * @param crmResponse Refers the {@link CrmResponse}
     */
    public void buildRedirectUrlWithState(final CrmRequest request, final String url, final CrmResponse crmResponse) {
        final String parameter = request.getParameter(LOCAL_REDIRECTION_REQUIRED);

        if (Objects.isNull(parameter)) {
            crmResponse.setTemporaryRedirectUrl(url);
        } else {
            final StringBuilder urlBuilder = new StringBuilder();
            final JsonObject stateJson = Json.object();

            stateJson.put(LOCAL_REDIRECTION_REQUIRED, Boolean.valueOf(parameter));
            crmResponse.setTemporaryRedirectUrl(urlBuilder.append(url).append("&state=")
                    .append(Base64.getEncoder().encodeToString(stateJson.getBytes())).toString());
        }
    }

    /**
     * <p>
     * Handles the redirection by placing the required redirection url in response
     * </p>
     *
     * @param request     Refers to the {@link CrmRequest}
     * @param crmResponse Refers to the {@link CrmResponse}
     * @param serverUrl   Refers to the server redirect url
     * @param localUrl    Refers to the local redirect url
     */
    public void handleClientRedirect(final CrmRequest request, final CrmResponse crmResponse, final String serverUrl,
                                     final String localUrl) {
        final String stateParameter = request.getParameter(STATE);

        if (Objects.isNull(stateParameter)) {
            crmResponse.setTemporaryRedirectUrl(serverUrl);
        } else {
            final JsonObject stateJson = Json.object(Base64.getDecoder().decode(stateParameter));
            final boolean isHeaderExists = stateJson.optBoolean(LOCAL_REDIRECTION_REQUIRED);
            final String redirectUrl = isHeaderExists && Env.isDevMode() ? localUrl : serverUrl;

            crmResponse.setTemporaryRedirectUrl(redirectUrl);
        }
    }
}