package com.tlc.email.common.publish;

import com.tlc.email.common.model.Thread;

import java.util.Collection;

/**
 * <p>
 * Handles publishing email requests for background sync actions,
 * triggering the email synchronization based on the provided request and threads.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.1
 */
public interface EmailRequestPublisher<T> {

    /**
     * Publishes an email sync request.
     *
     * @param request the email request object to be published
     */
    void publish(final T request);

    /**
     * Publishes an email sync request along with a collection of email threads.
     *
     * @param request the email request object to be published
     * @param threads a collection of email threads associated with the request
     */
    void publish(final T request, final Collection<Thread> threads);
}