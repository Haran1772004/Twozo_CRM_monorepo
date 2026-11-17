package com.tlc.notification.model.call;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Utility class for storing and managing key-value parameters, typically used
 * in query strings, API calls, or dynamic URL construction.
 *
 * Provides functionality to add, retrieve, and format parameters as a string
 * with standard delimiters.
 *
 * Example output format: key1=value1&key2=value2
 *
 * @author Sathish
 * @version 1.0
 */
public class Parameters {

    private static final String EQUAL = "=";
    private static final String PARAM_CONNECTOR = "&";

    private final Map<String, Object> params;

    public Parameters() {
        params = new HashMap<>();
    }

    /**
     * Adds a single parameter to the internal map.
     *
     * @param key the parameter name
     * @param value the parameter value
     */
    public void addParam(final String key, final Object value) {
        params.put(key, value);
    }

    /**
     * Adds all entries from the given map to the internal parameter map.
     * Existing keys will be overwritten.
     *
     * @param params the map of parameters to add
     */
    public void addParams(final Map<String, Object> params) {
        this.params.putAll(params);
    }

    /**
     * Retrieves the value associated with the given key.
     *
     * @param key the name of the parameter
     * @return the corresponding value, or null if not found
     */
    public Object getParam(final String key) {
        return params.get(key);
    }

    /**
     * Returns all parameters formatted as a URL-style query string.
     * Example: key1=value1&key2=value2
     *
     * @return the formatted parameter string
     */
    public String getParamsAsString() {
        final StringBuilder paramBuilder = new StringBuilder();
        final Set<Map.Entry<String, Object>> paramEntries = params.entrySet();
        final Iterator<Map.Entry<String, Object>> paramIterator = paramEntries.iterator();

        while (paramIterator.hasNext()) {
            final Map.Entry<String, Object> parameter = paramIterator.next();

            paramBuilder.append(parameter.getKey()).append(EQUAL).append(parameter.getValue());

            if (paramIterator.hasNext()) {
                paramBuilder.append(PARAM_CONNECTOR);
            }
        }
        return paramBuilder.toString();
    }
}
