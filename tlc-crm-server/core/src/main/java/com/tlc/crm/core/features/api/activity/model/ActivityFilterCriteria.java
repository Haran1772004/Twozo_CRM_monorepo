package com.tlc.crm.core.features.api.activity.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.entity.internal.status.ActivityErrorCodes;
import com.tlc.crm.entity.sql.resource.OMACACTIVITY;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * <p>
 * An Enum containing Activity filter criteria types
 * </p>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public enum ActivityFilterCriteria {

    ALL_ACTIVITIES((short) 1, "allActivities", OrderByClause.OrderType.ASCENDING),
    UPCOMING_ACTIVITIES((short) 2, "upcomingActivities", OrderByClause.OrderType.ASCENDING),
    COMPLETED_ACTIVITIES((short) 3, "completedActivities", OrderByClause.OrderType.DESCENDING),
    OVERDUE_ACTIVITIES((short) 4, "overdueActivities", OrderByClause.OrderType.ASCENDING),
    ACTIVITIES_TO_DO((short) 5, "activitiesToDo", OrderByClause.OrderType.ASCENDING);

    private final Short id;
    private final String name;
    private final OrderByClause.OrderType orderType;

    ActivityFilterCriteria(final Short id, final String name, final OrderByClause.OrderType orderType) {
        this.id = id;
        this.name = name;
        this.orderType = orderType;
    }

    public Short getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public OrderByClause.OrderType getOrderType() {
        return orderType;
    }

    /**
     * <p>
     * Gets the optional {@link WhereClause}
     * </p>
     *
     * @return The {@link WhereClause}
     */
    public Optional<WhereClause> getWhereClause() {
        final Table activityTable = Table.get(OMACACTIVITY.TABLE);
        final long currentTime = Instant.now().toEpochMilli();

        return switch (this) {
            case UPCOMING_ACTIVITIES -> Optional.of(new WhereClause(Criteria.eq(activityTable.getColumn(OMACACTIVITY.IS_DONE), false))
                    .and(new WhereClause(Criteria.ge(activityTable.getColumn(OMACACTIVITY.START_TIME), currentTime))));
            case OVERDUE_ACTIVITIES -> Optional.of(new WhereClause(Criteria.eq(activityTable.getColumn(OMACACTIVITY.IS_DONE), false))
                    .and(new WhereClause(Criteria.le(activityTable.getColumn(OMACACTIVITY.START_TIME), currentTime))));
            case COMPLETED_ACTIVITIES -> Optional.of(new WhereClause(Criteria.eq(activityTable.getColumn(OMACACTIVITY.IS_DONE), true)));
            case ACTIVITIES_TO_DO -> Optional.of(new WhereClause(Criteria.eq(activityTable.getColumn(OMACACTIVITY.IS_DONE), false)));
            default -> Optional.empty();
        };
    }

    /**
     * <p>
     * Gets the {@link ActivityFilterCriteria} with name
     * </p>
     *
     * @param criteriaType The {@link ActivityFilterCriteria} name
     * @return The {@link ActivityFilterCriteria}
     */
    public static ActivityFilterCriteria get(final String criteriaType) {
        return switch (criteriaType.toLowerCase()) {
            case "allactivities" -> ALL_ACTIVITIES;
            case "upcomingactivities" -> UPCOMING_ACTIVITIES;
            case "completedactivities" -> COMPLETED_ACTIVITIES;
            case "overdueactivities" -> OVERDUE_ACTIVITIES;
            case "activitiestodo" -> ACTIVITIES_TO_DO;
            default -> throw  ErrorCode.get(ActivityErrorCodes.ACTIVITY_FILTER_CRITERIA_NOT_FOUND, "i18n.activity.filter.criteria.not.found");
        };
    }

    /**
     * <p>
     * Gets the dependent criteria list
     * </p>
     *
     * @param activityFilterCriteria Represents the {@link ActivityFilterCriteria}
     * @return The collection of dependent {@link ActivityFilterCriteria}
     */
    public static Collection<ActivityFilterCriteria> getDependentCriteriaList(final ActivityFilterCriteria activityFilterCriteria) {
        return switch (activityFilterCriteria) {
            case ALL_ACTIVITIES -> List.of(ActivityFilterCriteria.OVERDUE_ACTIVITIES, ActivityFilterCriteria.UPCOMING_ACTIVITIES, ActivityFilterCriteria.COMPLETED_ACTIVITIES);
            default -> Collections.emptyList();
        };
    }
}