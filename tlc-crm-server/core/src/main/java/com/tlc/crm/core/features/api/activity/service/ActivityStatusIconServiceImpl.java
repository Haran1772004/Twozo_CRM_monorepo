package com.tlc.crm.core.features.api.activity.service;

import com.tlc.commons.Counter;
import com.tlc.commons.service.registry.auto.service.AutoServiceRegistrar;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.entity.features.api.activity.model.Activity;
import com.tlc.crm.entity.features.field.activity.ActivityFields;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.service.ActivityStatusIconService;
import com.tlc.crm.entity.service.registry.ActivityStatusFetchServiceRegistry;
import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.l10n.temporal.model.TimezoneInfo;
import com.tlc.l10n.temporal.resolver.DateTimeUtil;
import com.tlc.l10n.temporal.service.TemporalService;

import java.time.Instant;
import java.util.Collection;

/**
 * <p>
 * Pertains to Activity status icon for entities.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
@AutoServiceRegistrar(registryClazz = ActivityStatusFetchServiceRegistry.class)
public class ActivityStatusIconServiceImpl implements ActivityStatusIconService {

    private final DateTimeUtil dateTimeUtil;

    private ActivityStatusIconServiceImpl() {
        dateTimeUtil = DateTimeUtil.getInstance();
    }

    private static class Instance {
        private static final ActivityStatusIconServiceImpl INSTANCE = new ActivityStatusIconServiceImpl();
    }

    /**
     * <p>
     * Gets {@link ActivityStatusIconServiceImpl}.
     * </p>
     *
     * @return {@link ActivityStatusIconServiceImpl}.
     */
    public static ActivityStatusIconServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Sets the activity icon type related to the deal.
     * </p>
     *
     * @param activities   Represents the collection of {@link Activity}
     * @param timezoneInfo Represents the {@link TimezoneInfo}
     * @return the activity icon type related to the deal.
     */
    public String getIconTypeName(final Collection<Activity> activities, final TimezoneInfo timezoneInfo) {
        final String activityIconType;

        if (activities.isEmpty()) {
            activityIconType = ActivityFields.NO_ACTIVITIES.getName();
        } else {
            final Instant instant = Instant.now();
            final long currentTime = instant.toEpochMilli();
            final Counter overDueActivitiesCount = new Counter();
            final Counter todayActivitiesCount = new Counter();
            final TemporalService temporalService = TemporalServiceProvider.get();
            final long startTime = temporalService.getStartTimeOfDay(timezoneInfo);
            final long endTime = temporalService.getEndTimeOfDay(timezoneInfo);

            for (final Activity activity : activities) {
                final Long activityStartTime = dateTimeUtil.getTimeStamp(activity.getStartTime());
                final Long activityEndTime = dateTimeUtil.getTimeStamp(activity.getEndTime());

                if (activityEndTime < currentTime) {
                    overDueActivitiesCount.getAndIncrement();
                    break;
                } else if (activityStartTime >= startTime && activityStartTime <= endTime) {
                    todayActivitiesCount.getAndIncrement();
                }
            }

            if (overDueActivitiesCount.getCount() > 0) {
                activityIconType = ActivityFields.OVER_DUE.getName();
            } else if (todayActivitiesCount.getCount() > 0) {
                activityIconType = ActivityFields.SCHEDULED.getName();
            } else {
                activityIconType = ActivityFields.UPCOMING_ACTIVITIES.getName();
            }
        }

        return activityIconType;
    }

    /**
     * <p>
     * Gets the activity status icon type.
     * </p>
     *
     * @param orgId        An organization's id.
     * @param entityId     An entity's id.
     * @param source       {@link Source}.
     * @param timezoneInfo {@link TimezoneInfo}.
     * @return the activity icon type.
     */
    @Override
    public String getIconTypeName(final Long orgId, final Long entityId, final Source source, final TimezoneInfo timezoneInfo) {
        return getIconTypeName(DealManager.getInstance().getActivitiesByDeal(orgId, entityId), timezoneInfo);
    }

    /**
     * <p>
     * Gets {@link FieldKeys}.
     * </p>
     *
     * @return {@link FieldKeys}.
     */
    @Override
    public FieldKeys getType() {
        return FieldKeys.ACTIVITY_STATUS;
    }
}
