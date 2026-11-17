package com.tlc.crm.core.segment.builder;

import com.tlc.crm.core.segment.models.Segment;
import com.tlc.crm.core.segment.models.SegmentCondition;
import com.tlc.crm.core.segment.models.SegmentConditionCriteria;
import com.tlc.crm.core.segment.models.SegmentConditionGroup;
import com.tlc.crm.entity.filter.api.models.FilterConnector;
import com.tlc.crm.entity.filter.api.models.FilterCriteria;
import com.tlc.crm.entity.list.filter.handler.FilterCriteriaHandler;
import com.tlc.crm.entity.list.model.Provider;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Connector;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.MultiQuery;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.util.WhereClauseUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * Builds grouped filter queries for segments. Segments have groups, where each group
 * contains multiple criteria. This builder processes each group separately, builds
 * a WHERE clause per group, then combines groups using their connectors to preserve
 * proper grouped boolean logic: (Group1 Criteria) <connector> (Group2 Criteria).
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public final class SegmentCriteriaBuilder {

    private final FilterCriteriaHandler filterCriteriaHandler;

    private SegmentCriteriaBuilder() {
        this.filterCriteriaHandler = FilterCriteriaHandler.getInstance();
    }

    private static class InstanceHolder {
        private static final SegmentCriteriaBuilder INSTANCE = new SegmentCriteriaBuilder();
    }

    public static SegmentCriteriaBuilder getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * <p>
     * Applies the segment criteria based on their groups.
     * </p>
     *
     * @param orgId      Represents the organization id.
     * @param provider   Represents the provider (e.g., CONTACT_LIST).
     * @param multiQuery Represents the query to which grouped criteria will be applied.
     * @param segment    Represents the segment whose groups are processed.
     */
    public void applySegmentCriteria(final Long orgId, final Provider provider, final MultiQuery multiQuery, final Segment segment) {
        final Collection<SegmentConditionGroup> segmentConditionGroups = segment.getSegmentConditionGroups();

        if (Objects.isNull(segmentConditionGroups) || segmentConditionGroups.isEmpty()) {
            return;
        }
        WhereClause criteria = null;
        final Table baseTable = multiQuery.getBaseTable();

        for (final SegmentConditionGroup segmentConditionGroup : segmentConditionGroups) {
            final Collection<SegmentCondition> segmentGroupConditions = segmentConditionGroup.getConditions();

            if (Objects.isNull(segmentGroupConditions) || segmentGroupConditions.isEmpty()) {
                continue;
            }
            final Collection<FilterCriteria> groupCriteriaList = buildGroupCriteriaList(segmentConditionGroup);

            if (groupCriteriaList.isEmpty()) {
                continue;
            }
            final SelectQuery selectQuery = SelectQuery.get(baseTable);
            final Column baseTablePkColumn = baseTable.getPKColumn();

            selectQuery.addSelectClause(baseTablePkColumn);
            filterCriteriaHandler.applyCriteria(orgId, provider, selectQuery, groupCriteriaList);
            final WhereClause whereClause = new WhereClause(Criteria.in(baseTablePkColumn, selectQuery));

            if (Objects.isNull(criteria)) {
                criteria = whereClause;
            } else {
                final FilterConnector filterConnector = segmentConditionGroup.getConnector();
                final Connector connector = Objects.nonNull(filterConnector) ? filterConnector.getConnector() : Connector.AND;

                if (Objects.equals(Connector.OR, connector)) {
                    criteria = WhereClauseUtil.connect(criteria, whereClause, Connector.OR);
                } else {
                    criteria = WhereClauseUtil.connect(criteria, whereClause, Connector.AND);
                }
            }
        }

        if (Objects.nonNull(criteria)) {
            multiQuery.setWhereClause(criteria);
        }
    }

    /**
     * <p>
     * Builds the flat filter criteria list for a single segment group by walking its
     * conditions and resolving each condition's connector.
     * </p>
     *
     * @param segmentConditionGroup Represents the segment group whose conditions are walked.
     * @return a flat, ordered {@link Collection} of {@link FilterCriteria} for the group.
     */
    private Collection<FilterCriteria> buildGroupCriteriaList(final SegmentConditionGroup segmentConditionGroup) {
        final Collection<FilterCriteria> groupCriteriaList = new ArrayList<>();
        final Collection<SegmentCondition> segmentConditions = segmentConditionGroup.getConditions();

        if (Objects.isNull(segmentConditions) || segmentConditions.isEmpty()) {
            return groupCriteriaList;
        }

        boolean firstConditionInGroup = true;

        for (final SegmentCondition segmentCondition : segmentConditionGroup.getConditions()) {
            final SegmentCondition.Type segmentConditionType = segmentCondition.getType();

            if (Objects.isNull(segmentConditionType)) {
                continue;
            }

            switch (segmentConditionType) {
                case CONTACT_PROPERTY, COMPANY_PROPERTY -> {
                    final SegmentConditionCriteria conditionCriteria = segmentCondition.getConditionCriteria();
                    final FilterCriteria filterCriteria;

                    if (Objects.nonNull(conditionCriteria) && Objects.nonNull(filterCriteria = conditionCriteria.getFilterCriteria())) {
                        filterCriteria.setConnector(firstConditionInGroup ? null
                                : Objects.requireNonNullElse(segmentCondition.getConnector(), FilterConnector.AND));

                        groupCriteriaList.add(filterCriteria);
                        firstConditionInGroup = false;
                    }
                }
            }
        }

        return groupCriteriaList;
    }
}