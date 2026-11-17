package com.tlc.notification.fields;

/**
 * <p>
 * Provides the keys of the notification properties file.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public enum NotificationKeys {

    GMAIL("gmail"),
    OUTLOOK("outlook"),
    APP_AUTHORITY("authority"),
    APP_RESPONSE_TYPE("responseType"),
    APP_CLIENT_ID("client.id"),
    APP_SCOPE("scope.email"),
    APP_REDIRECT_URL("redirectUrl.email"),
    APP_GRANT_TYPE("grantType"),
    APP_CLIENT_SECRET("client.secret"),
    APP_ACCESS_TOKEN_URL("accessTokenUrl"),
    APP_GMAIL_TOKEN_INFO_URL("tokenInfoUrl"),
    APP_OUTLOOK_DEFAULT_SCOPE("defaultScope"),
    APP_OUTLOOK_COMMON_AUTHORITY("commonAuthority"),
    APP_OUTLOOK_TENANT_AUTHORITY("tenantAuthority"),
    AUTHORIZATION("Authorization"),
    ACCEPT("Accept"),
    APPLICATION_JSON("application/json"),
    BEARER("Bearer "),
    VALUE("value"),
    ID("id"),
    PLAIN_TEXT("text/plain"),
    MULTIPART("multipart"),
    HTML_CONTENT_TYPE("text/html"),
    CONTENT_TYPE("Content-Type"),
    CONTENT_BYTES("contentBytes"),
    NAME("name"),
    ATTACHMENTS("attachments"),
    IS_INLINE("isInline"),
    SIZE("size"),
    MEDIA_CONTENT_TYPE("@odata.mediaContentType"),
    REQUEST_BODY("requestBody"),
    NEXT_LINK("@odata.nextLink"),
    EMAIL("email"),
    ;

    private final String key;

    NotificationKeys(final String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}