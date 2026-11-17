package com.tlc.crm.common.event.listener.registry;

import com.tlc.crm.common.event.listener.CompositeEventListener;
import com.tlc.crm.common.event.listener.EventListener;
import com.tlc.crm.common.event.listener.PostEventListener;
import com.tlc.crm.common.event.listener.PreEventListener;
import com.tlc.crm.notification.app.Event;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>
 * Facilitates to register {@link EventListener} .
 * </p>
 *
 * @author Ajay.
 * @version 1.0
 */
public final class EventListenerRegistry {

    private final Map<Event, Collection<PreEventListener>> preEventListeners;
    private final Map<Event, Collection<PostEventListener>> postEventListeners;
    private final Map<Event, Collection<CompositeEventListener>> compositeEventListeners;

    private EventListenerRegistry() {
        preEventListeners = new HashMap<>();
        postEventListeners = new HashMap<>();
        compositeEventListeners = new HashMap<>();
    }

    private static class Instance {
        private static final EventListenerRegistry INSTANCE = new EventListenerRegistry();
    }

    /**
     * <p>
     * Gets the {@link EventListenerRegistry}.
     * </p>
     *
     * @return {@link EventListenerRegistry}
     */
    public static EventListenerRegistry getInstance() {
        return EventListenerRegistry.Instance.INSTANCE;
    }

    /**
     * <p>
     * Registers {@link PostEventListener}.
     * </p>
     *
     * @param postEventListener Represents {@link PostEventListener}.
     */
    public void registerPostEventListener(final PostEventListener postEventListener) {
        postEventListeners.computeIfAbsent(postEventListener.event(), postEventListeners -> new HashSet<>()).add(postEventListener);
    }

    /**
     * <p>
     * Registers {@link PreEventListener}.
     * </p>
     *
     * @param preEventListener Represents {@link PreEventListener}.
     */
    public void registerPreEventListener(final PreEventListener preEventListener) {
        preEventListeners.computeIfAbsent(preEventListener.event(), preEventListeners -> new HashSet<>()).add(preEventListener);
    }

    /**
     * <p>
     * Registers {@link PostEventListener}.
     * </p>
     *
     * @param compositeEventListener Represents {@link CompositeEventListener}.
     */
    public void registerCompositeEventListener(final CompositeEventListener compositeEventListener) {
        compositeEventListeners.computeIfAbsent(compositeEventListener.event(), compositeEventListeners -> new HashSet<>()).add(compositeEventListener);
    }

    /**
     * <p>
     * Gets the Collection of {@link PreEventListener}.
     * </p>
     *
     * @return Collection of {@link PreEventListener}.
     */
    public Collection<PreEventListener> getPreEventListeners() {
        final Collection<PreEventListener> preEventListeners = new HashSet<>();

        preEventListeners.addAll(this.preEventListeners.values().stream().flatMap(Collection::stream).collect(Collectors.toUnmodifiableSet()));
        preEventListeners.addAll(getCompositeEventListeners());

        return Collections.unmodifiableCollection(preEventListeners);
    }

    /**
     * <p>
     * Gets the Collection of {@link PostEventListener}.
     * </p>
     *
     * @return Collection of {@link PostEventListener}.
     */
    public Collection<PostEventListener> getPostEventListeners() {
        final Collection<PostEventListener> postEventListeners = new HashSet<>();

        postEventListeners.addAll(this.postEventListeners.values().stream().flatMap(Collection::stream).collect(Collectors.toUnmodifiableSet()));
        postEventListeners.addAll(getCompositeEventListeners());

        return Collections.unmodifiableCollection(postEventListeners);
    }

    /**
     * <p>
     * Gets the Collection of {@link CompositeEventListener}.
     * </p>
     *
     * @return Collection of {@link CompositeEventListener}.
     */
    public Collection<CompositeEventListener> getCompositeEventListeners() {
        return compositeEventListeners.values().stream().flatMap(Collection::stream).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * <p>
     * Gets the collection of {@link PostEventListener} based on the {@link Event}
     * </p>
     *
     * @return the {@link PostEventListener}.
     */
    public Collection<PostEventListener> getPostEventListeners(final Event event) {
        return postEventListeners.get(event);
    }

    /**
     * <p>
     * Fetches the collection of {@link PreEventListener} based on the {@link Event}
     * </p>
     *
     * @return the {@link PreEventListener}.
     */
    public Collection<PreEventListener> getPreEventListeners(final Event event) {
        return preEventListeners.get(event);
    }

    /**
     * <p>
     * Fetches the collection of {@link PostEventListener} for the given events
     * </p>
     *
     * @param events Which contains the collection of {@link Event}
     * @return The collection of {@link PostEventListener}
     */
    public Collection<PostEventListener> getPostEventListeners(final Collection<Event> events) {
        return events.stream().map(postEventListeners::get).filter(Objects::nonNull).flatMap(Collection::stream).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * <p>
     * Fetches the collection of {@link PreEventListener} for the given events
     * </p>
     *
     * @param events Which contains the collection of {@link Event}
     * @return The collection of {@link PreEventListener}
     */
    public Collection<PreEventListener> getPreEventListeners(final Collection<Event> events) {
        return events.stream().map(preEventListeners::get).filter(Objects::nonNull).flatMap(Collection::stream).collect(Collectors.toUnmodifiableSet());
    }
}