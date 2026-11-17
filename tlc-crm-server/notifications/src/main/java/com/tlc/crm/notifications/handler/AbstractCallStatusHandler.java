package com.tlc.crm.notifications.handler;

import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.handler.EventDataHandler;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.manager.LifeCycleStageManager;
import com.tlc.crm.core.api.contact.manager.LifeCycleStatusManager;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.contact.model.LifeCycleStage;
import com.tlc.crm.core.api.contact.model.converter.ContactConverter;
import com.tlc.crm.notification.app.Event;
import com.tlc.crm.notifications.api.call.model.PhoneSettings;
import com.tlc.l10n.model.number.PhoneNumberUtil;
import com.tlc.l10n.model.number.PhoneNumber;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Abstract Class for handling call Status
 *
 * @author Ryan Sam Daniel
 * @version 1.0
 */
public abstract class AbstractCallStatusHandler implements CallStatusHandler {
    private final ContactConverter contactConverter;
    protected final ContactManager contactManager;
    private final EventDataHandler eventDataHandler;
    private final LifeCycleStageManager lifeCycleStageManager;
    private final LifeCycleStatusManager lifeCycleStatusManager;

    protected AbstractCallStatusHandler() {
        this.contactConverter = ContactConverter.getInstance();
        this.contactManager = ContactManager.getInstance();
        this.eventDataHandler = EventDataHandler.getInstance();
        this.lifeCycleStageManager = LifeCycleStageManager.getInstance();
        this.lifeCycleStatusManager = LifeCycleStatusManager.getInstance();
    }

    /**
     * Processes web-based call status updates (non click-to-call).
     * Handles child call events (INITIATED, IN_PROGRESS, CANCELED, NO_ANSWER, BUSY, FAILED)
     * by looking up the parent call and updating or creating child call records accordingly.
     *
     * @param orgId         Refers the organization id.
     * @param crmUserId     Refers the crm user id.
     * @param phoneNumber   Refers the phone number to create contact.
     * @param phoneSettings the {@link PhoneSettings} for lifeCycleStage details
     */
    public Contact createContactWithPreferredStage(final Long orgId, final Long crmUserId, final String phoneNumber,
                                                   final PhoneSettings phoneSettings) {
        final PhoneNumber number = PhoneNumberUtil.parse(phoneNumber);
        final Collection<Contact> createdContact = contactManager.getContactsByPhone(orgId, number.getNumberWithCountryCode());

        if (createdContact.isEmpty()) {
            final Long lifeCycleStageId = phoneSettings.getLifeCycleStageId();
            final LifeCycleStage currentLifeCycleStage = lifeCycleStageManager.get(orgId, Objects.isNull(lifeCycleStageId)
                    ? lifeCycleStageManager.getDefaultStage(orgId).id() : lifeCycleStageId);
            final Contact newContact = contactManager.create(contactConverter.buildContactFromPhoneNumber(
                    orgId, crmUserId, phoneNumber, phoneSettings.getLifeCycleStageId(),
                    lifeCycleStatusManager.getDefaultStatusId(orgId, currentLifeCycleStage.id())));
            final EventData eventData = new EventData();

            eventData.setAction(Action.CREATE);
            eventData.setOrgId(orgId);
            eventData.setUserId(crmUserId);
            eventData.setValues(List.of(newContact));
            eventDataHandler.handleEvents(eventData, Event.Type.POST);
            return newContact;
        }
        return createdContact.iterator().next();
    }
}
