package com.tlc.crm.core.list.dynamic;

import com.tlc.crm.core.features.api.activity.manager.SourceActivityManager;
import com.tlc.crm.core.list.dynamic.model.ListDynamicFields;
import com.tlc.crm.entity.features.api.activity.model.Activity;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.list.dynamic.model.DynamicFieldRequest;
import com.tlc.crm.entity.list.dynamic.model.DynamicFieldsData;

import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * Implements {@link DynamicFieldsFetcher} the for fetching dynamic field data to activity
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public class ActivityDynamicFieldsFetcher extends DynamicFieldsFetcher {

    private final SourceActivityManager sourceActivityManager;

    private ActivityDynamicFieldsFetcher() {
        sourceActivityManager = SourceActivityManager.getInstance();
    }

    public static class InstanceHolder {
        private static final ActivityDynamicFieldsFetcher INSTANCE = new ActivityDynamicFieldsFetcher();
    }

    /**
     * <p>
     * Gets the {@link ActivityDynamicFieldsFetcher} instance
     * </p>
     *
     * @return The {@link ActivityDynamicFieldsFetcher}
     */
    public static ActivityDynamicFieldsFetcher getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param dynamicFieldRequest Represents the {@link DynamicFieldRequest}
     * @return The {@link DynamicFieldsData}
     */
    @Override
    public DynamicFieldsData get(final DynamicFieldRequest dynamicFieldRequest) {
        final Collection<String> activityFields = getActivityFields();
        final long orgId = dynamicFieldRequest.orgId();
        final Collection<Long> entityIds = dynamicFieldRequest.entityIds();
        final Source source = dynamicFieldRequest.provider().getSource();
        final Collection<String> dynamicFields = dynamicFieldRequest.dynamicFields();
        final boolean activityFieldRequired = dynamicFields.stream().anyMatch(activityFields::contains);
        final DynamicFieldsData dynamicFieldsData =  new DynamicFieldsData();

        if (activityFieldRequired) {
            final Map<Long, Collection<Activity>> activitiesMap = sourceActivityManager.fetchActivitiesBySource(orgId, entityIds, source);

            if (!activitiesMap.isEmpty()) {
                final Map<String, Map<String, Object>> entityRows = new HashMap<>();

                entityIds.forEach(id -> {

                    if (activitiesMap.containsKey(id)) {
                        final Map<String, Object> entityRow = new HashMap<>();
                        final Collection<Activity> activities = activitiesMap.get(id);

                        for (final String fieldName : dynamicFields) {

                            switch (fieldName) {
                                case ListDynamicFields.LAST_ACTIVITY_DATE -> {
                                    final long currentTime = Instant.now().toEpochMilli();

                                    activities.stream().filter(activity -> currentTime >= activity.getStartTime().getTimeStamp() && activity.getDone())
                                            .map(activity -> activity.getStartTime().getTimeStamp()).max(Long::compareTo)
                                            .ifPresent(lastActivityDate -> entityRow.put(fieldName, lastActivityDate));
                                }
                                case ListDynamicFields.NEXT_ACTIVITY_DATE -> {
                                    final long currentTime = Instant.now().toEpochMilli();

                                    activities.stream().filter(activity -> currentTime < activity.getStartTime().getTimeStamp() && !activity.getDone())
                                            .map(activity -> activity.getStartTime().getTimeStamp()).min(Long::compareTo)
                                            .ifPresent(nextActivityDate -> entityRow.put(fieldName, nextActivityDate));
                                }
                                case ListDynamicFields.LAST_ACTIVITY_TYPE -> {
                                    final long currentTime = Instant.now().toEpochMilli();

                                    activities.stream().filter(activity -> currentTime > activity.getStartTime().getTimeStamp() && activity.getDone())
                                            .max(Comparator.comparing(activity -> activity.getStartTime().getTimeStamp()))
                                            .ifPresent(activity -> entityRow.put(fieldName, activity.getType().getName()));
                                }
                                case ListDynamicFields.UPCOMING_ACTIVITIES -> {
                                    final long upcomingActivities = activities.stream()
                                            .filter(activity -> !activity.getDone() && activity.getStartTime().getTimeStamp() > Instant.now().toEpochMilli())
                                            .count();

                                    if (upcomingActivities > 0) {
                                        entityRow.put(fieldName, upcomingActivities);
                                    }
                                }
                                case ListDynamicFields.DONE_ACTIVITIES -> {
                                    final long doneActivities = activities.stream().filter(Activity::getDone).count();

                                    if (doneActivities > 0) {
                                        entityRow.put(fieldName, doneActivities);
                                    }
                                }
                                case ListDynamicFields.TOTAL_ACTIVITIES -> {
                                    final int totalActivities = activities.size();

                                    if (totalActivities > 0) {
                                        entityRow.put(fieldName, totalActivities);
                                    }
                                }
                            }
                        }
                        entityRows.put(id.toString(), entityRow);
                    }
                });
                dynamicFieldsData.addAll(entityRows);
            }
        }
        return dynamicFieldsData;
    }

    /**
     * <p>
     * Fetches the collections of activity fields
     * </p>
     *
     * @return The collection of activity fields
     */
    private Collection<String> getActivityFields() {
        return Set.of(ListDynamicFields.LAST_ACTIVITY_TYPE, ListDynamicFields.NEXT_ACTIVITY_DATE, ListDynamicFields.LAST_ACTIVITY_DATE, ListDynamicFields.DONE_ACTIVITIES, ListDynamicFields.TOTAL_ACTIVITIES, ListDynamicFields.UPCOMING_ACTIVITIES);
    }
}