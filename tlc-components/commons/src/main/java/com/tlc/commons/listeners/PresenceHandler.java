package com.tlc.commons.listeners;

/**
 * Handles actions based on the presence or absence of an item.
 *
 * @param <T> the type of the item
 * @author Sundar
 * @version 1.0
 */
public interface PresenceHandler<T> extends FailableAction {

    /**
     * Invoked when the item is present.
     *
     * @param t the item that is present
     */
    void ifPresent(final T t);

    /**
     * Invoked when the item is absent.
     *
     * @param t the item that is absent
     */
    void ifAbsent(final T t);
}