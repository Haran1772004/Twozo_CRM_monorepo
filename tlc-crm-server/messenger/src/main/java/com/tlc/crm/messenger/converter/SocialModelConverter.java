package com.tlc.crm.messenger.converter;

import com.tlc.commons.json.JsonObject;
import com.tlc.social.common.model.SocialEntity;

/**
 * Converts the social model into different form
 *
 * @param <T> Represents the social model type
 * @author Maharaja S
 * @version 1.0
 */
public interface SocialModelConverter<T extends SocialEntity> {

    /**
     * <p>
     * Converts the social model into json data
     * </p>
     *
     * @param socialModel Represents the social model
     * @return the converted json
     */
    JsonObject convert(final T socialModel);
}