package com.tlc.notification.service.template;

import com.tlc.notification.model.template.PlaceHolder;

import java.util.Collection;

/**
 * <p>
 * A service to resolve and place the appropriate values of the placeholders in the content provided
 * </p>
 *
 * @author  AswiniN
 * @version 1.0
 */
public interface PlaceHolderResolver {

    /**
     * <p>
     * Resolves the data with {@link PlaceHolder} values provided
     * </p>
     *
     * @param data     Refers to the data to be resolved
     * @param placeHolders Represents the {@link Collection} of {@link PlaceHolder}
     * @return {@link String} resolved data
     */
    String resolve(final String data, final Collection<PlaceHolder> placeHolders);
}