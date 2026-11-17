package com.tlc.commons.executor.context.session;

import com.tlc.commons.context.Session;
import com.tlc.commons.executor.context.CommonContextType;
import com.tlc.commons.executor.context.Context;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.StringUtil;

import java.util.Map;
import java.util.Objects;

/**
 * Context to hold the session values
 *
 * @author sundar
 * @version 1.0
 */
public class SessionContext implements Session, Context {

    private final JsonObject sessionJson;

    public SessionContext() {
        this.sessionJson = Json.object();
    }

    public SessionContext(final Map<String, Object> sessionMap) {
        this(Json.object(sessionMap));
    }

    SessionContext(final JsonObject sessionJson) {
        this.sessionJson = sessionJson;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Type} that represents the type of the context
     */
    @Override
    public Type getType() {
        return CommonContextType.SESSION;
    }

    /**
     * {@inheritDoc}
     *
     * @return a new instance of the context, identical to the original
     */
    @Override
    public Context copy() {
        return new SessionContext(sessionJson.toMap());
    }

    /**
     * Adds the values into the session context
     *
     * @param key   Represents the key of the session value
     * @param value Represents the session value
     */
    public void add(final String key, final Object value) {
        sessionJson.put(formatKey(Objects.requireNonNull(key)), Objects.requireNonNull(value));
    }

    /**
     * Gets the session value associated with the key
     *
     * @param key Represents the key of the session value
     * @return the session value associated
     */
    public Object get(final String key) {
        return sessionJson.get(formatKey(Objects.requireNonNull(key)));
    }

    /**
     * Checks whether the session value is exist or not
     *
     * @param key Represents the key of the session value
     * @return true if the session value is exist; false otherwise
     */
    public boolean contains(final String key) {
        return sessionJson.containsKey(formatKey(Objects.requireNonNull(key)));
    }

    /**
     * {@inheritDoc}
     *
     * @return A {@link JsonObject} containing the session data
     */
    @Override
    public JsonObject getData() {
        return sessionJson;
    }

    /**
     * {@inheritDoc}
     *
     * @return The session id
     */
    @Override
    public String sessionId() {
        return StringUtil.EMPTY_STRING;
    }

    /**
     * Trims whitespace and converts the session key to lowercase.
     *
     * @param key the session key to format
     * @return the formatted session key
     */
    private String formatKey(final String key) {
        return key.trim().toLowerCase();
    }
}