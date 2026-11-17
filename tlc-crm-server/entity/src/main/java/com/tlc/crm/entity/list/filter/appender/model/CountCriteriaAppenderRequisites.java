package com.tlc.crm.entity.list.filter.appender.model;

import com.tlc.crm.entity.list.filter.models.JoinPreference;
import com.tlc.crm.entity.list.filter.models.JoinPriorityDetails;
import com.tlc.crm.entity.list.filter.source.FilterSourceType;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.common.util.NumberUtils;
import com.tlc.sql.common.api.dml.Column;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Represents a specific set of filtering requirements based on a count-based condition,
 * including filtering logic tied to nullable criteria, join priority, and related source types.
 *
 * <p>This class extends {@link CriteriaAppenderRequisites} to define additional filtering metadata
 * specific to count-based queries or constraints.</p>
 *
 * <p>Use the nested {@link Builder} class to construct instances fluently.</p>
 *
 * @author Sathish
 * @version 1.0
 */
public class CountCriteriaAppenderRequisites extends CriteriaAppenderRequisites {

    private long count;
    private FilterSourceType relatedSourceType;
    private boolean isNullableCriteriaRequired;
    private Collection<JoinPriorityDetails> priorityDetailHolders;
    private FieldInfo fieldInfo;

    private CountCriteriaAppenderRequisites(final Column column) {
        super(column);
        this.isNullableCriteriaRequired = true;
        this.priorityDetailHolders = List.of(new JoinPriorityDetails(JoinPreference.ANY));
    }

    public long getCount() {
        return count;
    }

    public FilterSourceType getRelatedSourceType() {
        return relatedSourceType;
    }

    public boolean isNullableCriteriaRequired() {
        return isNullableCriteriaRequired;
    }

    public Collection<JoinPriorityDetails> getPriorityDetailHolders() {
        return priorityDetailHolders;
    }

    public FieldInfo getFieldInfo() {
        return fieldInfo;
    }

    public static class Builder {

        private long count;
        private FilterSourceType relatedSourceType;
        private boolean isNullableCriteriaRequired;
        private Collection<JoinPriorityDetails> priorityDetailHolders;
        private FieldInfo fieldInfo;

        public Builder setCount(final long count) {
            this.count = count;
            return this;
        }

        public Builder setCount(final Object count) {
            this.count = Objects.isNull(count) ? 0 : NumberUtils.parseNumber(count).longValue();
            return this;
        }

        public Builder setRelatedSourceType(final FilterSourceType relatedSourceType) {
            this.relatedSourceType = relatedSourceType;
            return this;
        }

        public Builder setNullableCriteriaRequired(final boolean nullableCriteriaRequired) {
            isNullableCriteriaRequired = nullableCriteriaRequired;
            return this;
        }

        public Builder setPriorityDetailHolders(final Collection<JoinPriorityDetails> priorityDetailHolders) {
            this.priorityDetailHolders = priorityDetailHolders;
            return this;
        }

        public Builder setFieldInfo(final FieldInfo fieldInfo) {
            this.fieldInfo = fieldInfo;
            return this;
        }

        public CountCriteriaAppenderRequisites build(final Column column) {
            final CountCriteriaAppenderRequisites countCriteriaRequisites = new CountCriteriaAppenderRequisites(column);

            countCriteriaRequisites.count = count;
            countCriteriaRequisites.relatedSourceType = relatedSourceType;
            countCriteriaRequisites.isNullableCriteriaRequired = isNullableCriteriaRequired;
            countCriteriaRequisites.priorityDetailHolders = priorityDetailHolders;
            countCriteriaRequisites.fieldInfo = fieldInfo;

            return countCriteriaRequisites;
        }
    }
}
