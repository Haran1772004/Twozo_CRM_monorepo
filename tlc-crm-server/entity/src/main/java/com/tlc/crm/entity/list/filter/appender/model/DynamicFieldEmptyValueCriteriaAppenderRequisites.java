package com.tlc.crm.entity.list.filter.appender.model;

import com.tlc.crm.entity.list.filter.source.FilterSourceType;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.WhereClause;

/**
 * Represents filtering requirements specifically for handling empty or null values in a dynamic field.
 * <p>
 * This class extends {@link CriteriaAppenderRequisites} and adds field-specific metadata and
 * where-clauses used to build dynamic filtering logic.
 * </p>
 *
 * <p>Use {@link Builder} to construct instances of this class.</p>
 *
 * Example:
 * - Filter records where a comparator is empty or not empty.
 * - Apply positive/negative filtering conditions dynamically.
 *
 * @author Sathish
 * @version 1.0
 */
public class DynamicFieldEmptyValueCriteriaAppenderRequisites extends CriteriaAppenderRequisites {

    private FieldInfo fieldInfo;
    private FilterSourceType relatedSourceType;
    private WhereClause positiveCriteria;
    private WhereClause negativeCriteria;

    private DynamicFieldEmptyValueCriteriaAppenderRequisites(final Column column) {
        super(column);
    }

    public FieldInfo getFieldInfo() {
        return fieldInfo;
    }

    public FilterSourceType getRelatedSourceType() {
        return relatedSourceType;
    }

    public WhereClause getPositiveCriteria() {
        return positiveCriteria;
    }

    public WhereClause getNegativeCriteria() {
        return negativeCriteria;
    }

    public static class Builder {

        private FieldInfo fieldInfo;
        private FilterSourceType relatedSourceType;
        private WhereClause positiveCriteria;
        private WhereClause negativeCriteria;

        public Builder setFieldInfo(final FieldInfo fieldInfo) {
            this.fieldInfo = fieldInfo;
            return this;
        }

        public Builder setRelatedSourceType(final FilterSourceType relatedSourceType) {
            this.relatedSourceType = relatedSourceType;
            return this;
        }

        public Builder setPositiveCriteria(final WhereClause positiveCriteria) {
            this.positiveCriteria = positiveCriteria;
            return this;
        }

        public Builder setNegativeCriteria(final WhereClause negativeCriteria) {
            this.negativeCriteria = negativeCriteria;
            return this;
        }

        public DynamicFieldEmptyValueCriteriaAppenderRequisites build(final Column column) {
            final DynamicFieldEmptyValueCriteriaAppenderRequisites dynamicFieldEmptyValueCriteriaRequisites = new DynamicFieldEmptyValueCriteriaAppenderRequisites(column);

            dynamicFieldEmptyValueCriteriaRequisites.fieldInfo = fieldInfo;
            dynamicFieldEmptyValueCriteriaRequisites.relatedSourceType = relatedSourceType;
            dynamicFieldEmptyValueCriteriaRequisites.positiveCriteria = positiveCriteria;
            dynamicFieldEmptyValueCriteriaRequisites.negativeCriteria = negativeCriteria;

            return dynamicFieldEmptyValueCriteriaRequisites;
        }
    }
}
