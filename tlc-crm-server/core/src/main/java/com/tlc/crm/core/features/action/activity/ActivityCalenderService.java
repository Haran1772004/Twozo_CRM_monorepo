package com.tlc.crm.core.features.action.activity;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.core.features.api.activity.model.ActivityHolder;
import com.tlc.crm.core.internal.activity.handler.ActivityCalendarSyncHandler;
import com.tlc.crm.entity.features.api.activity.manager.ActivityManager;
import com.tlc.crm.entity.features.api.activity.manager.ActivityTypeManager;
import com.tlc.crm.entity.features.api.activity.model.Activity;
import com.tlc.crm.entity.features.api.activity.model.ActivityType;
import com.tlc.crm.entity.features.field.activity.ActivityFields;
import com.tlc.notification.model.email.EmailAddress;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * Provides the service for activity calendar actions.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.1
 */
public final class ActivityCalenderService {

    private final ActivityTypeManager activityTypeManager;
    private final ActivityManager activityManager;

    private ActivityCalenderService() {
        this.activityTypeManager = ActivityTypeManager.getInstance();
        this.activityManager = ActivityManager.getInstance();
    }

    /**
     * <p>
     * Singleton holder for lazy initialization.
     * </p>
     */
    private static class Instance {
        private static final ActivityCalenderService INSTANCE = new ActivityCalenderService();
    }

    /**
     * <p>
     * Provides the singleton instance of {@link ActivityCalenderService}.
     * </p>
     *
     * @return The singleton handler instance.
     */
    public static ActivityCalenderService getInstance() {
        return ActivityCalenderService.Instance.INSTANCE;
    }

    /**
     * <p>
     * Sets the event id in the activity
     * </p>
     *
     * @param orgId           Represents an organization id.
     * @param activityHolders Represents the collection of {@link ActivityHolder}
     * @param action          Represents the {@link Action}.
     */
    public void setEventIdsToActivities(final long orgId, final Collection<ActivityHolder> activityHolders, final Action action) {
        final Optional<ActivityHolder> activityHolder = activityHolders.stream().findFirst();

        if (activityHolder.isPresent()) {
            final Long creatorId = activityHolder.get().getActivity().getCreatorId();
            final String syncedCalendarEmailAddress = activityManager.getSyncedCalendarEmailAddress(orgId, creatorId);

            if (Objects.nonNull(syncedCalendarEmailAddress)) {
                final EmailAddress emailAddress = new EmailAddress(syncedCalendarEmailAddress);
                final String calendarSyncConfig = activityManager.getCalendarSyncConfig(orgId, creatorId, emailAddress);

                if (Objects.nonNull(calendarSyncConfig)) {
                    final JsonObject configJson = Json.object(calendarSyncConfig);
                    final Set<Long> typeIds = configJson.getJsonArray(ActivityFields.ACTIVITY_TYPES.getName()).toList()
                            .stream().map(typeId -> ((Integer) typeId).longValue()).collect(Collectors.toUnmodifiableSet());
                    final Collection<Long> activityTypeIds = activityTypeManager.get(orgId, typeIds).stream()
                            .map(ActivityType::id).collect(Collectors.toUnmodifiableSet());

                    for (final ActivityHolder holder : activityHolders) {
                        final Activity activity = holder.getActivity();
                        final Long typeId = activity.getType().id();

                        if (Objects.requireNonNull(action) == Action.DELETE) {

                            if (activityTypeIds.contains(typeId)) {
                                activity.setEventId(activityManager.getEventId(orgId, holder.getActivity().id()));
                            }
                        }
                    }
                }
            }
        }
    }
}