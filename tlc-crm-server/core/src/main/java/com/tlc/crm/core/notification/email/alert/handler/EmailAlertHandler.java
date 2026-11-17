package com.tlc.crm.core.notification.email.alert.handler;

import com.tlc.crm.common.models.Action;
import com.tlc.validator.TlcModel;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * Handler which provides abstract way to deal with email alert template types
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public interface EmailAlertHandler {

    /**
     * <p>
     * Gets the instance of  {@link EmailAlertHandler}
     * </p>
     *
     * @return The {@link EmailAlertHandler} instance
     */
    static EmailAlertHandler getInstance() {
        return EmailAlertHandlerImpl.getInstance();
    }

    /**
     * <p>
     * Sends email alerts based on the provided parameters.
     * </p>
     *
     * @param orgId          Represents the organization ID.
     * @param userId         Represents the user ID.
     * @param models         Represents the collection of models which triggered the event.
     * @param previousValues Represents the map of previous values of the models with model ID as key.
     * @param action         Represents the action performed (CREATE or UPDATE).
     * @param template       Represents the template for the alert event.
     */
    void sendEmailAlerts(final long orgId, final long userId, final Collection<? extends TlcModel> models,
                         final Map<Long, TlcModel> previousValues, final Action action, final String template);
}
