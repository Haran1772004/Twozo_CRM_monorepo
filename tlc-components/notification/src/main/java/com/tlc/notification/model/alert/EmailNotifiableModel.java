package com.tlc.notification.model.alert;

import com.tlc.notification.model.NotifiableModel;

/**
 * <p>
 * A model the represents the email notifiable
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public interface EmailNotifiableModel extends NotifiableModel {

    /**
     * <p>
     * Indicates whether the template requires a date time field.
     * </p>
     *
     * @return true if the template requires a date time field, false otherwise
     */
     default boolean hasDateTimeField() {
        return false;
    }
}
