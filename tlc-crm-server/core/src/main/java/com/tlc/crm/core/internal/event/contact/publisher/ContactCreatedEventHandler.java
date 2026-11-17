package com.tlc.crm.core.internal.event.contact.publisher;

import com.tlc.commons.util.JavaUtil;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.event.model.SecureEventData;
import com.tlc.crm.common.handler.EventDataHandler;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.notification.app.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * <p>
 * Handles the post events for the created contacts.
 * </p>
 *
 * @author Naveen Kumar K
 * @version 1.0
 */
public final class ContactCreatedEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContactCreatedEventHandler.class);

    private final ContactManager contactManager;
    private final EventDataHandler eventDataHandler;

    private ContactCreatedEventHandler() {
        contactManager = ContactManager.getInstance();
        eventDataHandler = EventDataHandler.getInstance();
    }

    private static class Instance {
        private static final ContactCreatedEventHandler INSTANCE = new ContactCreatedEventHandler();
    }

    /**
     * <p>
     * Gets the {@link ContactCreatedEventHandler}.
     * </p>
     *
     * @return {@link ContactCreatedEventHandler}
     */
    public static ContactCreatedEventHandler getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Triggers the post events, such as indexing and auto-assignment, for the given created contacts.
     * Invoke only after the contacts are committed.
     * </p>
     *
     * @param orgId      Represents an organization id
     * @param userId     Represents the user id
     * @param contactIds Which contains the collection of created contact IDs
     */
    public void triggerEvents(final Long orgId, final Long userId, final Collection<Long> contactIds) {
        if (contactIds.isEmpty()) {
            return;
        }
        final Collection<Contact> contacts = contactManager.get(orgId, contactIds);

        for (final Collection<Contact> models : JavaUtil.splitCollection(contacts, NumericalConstant.TWENTY)) {
            final EventData eventData = new SecureEventData(orgId, userId);

            eventData.setAction(Action.CREATE);
            eventData.setValues(models);

            try {
                eventDataHandler.handleEvents(eventData, Event.Type.POST);
                LOGGER.debug("Created contact: Post event execution initiated for the created contacts for Org ID: {}", orgId);
            } catch (Exception exception) {
                LoggerUtil.logError(LOGGER, exception, "Created contact: Post event execution failed for the created contacts for Org ID: {}", orgId);
            }
        }
    }
}