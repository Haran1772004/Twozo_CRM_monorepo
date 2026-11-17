package com.tlc.crm.core.features.action.call.model;

/**
 * Represents an outcome for a phone call.
 *
 * @author Sathish
 * @version 1.0
 */
public class Outcome {

    private Long id;
    private String name;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }
}
