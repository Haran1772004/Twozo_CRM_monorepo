package com.tlc.crm.entity.filter.api.models;

/**
 * <p>
 * A model that represents the value data.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class CriteriaValue {

    private Long id;
    private Long valueEntityId;
    private Object value;

    public Long getId() {
        return id;
    }

    public void setId(final long id) {
        this.id = id;
    }

    public Long getValueEntityId() {
        return valueEntityId;
    }

    public void setValueEntityId(final long valueEntityId) {
        this.valueEntityId = valueEntityId;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(final Object value) {
        this.value = value;
    }
}
