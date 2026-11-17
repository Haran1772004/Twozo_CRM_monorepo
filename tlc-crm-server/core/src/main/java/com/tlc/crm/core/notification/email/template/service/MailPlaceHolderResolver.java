package com.tlc.crm.core.notification.email.template.service;

import com.tlc.crm.core.notification.email.template.service.impl.MailPlaceHolderResolverImpl;
import com.tlc.notification.model.template.PlaceHolder;
import com.tlc.notification.service.template.PlaceHolderResolver;

import java.util.Collection;

/**
 * <p>
 * A resolver which handles the placeholder resolving functionality for email
 * </p>
 *
 * @author  Mohammed Asif J
 * @version 1.0
 */
public interface MailPlaceHolderResolver extends PlaceHolderResolver {

    /**
     * <p>
     * Fetches the {@link MailPlaceHolderResolver} instance
     * </p>
     *
     * @return the instance of {@link MailPlaceHolderResolver}
     */
    static MailPlaceHolderResolver getInstance() {
        return MailPlaceHolderResolverImpl.getInstance();
    }

    /**
     * <p>
     * Resolves the {@link PlaceHolder} values provided
     * </p>
     *
     * @param content      Refers to the html content with input tags to be resolved
     * @param placeHolders Represents the {@link Collection} of {@link PlaceHolder}
     * @return {@link String} resolved content
     */
    String resolveWithInputTags(final String content, final Collection<PlaceHolder> placeHolders);

    /**
     * <p>
     * Removes the {@link PlaceHolder} tags in the provided content.
     * </p>
     *
     * @param content Refers to the content for which tags are to be removed
     * @return {@link String} resolved content
     */
    String removePlaceHolderTag(final String content);
}
