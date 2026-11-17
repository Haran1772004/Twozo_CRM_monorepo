package com.tlc.web.versioning;

import com.tlc.web.fields.VersionAttributes;

import java.util.stream.Stream;

/**
 * <p>
 * Utility class for resolving API version from requests.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public final class ApiVersionUtils {

    private ApiVersionUtils() {
    }

    private static class Instance {
        private static final ApiVersionUtils INSTANCE = new ApiVersionUtils();
    }

    /**
     * <p>
     * Gets {@link ApiVersionUtils}.
     * </p>
     *
     * @return {@link ApiVersionUtils} instance.
     */
    public static ApiVersionUtils getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Resolves the API version from the request header.
     * </p>
     *
     * @param versionHeader Header's value.
     * @return The version string, defaults to "1" if not specified.
     */
    public String resolveVersion(final Stream<String> versionHeader) {
        return resolveVersion(versionHeader, VersionAttributes.DEFAULT_VERSION);
    }

    /**
     * <p>
     * Resolves the API version from the request header with custom default.
     * </p>
     *
     * @param versionHeader  Header's value.
     * @param defaultVersion The default version if header is not present.
     * @return The version string.
     */
    public String resolveVersion(final Stream<String> versionHeader, final String defaultVersion) {
        return versionHeader.filter(version -> !version.isBlank()).map(String::trim).findFirst().orElse(defaultVersion);
    }
}