package com.tlc.crm.common.event.listener;

/**
 * <p>
 * Listens for both pre & post events that are to be processed.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public interface CompositeEventListener extends PreEventListener, PostEventListener {
}