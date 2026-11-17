package com.tlc.crm.common.action.event;

import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.notification.app.Event;

import java.util.Collection;
/**
 * <p>
 * An interface responsible for triggering actions pre before and post specific events, determined by the provided event type
 * </p>
 *
 * <span style="color:red"><b>Note:</b></span>
 * <ul>
 *     <li>
 *        If eventType is {@link Event.Type#PRE}, it processes actions that need to occur before the main event
 *     </li>
 *     <li>
 *         If eventType is {@link Event.Type#POST}, it processes actions that need to occur after the main event
 *     </li>
 * </ul>
 *
 * @author Kishore J
 * @version 1.0
 */
public interface EventAction {

    /**
     * <p>
     * Initiates event processing for the provided collection of {@link Event}, {@link EventData}, and {@link Event.Type},
     * distinguishing between pre-event and post-event actions, and delegates tasks to corresponding event listeners.
     * </p>
     *
     * @param events    Which contains the collection of {@link Event}
     * @param eventData Represents the {@link EventData}
     * @param eventType Represents the {@link Event.Type}
     */
    void triggerEvents(final Collection<Event> events, final EventData eventData, final Event.Type eventType);

    /**
     * <p>
     * Initiates event processing for the specified {@link Event}, {@link EventData}, and {@link Event.Type},
     * distinguishing between pre-event and post-event actions, and delegates tasks to corresponding event listeners.
     * </p>
     *
     * @param event     Represents the {@link Event}
     * @param eventData Represents the {@link EventData}
     * @param eventType Represents the {@link Event.Type}
     */
    void triggerEvent(final Event event, final EventData eventData, final Event.Type eventType);

    /**
     * <p>
     * Initiates event processing for the specified {@link EventData} and {@link Event.Type},
     * distinguishing between pre-event and post-event actions, and delegates tasks to corresponding event listeners.
     * </p>
     *
     * @param eventData Represents the {@link EventData}
     * @param eventType Represents the {@link Event.Type}
     */
    void triggerEvents(final EventData eventData, final Event.Type eventType);
}
