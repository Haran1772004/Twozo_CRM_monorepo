package com.tlc.crm.workflow.initializer;

import com.tlc.commons.service.initializer.ServiceInitializer;
import com.tlc.commons.util.Configurations;

import com.tlc.crm.common.event.listener.registry.EventListenerRegistry;

import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.crm.workflow.v2.internal.async.consumer.WorkflowV2EmailActionController;
import com.tlc.crm.workflow.cache.internal.WorkflowCacheLoader;
import com.tlc.crm.workflow.internal.async.consumer.ActionConsumerController;
import com.tlc.crm.workflow.internal.async.consumer.TriggerConsumerController;
import com.tlc.crm.workflow.internal.async.scheduler.WorkflowEntityUpdateTriggerCleaner;
import com.tlc.crm.workflow.internal.async.scheduler.WorkflowExecutionHistoryCleaner;
import com.tlc.crm.workflow.v2.internal.async.consumer.TriggerConsumerControllerV2;
import com.tlc.crm.workflow.v2.internal.async.scheduler.WorkflowV2DelayScheduleSweeper;
import com.tlc.crm.workflow.v2.internal.async.scheduler.WorkflowV2ExecutionHistoryCleaner;
import com.tlc.crm.workflow.v2.internal.async.scheduler.WorkflowV2WaitExpirySweeper;
import com.tlc.crm.workflow.internal.listener.WorkflowUpdateRowListener;
import com.tlc.crm.workflow.internal.trigger.event.WorkflowTriggerEventListener;
import com.tlc.crm.workflow.internal.trigger.service.TriggerRequestService;
import com.tlc.crm.workflow.internal.trigger.service.impl.TriggerRequestServiceImpl;
import com.tlc.crm.workflow.sql.resource.OMWFLWORKFLOW;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.ds.WritableDataStore;

/**
 * <p>
 * An initializer that initializes the service which needs to be started.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class WorkflowServiceInitializer implements ServiceInitializer {

    private final TriggerConsumerController triggerConsumerController;
    private final ActionConsumerController actionConsumerController;
    private final TriggerRequestService triggerRequestService;
    private final WorkflowTriggerEventListener workflowTriggerEventListener;
    private final EventListenerRegistry eventListenerRegistry;
    private final WorkflowExecutionHistoryCleaner workflowExecutionHistoryCleaner;
    private final WorkflowCacheLoader workflowCacheLoader;
    private final WorkflowEntityUpdateTriggerCleaner workflowEntityUpdateTriggerCleaner;
    private final WritableDataStore writableDataStore;
    private final WorkflowUpdateRowListener workflowUpdateRowListener;
    private final TriggerConsumerControllerV2 triggerConsumerControllerV2;
    private final WorkflowV2DelayScheduleSweeper workflowV2DelayScheduleSweeper;
    private final WorkflowV2WaitExpirySweeper workflowV2WaitExpirySweeper;
    private final WorkflowV2ExecutionHistoryCleaner workflowV2ExecutionHistoryCleaner;
    private final WorkflowV2EmailActionController workflowV2EmailActionController;

    private WorkflowServiceInitializer() {
        triggerRequestService = new TriggerRequestServiceImpl();
        triggerConsumerController = new TriggerConsumerController();
        actionConsumerController = new ActionConsumerController();
        workflowTriggerEventListener = WorkflowTriggerEventListener.getInstance();
        eventListenerRegistry = EventListenerRegistry.getInstance();
        workflowExecutionHistoryCleaner = WorkflowExecutionHistoryCleaner.getInstance();
        workflowCacheLoader = WorkflowCacheLoader.getInstance();
        workflowEntityUpdateTriggerCleaner = WorkflowEntityUpdateTriggerCleaner.getInstance();
        this.writableDataStore = DataStoreProvider.getWritableDataStore();
        this.workflowUpdateRowListener = WorkflowUpdateRowListener.getInstance();
        this.triggerConsumerControllerV2 = new TriggerConsumerControllerV2();
        this.workflowV2DelayScheduleSweeper = WorkflowV2DelayScheduleSweeper.getInstance();
        this.workflowV2WaitExpirySweeper = WorkflowV2WaitExpirySweeper.getInstance();
        this.workflowV2ExecutionHistoryCleaner = WorkflowV2ExecutionHistoryCleaner.getInstance();
        this.workflowV2EmailActionController = new WorkflowV2EmailActionController();
    }

    private static final class Instance {
        private static final WorkflowServiceInitializer WORK_FLOW_SERVICE_INITIALIZER = new WorkflowServiceInitializer();
    }

    public static WorkflowServiceInitializer getInstance() {
        return Instance.WORK_FLOW_SERVICE_INITIALIZER;
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Holds the configuration properties.
     */
    @Override
    public void preInitialize(final Configurations configurations) {
        triggerRequestService.start(configurations);
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Holds the configuration properties.
     */
    @Override
    public void postInitialize(final Configurations configurations) {
//        triggerConsumerController.start(configurations); // TODO remove once migrated
//        actionConsumerController.start(null);
        triggerConsumerControllerV2.start(configurations);

        eventListenerRegistry.registerPostEventListener(workflowTriggerEventListener);
        workflowExecutionHistoryCleaner.start(null);
        workflowV2ExecutionHistoryCleaner.start(null);
        workflowV2DelayScheduleSweeper.start(configurations);
        workflowV2WaitExpirySweeper.start(configurations);
        workflowEntityUpdateTriggerCleaner.start(null);
        workflowV2EmailActionController.start(configurations);
        workflowCacheLoader.load();
        writableDataStore.registerUpdatedEntryListener(Table.get(OMWFLWORKFLOW.TABLE), workflowUpdateRowListener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        triggerConsumerController.stop();
        actionConsumerController.stop();
        triggerConsumerControllerV2.stop();
        triggerRequestService.stop();
        workflowExecutionHistoryCleaner.stop();
        workflowV2ExecutionHistoryCleaner.stop();
        workflowV2DelayScheduleSweeper.stop();
        workflowV2WaitExpirySweeper.stop();
        workflowEntityUpdateTriggerCleaner.stop();
        workflowCacheLoader.clear();
        workflowV2EmailActionController.stop();
    }
}
