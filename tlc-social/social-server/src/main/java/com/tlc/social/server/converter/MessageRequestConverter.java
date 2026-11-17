package com.tlc.social.server.converter;

import com.tlc.commons.json.JsonObject;
import com.tlc.social.server.model.MessageRequest;

/**
 * A converter which provides various ways to convert data from/into {@link MessageRequest}
 *
 * @param <T> Represents the type which implements {@link MessageRequest}
 * @author Maharaja S
 * @version 1.0
 */
public interface MessageRequestConverter<T extends MessageRequest> {

    /**
     * <p>
     * Converts the json data into the {@link MessageRequest}
     * </p>
     *
     * @param jsonObject Represents the json data to be converted
     * @return the {@link MessageRequest} type model
     */
    T convert(final JsonObject jsonObject);
}
