package com.tlc.crm.email.notification.registrar;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.service.registrar.ResourceRegistrar;

import com.tlc.crm.common.registrar.model.CrmServerRegistrarData;

import com.tlc.crm.email.notification.email.status.EmailErrorCodes;
import com.tlc.crm.notification.app.AppNotificationEventType;
import com.tlc.crm.notification.app.Notifiable;
import com.tlc.crm.notification.app.service.NotificationLoader;
import com.tlc.crm.notification.app.service.NotificationLoaderHolder;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Objects;

/**
 * <p>
 * The registrar that registers the notification related actions.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see ResourceRegistrar
 */
public class EmailNotificationResourceRegistrar implements ResourceRegistrar<CrmServerRegistrarData> {

    private final NotificationLoaderHolder notificationLoaderHolder;

    private EmailNotificationResourceRegistrar() {
        notificationLoaderHolder = NotificationLoaderHolder.getInstance();
    }

    private static final class Instance {
        private static final EmailNotificationResourceRegistrar NOTIFICATION_REGISTRAR = new EmailNotificationResourceRegistrar();
    }

    public static EmailNotificationResourceRegistrar getInstance() {
        return Instance.NOTIFICATION_REGISTRAR;
    }

    /**
     * {@inheritDoc}
     *
     * @param registrarData Holds the {@link CrmServerRegistrarData} which is needed for registering the resources/actions.
     */
    @Override
    public void register(final CrmServerRegistrarData registrarData) {
        final String moduleName = registrarData.getModuleName();

        for (final Class<?> classObj : registrarData.getClasses()) {
            try {
                if (classObj.getName().contains("action.email.notification")) {
                    final Class<Notifiable> notificationTypesClass = Notifiable.class;
                    final Notifiable notifiable = classObj.getAnnotation(notificationTypesClass);

                    if (Objects.nonNull(notifiable)) {
                        final Constructor<NotificationLoader> constructor = (Constructor<NotificationLoader>) classObj.getConstructor();
                        final AppNotificationEventType appNotificationEventType = Arrays.stream(notifiable.types()).findFirst().orElseGet(() -> null);

                        if (Objects.nonNull(appNotificationEventType)) {
                            notificationLoaderHolder.put(moduleName, appNotificationEventType, constructor.newInstance());
                        }
                    }
                }
            } catch (Exception exp) {
                throw ErrorCode.get(EmailErrorCodes.FAILED_TO_REGISTER_ACTIONS, exp);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param registrarData Holds the {@link CrmServerRegistrarData} which is needed for removing the resources/actions.
     */
    @Override
    public void unregister(final CrmServerRegistrarData registrarData) {
        notificationLoaderHolder.remove(registrarData.getModuleName());
    }
}
