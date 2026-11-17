package com.tlc.notification.internal.listener;

import com.tlc.notification.listeners.ErrorListener;
import com.tlc.notification.listeners.ListenerOrder;
import com.tlc.notification.listeners.NotificationType;
import com.tlc.notification.listeners.PreWorkListener;
import com.tlc.notification.listeners.SuccessListener;
import com.tlc.notification.model.Notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * <p>
 * Container of notification listener.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
class NotificationListenerContainer {

    private final Map<NotificationType, SortedMap<ListenerOrder.Priority, List<PreWorkListener<Notification<?, ?>>>>> preWorkListenerCache;
    private final Map<NotificationType, SortedMap<ListenerOrder.Priority, List<SuccessListener<Notification<?, ?>>>>> successListenerCache;
    private final Map<NotificationType, SortedMap<ListenerOrder.Priority, List<ErrorListener<Notification<?, ?>>>>> errorListenerCache;

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationListenerContainer.class);

    /**
     * <p>
     * Provides the instance of {@link NotificationListenerContainer}.
     * </p>
     */
    private final static class Instance {
        private static final NotificationListenerContainer INSTANCE = new NotificationListenerContainer();
    }

    /**
     * <p>
     * Provides the {@link NotificationListenerContainer} object.
     * </p>
     *
     * @return {@link NotificationListenerContainer} object
     */
    static NotificationListenerContainer getInstance() {
        return Instance.INSTANCE;
    }

    private NotificationListenerContainer() {
        this.preWorkListenerCache = new HashMap<>();
        this.successListenerCache = new HashMap<>();
        this.errorListenerCache = new HashMap<>();
    }

    /**
     * <p>
     * Adds the {@link PreWorkListener} object in pre work listener cache.
     * </p>
     *
     * @param notificationType {@link NotificationType} object
     * @param listener         {@link PreWorkListener} object
     */
    void addPreWorkListener(final NotificationType notificationType, final PreWorkListener<Notification<?, ?>> listener) {
        LOGGER.info("Pre work listener received from {}", listener.getClass().getName());
        preWorkListenerCache.computeIfAbsent(notificationType, k -> new TreeMap<>()).computeIfAbsent(listener
                .getPriority(), k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    /**
     * <p>
     * Adds the {@link SuccessListener} object in success listener cache.
     * </p>
     *
     * @param notificationType {@link NotificationType} object
     * @param listener         {@link SuccessListener} object
     */
    void addSuccessListener(final NotificationType notificationType, final SuccessListener<Notification<?, ?>> listener) {
        LOGGER.info("Success listener received from {}", listener.getClass().getName());
        successListenerCache.computeIfAbsent(notificationType, k -> new TreeMap<>()).computeIfAbsent(listener
                .getPriority(), k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    /**
     * <p>
     * Adds the {@link ErrorListener} in error listener cache.
     * </p>
     *
     * @param notificationType {@link NotificationType} object
     * @param listener         {@link ErrorListener} object
     */
    void addErrorListener(final NotificationType notificationType, final ErrorListener<Notification<?, ?>> listener) {
        LOGGER.info("Error listener received from {}", listener.getClass().getName());
        errorListenerCache.computeIfAbsent(notificationType, k -> new TreeMap<>()).computeIfAbsent(listener
                .getPriority(), k -> new CopyOnWriteArrayList<>()).add(listener);

    }

    /**
     * <p>
     * Retrieves the collection of {@link PreWorkListener} from pre work listener cache.
     * </p>
     *
     * @param notificationType {@link NotificationType} object
     * @return collection of {@link PreWorkListener}
     */
    Collection<List<PreWorkListener<Notification<?, ?>>>> getPreWorkListeners(final NotificationType notificationType) {
        final Map<ListenerOrder.Priority, List<PreWorkListener<Notification<?, ?>>>> listenersMap = preWorkListenerCache.get(notificationType);

        return Objects.nonNull(listenersMap) ? listenersMap.values() : null;
    }

    /**
     * <p>
     * Retrieves the collection of {@link SuccessListener} from success listener cache.
     * </p>
     *
     * @param notificationType {@link NotificationType} object
     * @return collection of {@link SuccessListener}
     */
    Collection<List<SuccessListener<Notification<?, ?>>>> getSuccessListeners(final NotificationType notificationType) {
        final Map<ListenerOrder.Priority, List<SuccessListener<Notification<?, ?>>>> listenersMap = successListenerCache.get(notificationType);

        return Objects.nonNull(listenersMap) ? listenersMap.values() : null;
    }

    /**
     * <p>
     * Retrieves the collection of {@link ErrorListener} from error listener cache.
     * </p>
     *
     * @param notificationType {@link NotificationType} object
     * @return collection of {@link ErrorListener}
     */
    Collection<List<ErrorListener<Notification<?, ?>>>> getErrorListeners(final NotificationType notificationType) {
        final Map<ListenerOrder.Priority, List<ErrorListener<Notification<?, ?>>>> listenersMap = errorListenerCache.get(notificationType);

        return Objects.nonNull(listenersMap) ? listenersMap.values() : null;
    }

    /**
     * <p>
     * Checks the container has {@link ErrorListener} for given notification type.
     * </p>
     *
     * @param notificationType {@link NotificationType} object
     * @return boolean (present or not)
     */
    boolean hasOnErrorListeners(final NotificationType notificationType) {
        return errorListenerCache.containsKey(notificationType);
    }
}
