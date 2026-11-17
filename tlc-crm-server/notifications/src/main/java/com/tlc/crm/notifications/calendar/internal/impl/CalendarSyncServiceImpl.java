package com.tlc.crm.notifications.calendar.internal.impl;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.iam.SecureUserTaskProcessor;
import com.tlc.crm.core.features.api.activity.manager.SourceActivityManager;
import com.tlc.crm.core.features.api.activity.model.ActivityHolder;
import com.tlc.crm.core.features.api.activity.model.converter.calendar.CalendarEventMapper;

import com.tlc.crm.entity.async.SyncInfo;
import com.tlc.crm.entity.features.api.activity.manager.ActivityManager;
import com.tlc.crm.entity.features.api.activity.model.ActivityType;

import com.tlc.crm.notifications.calendar.api.manager.CalendarSyncManager;
import com.tlc.crm.notifications.calendar.api.model.CalendarSyncSettings;
import com.tlc.crm.notifications.calendar.api.model.internal.CalendarSyncConfig;
import com.tlc.crm.notifications.calendar.fields.CalendarFields;
import com.tlc.crm.notifications.calendar.internal.CalendarSyncService;

import com.tlc.crm.common.sync.SyncMode;
import com.tlc.notification.model.calendar.CalendarEvent;
import com.tlc.notification.model.calendar.CalendarSyncRequest;
import com.tlc.notification.model.email.EmailAddress;
import com.tlc.notification.service.calendar.CalendarService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Provides the service for calendar sync
 * </p>
 *
 * @author Ajith venkadesh, Kowsalya SP
 * @version 1.0
 */
public class CalendarSyncServiceImpl implements CalendarSyncService, SecureUserTaskProcessor {

    private final static Logger LOGGER = LoggerFactory.getLogger(CalendarSyncServiceImpl.class);

    private final SourceActivityManager sourceActivityManager;
    private final CalendarSyncManager calendarSyncManager;
    private final CalendarEventMapper calendarEventMapper;
    private final ActivityManager activityManager;
    private final CalendarService calendarService;

    private CalendarSyncServiceImpl() {
        this.sourceActivityManager = SourceActivityManager.getInstance();
        this.calendarSyncManager = CalendarSyncManager.getInstance();
        this.calendarEventMapper = CalendarEventMapper.getInstance();
        this.activityManager = ActivityManager.getInstance();
        this.calendarService = CalendarService.getInstance();
    }

    private static class Instance {
        private static final CalendarSyncServiceImpl INSTANCE = new CalendarSyncServiceImpl();
    }

