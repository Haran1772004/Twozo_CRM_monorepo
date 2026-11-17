package com.tlc.web;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.executor.context.session.SessionContext;
import com.tlc.commons.executor.context.session.SessionContextHolder;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.StringUtil;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.web.status.WebErrorCodes;

import java.net.URL;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * Offers assistance for establishing a http url connection.
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public final class HttpUtil {

    private HttpUtil() {

    }

    /**
     * <p>
     * Makes a http request and returns the response as a {@link JsonObject}.
     * The connection will be closed before returning the variable, even if an error occurs.
     * </p>
     *
     * @param httpRequest Represents the {@link HttpRequest}
     * @return {@link JsonObject}
     */
    public static JsonObject makeRequest(final HttpRequest httpRequest) {
        try (final HttpConnectionHelper httpConnectionHelper = new HttpConnectionHelper()) {
            return httpConnectionHelper.getResponse(httpRequest);
        } catch (Exception exception) {
            throw ErrorCode.get(WebErrorCodes.INVALID_CONNECTION_RESPONSE, exception);
        }
    }

    /**
     * Builds the absolute URL by appending the given relative URL with the domain
     *
     * @param relativeUrl Represents the relative URL
     * @return The absolute URL
     */
    public static String buildAbsoluteUrl(final String relativeUrl) {
        Objects.requireNonNull(relativeUrl);
        final String domain = getDomain();

        if (Objects.nonNull(domain) && !domain.isEmpty() && !relativeUrl.contains(domain)) {
            return StringUtil.join(relativeUrl.startsWith(StringUtil.Delimiter.FORWARD_SLASH.getDelimiter())
                    ? StringUtil.Delimiter.EMPTY_STRING : StringUtil.Delimiter.FORWARD_SLASH, domain, relativeUrl);
        }
        return relativeUrl;
    }

    /**
     * Resolves the application domain by checking the current session context
     * before falling back to the global configuration.
     * <p>
     * The resolution order is:
     * <ol>
     * <li><b>Session Context:</b> Checks the {@link SessionContextHolder} for a "domain" attribute associated with the current thread.</li>
     * <li><b>Configuration Properties:</b> If no session is active or the attribute is missing, it retrieves the "default.domain" value from the 'web' configuration block in {@link ConfHolder}.</li>
     * </ol>
     * This ensures domain-specific logic works for multi-tenant sessions while providing a safe default for background or system tasks.
     * </p>
     *
     * @return The resolved domain string. Returns the default domain if no session-specific domain is found.
     */
    public static String getDomain() {
        final Optional<SessionContext> context = SessionContextHolder.getInstance().getContext();

        return String.valueOf(context.map(sessionContext -> sessionContext.get("domain"))
                .orElseGet(() -> ConfHolder.getInstance().getProperties("web").getConfig("default.domain")));
    }
}
