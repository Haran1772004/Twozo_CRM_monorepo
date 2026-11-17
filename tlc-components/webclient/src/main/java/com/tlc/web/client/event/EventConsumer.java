package com.tlc.web.client.event;

/**
 * A consumer tha consumes the published content
 *
 * @param <T> Type of content to be consumed
 * @author Selvakumar
 * @version 1.0
 */
public interface EventConsumer<T> {

    /**
     * Consumes the published content
     *
     * @param data Data to be consumed
     */
    <R> R consume(T data);
}
