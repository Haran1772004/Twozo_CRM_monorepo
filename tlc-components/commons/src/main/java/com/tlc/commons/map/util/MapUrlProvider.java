package com.tlc.commons.map.util;

import java.util.Objects;

/**
 * <p>
 * Provides the map url based on the given parameters
 * </p>
 *
 * @author Muthu Kumar V
 * @version 1.0
 */
public final class MapUrlProvider {

    private static final String APPLE_MAP_URL_TEMPLATE = "https://maps.apple.com/?q=${LATITUDE},${LONGITUDE}&ll=${LATITUDE},${LONGITUDE}";
    private static final String GOOGLE_MAP_URL_TEMPLATE = "https://www.google.com/maps/search/?api=1&query=${LATITUDE},${LONGITUDE}";
    private static final String OSM_URL_TEMPLATE = "https://www.openstreetmap.org/?mlat=${LATITUDE}&mlon=${LONGITUDE}#map=${ZOOM_LEVEL}/${LATITUDE}/${LONGITUDE}";

    private static final String DEFAULT_OSM_ZOOM_LEVEL = "19";

    private static final String LATITUDE_KEY = "${LATITUDE}";
    private static final String LONGITUDE_KEY = "${LONGITUDE}";
    private static final String ZOOM_LEVEL_KEY = "${ZOOM_LEVEL}";

    /**
     * <p>
     * Fetches the location url with the given parameters
     * </p>
     *
     * @param operatingSystem Represents the operating system name
     * @param latitude        Represents the latitude param
     * @param longitude       Represents the longitude param
     * @param zoomLevel       Represents the zoom level
     * @return The location url
     */
    public static String fetchUrl(final String operatingSystem, final String latitude, final String longitude, final String zoomLevel) {
        final String os = Objects.toString(operatingSystem, "").toLowerCase();

        if (os.contains("mac") || os.contains("ios")) {
            return buildUrl(APPLE_MAP_URL_TEMPLATE, latitude, longitude, null);
        } else if (os.contains("windows") || os.contains("linux") || os.contains("android") || os.contains("chrome")) {
            return buildUrl(GOOGLE_MAP_URL_TEMPLATE, latitude, longitude, null);
        } else {
            return buildUrl(OSM_URL_TEMPLATE, latitude, longitude, Objects.nonNull(zoomLevel) ? zoomLevel : DEFAULT_OSM_ZOOM_LEVEL);
        }
    }

    /**
     * <p>
     * Fetches the location url with the given parameters
     * </p>
     *
     * @param latitude  Represents the latitude param
     * @param longitude Represents the longitude param
     * @return The location url
     */
    public static String fetchUrl(final String latitude, final String longitude) {
        return buildUrl(GOOGLE_MAP_URL_TEMPLATE, latitude, longitude, null);
    }

    /**
     * <p>
     * Builds the location url with the given parameters
     * </p>
     *
     * @param urlTemplate Represents the location url template
     * @param latitude    Represents the latitude param
     * @param longitude   Represents the longitude param
     * @param zoomLevel   Represents the zoom level
     * @return The location url
     */
    private static String buildUrl(final String urlTemplate, final String latitude, final String longitude, final String zoomLevel) {
        String resolvedUrl = urlTemplate.replace(LATITUDE_KEY, latitude)
                .replace(LONGITUDE_KEY, longitude);

        if (zoomLevel != null) {
            resolvedUrl = resolvedUrl.replace(ZOOM_LEVEL_KEY, zoomLevel);
        }
        return resolvedUrl;
    }
}