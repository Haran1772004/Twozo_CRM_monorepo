package com.tlc.crm.entity.filter.api.models;

/**
 * <p>
 * A model that represents the filter option data.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class CriteriaOption extends CriteriaField {

    public CriteriaOption() {
        super(false);
    }

    private SourceType sourceType;

    public SourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(final SourceType sourceType) {
        this.sourceType = sourceType;
    }
}
