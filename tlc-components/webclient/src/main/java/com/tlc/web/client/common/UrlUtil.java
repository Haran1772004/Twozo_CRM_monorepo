package com.tlc.web.client.common;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

/**
 * Utility class for URL related operations
 *
 * @author Maharaja S
 * @version 1.0
 */
public final class UrlUtil {

    /**
     * Appends the query params with the provided uri
     *
     * @param requestUri  Represents the request uri
     * @param queryParams Represents the query params
     * @return the appended url
     */
    public static String appendQueryParams(final String requestUri, final Map<String, String> queryParams) {
        if (Objects.isNull(requestUri) || Objects.isNull(queryParams) || queryParams.isEmpty()) {
            return requestUri;
        }
        final StringBuilder url = new StringBuilder(requestUri);

        url.append("?");

        for (final Map.Entry<String, String> param : queryParams.entrySet()) {
            url.append(URLEncoder.encode(param.getKey(), StandardCharsets.UTF_8));
            url.append("=");
            url.append(URLEncoder.encode(param.getValue(), StandardCharsets.UTF_8));
            url.append("&");
        }
        url.deleteCharAt(url.length() - 1);

        return url.toString();
    }
}