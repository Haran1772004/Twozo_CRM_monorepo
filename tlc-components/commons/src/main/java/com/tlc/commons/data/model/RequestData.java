package com.tlc.commons.data.model;

import java.util.Deque;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Represents a request containing raw JSON data.
 * Provides access to the JSON payload delivered by the request.
 *
 * @author Sathish
 * @version 1.0
 */
public class RequestData<T> {

    private final T data;
    private final Map<String, Deque<String>> params;

    public RequestData(final T data, final Map<String, Deque<String>> params) {
        this.data = data;
        this.params = params;
    }

    public T getData() {
        return data;
    }

    public Stream<String> getParams(final String name) {
        return params.get(name).stream();
    }

    public Optional<String> getParam(final String name) {
        return params.containsKey(name) ? Optional.ofNullable(params.get(name).getFirst()) : Optional.empty();
    }
}
