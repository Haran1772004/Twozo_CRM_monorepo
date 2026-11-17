package com.tlc.web;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import ua_parser.Client;
import ua_parser.Parser;

import java.util.Objects;

/**
 * A parser that parses the User-Agent value of http request
 *
 * @author Selvakumar
 * @version 1.0
 */
public class UserAgent {

    public static final UserAgent DEFAULT = new UserAgent();
    private static final Parser parser = new Parser();
    private static final String UNKNOWN = "Unknown";
    private static final String OTHER = "Other";
    private static final String DEVICE = "device";
    private static final String OS = "os";
    private static final String BROWSER = "browser";
    private final String osFamily;
    private final String browserFamily;
    private final String deviceFamily;

    private JsonObject jsonRepresentation;

    public UserAgent(final String userAgent) {
        Objects.requireNonNull(userAgent);
        final Client uaDetail = parser.parse(userAgent);

        this.osFamily = getValidData(uaDetail.os.family);
        this.deviceFamily = getValidData(uaDetail.device.family);
        this.browserFamily = getValidData(uaDetail.userAgent.family);
    }

    private UserAgent() {
        this.osFamily = UNKNOWN;
        this.deviceFamily = UNKNOWN;
        this.browserFamily = UNKNOWN;
    }

    public String getOperatingSystem() {
        return this.osFamily;
    }

    public String getBrowser() {
        return this.browserFamily;
    }

    public String getDevice() {
        return this.deviceFamily;
    }

    /**
     * Fetches the JSON representation of the user agent
     * @return {@link JsonObject}
     */
    public JsonObject toJson() {
        if (Objects.isNull(this.jsonRepresentation)) {
            this.jsonRepresentation = Json.object();

            jsonRepresentation.put(DEVICE, this.deviceFamily);
            jsonRepresentation.put(OS, this.osFamily);
            jsonRepresentation.put(BROWSER, browserFamily);
        }

        return jsonRepresentation;
    }

    private String getValidData(final String data) {
        return Objects.isNull(data) ? UNKNOWN : OTHER.equals(data) ? UNKNOWN : data;
    }

}
