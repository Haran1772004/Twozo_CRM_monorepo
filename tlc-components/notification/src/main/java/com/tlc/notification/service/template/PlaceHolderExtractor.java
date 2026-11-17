package com.tlc.notification.service.template;

import com.tlc.notification.model.template.Template;
import com.tlc.notification.model.template.PlaceHolder;

import java.util.Collection;

/**
 * Extractor responsible for processing the given content of template and provides the
 * {@link Collection} of placeholders.
 *
 * @author AswiniN
 * @version 1.0
 */
public interface PlaceHolderExtractor<T extends Template> {

    /**
     * <p>
     * Fetches the {@link PlaceHolder} for the provided template
     * </p>
     *
     * @param template Refers to the template
     * @return {@link Collection} of {@link PlaceHolder}
     */
    Collection<? extends PlaceHolder> getPlaceHolders(T template);
}