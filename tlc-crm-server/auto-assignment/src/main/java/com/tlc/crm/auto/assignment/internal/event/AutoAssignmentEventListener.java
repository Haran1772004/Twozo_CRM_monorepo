package com.tlc.crm.auto.assignment.internal.event;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.JavaUtil;

import com.tlc.crm.auto.assignment.api.manager.AutoAssignmentManager;
import com.tlc.crm.auto.assignment.api.model.AssignmentModule;
import com.tlc.crm.auto.assignment.fields.AutoAssignmentAttributes;

import com.tlc.crm.common.event.listener.PostEventListener;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.models.Action;

import com.tlc.crm.core.auto.assignment.AutoAssignment;
import com.tlc.crm.core.auto.assignment.event.AutoAssignmentEvent;
import com.tlc.crm.core.queue.EntityEventPublisher;
import com.tlc.crm.entity.fields.source.Source;

import com.tlc.crm.notification.app.Event;

import com.tlc.validator.TlcModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Indicates the {@link AutoAssignmentEventListener} that listens for the event
 * during trigger process that are to be processed.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class AutoAssignmentEventListener implements PostEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutoAssignmentEventListener.class);

    private final AutoAssignmentManager autoAssignmentManager;
    private final EntityEventPublisher entityEventPublisher;

    private AutoAssignmentEventListener() {
        this.autoAssignmentManager = AutoAssignmentManager.getInstance();
        this.entityEventPublisher = EntityEventPublisher.getInstance();
    }

    private static class InstanceHolder {
        private static final AutoAssignmentEventListener INSTANCE = new AutoAssignmentEventListener();
    }

    public static AutoAssignmentEventListener getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Event}.
     */
    @Override
    public Event event() {
        return AutoAssignmentEvent.ASSIGN_OWNER;
    }

    /**
     * {@inheritDoc}
     *
     * @param eventData Represents {@link EventData}.
     */
    @Override
    public void processPostEvent(final EventData eventData) {
        if (Objects.equals(Action.CREATE, eventData.getAction())) {
            final Collection<TlcModel> models = eventData.getValues();

            models.stream().findFirst().ifPresent(tlcModel -> {
                final AutoAssignment autoAssignment = tlcModel.getClass().getAnnotation(AutoAssignment.class);

                if (Objects.nonNull(autoAssignment)) {
                    final Source source = autoAssignment.source();
                    final long orgId = eventData.getOrgId();

                    if (autoAssignmentManager.exists(orgId, AssignmentModule.getBySource(source))) {
                        final long userId = eventData.getUserId();
                        final Collection<Collection<TlcModel>> splitCollection = JavaUtil.splitCollection(models, 3);

                        for (final Collection<TlcModel> modelList : splitCollection) {
                            final JsonObject jsonObject = Json.object();
                            final Collection<Long> modelIds = modelList.stream().map(TlcModel::id).filter(Objects::nonNull).collect(Collectors.toUnmodifiableSet());

                            jsonObject.put(AutoAssignmentAttributes.ORG_ID, orgId);
                            jsonObject.put(AutoAssignmentAttributes.USER_ID, userId);
                            jsonObject.put(AutoAssignmentAttributes.MODEL_IDS, modelIds);
                            jsonObject.put(AutoAssignmentAttributes.SOURCE, source);

                            final String requestId = Base64.getEncoder().encodeToString(String.format("%s:%s:%s", orgId, userId, modelIds).getBytes());

                            entityEventPublisher.publishEntityCreatedEvent(orgId, requestId, jsonObject, source);
                        }
                    }
                }
            });
        }
    }

}
