package com.tlc.crm.core.internal.event.contact.lifecyclestage;

import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.event.listener.CompositeEventListener;
import com.tlc.crm.core.api.contact.manager.LifeCycleStageManager;
import com.tlc.crm.core.api.contact.manager.LifeCycleStatusManager;
import com.tlc.crm.core.api.contact.model.LifeCycleStage;
import com.tlc.crm.core.api.contact.model.LifeCycleStatus;
import com.tlc.crm.core.internal.contact.handler.LifeCycleMigrationHandler;
import com.tlc.crm.core.internal.event.CoreEvent;
import com.tlc.crm.notification.app.Event;
import com.tlc.validator.TlcModel;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * Listens the delete event of the lifecycle stage/status of the contact.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public final class LifeCycleDeleteEventListener implements CompositeEventListener {

    private final LifeCycleMigrationHandler lifeCycleMigrationHandler;
    private final LifeCycleStageManager stageManager;
    private final LifeCycleStatusManager statusManager;

    private LifeCycleDeleteEventListener() {
        lifeCycleMigrationHandler = LifeCycleMigrationHandler.getInstance();
        stageManager = LifeCycleStageManager.getInstance();
        statusManager = LifeCycleStatusManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Event}.
     */
    @Override
    public Event event() {
        return CoreEvent.LIFECYCLE_DELETE_EVENT;
    }

    private static class Instance {
        private static final LifeCycleDeleteEventListener INSTANCE = new LifeCycleDeleteEventListener();
    }

    /**
     * <p>
     * Provides the {@link LifeCycleDeleteEventListener}.
     * </p>
     *
     * @return {@link LifeCycleDeleteEventListener}
     */
    public static LifeCycleDeleteEventListener getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param eventData Represents {@link EventData}.
     */
    @Override
    public void processPreEvent(final EventData eventData) {
        final Long orgId = eventData.getOrgId();
        final Collection<? extends TlcModel> models = eventData.getValues();
        final boolean isLifecycleStages = models.stream().anyMatch(LifeCycleStage.class::isInstance);

        switch (eventData.getAction()) {
            case UPDATE -> {
                if (isLifecycleStages) {
                    final Set<Long> stageIds = new HashSet<>();
                    final Set<Long> statusIds = new HashSet<>();

                    for (final TlcModel model : models) {
                        stageIds.add(model.id());
                        statusIds.addAll(((LifeCycleStage) model).getStatuses().stream().map(LifeCycleStatus::id).filter(Objects::nonNull).toList());
                    }
                    final Collection<Long> statusesToBeDeleted = statusManager.getRemainingStatuses(orgId, stageIds, statusIds);

                    if (!statusesToBeDeleted.isEmpty()) {
                        lifeCycleMigrationHandler.handleStatusRemoval(orgId, statusesToBeDeleted);
                    }
                }
            }
            case DELETE -> {
                if (isLifecycleStages) {
                    final Set<Long> stageIds = models.stream().map(TlcModel::id).collect(Collectors.toSet());

                    lifeCycleMigrationHandler.handleStageRemoval(orgId, stageIds);
                } else if (models.stream().anyMatch(LifeCycleStatus.class::isInstance)) {
                    final Set<Long> statusIds = models.stream().map(TlcModel::id).collect(Collectors.toSet());

                    lifeCycleMigrationHandler.handleStatusRemoval(orgId, statusIds);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param eventData Represents {@link EventData}.
     */
    @Override
    public void processPostEvent(final EventData eventData) {
        final long orgId = eventData.getOrgId();
        final Collection<? extends TlcModel> models = eventData.getValues();

        if (models.stream().anyMatch(LifeCycleStage.class::isInstance)) {
            final short removedStageOrder = ((Collection<LifeCycleStage>) models).stream().map(LifeCycleStage::getOrder)
                    .min(Comparator.comparing(Function.identity())).orElseGet(() -> (short) 1);

            stageManager.reArrangeStageOrders(orgId, removedStageOrder);
        } else if (models.stream().anyMatch(LifeCycleStatus.class::isInstance)) {
            final Optional<LifeCycleStatus> removedStatus = ((Collection<LifeCycleStatus>) models).stream()
                    .min(Comparator.comparingInt(LifeCycleStatus::getOrder));

            removedStatus.ifPresent(status -> statusManager.reArrangeStatusOrder(orgId, status.getStageId(), status.getOrder()));
        }
    }
}