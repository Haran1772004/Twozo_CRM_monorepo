package com.tlc.commons.template.service;

import com.tlc.commons.service.Service;

/**
 * <p>
 * A service for processing templates
 * </p>
 *
 * @param <K> the type of the input to the service
 * @param <V> the type of the output from the service
 * @author Kishore J
 * @version 1.0
 */
public interface TemplateService<K, V> extends Service {

    /**
     * <p>
     * Fetches the template wrapper object
     * </p>
     *
     * @param input the input to be processed
     * @return The template wrapper object
     */
    V get(final K input);
}
