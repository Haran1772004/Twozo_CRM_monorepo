package com.tlc.crm.entity.list.filter.appender.model;

import com.tlc.sql.common.api.dml.Column;

/**
 * Represents a filter criteria used specifically for nullable fields.
 *
 * <p>This class extends {@link CriteriaAppenderRequisites} to encapsulate nullability-based filtering.</p>
 *
 * <p>Use the {@link Builder} class to create instances of this criteria.</p>
 *
 * @author Sathish
 * @version 1.0
 */
public class NullableFieldCriteriaAppenderRequisites extends CriteriaAppenderRequisites {

    private final boolean validateEmptyValueCriteria;

    private NullableFieldCriteriaAppenderRequisites(final Column column, final boolean validateEmptyValueCriteria) {
        super(column);
        this.validateEmptyValueCriteria = validateEmptyValueCriteria;
    }

    public boolean validateEmptyValueCriteria() {
        return validateEmptyValueCriteria;
    }

    public static class Builder {

        private boolean validateEmptyValueCriteria;

        public void setValidateEmptyValueCriteria(final boolean validateEmptyValueCriteria) {
            this.validateEmptyValueCriteria = validateEmptyValueCriteria;
        }

        public NullableFieldCriteriaAppenderRequisites build(final Column column) {
            return new NullableFieldCriteriaAppenderRequisites(column, validateEmptyValueCriteria);
        }
    }
}
