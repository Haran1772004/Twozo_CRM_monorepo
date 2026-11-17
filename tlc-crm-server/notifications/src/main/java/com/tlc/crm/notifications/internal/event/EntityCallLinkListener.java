package com.tlc.crm.notifications.internal.event;

import com.tlc.crm.common.event.listener.PostEventListener;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.entity.models.type.Phone;
import com.tlc.crm.notifications.api.call.manager.CallHistoryManager;
import com.tlc.crm.notification.app.Event;
import com.tlc.crm.notification.app.event.NotificationEvent;
import com.tlc.validator.TlcModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>
 * Listens for contact creation events and links existing call histories to the contact.
 * </p>
 *
 * @author Ramesh
 * @version 1.0
 */
public final class EntityCallLinkListener implements PostEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityCallLinkListener.class);

    private final CallHistoryManager callHistoryManager;

    private EntityCallLinkListener() {
        callHistoryManager = CallHistoryManager.getInstance();
    }

    private static class Instance {
        private static final EntityCallLinkListener INSTANCE = new EntityCallLinkListener();
    }

    /**
     * <p>
     * Gets the {@link EntityCallLinkListener}.
     * </p>
     *
     * @return {@link EntityCallLinkListener}
     */
    public static EntityCallLinkListener getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param eventData Represents the {@link EventData}
     */
    @Override
    public void processPostEvent(final EventData eventData) {
        if (Action.CREATE == eventData.getAction()) {
            final Collection<? extends TlcModel> models = eventData.getValues();

            for (final TlcModel model : models) {
                if (model instanceof Contact contact) {
                    try {
                        linkExistingCallHistoriesToContact(contact);
                    } catch (final Exception exception) {
                        LOGGER.error("Failed to link existing call histories to contact, Org ID : {}, Contact ID : {}",
                                contact.orgId(), contact.id(), exception);
                    }
                } else if (model instanceof Deal deal) {
                    try {
                        linkExistingCallHistoriesToDeal(deal);
                    } catch (final Exception exception) {
                        LOGGER.error("Failed to link existing call histories to deal, Org ID : {}, Deal ID : {}",
                                deal.orgId(), deal.id(), exception);
                    }
                }
            }
        } else if (Action.UPDATE == eventData.getAction()) {
            final Collection<? extends TlcModel> models = eventData.getValues();

            for (final TlcModel model : models) {
                if (model instanceof Contact contact) {
                    try {
                        if (Objects.nonNull(contact.getCompany())) {
                            linkExistingCallHistoriesToCompany(contact);
                        }
                    } catch (final Exception exception) {
                        LOGGER.error("Failed to link existing call histories to contact and company if present, Org ID : {}, Contact ID : {}",
                                contact.orgId(), contact.id(), exception);
                    }
                }
            }
        }
    }

    /**
     * <p>
     * Finds the call detail IDs associated with the given deal's primary contact
     * and links those call histories to the given contact.
     * </p>
     *
     * @param deal Represents the newly created {@link Deal}
     */
    private void linkExistingCallHistoriesToDeal(final Deal deal) {
        final Long orgId = deal.orgId();
        final Contact contact = deal.getPrimaryContact();

        if (Objects.isNull(contact)) {
            return;
        }

        callHistoryManager.linkCallDetailsToDeal(orgId, contact, deal);
    }

    /**
     * <p>
     * Finds the call detail IDs associated with the given contact's phone numbers
     * that are not currently mapped to any contact, and links those call histories
     * to the given contact.
     * </p>
     *
     * @param contact Represents the newly created {@link Contact}
     */
    private void linkExistingCallHistoriesToContact(final Contact contact) {
        final Collection<Phone> phones = contact.getPhones();

        if (Objects.isNull(phones) || phones.isEmpty()) {
            return;
        }

        final Long orgId = contact.orgId();
        final Collection<Long> phoneIds = phones.stream().map(Phone::id).filter(Objects::nonNull).collect(Collectors.toSet());

        if (phoneIds.isEmpty()) {
            return;
        }

        final Collection<Long> callDetailsIds = callHistoryManager.getUnmappedCallDetailsIds(orgId, phoneIds);

        if (!callDetailsIds.isEmpty()) {
            callHistoryManager.linkCallDetailsToContact(orgId, contact, callDetailsIds);
        }
    }

    /**
     * <p>
     * Finds the call detail IDs associated with the given contacts's primary company
     * and links those call histories to the given contact.
     * </p>
     *
     * @param contact Represents the newly created {@link Contact}
     */
    private void linkExistingCallHistoriesToCompany(final Contact contact) {
        final Long orgId = contact.orgId();
        final Collection<Long> contactCallHistoryIds = callHistoryManager.getContactCallHistoryIds(orgId, contact.id());

        if (!contactCallHistoryIds.isEmpty()) {
            callHistoryManager.linkCallDetailsToContact(orgId, contact, contactCallHistoryIds);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Event event() {
        return NotificationEvent.CONTACT_LINK_EVENT;
    }
}