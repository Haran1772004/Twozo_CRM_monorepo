package com.tlc.notification.internal.config;

import com.tlc.commons.code.ErrorCode;

import com.tlc.notification.service.notification.WhatsappService;
import com.tlc.notification.service.provider.WhatsappServiceProvider;
import com.tlc.notification.internal.listener.ErrorHandler;
import com.tlc.notification.internal.listener.SuccessHandler;
import com.tlc.notification.internal.listener.PreWorkHandler;
import com.tlc.notification.internal.status.NotificationErrorCodes;
import com.tlc.notification.model.message.WhatsappMessage;
import com.tlc.notification.listeners.NotificationType;
import com.tlc.notification.model.Notification;

import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * <p>
 * Provides the whatsapp service.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class WhatsappServiceImpl implements WhatsappService {

    private final WhatsappServiceProvider serviceProvider;

    public WhatsappServiceImpl(final WhatsappServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void send(final WhatsappMessage message) {
        try {
            beforeSend(message);
            serviceProvider.sendWhatsappMessage(message);
            onSuccess(message);
        } catch (Throwable throwable) {
            onFailure(message);
            throw ErrorCode.get(NotificationErrorCodes.UNABLE_TO_SEND_WHATSAPP_MESSAGE, throwable);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<WhatsappMessage> receive(final WhatsappMessage message) {
        return null;
    }

    /**
     * <p>
     * Listens the {@link WhatsappMessage} on aborted.
     * </p>
     *
     * @param message {@link WhatsappMessage} object for listening
     */
    private void onFailure(final WhatsappMessage message) {
        new ErrorHandler().handle(constructDataMap(message));
    }

    /**
     * <p>
     * Listens the {@link WhatsappMessage} on before send.
     * </p>
     *
     * @param message {@link WhatsappMessage} object for listening.
     */
    private void beforeSend(final WhatsappMessage message) {
        new PreWorkHandler().handle(constructDataMap(message));
    }

    /**
     * <p>
     * Listens the {@link WhatsappMessage} on after sent.
     * </p>
     *
     * @param message {@link WhatsappMessage} object for listening.
     */
    private void onSuccess(final WhatsappMessage message) {
        new SuccessHandler().handle(constructDataMap(message));
    }

    /**
     * <p>
     * Constructs the data map for listening purpose
     * </p>
     *
     * @param message {@link WhatsappMessage} object for data map construction
     * @return {@link SortedMap}
     */
    private SortedMap<NotificationType, List<Notification<?, ?>>> constructDataMap(final WhatsappMessage message) {
        final SortedMap<NotificationType, List<Notification<?, ?>>> dataMap = new TreeMap<>();

        dataMap.put(NotificationType.WHATSAPP, List.of(message));
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
