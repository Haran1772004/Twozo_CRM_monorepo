package com.tlc.notification.model.template;

/**
 * <p>
 * An entity to represent the placeholder fields
 * </p>
 *
 * @author AswiniN
 * @version 1.0
 */
public abstract class PlaceHolder {

    private String key;
    private Object value;

    public String getKey() {
        return key;
    }

    public void setKey(final String key) {
        this.key = key;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(final Object value) {
        this.value = value;
    }
}