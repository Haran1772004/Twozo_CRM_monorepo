package com.tlc.crm.notifications.initializer;

import com.tlc.commons.service.initializer.ServiceInitializer;
import com.tlc.commons.util.Configurations;
import com.tlc.crm.common.event.listener.registry.EventListenerRegistry;
import com.tlc.crm.core.features.timeline.store.ConversableTimelineManagerStore;
import com.tlc.crm.entity.timeline.model.Timeline;
import com.tlc.crm.notifications.api.call.manager.impl.CallTimelineManager;
import com.tlc.crm.notifications.calendar.schedule.CalendarSyncController;
import com.tlc.crm.notifications.internal.controller.NotificationActionController;
import com.tlc.crm.notifications.internal.event.EntityCallLinkListener;
import com.tlc.crm.notifications.internal.event.NotificationEventListener;
import com.tlc.crm.notifications.internal.event.NotificationUserUpdatePostEventListener;
import com.tlc.crm.notifications.internal.event.handler.MCubeUserUpdateHandler;
import com.tlc.crm.notifications.internal.event.handler.TeleCMIUserUpdateHandler;
import com.tlc.crm.notifications.internal.event.handler.TelephoneUserUpdateHandlerRegistry;
import com.tlc.crm.notifications.internal.loader.phone.PhoneNumberLocationLoader;
import com.tlc.crm.notifications.internal.scheduler.NotificationAlertMonitor;
import com.tlc.crm.notifications.internal.scheduler.call.CallCompletionMonitor;
import com.tlc.email.server.internal.provider.DomainAuthenticatedEmailServiceProvider;

/**
 * <p>
 * An initializer that initializes the service which needs to be started.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class NotificationServiceInitializer implements ServiceInitializer {

    private final NotificationActionController notificationActionController;
    private final NotificationEventListener notificationEventListener;
    private final EventListenerRegistry eventListenerRegistry;
    private final CalendarSyncController calendarSyncController;
    private final PhoneNumberLocationLoader phoneNumberLocationLoader;
    private final CallCompletionMonitor callCompletionMonitor;
    private final ConversableTimelineManagerStore conversableTimelineManagerStore;
    private final NotificationAlertMonitor notificationAlertMonitor;
    private final TelephoneUserUpdateHandlerRegistry telephoneUserUpdateHandlerRegistry;
    private final TeleCMIUserUpdateHandler teleCMIUserUpdateHandler;
    private final MCubeUserUpdateHandler mCubeUserUpdateHandler;

    private NotificationServiceInitializer() {
        notificationActionController = new NotificationActionController();
        notificationEventListener = NotificationEventListener.getInstance();
        eventListenerRegistry = EventListenerRegistry.getInstance();
        calendarSyncController = CalendarSyncController.getInstance();
        phoneNumberLocationLoader = PhoneNumberLocationLoader.getInstance();
        callCompletionMonitor = CallCompletionMonitor.getInstance();
        conversableTimelineManagerStore = ConversableTimelineManagerStore.getInstance();
        notificationAlertMonitor = NotificationAlertMonitor.getInstance();
        telephoneUserUpdateHandlerRegistry = TelephoneUserUpdateHandlerRegistry.getInstance();
        teleCMIUserUpdateHandler = TeleCMIUserUpdateHandler.getInstance();
        mCubeUserUpdateHandler = MCubeUserUpdateHandler.getInstance();
    }

    private static final class Instance {
        private static final NotificationServiceInitializer NOTIFICATION_SERVICE_INITIALIZER = new NotificationServiceInitializer();
    }

    public static NotificationServiceInitializer getInstance() {
        return Instance.NOTIFICATION_SERVICE_INITIALIZER;
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Holds the configuration properties.
     */
    @Override
    public void preInitialize(final Configurations configurations) {
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Holds the configuration properties.
     */
    @Override
    public void postInitialize(final Configurations configurations) {
        eventListenerRegistry.registerPostEventListener(notificationEventListener);
        eventListenerRegistry.registerPostEventListener(NotificationUserUpdatePostEventListener.getInstance());
        eventListenerRegistry.registerPostEventListener(EntityCallLinkListener.getInstance());
        notificationActionController.start(null);
        calendarSyncController.start(null);
        callCompletionMonitor.start(null);
        notificationAlertMonitor.start(null);

        telephoneUserUpdateHandlerRegistry.add(teleCMIUserUpdateHandler);
        telephoneUserUpdateHandlerRegistry.add(mCubeUserUpdateHandler);

        conversableTimelineManagerStore.add(Timeline.Category.CALLS, CallTimelineManager.getInstance());
        DomainAuthenticatedEmailServiceProvider.getInstance().start(configurations.getNestedConfigs("mailService"));
        phoneNumberLocationLoader.load(configurations.getInt("call.location.loader.iterations"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        calendarSyncController.stop();
        notificationActionController.stop();
        conversableTimelineManagerStore.clear();
        notificationAlertMonitor.stop();
    }
}