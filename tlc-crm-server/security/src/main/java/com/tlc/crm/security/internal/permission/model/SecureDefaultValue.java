package com.tlc.crm.security.internal.permission.model;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.security.permission.SecureDefaultValueProviderRegistry;

import java.util.Optional;

/**
 * Represents an alternative value for columns based on the permission constraints
 *
 * @author sundar
 * @version 1.0
 */
public class SecureDefaultValue {

    private static final String TYPE = "type";
    private static final String PROVIDER = "provider";
    private static final String VALUE_KEY = "value";

    private final Object value;
    private final SecureDefaultValue.Type valueType;
    private String providerName;

    public SecureDefaultValue(final JsonObject valueJson) {
        if (valueJson.containsKey(VALUE_KEY)) {
            final Object value = valueJson.get(VALUE_KEY);

            if (value instanceof final String stringValue) {
                this.value = stringValue.isEmpty() ? null : stringValue;
            } else {
                this.value = value;
            }
            this.valueType = Type.DIRECT;
        } else {
            this.valueType = Type.valueOf(valueJson.getString(TYPE));
            this.providerName = valueJson.getString(PROVIDER);
            this.value = valueJson.opt(VALUE_KEY);
        }
    }

    public SecureDefaultValue(final String providerName) {
        this.providerName = providerName;
        this.valueType = Type.CUSTOMIZED;
        this.value = null;
    }

    /**
     * Gets the value
     *
     * @return the value
     */
    public Optional<Object> getValue() {
        return Optional.ofNullable(switch (valueType) {
            case DIRECT -> value;
            case CUSTOMIZED -> getCustomValue();
        });
    }

    /**
     * Gets the custom value
     *
     * @return the custom value
     */
    protected Object getCustomValue() {
        return SecureDefaultValueProviderRegistry.get().get(providerName).get();
    }

    /**
     * Represents the type of the default value
     *
     * @author sundar
     * @version 1.0
     */
    enum Type {

        /**
         * Directly typed values that can be used directly.
         */
        DIRECT,

        /**
         * Values obtained through desired logic.
         */
        CUSTOMIZED
    }
}