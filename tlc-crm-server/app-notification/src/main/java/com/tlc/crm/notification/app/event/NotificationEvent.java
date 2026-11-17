package com.tlc.crm.notification.app.event;

import com.tlc.crm.notification.app.Event;

/**
 * <p>
 * Inscribes the notification related events
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public enum NotificationEvent implements Event {

    APP_NOTIFICATION_EVENT,
    EMAIL_ALERT_EVENT,
    CONTACT_LINK_EVENT;
}
