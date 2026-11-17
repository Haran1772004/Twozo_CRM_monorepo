package com.tlc.crm.core.internal.event.index;

import com.tlc.crm.common.event.listener.PostEventListener;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.core.internal.event.CoreEvent;
import com.tlc.crm.core.internal.event.index.publisher.IndexDataPublisher;
import com.tlc.crm.notification.app.Event;
import com.tlc.validator.TlcModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;

/**
 * <p>
 * Handles data for the purpose of indexing action.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public final class IndexEventListener implements PostEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(IndexEventListener.class);

    private final IndexDataPublisher indexDataPublisher;

    private IndexEventListener() {
        indexDataPublisher = IndexDataPublisher.getInstance();
    }

    /**
     * <p>
     * Provides the name of the event.
     * </p>
     *
     * @return {@link Event}.
     */
    @Override
    public Event event() {
        return CoreEvent.INDEX_EVENT;
    }

    private static class Instance {
        private static final IndexEventListener INSTANCE = new IndexEventListener();
    }

    /**
     * <p>
     * Gets the {@link IndexEventListener}.
     * </p>
     *
     * @return {@link IndexEventListener}
     */
    public static IndexEventListener getInstance() {
        return IndexEventListener.Instance.INSTANCE;
    }

    /**
     * <p>
     * Initiates the post process action which is publishing.
     * </p>
     *
     * @param eventData Represents {@link EventData}.
     */
    @Override
    public void processPostEvent(final EventData eventData) {
        publish(eventData);
    }

    /**
     * <p>
     * Publishes data for the purpose of indexing
     * </p>
     *
     * @param eventData Represents {@link EventData}.
     */
    private void publish(final EventData eventData) {
        LOGGER.info("IndexEventListener listened to post event");
        final Collection<? extends TlcModel> models = new ArrayList<>(eventData.getValues());

        indexDataPublisher.publishIndexData(eventData.getOrgId(), models, eventData.getAction());
    }
}
