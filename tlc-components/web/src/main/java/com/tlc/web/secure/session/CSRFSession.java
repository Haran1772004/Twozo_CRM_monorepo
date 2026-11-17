package com.tlc.web.secure.session;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.web.internal.fields.CSRFFields;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents a CSRF token session details
 *
 * @author sundar
 * @version 1.0
 */
public class CSRFSession implements WebSession, Serializable {

    private String sessionId;
    private String tokenSource;
    private String token;
    private long issuedAt;

    public void setSessionId(final String sessionId) {
        this.sessionId = sessionId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(final String token) {
        this.token = token;
    }

    public long getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(long issuedAt) {
        this.issuedAt = issuedAt;
    }

    /**
     * {@inheritDoc}
     *
     * @return A {@link JsonObject} containing the session data
     */
    @Override
    public JsonObject getData() {
        final JsonObject sessionJson = Json.object();

        sessionJson.put(CSRFFields.TOKEN_SOURCE, tokenSource);
        sessionJson.put(CSRFFields.SESSION_ID, sessionId);
        sessionJson.put(CSRFFields.ISSUED_AT, issuedAt);

        return sessionJson;
    }

    @Override
    public boolean equals(final Object object) {
        if (!(object instanceof final CSRFSession that)) {
            return false;
        }
        return issuedAt == that.issuedAt && Objects.equals(sessionId, that.sessionId) && Objects.equals(tokenSource, that.tokenSource);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionId, tokenSource, issuedAt);
    }

    @Override
    public String sessionId() {
        return sessionId;
    }

    public String getTokenSource() {
        return tokenSource;
    }

    public void setTokenSource(String tokenSource) {
        this.tokenSource = tokenSource;
    }
}