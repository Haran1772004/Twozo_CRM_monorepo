package com.tlc.iam.server.action.event.listener;

import com.tlc.iam.common.events.IamEvents;
import com.tlc.iam.common.models.converter.ApplicationConverter;
import com.tlc.iam.server.internal.ApplicationRegistry;
import com.tlc.web.client.event.EventConsumer;
import com.tlc.web.client.event.EventListener;

/**
 * Listens to the application fetch event and handles it
 *
 * @author Maharaja S
 * @version 1.0
 */
@EventListener(name = IamEvents.APPLICATION_FETCH)
public class ApplicationFetchHandler implements EventConsumer<String> {

    private final ApplicationRegistry applicationRegistry;
    private final ApplicationConverter applicationConverter;

    public ApplicationFetchHandler() {
        applicationRegistry = ApplicationRegistry.getInstance();
        applicationConverter = ApplicationConverter.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param applicationName Represents the name of the application
     */
    @Override
    public byte[] consume(final String applicationName) {
        return applicationConverter.convertToJson(applicationRegistry.get(applicationName)).getBytes();
    }
}