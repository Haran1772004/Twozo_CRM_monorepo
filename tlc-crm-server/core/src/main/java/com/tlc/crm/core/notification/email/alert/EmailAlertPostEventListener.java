package com.tlc.crm.core.notification.email.alert;

import com.tlc.crm.common.event.listener.PostEventListener;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.models.Action;

import com.tlc.crm.core.notification.email.alert.handler.EmailAlertHandlerImpl;

import com.tlc.crm.notification.app.Event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.tlc.crm.notification.app.event.NotificationEvent.EMAIL_ALERT_EVENT;

/**
 * <p>
 * Handles post process actions for email alert event
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public class EmailAlertPostEventListener implements PostEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailAlertPostEventListener.class);

    private final EmailAlertHandlerImpl emailAlertHandler;

    private EmailAlertPostEventListener() {
        emailAlertHandler = EmailAlertHandlerImpl.getInstance();
    }

    private static class InstanceHolder {
        private static final EmailAlertPostEventListener INSTANCE = new EmailAlertPostEventListener();
    }

    /**
     * Gets the instance of {@link EmailAlertPostEventListener}
     *
     * @return The {@link EmailAlertPostEventListener} instance
     */
    public static EmailAlertPostEventListener getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param eventData Represents {@link EventData}.
     */
    @Override
    public void processPostEvent(final EventData eventData) {
        final long orgId = eventData.getOrgId();
        final long userId = eventData.getUserId();
        final Action action = eventData.getAction();

        if (Action.CREATE == action || Action.UPDATE == action) {
            emailAlertHandler.sendEmailAlerts(orgId, userId, eventData.getValues(),
                    eventData.getPreviousValues(), action, eventData.getApiType());
            LOGGER.debug("Email alert triggered by post event listener for org ID {} and user ID {}", orgId, userId);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link Event}
     */
    @Override
    public Event event() {
        return EMAIL_ALERT_EVENT;
    }
}
