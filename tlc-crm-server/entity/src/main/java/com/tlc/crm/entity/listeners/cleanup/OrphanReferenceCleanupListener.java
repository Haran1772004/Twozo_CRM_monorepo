package com.tlc.crm.entity.listeners.cleanup;

import com.tlc.crm.common.event.listener.PostEventListener;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.Entity;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.entity.service.cleanup.OrphanReferenceCleanupService;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.notification.app.Event;
import com.tlc.validator.TlcModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * Post-event listener that triggers orphan reference cleanup in Custom Wide Tables
 * after entity deletion.
 * </p>
 * <p>
 * This listener processes DELETE actions and delegates to the cleanup service
 * to remove orphan references from custom field columns.
 * </p>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public final class OrphanReferenceCleanupListener implements PostEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrphanReferenceCleanupListener.class);

    private final OrphanReferenceCleanupService cleanupService;

    private OrphanReferenceCleanupListener() {
        this.cleanupService = OrphanReferenceCleanupService.getInstance();
    }

    private static class InstanceHolder {
        private static final OrphanReferenceCleanupListener INSTANCE = new OrphanReferenceCleanupListener();
    }

    /**
     * Gets the singleton instance of the listener.
     *
     * @return the singleton instance
     */
    public static OrphanReferenceCleanupListener getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns a custom event for orphan reference cleanup.
     * This listener will be triggered alongside other POST event listeners.
     * </p>
     *
     * @return the event this listener handles
     */
    @Override
    public Event event() {
        return CustomFieldCleanupEvent.ORPHAN_REFERENCE_CLEANUP;
    }

    /**
     * {@inheritDoc}
     *
     * @param eventData the event data containing deleted entity information
     */
    @Override
    public void processPostEvent(final EventData eventData) {
        if (!isDeleteAction(eventData)) {
            return;
        }
        final long orgId = eventData.getOrgId();
        final Collection<TlcModel> deletedModels = eventData.getValues();

        if (Objects.isNull(deletedModels) || deletedModels.isEmpty()) {
            return;
        }
        LOGGER.info("Processing orphan reference cleanup for {} deleted model(s): orgId={}", deletedModels.size(), orgId);

        try {
            processDeletedModels(orgId, deletedModels);
        } catch (final Exception exception) {
            LOGGER.error("Error during orphan reference cleanup: orgId={}, error={}", orgId, exception.getMessage(), exception);
        }
    }

    /**
     * Checks if the event represents a DELETE action.
     */
    private boolean isDeleteAction(final EventData eventData) {
        return Objects.nonNull(eventData) && Action.DELETE == eventData.getAction();
    }

    /**
     * Processes the deleted models and triggers appropriate cleanup.
     */
    private void processDeletedModels(final long orgId, final Collection<TlcModel> deletedModels) {
        for (final TlcModel model : deletedModels) {

            if (Objects.isNull(model) || Objects.isNull(model.id())) {
                continue;
            }
            processModel(orgId, model);
        }
    }

    /**
     * Processes a single deleted model and determines cleanup action based on its type.
     */
    private void processModel(final long orgId, final TlcModel model) {
        if (model instanceof Entity entity) {
            final String entityType = entity.getEntityType();

            switch (entityType) {
                case "Contact" -> cleanupService.cleanupEntityReferences(orgId, model.id(), StoreType.CONTACT, Source.CONTACT);
                case "Company" -> cleanupService.cleanupEntityReferences(orgId, model.id(), StoreType.COMPANY, Source.COMPANY);
            }
        } else if (model instanceof Option option) {
            processOptionDeletion(orgId, option);
        }
    }

    /**
     * Processes option deletion and triggers the appropriate cleanup.
     */
    private void processOptionDeletion(final long orgId, final Option option) {
            final Long fieldId = option.getFieldId();

            if (Objects.nonNull(fieldId)) {
                LOGGER.debug("Processing Option deletion cleanup: orgId={}, optionId={}, fieldId={}", orgId, option.id(), fieldId);
                cleanupService.cleanupOptionReferences(orgId, option.id(), fieldId);
            } else {
                LOGGER.warn("Option fieldId is null, cannot cleanup: optionId={}", option.id());
            }

    }

    /**
     * <p>
     * Custom event for orphan reference cleanup.
     * </p>
     */
    public static final class CustomFieldCleanupEvent implements Event {

        /**
         * Singleton instance for the orphan reference cleanup event.
         */
        public static final CustomFieldCleanupEvent ORPHAN_REFERENCE_CLEANUP = new CustomFieldCleanupEvent();

        private static final String EVENT_NAME = "ORPHAN_REFERENCE_CLEANUP";

        private CustomFieldCleanupEvent() {
        }

        @Override
        public String name() {
            return EVENT_NAME;
        }
    }
}