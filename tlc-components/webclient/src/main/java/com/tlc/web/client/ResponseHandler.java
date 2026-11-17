package com.tlc.web.client;

/**
 * A handler that handles the response
 *
 * @param <T> Input type
 * @param <R> Output type
 * @author Selvakumar
 * @version 1.0
 */
@FunctionalInterface
public interface ResponseHandler<T, R> {

    /**
     * Handles the data and builds the response
     *
     * @param data Data that has to be handled
     * @return Response of the handler
     */
    R handle(T data);

}
