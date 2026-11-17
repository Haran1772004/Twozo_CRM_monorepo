package com.tlc.crm.common.action.secure;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.action.CrmEventRequest;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.event.EventAction;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.event.model.SecureEventData;
import com.tlc.crm.common.fields.ActionFields;
import com.tlc.crm.common.handler.EventDataHandler;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.notification.app.Event;
import com.tlc.validator.TlcModel;

import java.util.Collection;
import java.util.Collections;

/**
 * <p>
 * Handles the secure event related actions
 * </p>
 * <span style="color:red"><b>Note:</b></span>
 * <ul>
 *     <li>
 *         When requiring an {@link EventData} to trigger a specific action, should extend the {@link SecureEventAction}
 *     </li>
 * </ul>
 *
 * @author Kishore J
 * @version 1.0
 */
public abstract class SecureEventAction extends AuthorizationController implements EventAction {

    private final EventDataHandler eventDataHandler;

    protected SecureEventAction() {
        eventDataHandler = EventDataHandler.getInstance();
    }

    /**
     * <p>
     * Processes the CRM request and triggers events
     * </p>
     *
     * @param request  Represents the {@link CrmRequest}
     * @param response Represents the {@link CrmResponse}
     */
    @Override
    public void authorizedProcess(final CrmRequest request, final CrmResponse response) throws Exception {
        final EventData eventData = new SecureEventData(request.orgId(), request.userId());
        final JsonObject requestJson = request.getRequestJson();
        final String type = requestJson.getString(ActionFields.TYPE);

        eventData.setLocale(request.locale());

        if (stateRequiredTypes().contains(type)) {
            final Collection<TlcModel> currentModels = getCurrentModels(request);

            if (!currentModels.isEmpty()) {
                eventData.setPreviousValues(currentModels);
                eventData.setApiType(type);
            }
        }
        final CrmEventRequest eventRequest = new CrmEventRequest(request, eventData);

        secureProcess(eventRequest, response);
    }

    /**
     * <p>
     * Processes the CRM request and triggers events
     * </p>
     *
     * @param request   Represents the {@link CrmRequest}
     * @param response  Represents the {@link CrmResponse}
     */
    protected abstract void secureProcess(final CrmRequest request, final CrmResponse response);

    /**
     * {@inheritDoc}
     *
     * @param events    Which contains the collection of {@link Event}
     * @param eventData Represents the {@link EventData}
     * @param eventType Represents the {@link Event.Type}
     */
    @Override
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
    @Override
    public void triggerEvent(final Event event, final EventData eventData, final Event.Type eventType) {
        eventDataHandler.handleEvent(event, eventData, eventType);
    }

    /**
     * {@inheritDoc}
     *
     * @param eventData Represents the {@link EventData}
     * @param eventType Represents the {@link Event.Type}
     */
    @Override
    public void triggerEvents(final EventData eventData, final Event.Type eventType) {
        eventDataHandler.handleEvents(eventData, eventType);
    }

    /**
     * <p>
     * Fetches the set of API types that need to include old values in the event data
     * </p>
     *
     * @return The collection of api type
     */
    protected Collection<String> stateRequiredTypes() {
        return Collections.emptyList();
    }

    /**
     * <p>
     * Fetches the current models based on the request information
     * </p>
     *
     * @param request Represents the {@link CrmRequest}
     * @returnt The collection of {@link TlcModel}
     */
    protected Collection<TlcModel> getCurrentModels(final CrmRequest request) {
        return Collections.emptyList();
    }

    /**
     * <p>
     * Executes pre action for the given action and models, {@link EventData} and triggering the PRE event
     * </p>
     *
     * @param action    Represents the {@link Action}
     * @param models    Which contains the collection of models
     * @param eventData Represents the  {@link EventData}
     */
    protected void preAction(final Action action, final Collection<? extends TlcModel> models, final EventData eventData) {
        eventData.setAction(action);
        eventData.setValues(models);
        triggerEvents(eventData, Event.Type.PRE);
    }

    /**
     * <p>
     * Executes post action for the given action and models, {@link EventData} and triggering the POST event
     * </p>
     *
     * @param action    Represents the {@link Action}
     * @param models    Which contains the collection of models
     * @param eventData Represents the  {@link EventData}
     */
    protected void postAction(final Action action, final Collection<? extends TlcModel> models, final EventData eventData) {
        eventData.setAction(action);
        eventData.setValues(models);
        triggerEvents(eventData, Event.Type.POST);
    }

    /**
     * <p>
     * Executes post action for the given action and models, {@link EventData} and triggering the POST event
     * </p>
     *
     * @param orgId  Represents an organization's id.
     * @param userId Represents a user's id.
     * @param action Represents the {@link Action}
     * @param models Which contains the collection of models
     */
    protected void postAction(final Long orgId, final Long userId, final Action action,
                              final Collection<? extends TlcModel> models) {
        final EventData eventData = new EventData();

        eventData.setOrgId(orgId);
        eventData.setUserId(userId);
        eventData.setAction(action);
        eventData.setValues(models);
        triggerEvents(eventData, Event.Type.POST);
    }
}