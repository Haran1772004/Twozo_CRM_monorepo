package com.tlc.crm.common.action.pub;

import com.tlc.crm.common.action.CrmEventRequest;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.event.EventAction;
import com.tlc.crm.notification.app.Event;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.handler.EventDataHandler;

import java.util.Collection;

/**
 * <p>
 * Process the public event actions
 * </p>
 *
 * <span style="color:red"><b>Note:</b></span>
 *  <ul>
 *     <li>
 *         When requiring an {@link EventData} to trigger a specific action, should extend the {@link PublicEventAction}
 *     </li>
 *  </ul>
 *
 * @author Kishore J
 * @version 1.0
 */
public abstract class PublicEventAction extends CrmPublicAction implements EventAction {

    private final EventDataHandler eventDataHandler;

    protected PublicEventAction() {
        eventDataHandler = EventDataHandler.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request  Represents the {@link CrmRequest}
     * @param response Represents the {@link CrmResponse}
     */
    @Override
    public final void pProcess(final CrmRequest request, final CrmResponse response) throws Exception {
        publicProcess(request, response);
    }

    /**
     * {@inheritDoc}
     *
     * @param events    Which contains the collection of {@link Event}
     * @param eventData Represents the {@link EventData}
     * @param eventType Represents the {@link Event.Type}
     */
    public void triggerEvents(final Collection<Event> events, final EventData eventData, final Event.Type eventType) {
        eventDataHandler.handleEvents(events, eventData, eventType);
    }

    /**
     * {@inheritDoc}
     *
     * @param event     Represents the {@link Event}
     * @param eventData Represents the {@link EventData}
     * @param eventType Represents the {@link Event.Type}
     */
    public void triggerEvent(final Event event, final EventData eventData, final Event.Type eventType) {
        eventDataHandler.handleEvent(event, eventData, eventType);
    }

    /**
     * {@inheritDoc}
     *
     * @param eventData Represents the {@link EventData}
     * @param eventType Represents the {@link Event.Type}
     */
    public void triggerEvents(final EventData eventData, final Event.Type eventType) {
        eventDataHandler.handleEvents(eventData, eventType);
    }

    /**
     * <p>
     * Processes the given request, response and event data
     * </p>
     *
     * @param request     Represents thee {@link CrmRequest}
     * @param crmResponse Represents the {@link CrmResponse}
     */
    public abstract void publicProcess(final CrmRequest request, final CrmResponse crmResponse) throws Exception;
}