    /**
     * <p>
     * Gets the {@link CalendarSyncServiceImpl} Instance
     * </p>
     *
     * @return {@link CalendarSyncServiceImpl}
     */
    public static CalendarSyncServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void syncCalendarEvents() {
        final Collection<CalendarSyncSettings> calendarSyncSettings = calendarSyncManager.retrieveSyncedCalendarData();

        if (!calendarSyncSettings.isEmpty()) {

            for (CalendarSyncSettings calendarSyncSetting : calendarSyncSettings) {
                final Long orgId = calendarSyncSetting.orgId();
                final Long userId = calendarSyncSetting.getUserId();

                secureProcess(orgId, userId, () -> {
                    final String emailId = calendarSyncSetting.getAddress().getEmailId();
                    final EmailAddress emailAddress = new EmailAddress(emailId);
                    final String calendarSyncConfig = activityManager.getCalendarSyncConfig(orgId, userId, emailAddress);
                    final long currentTime = Instant.now().toEpochMilli();

                    if (Objects.nonNull(calendarSyncConfig)) {
                        final JsonObject syncJson = Json.object(calendarSyncConfig).getJsonObject(CalendarFields.SYNC_TYPE);
                        final CalendarSyncRequest calendarSyncRequest = new CalendarSyncRequest.Builder()
                                .emailAddress(emailAddress)
                                .startTime(calendarSyncSetting.getLastSyncTime())
                                .endTime(currentTime)
                                .orgId(orgId)
                                .build();
                        try{
                            final Collection<CalendarEvent> calendarEvents = calendarService.retrieve(calendarSyncRequest);

                            if (!calendarEvents.isEmpty()) {
                                final Collection<ActivityHolder> activityHolders = calendarEventMapper.convertEventsToActivityHolders(orgId, userId,
                                        syncJson.getLong(CalendarFields.CALENDAR_EVENT_ID), calendarEvents);
                                final Long syncId = activityManager.getCalendarSyncId(orgId, userId, true);

                                if (Objects.nonNull(syncId)) {
                                    sourceActivityManager.loadActivityEventIntoContainer(orgId, userId, syncId, activityHolders);
                                }
                            }
                            calendarSyncManager.updateCalendarLastSyncTime(orgId, userId, currentTime);
                            
                            LOGGER.debug("Calendar Sync: Calendar auto sync for retrieve events completed");
                        } catch (final Exception exception) {
                            LOGGER.debug("Calendar Sync: Calendar auto sync for retrieve events failed for orgId: {}, userId: {}, reason: {}",
                                    orgId, userId, exception.getMessage());
                        }
                    }
                });
            }
        }
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param userId   Represents the user id
     * @param syncInfo Represents the {@link SyncInfo}
     */
    @Override
    public void updateCalendarSyncStatus(final long orgId, final long userId, final SyncInfo syncInfo) {
        calendarSyncManager.updateCalendarSyncStatus(orgId, userId, syncInfo);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param syncId Represents the calendar sync id
     * @return {@link SyncInfo}
     */
    @Override
    public SyncInfo getCalendarSyncInfo(final long orgId, final long syncId) {
        return calendarSyncManager.getCalendarSyncInfo(orgId, syncId);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param userId   Represents the user id
     * @param settings Represents the {@link CalendarSyncSettings}
     */
    @Override
    public void importActivityEvents(final long orgId, final long userId, final CalendarSyncSettings settings) {
        final String emailId = calendarSyncManager.getCalendarSyncEmailAddress(orgId, userId, settings.id());
        final EmailAddress emailAddress = new EmailAddress(emailId);
        final CalendarSyncConfig calendarSyncConfig = settings.getCalendarSyncConfig();

        if (Objects.nonNull(calendarSyncConfig)) {
            final Map<SyncMode, ActivityType> calendarSyncModes = calendarSyncConfig.getSyncTypeConfig();
            final long currentTime = Instant.now().toEpochMilli();

            if (!calendarSyncModes.isEmpty()) {
                final SyncMode syncMode = calendarSyncModes.keySet().stream().toList().get(0);

                if (SyncMode.TWO_WAY == syncMode) {
                    final CalendarSyncRequest calendarSyncRequest = new CalendarSyncRequest.Builder()
                            .emailAddress(emailAddress)
                            .startTime(currentTime)
                            .endTime(Instant.now().plus(30, ChronoUnit.DAYS).toEpochMilli())
                            .orgId(orgId)
                            .build();
                    final Collection<CalendarEvent> calendarEvents = calendarService.retrieve(calendarSyncRequest);

                    if (!calendarEvents.isEmpty()) {
                        final Collection<ActivityHolder> activityHolders = calendarEventMapper.convertEventsToActivityHolders(orgId, userId, calendarSyncModes.get(syncMode).id(), calendarEvents);
                        final Long syncId = activityManager.getCalendarSyncId(orgId, userId, false);

                        if (Objects.nonNull(syncId)) {
                            sourceActivityManager.loadActivityEventIntoContainer(orgId, userId, syncId, activityHolders);
                        }
                    }
                }
                calendarSyncManager.updateCalendarLastSyncTime(orgId, userId, currentTime);
            }
        }
    }
}