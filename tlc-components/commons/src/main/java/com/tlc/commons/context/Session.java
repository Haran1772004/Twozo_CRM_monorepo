package com.tlc.commons.context;

import com.tlc.commons.json.JsonObject;

/**
 * Represents the session details
 *
 * @author sundar
 * @version 1.0
 */
public interface Session {

    /**
     * <p>
     * Fetches the {@link JsonObject} associated with this session
     * </p>
     *
     * @return A {@link JsonObject} containing the session data
     */
    JsonObject getData();

    /**
     * <p>
     * Fetches the session id
     * </p>
     *
     * @return The session id
     */
    String sessionId();
}
