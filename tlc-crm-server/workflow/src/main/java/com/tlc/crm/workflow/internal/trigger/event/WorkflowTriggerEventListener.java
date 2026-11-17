package com.tlc.crm.workflow.internal.trigger.event;

import com.tlc.commons.util.JavaUtil;
import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.common.event.listener.PostEventListener;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.core.workflow.handler.TriggerModelHandler;
import com.tlc.crm.core.workflow.internal.annotation.Trigger;
import com.tlc.crm.core.workflow.model.ModelSource;
import com.tlc.crm.core.workflow.model.TriggerInfo;
import com.tlc.crm.core.workflow.publish.TriggerDataPublisher;
import com.tlc.crm.notification.app.Event;
import com.tlc.crm.workflow.api.manager.WorkflowManager;
import com.tlc.crm.workflow.api.model.TriggerSource;
import com.tlc.crm.workflow.api.model.TriggerSource.TriggerEvent;
import com.tlc.crm.workflow.internal.status.WorkflowErrorCodes;
import com.tlc.crm.workflow.v2.api.manager.WorkflowV2Manager;
import com.tlc.validator.TlcModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;

/**
 * Indicates the {@link WorkflowTriggerEventListener} that listens for the event
 * during trigger process that are to be processed.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class WorkflowTriggerEventListener implements PostEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkflowTriggerEventListener.class);

    private final TriggerModelHandler triggerModelHandler;
    private final TriggerDataPublisher triggerDataPublisher;
    private final WorkflowManager workflowManager;
    private final WorkflowV2Manager workflowV2Manager;

    private WorkflowTriggerEventListener() {
        this.triggerModelHandler = TriggerModelHandler.getInstance();
        this.triggerDataPublisher = TriggerDataPublisher.getInstance();
        this.workflowManager = WorkflowManager.getInstance();
        this.workflowV2Manager = WorkflowV2Manager.getInstance();
    }

    private static class InstanceHolder {
        private static final WorkflowTriggerEventListener INSTANCE = new WorkflowTriggerEventListener();
    }

    public static WorkflowTriggerEventListener getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @return The event.
     */
    @Override
    public Event event() {
        return WorkflowEvents.TRIGGER;
    }

    /**
     * {@inheritDoc}
     *
     * @param eventData Represents {@link EventData}.
     */
    @Override
    public void processPostEvent(final EventData eventData) {
        final Long orgId = eventData.getOrgId();
        final Collection<? extends TlcModel> models = eventData.getValues();

        models.stream().findFirst().ifPresent(tlcModel -> {
            final Trigger trigger = tlcModel.getClass().getAnnotation(Trigger.class);

            if (Objects.nonNull(trigger)) {
                final Action action = eventData.getAction();
                final TriggerEvent triggerEvent = switch (action) {
                    case CREATE -> TriggerEvent.CREATED;
                    case UPDATE, PARTIAL_UPDATE -> TriggerEvent.EDITED;
                    case DELETE -> TriggerEvent.DELETED;
                    default -> throw ErrorCode.get( WorkflowErrorCodes.INVALID_ACTION_TYPE, "i18n.workflow.trigger.invalid.action" );
                };

                final TriggerSource triggerSource = TriggerSource.getByName(trigger.source().getName());
                final boolean hasV1 = workflowManager.exists(orgId, triggerSource, triggerEvent);
                final boolean hasV2 = workflowV2Manager.exists(orgId, triggerSource, triggerEvent);

                if (hasV1 || hasV2) {
                    final Collection<? extends Collection<? extends TlcModel>> groupedModels = JavaUtil.splitCollection(models, 3);
                    final Map<Long, TlcModel> previousModels = eventData.getPreviousValues();
                    final boolean hasPreviousModel = Objects.nonNull(previousModels) && !previousModels.isEmpty();
                    final Collection<Long> entityTriggerIds = new HashSet<>();

                    for (final Collection<? extends TlcModel> tlcModels : groupedModels) {
                        entityTriggerIds.clear();

                        if ((action == Action.UPDATE || action == Action.PARTIAL_UPDATE) && hasPreviousModel) {

                            for (final TlcModel model : tlcModels) {
                                final Long id = model.id();

                                if (previousModels.containsKey(id)) {
                                    entityTriggerIds.add(triggerModelHandler.create(orgId, trigger.source(), previousModels.get(id)));
                                }
                            }
                        } else if (action == Action.DELETE) {

                            for (final TlcModel model : tlcModels) {
                                entityTriggerIds.add(triggerModelHandler.create(orgId, trigger.source(), previousModels.get(model.id())));
                            }
                        }

                        final TriggerInfo triggerInfo = new TriggerInfo(trigger.source(), eventData.getAction(),
                                ModelSource.WEB_REQUEST, entityTriggerIds, true);

                        triggerDataPublisher.publish(orgId, eventData.getUserId(), tlcModels, triggerInfo);
                    }
                }
            }
        });
    }

}