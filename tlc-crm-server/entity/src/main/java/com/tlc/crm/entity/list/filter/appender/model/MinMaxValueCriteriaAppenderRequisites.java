package com.tlc.crm.entity.list.filter.appender.model;

import com.tlc.crm.entity.list.filter.models.JoinPriorityDetails;
import com.tlc.crm.entity.list.filter.source.FilterSourceType;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.WhereClause;

/**
 * Represents the filtering configuration required for applying min/max (range-based) constraints
 * to a particular column within dynamic filter queries.
 *
 * <p>
 * This class encapsulates metadata such as the source type, whether to use alias tables,
 * join preferences, and the field being evaluated.
 * </p>
 *
 * <p>
 * Use the {@link Builder} class to fluently construct an instance.
 * </p>
 *
 * Example use cases:
 * <ul>
 *     <li>Filtering records within a numerical or date</li>
 *     <li>Dynamic construction of WHERE clause conditions with alias usage</li>
 * </ul>
 *
 * @author Sathish
 * @version 1.0
 */
public class MinMaxValueCriteriaAppenderRequisites extends CriteriaAppenderRequisites {

    private FieldInfo fieldInfo;
    private boolean useAliasTable;
    private WhereClause criteria;
    private FilterSourceType relatedSourceType;
    private JoinPriorityDetails joinPriorityDetails;

    private MinMaxValueCriteriaAppenderRequisites(final Column column) {
        super(column);
    }

    public FieldInfo getFieldInfo() {
        return fieldInfo;
    }

    public boolean useAliasTable() {
        return useAliasTable;
    }

    public WhereClause getCriteria() {
        return criteria;
    }

    public FilterSourceType getRelatedSourceType() {
        return relatedSourceType;
    }

    public JoinPriorityDetails getJoinPriorityDetails() {
        return joinPriorityDetails;
    }

    public static class Builder {

        private FieldInfo fieldInfo;
        private boolean useAliasTable;
        private WhereClause criteria;
        private FilterSourceType relatedSourceType;
        private JoinPriorityDetails joinPriorityDetails;

        public Builder setFieldInfo(final FieldInfo fieldInfo) {
            this.fieldInfo = fieldInfo;
            return this;
        }

        public Builder setUseAliasTable(final boolean useAliasTable) {
            this.useAliasTable = useAliasTable;
            return this;
        }

        public Builder setCriteria(final WhereClause criteria) {
            this.criteria = criteria;
            return this;
        }

        public Builder setRelatedSourceType(final FilterSourceType relatedSourceType) {
            this.relatedSourceType = relatedSourceType;
            return this;
        }

        public Builder setJoinPriorityDetails(final JoinPriorityDetails joinPriorityDetails) {
            this.joinPriorityDetails = joinPriorityDetails;
            return this;
        }

        public MinMaxValueCriteriaAppenderRequisites build(final Column column) {
            final MinMaxValueCriteriaAppenderRequisites emptyValueCriteriaRequisites = new MinMaxValueCriteriaAppenderRequisites(column);

            emptyValueCriteriaRequisites.fieldInfo = fieldInfo;
            emptyValueCriteriaRequisites.useAliasTable = useAliasTable;
            emptyValueCriteriaRequisites.criteria = criteria;
            emptyValueCriteriaRequisites.relatedSourceType = relatedSourceType;
            emptyValueCriteriaRequisites.joinPriorityDetails = joinPriorityDetails;

            return emptyValueCriteriaRequisites;
        }
    }
}
