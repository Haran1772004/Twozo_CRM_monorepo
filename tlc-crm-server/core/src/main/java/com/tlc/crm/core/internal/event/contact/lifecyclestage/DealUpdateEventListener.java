package com.tlc.crm.core.internal.event.contact.lifecyclestage;

import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.event.listener.PostEventListener;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.internal.contact.handler.ContactStageUpdateHandler;
import com.tlc.crm.core.internal.event.CoreEvent;
import com.tlc.crm.notification.app.Event;
import com.tlc.validator.TlcModel;

import java.util.Collection;

/**
 * <p>
 * Updates the lifecycle stage/status of the contact based on the deal actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public final class DealUpdateEventListener implements PostEventListener {

    private final ContactStageUpdateHandler stageUpdateHandler;

    private DealUpdateEventListener() {
        stageUpdateHandler = ContactStageUpdateHandler.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Event}.
     */
    @Override
    public Event event() {
        return CoreEvent.LIFECYCLE_UPDATE_EVENT;
    }

    private static class Instance {
        private static final DealUpdateEventListener INSTANCE = new DealUpdateEventListener();
    }

    /**
     * <p>
     * Provides the {@link DealUpdateEventListener}.
     * </p>
     *
     * @return {@link DealUpdateEventListener}
     */
    public static DealUpdateEventListener getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param eventData Represents {@link EventData}.
     */
    @Override
    public void processPostEvent(final EventData eventData) {
        final Collection<? extends TlcModel> models = eventData.getValues();

        if (models.stream().anyMatch(Deal.class::isInstance)) {
            stageUpdateHandler.handle(eventData.getOrgId(), eventData.getAction(), (Collection<Deal>) models);
        }
    }
}