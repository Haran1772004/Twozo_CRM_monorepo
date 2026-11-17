package com.tlc.crm.notifications.internal.loader.notifications.impl.alerts;

import com.tlc.crm.core.api.iam.manager.UserNotificationSettingsManager;
import com.tlc.crm.notifications.api.model.converter.AppNotificationConverter;
import com.tlc.crm.notifications.internal.loader.notifications.AppNotificationAlertLoader;
import com.tlc.crm.notifications.api.model.alert.converter.NotificationAlertConverter;
import com.tlc.crm.notifications.service.NotificationAlertManager;
import com.tlc.validator.TlcModel;

/**
 * <p>
 * Loads alerts into DB.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public abstract class AbstractAppNotificationAlertLoader<T extends TlcModel> implements AppNotificationAlertLoader<T> {

    protected final NotificationAlertManager notificationAlertManager;
    protected final NotificationAlertConverter notificationAlertConverter;
    protected final AppNotificationConverter appNotificationConverter;
    protected final UserNotificationSettingsManager notificationSettingsManager;

    protected AbstractAppNotificationAlertLoader() {
        notificationAlertManager = NotificationAlertManager.getInstance();
        notificationAlertConverter = NotificationAlertConverter.getInstance();
        appNotificationConverter = AppNotificationConverter.getInstance();
        notificationSettingsManager = UserNotificationSettingsManager.getInstance();
    }
}