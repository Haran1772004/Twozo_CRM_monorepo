package com.tlc.notification.internal.config;

import com.tlc.commons.code.ErrorCode;

import com.tlc.notification.service.notification.SmsService;
import com.tlc.notification.service.provider.SmsServiceProvider;
import com.tlc.notification.internal.listener.ErrorHandler;
import com.tlc.notification.internal.listener.SuccessHandler;
import com.tlc.notification.internal.listener.PreWorkHandler;
import com.tlc.notification.internal.status.NotificationErrorCodes;
import com.tlc.notification.model.message.Sms;
import com.tlc.notification.listeners.NotificationType;
import com.tlc.notification.model.Notification;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * <p>
 * Provides the SMS service.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class SmsServiceImpl implements SmsService {

    private final SmsServiceProvider serviceProvider;

    public SmsServiceImpl(final SmsServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void send(final Sms message) {
        try {
            beforeSend(message);
            serviceProvider.sendMessage(message);
            onSuccess(message);
        } catch (Throwable throwable) {
            onFailure(message);
            throw ErrorCode.get(NotificationErrorCodes.UNABLE_TO_SEND_SMS, throwable);
        }
    }

    /**
     * <p>
     * Listens the {@link Sms} on aborted.
     * </p>
     *
     * @param message {@link Sms} object for listening
     */
    private void onFailure(final Sms message) {
        new ErrorHandler().handle(constructDataMap(message));
    }

    /**
     * <p>
     * Listens the {@link Sms} on before send.
     * </p>
     *
     * @param message {@link Sms} object for listening.
     */
    private void beforeSend(final Sms message) {
        new PreWorkHandler().handle(constructDataMap(message));
    }

    /**
     * <p>
     * Listens the {@link Sms} on after sent.
     * </p>
     *
     * @param message {@link Sms} object for listening.
     */
    private void onSuccess(final Sms message) {
        new SuccessHandler().handle(constructDataMap(message));
    }

    /**
     * <p>
     * Constructs the data map for listening purpose.
     * </p>
     *
     * @param message {@link Sms} object for data map construction.
     * @return {@link SortedMap}
     */
    private SortedMap<NotificationType, List<Notification<?, ?>>> constructDataMap(final Sms message) {
        final SortedMap<NotificationType, List<Notification<?, ?>>> dataMap = new TreeMap<>();

        dataMap.put(NotificationType.SMS, List.of(message));
        return dataMap;
    }

    /**
     * <p>
     * Stops the external service client by assigning null value.
     * </p>
     */
    @Override
    public void stop() {
        serviceProvider.stop();
    }
}
