package com.tlc.entity.model;

import com.tlc.commons.model.Application;

/**
 * Represents the application details.
 *
 * @author Sathish
 * @version 1.0
 */
public class TwozoApplication implements Application {

    private Long id;
    private String name;

    public void setId(final Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
