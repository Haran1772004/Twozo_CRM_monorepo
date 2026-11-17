package com.tlc.social.common.converter;

import com.tlc.commons.json.JsonObject;
import com.tlc.social.common.model.SocialEntity;

/**
 * Represents an abstract way to convert the social entities
 *
 * @param <T> Represents the social entity type
 * @author Maharaja S
 * @version 1.0
 */
public interface SocialEntityConverter<T extends SocialEntity> {

    /**
     * <p>
     * Converts the json data into the {@link SocialEntity}
     * </p>
     *
     * @param jsonObject Represents the json data to be converted
     * @return the converted {@link T}
     */
    T convert(final JsonObject jsonObject);

    /**
     * <p>
     * Converts the {@link T} into json
     * </p>
     *
     * @param model Represents the {@link T} model
     * @return the converted json data
     */
    JsonObject convert(final T model);
}
