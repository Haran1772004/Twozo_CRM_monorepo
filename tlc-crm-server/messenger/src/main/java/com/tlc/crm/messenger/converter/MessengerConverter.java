package com.tlc.crm.messenger.converter;

import com.tlc.commons.json.JsonObject;

/**
 * A converter which provides abstract way to converter the messenger models
 *
 * @author Maharaja S
 * @version 1.0
 */
public interface MessengerConverter<T, U> {

    /**
     * <p>
     * Converts the provided models into the json object
     * </p>
     *
     * @param primaryModel   Represents the primary
     * @param secondaryModel Represents the secondary model
     * @return the json object
     */
    JsonObject convert(final T primaryModel, final U secondaryModel);
}
