package com.tlc.crm.core.internal.event;

import com.tlc.crm.notification.app.Event;

/**
 * <p>
 * Inscribes the core module events.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public enum CoreEvent implements Event {

    INDEX_EVENT,
    CONTACT_PUBLISH_EVENT,
    CONTACT_DELETE_EVENT,
    LIFECYCLE_UPDATE_EVENT,
    LIFECYCLE_DELETE_EVENT,
    ACTIVITY_EVENT,
    QUOTA_DEAL_EVENT,
    QUOTA_EVENT,
    ;
}
