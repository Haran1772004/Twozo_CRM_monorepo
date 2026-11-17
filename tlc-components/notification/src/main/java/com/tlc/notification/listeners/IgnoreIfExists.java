package com.tlc.notification.listeners;

import java.util.Set;

/**
 * <p>
 * Ignore exists check for notification type.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface IgnoreIfExists {

    /**
     * <p>
     * Checks the notification type is ignored or not.
     * </p>
     *
     * @param affectedNotifications set of {@link NotificationType} objects.
     * @return boolean (the given notification type is ignored or not)
     */
    default boolean ignore(final Set<NotificationType> affectedNotifications) {
        return false;
    }
}