package com.tlc.crm.entity.filter.api.models;

import com.tlc.crm.entity.list.filter.source.FilterSourceType;

/**
 * <p>
 * A model that represents the source type data.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class SourceType {

    private Long id;
    private FilterSourceType source;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public FilterSourceType getSource() {
        return source;
    }

    public void setSource(final FilterSourceType source) {
        this.source = source;
    }
}
